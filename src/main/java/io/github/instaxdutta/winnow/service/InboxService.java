package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.InboxMessage;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.model.InboxMessageDto;
import io.github.instaxdutta.winnow.model.PagedResponse;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import io.github.instaxdutta.winnow.repository.InboxMessageRepository;
import io.github.instaxdutta.winnow.repository.SmtpAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captured inbound messages.
 *
 * <p>The tenant of a message is the owner of the account it was captured from, so every
 * read resolves the account first and only then reads the messages.
 */
@Service
public class InboxService {

    private static final Logger LOG = LoggerFactory.getLogger(InboxService.class);

    private final InboxMessageRepository inbox;
    private final SmtpAccountRepository smtpAccounts;
    private final GmailAccountRepository gmailAccounts;

    public InboxService(
            InboxMessageRepository inbox, SmtpAccountRepository smtpAccounts, GmailAccountRepository gmailAccounts) {
        this.inbox = inbox;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
    }

    /**
     * Stores a batch of captured messages.
     *
     * <p>This is the only write path for the inbox, so the scheduler never touches the
     * repository directly. The insert is idempotent: the unique key on the account id and
     * the provider message id means a repeated poll does not duplicate a message, and a
     * duplicate in the same batch is dropped rather than failing the whole batch.
     *
     * @param ownerUserId the tenant that owns the account the messages came from
     * @param accountId   the account the messages were read from
     * @param messages    the messages to store
     * @return how many messages were newly stored
     */
    @Transactional
    public int store(Long ownerUserId, Long accountId, List<InboxMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        requireOwnedAccountFor(ownerUserId, accountId);
        List<InboxMessage> fresh = new ArrayList<>(messages.size());
        for (InboxMessage message : messages) {
            if (message.getProviderMessageId() == null) {
                continue;
            }
            if (inbox.existsByAccountIdAndProviderMessageId(accountId, message.getProviderMessageId())) {
                continue;
            }
            message.setAccountId(accountId);
            message.setCreatedAt(Instant.now());
            fresh.add(message);
        }
        if (fresh.isEmpty()) {
            return 0;
        }
        try {
            inbox.saveAll(fresh);
        } catch (DataIntegrityViolationException e) {
            // Another poll stored the same message first. The unique key did its job.
            LOG.debug("A captured message was already stored");
        }
        return fresh.size();
    }

    /**
     * Confirms that the account belongs to the tenant.
     *
     * @throws NotFoundException when the account does not exist or belongs to another tenant
     */
    private void requireOwnedAccountFor(Long ownerUserId, Long accountId) {
        boolean owned = smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId).isPresent()
                || gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId).isPresent();
        if (!owned) {
            throw NotFoundException.of("Account", accountId);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<InboxMessageDto> list(Long ownerUserId, Long accountId, Instant from, Instant to) {
        requireOwnedAccountFor(ownerUserId, accountId);
        List<InboxMessage> all = inbox.findByAccountIdAndReceivedAtBetweenOrderByReceivedAtDesc(accountId, from, to);
        return PagedResponse.of(
                all.stream().map(InboxService::toDto).toList(), all.size(), all.size(), 1, 0, all.size());
    }

    /**
     * The messages of the tenant that have not been read.
     */
    @Transactional(readOnly = true)
    public long unreadCount(Long ownerUserId) {
        long unread = 0;
        for (Long accountId : accountIds(ownerUserId)) {
            for (InboxMessage message : allMessages(accountId)) {
                if (!message.isRead() && message.getDeletedAt() == null) {
                    unread++;
                }
            }
        }
        return unread;
    }

    /**
     * The messages of the tenant.
     */
    @Transactional(readOnly = true)
    public long count(Long ownerUserId) {
        long count = 0;
        for (Long accountId : accountIds(ownerUserId)) {
            for (InboxMessage message : allMessages(accountId)) {
                if (message.getDeletedAt() == null) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<InboxMessage> allMessages(Long accountId) {
        return inbox.findByAccountIdAndReceivedAtBetweenOrderByReceivedAtDesc(
                accountId, Instant.EPOCH, Instant.now());
    }

    private List<Long> accountIds(Long ownerUserId) {
        List<Long> ids = new ArrayList<>();
        smtpAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId)
                .forEach(account -> ids.add(account.getId()));
        gmailAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId)
                .forEach(account -> ids.add(account.getId()));
        return ids;
    }

    private static InboxMessageDto toDto(InboxMessage message) {
        return new InboxMessageDto(
                message.getId(),
                message.getFromAddress(),
                message.getSubject(),
                message.getBodyHtml(),
                message.getReceivedAt(),
                message.isRead());
    }
}
