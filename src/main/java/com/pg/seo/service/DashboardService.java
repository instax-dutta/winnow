package com.pg.seo.service;

import com.pg.seo.domain.DeliveryStatus;
import com.pg.seo.domain.TemplateStatus;
import com.pg.seo.mail.QuotaService;
import com.pg.seo.model.DashboardDto;
import com.pg.seo.repository.CampaignRepository;
import com.pg.seo.repository.ClientGroupRepository;
import com.pg.seo.repository.ClientRepository;
import com.pg.seo.repository.EmailTemplateRepository;
import com.pg.seo.repository.ExtractionRunRepository;
import com.pg.seo.repository.GmailAccountRepository;
import com.pg.seo.repository.SmtpAccountRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The counters on the dashboard.
 *
 * <p>Every counter is scoped to the tenant. The original counted every row of every table,
 * so one operator saw another tenant's numbers.
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
