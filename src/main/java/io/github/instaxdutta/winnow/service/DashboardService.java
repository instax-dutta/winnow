package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import io.github.instaxdutta.winnow.mail.QuotaService;
import io.github.instaxdutta.winnow.model.DashboardDto;
import io.github.instaxdutta.winnow.repository.CampaignRepository;
import io.github.instaxdutta.winnow.repository.ClientGroupRepository;
import io.github.instaxdutta.winnow.repository.ClientRepository;
import io.github.instaxdutta.winnow.repository.EmailTemplateRepository;
import io.github.instaxdutta.winnow.repository.ExtractionRunRepository;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import io.github.instaxdutta.winnow.repository.SmtpAccountRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The counters on the dashboard.
 *
 * <p>Every counter is scoped to the tenant. Counting every row of every table shows one
 * operator another tenant's numbers.
 */
@Service
public class DashboardService {

    private final ClientRepository clients;
    private final ClientGroupRepository clientGroups;
    private final EmailTemplateRepository templates;
    private final SmtpAccountRepository smtpAccounts;
    private final GmailAccountRepository gmailAccounts;
    private final CampaignRepository campaigns;
    private final ExtractionRunRepository extractionRuns;
    private final QuotaService quota;
    private final InboxService inbox;

    public DashboardService(
            ClientRepository clients,
            ClientGroupRepository clientGroups,
            EmailTemplateRepository templates,
            SmtpAccountRepository smtpAccounts,
            GmailAccountRepository gmailAccounts,
            CampaignRepository campaigns,
            ExtractionRunRepository extractionRuns,
            QuotaService quota,
            InboxService inbox) {
        this.clients = clients;
        this.clientGroups = clientGroups;
        this.templates = templates;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
        this.campaigns = campaigns;
        this.extractionRuns = extractionRuns;
        this.quota = quota;
        this.inbox = inbox;
    }

    @Transactional(readOnly = true)
    public DashboardDto summary(Long ownerUserId) {
        Instant now = Instant.now();
        return new DashboardDto(
                clients.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId),
                clientGroups.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId),
                templates.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId),
                templates.countByOwnerUserIdAndStatusAndDeletedAtIsNull(ownerUserId, TemplateStatus.PENDING_APPROVAL),
                smtpAccounts.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId),
                gmailAccounts.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId),
                campaigns.countByOwnerUserId(ownerUserId),
                quota.countByStatus(ownerUserId, null, DeliveryStatus.SENT, Instant.EPOCH, now),
                quota.countByStatus(ownerUserId, null, DeliveryStatus.FAILED, Instant.EPOCH, now),
                extractionRuns.findByOwnerUserIdOrderByStartedAtDesc(ownerUserId).size(),
                inbox.count(ownerUserId));
    }
}
