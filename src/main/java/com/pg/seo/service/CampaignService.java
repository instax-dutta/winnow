package com.pg.seo.service;

import com.pg.seo.domain.AccountKind;
import com.pg.seo.domain.Campaign;
import com.pg.seo.domain.CampaignStatus;
import com.pg.seo.domain.ConflictException;
import com.pg.seo.domain.Delivery;
import com.pg.seo.domain.DeliveryStatus;
import com.pg.seo.domain.EmailTemplate;
import com.pg.seo.domain.GmailAccount;
import com.pg.seo.domain.NotFoundException;
import com.pg.seo.domain.SmtpAccount;
import com.pg.seo.domain.TemplateStatus;
import com.pg.seo.mail.QuotaService;
import com.pg.seo.model.CampaignDto;
import com.pg.seo.model.CampaignRequest;
import com.pg.seo.model.ClientDto;
import com.pg.seo.model.DeliveryDto;
import com.pg.seo.model.PagedResponse;
import com.pg.seo.repository.CampaignRepository;
import com.pg.seo.repository.DeliveryRepository;
import com.pg.seo.repository.EmailTemplateRepository;
import com.pg.seo.repository.GmailAccountRepository;
import com.pg.seo.repository.SmtpAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Send campaigns.
 *
 * <p>Creating a campaign resolves the recipients, refuses a template that was not approved,
 * checks the quota of the sending account and records one delivery row per recipient. The
 * deliveries are written in batches.
 */
@Service
public class CampaignService {

    /** The size of one insert batch. */
    private static final int DELIVERY_BATCH_SIZE = 200;

    /** The largest page a caller may ask for. */
    private static final int MAX_PAGE_SIZE = 500;

    private final CampaignRepository campaigns;
    private final DeliveryRepository deliveryRepository;
    private final EmailTemplateRepository templates;
    private final SmtpAccountRepository smtpAccounts;
    private final GmailAccountRepository gmailAccounts;
    private final ClientService clients;
    private final QuotaService quota;

    public CampaignService(
            CampaignRepository campaigns,
            DeliveryRepository deliveryRepository,
            EmailTemplateRepository templates,
            SmtpAccountRepository smtpAccounts,
            GmailAccountRepository gmailAccounts,
            ClientService clients,
            QuotaService quota) {
        this.campaigns = campaigns;
        this.deliveryRepository = deliveryRepository;
        this.templates = templates;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
        this.clients = clients;
        this.quota = quota;
    }

    @Transactional
    public CampaignDto create(Long ownerUserId, CampaignRequest request) {
        EmailTemplate template = templates
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(request.templateId(), ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Email template", request.templateId()));
        if (template.getStatus() != TemplateStatus.APPROVED) {
            throw new ConflictException(
                    "Only an approved template can be used in a campaign, not " + template.getStatus());
        }
        String fromEmail = requireAccount(ownerUserId, request.fromAccountKind(), request.fromAccountId());

        List<ClientDto> recipients = clients.resolveTargets(
                ownerUserId, request.targetType(), request.groupCode(), request.clientIds());
        quota.assertWithinQuota(ownerUserId, request.fromAccountId());

        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.setOwnerUserId(ownerUserId);
        campaign.setFromAccountId(request.fromAccountId());
        campaign.setFromAccountKind(request.fromAccountKind());
        campaign.setTargetType(request.targetType());
        campaign.setTemplate(template);
        campaign.setStatus(CampaignStatus.QUEUED);
        campaign.setRecipientCount(recipients.size());
        campaign.setCreatedAt(now);
        campaign.setCreatedBy(ownerUserId);
        campaign.setUpdatedAt(now);
        campaign.setUpdatedBy(ownerUserId);
        campaigns.save(campaign);

        List<Delivery> batch = new ArrayList<>(DELIVERY_BATCH_SIZE);
        for (ClientDto recipient : recipients) {
            Delivery delivery = new Delivery();
            delivery.setCampaign(campaign);
            delivery.setTemplate(template);
            delivery.setRecipientEmail(recipient.email());
            delivery.setStatus(DeliveryStatus.QUEUED);
            delivery.setCreatedAt(now);
            delivery.setCreatedBy(ownerUserId);
            batch.add(delivery);
            if (batch.size() >= DELIVERY_BATCH_SIZE) {
                deliveryRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            deliveryRepository.saveAll(batch);
        }
        return toDto(campaign, fromEmail);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CampaignDto> list(Long ownerUserId, int start, int length) {
        int pageSize = Math.min(Math.max(length, 1), MAX_PAGE_SIZE);
        int offset = Math.max(start, 0);
        List<Campaign> all = campaigns.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        List<Campaign> page = all.subList(
                Math.min(offset, all.size()), Math.min(offset + pageSize, all.size()));
        Map<Long, String> smtp = smtpEmails(smtpAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId));
        Map<Long, String> gmail = gmailEmails(gmailAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId));
        return PagedResponse.of(
                page.stream().map(campaign -> toDto(campaign, fromEmail(campaign, smtp, gmail))).toList(),
                all.size(),
                all.size(),
                1,
                offset,
                pageSize);
    }

    @Transactional(readOnly = true)
    public CampaignDto get(Long ownerUserId, Long id) {
        Campaign campaign = require(ownerUserId, id);
        Map<Long, String> smtp = smtpEmails(smtpAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId));
        Map<Long, String> gmail = gmailEmails(gmailAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId));
        return toDto(campaign, fromEmail(campaign, smtp, gmail));
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> deliveries(Long ownerUserId, Long campaignId) {
        require(ownerUserId, campaignId);
        return deliveryRepository.findByCampaignId(campaignId).stream()
                .map(CampaignService::toDto)
                .toList();
    }

    @Transactional
    public void cancel(Long ownerUserId, Long id) {
        Campaign campaign = require(ownerUserId, id);
        if (campaign.getStatus() == CampaignStatus.CANCELLED
                || campaign.getStatus() == CampaignStatus.SUCCESS
                || campaign.getStatus() == CampaignStatus.PARTIAL) {
            throw new ConflictException("A campaign in the state " + campaign.getStatus() + " cannot be cancelled");
        }
        Instant now = Instant.now();
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign.setFinishedAt(now);
        campaign.setUpdatedAt(now);
        campaign.setUpdatedBy(ownerUserId);
        campaigns.save(campaign);
    }

    private String requireAccount(Long ownerUserId, AccountKind kind, Long accountId) {
        if (accountId == null) {
            throw new NotFoundException("The campaign does not name a sending account");
        }
        if (kind == AccountKind.GMAIL) {
            return gmailAccounts
                    .findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId)
                    .map(GmailAccount::getEmail)
                    .orElseThrow(() -> NotFoundException.of("Gmail account", accountId));
        }
        return smtpAccounts
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(accountId, ownerUserId)
                .map(SmtpAccount::getEmail)
                .orElseThrow(() -> NotFoundException.of("SMTP account", accountId));
    }

    private Campaign require(Long ownerUserId, Long id) {
        return campaigns
                .findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Campaign", id));
    }

    static CampaignDto toDto(Campaign campaign, String fromEmail) {
        EmailTemplate template = campaign.getTemplate();
        return new CampaignDto(
                campaign.getId(),
                campaign.getFromAccountKind(),
                campaign.getFromAccountId(),
                fromEmail,
                campaign.getTargetType(),
                template == null ? null : template.getId(),
                template == null ? null : template.getName(),
                campaign.getRecipientCount(),
                campaign.getSuccessCount(),
                campaign.getFailureCount(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getStartedAt(),
                campaign.getFinishedAt());
    }

    private static Map<Long, String> smtpEmails(List<SmtpAccount> accounts) {
        Map<Long, String> emails = new HashMap<>();
        for (SmtpAccount account : accounts) {
            emails.put(account.getId(), account.getEmail());
        }
        return emails;
    }

    private static Map<Long, String> gmailEmails(List<GmailAccount> accounts) {
        Map<Long, String> emails = new HashMap<>();
        for (GmailAccount account : accounts) {
            emails.put(account.getId(), account.getEmail());
        }
        return emails;
    }

    private static String fromEmail(Campaign campaign, Map<Long, String> smtp, Map<Long, String> gmail) {
        Map<Long, String> source = campaign.getFromAccountKind() == AccountKind.GMAIL ? gmail : smtp;
        return source.get(campaign.getFromAccountId());
    }

    static DeliveryDto toDto(Delivery delivery) {
        return new DeliveryDto(
                delivery.getId(),
                delivery.getRecipientEmail(),
                delivery.getStatus(),
                delivery.getProviderMessageId(),
                delivery.getErrorDetail(),
                delivery.getSentAt());
    }
}
