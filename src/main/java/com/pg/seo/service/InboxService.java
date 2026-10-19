package com.pg.seo.service;

import com.pg.seo.domain.InboxMessage;
import com.pg.seo.domain.NotFoundException;
import com.pg.seo.model.InboxMessageDto;
import com.pg.seo.model.PagedResponse;
import com.pg.seo.repository.GmailAccountRepository;
import com.pg.seo.repository.InboxMessageRepository;
import com.pg.seo.repository.SmtpAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    private final InboxMessageRepository inbox;
    private final SmtpAccountRepository smtpAccounts;
    private final GmailAccountRepository gmailAccounts;

    public InboxService(
            InboxMessageRepository inbox, SmtpAccountRepository smtpAccounts, GmailAccountRepository gmailAccounts) {
        this.inbox = inbox;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
    }

    @Transactional(readOnly = true)
    public PagedResponse<InboxMessageDto> list(Long ownerUserId, Long accountId, Instant from, Instant to) {
        requireOwnedAccount(ownerUserId, accountId);
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

    private void requireOwnedAccount(Long ownerUserId, Long accountId) {
        boolean owned = smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId).isPresent()
                || gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId).isPresent();
        if (!owned) {
            throw NotFoundException.of("Account", accountId);
        }
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
