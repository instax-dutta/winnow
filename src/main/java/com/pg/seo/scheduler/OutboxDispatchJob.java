package com.pg.seo.scheduler;

import com.pg.seo.domain.GmailAccount;
import com.pg.seo.domain.Outbox;
import com.pg.seo.domain.SmtpAccount;
import com.pg.seo.mail.GmailMailSender;
import com.pg.seo.mail.MailSender;
import com.pg.seo.mail.SendRequest;
import com.pg.seo.mail.SendResult;
import com.pg.seo.mail.SmtpMailSender;
import com.pg.seo.repository.GmailAccountRepository;
import com.pg.seo.repository.SmtpAccountRepository;
import com.pg.seo.service.OutboxService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends the outbox rows that are due.
 *
 * <p>The rows are claimed with a conditional update, so two instances of the application
 * never send the same row. The send itself happens outside any transaction: the claim is
 * committed before the message goes out, and the outcome is written afterwards.
 *
 * <p>A row that cannot be sent is marked as failed and the job carries on. One broken row
 * never stops the schedule.
 */
@Component
public class OutboxDispatchJob {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatchJob.class);

    private final OutboxService outbox;
    private final SmtpAccountRepository smtpAccounts;
    private final GmailAccountRepository gmailAccounts;
    private final SmtpMailSender smtpSender;
    private final GmailMailSender gmailSender;
    private final int batchSize;

    public OutboxDispatchJob(
            OutboxService outbox,
            SmtpAccountRepository smtpAccounts,
            GmailAccountRepository gmailAccounts,
            SmtpMailSender smtpSender,
            GmailMailSender gmailSender,
            @Value("${seo.scheduler.outbox-batch-size:50}") int batchSize) {
        this.outbox = outbox;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
        this.smtpSender = smtpSender;
        this.gmailSender = gmailSender;
        this.batchSize = batchSize;
    }

    /**
     * Sends every row that is due.
     */
    @Scheduled(fixedDelayString = "${seo.scheduler.outbox-delay-ms:30000}")
    public void dispatchDue() {
        List<Outbox> rows;
        try {
            rows = outbox.claimDue(Instant.now(), batchSize);
        } catch (RuntimeException e) {
            LOG.error("The outbox could not be claimed ({})", e.getClass().getSimpleName());
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        LOG.info("Dispatching {} outbox rows", rows.size());
        for (Outbox row : rows) {
            try {
                dispatch(row);
            } catch (RuntimeException e) {
                LOG.error("The dispatch of outbox row {} failed ({})", row.getId(), e.getClass().getSimpleName());
            }
        }
    }

    private void dispatch(Outbox row) {
        Long ownerUserId = row.getOwnerUserId();
        Long accountId = row.getFromAccountId();
        if (accountId == null) {
            outbox.markFailed(row.getId(), "the row does not name a sending account");
            return;
        }
        Optional<SmtpAccount> smtp = smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId);
        if (smtp.isPresent()) {
            send(row, smtp.get().getEmail(), accountId, smtpSender);
            return;
        }
        Optional<GmailAccount> gmail = gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId);
        if (gmail.isPresent()) {
            send(row, gmail.get().getEmail(), accountId, gmailSender);
            return;
        }
        outbox.markFailed(row.getId(), "the sending account " + accountId + " was not found");
    }

    /**
     * Sends one row.
     *
     * <p>The request names the tenant and the account, because a provider adapter cannot
     * resolve a credential without them.
     */
    private void send(Outbox row, String fromEmail, Long accountId, MailSender sender) {
        SendRequest request = new SendRequest(
                fromEmail,
                row.getToEmail(),
                row.getSubject(),
                row.getBodyHtml(),
                null,
                Map.of(),
                null,
                row.getOwnerUserId(),
                accountId);
        try {
            SendResult result = sender.send(request);
            if (result != null && result.success()) {
                outbox.markSent(row.getId());
                return;
            }
            outbox.markFailed(row.getId(), result == null ? "the provider returned no result" : result.errorMessage());
        } catch (RuntimeException e) {
            LOG.warn("The outbox row {} could not be sent ({})", row.getId(), e.getClass().getSimpleName());
            outbox.markFailed(row.getId(), "the provider refused the message (" + e.getClass().getSimpleName() + ")");
        }
    }
}
