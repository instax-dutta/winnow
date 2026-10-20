package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.AccountKind;
import io.github.instaxdutta.winnow.domain.Campaign;
import io.github.instaxdutta.winnow.domain.CampaignStatus;
import io.github.instaxdutta.winnow.domain.ConflictException;
import io.github.instaxdutta.winnow.domain.Delivery;
import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import io.github.instaxdutta.winnow.domain.EmailTemplate;
import io.github.instaxdutta.winnow.domain.GmailAccount;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.domain.SmtpAccount;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import io.github.instaxdutta.winnow.mail.GmailMailSender;
import io.github.instaxdutta.winnow.mail.QuotaService;
import io.github.instaxdutta.winnow.mail.SendRequest;
import io.github.instaxdutta.winnow.mail.SendResult;
import io.github.instaxdutta.winnow.mail.SmtpMailSender;
import io.github.instaxdutta.winnow.model.CampaignDto;
import io.github.instaxdutta.winnow.model.CampaignRequest;
import io.github.instaxdutta.winnow.model.ClientDto;
import io.github.instaxdutta.winnow.model.DeliveryDto;
import io.github.instaxdutta.winnow.model.PagedResponse;
import io.github.instaxdutta.winnow.repository.CampaignRepository;
import io.github.instaxdutta.winnow.repository.DeliveryRepository;
import io.github.instaxdutta.winnow.repository.EmailTemplateRepository;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import io.github.instaxdutta.winnow.repository.SmtpAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
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
    private final SmtpMailSender smtpSender;
    private final GmailMailSender gmailSender;
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
            QuotaService quota,
            SmtpMailSender smtpSender,
            GmailMailSender gmailSender) {
        this.campaigns = campaigns;
        this.deliveryRepository = deliveryRepository;
        this.templates = templates;
        this.smtpAccounts = smtpAccounts;
        this.gmailAccounts = gmailAccounts;
        this.clients = clients;
        this.quota = quota;
        this.smtpSender = smtpSender;
        this.gmailSender = gmailSender;
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

    /**
     * Claims the campaigns that are waiting to be sent.
     *
     * <p>The claim is a conditional update, so two dispatchers racing for the same campaign
     * produce exactly one winner. The loser does not send the mail a second time.
     *
     * @param limit how many campaigns to claim at most
     * @return the ids of the campaigns this caller won
     */
    @Transactional
    public List<Long> claimQueued(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<Campaign> queued = campaigns.findByStatusOrderByCreatedAtAsc(
                CampaignStatus.QUEUED, PageRequest.of(0, limit));
        List<Long> claimed = new ArrayList<>(queued.size());
        for (Campaign campaign : queued) {
            if (campaigns.claim(campaign.getId(), CampaignStatus.QUEUED, CampaignStatus.RUNNING, Instant.now()) == 1) {
                claimed.add(campaign.getId());
            }
        }
        return claimed;
    }

    /**
     * Sends the queued recipients of one campaign.
     *
     * <p>The method is deliberately not transactional: it holds a network call per
     * recipient, and a transaction must not stay open across one. Every write is a
     * repository call, which is transactional on its own.
     *
     * <p>A recipient that fails does not fail the campaign. The failure is recorded on the
     * delivery row and the campaign ends as partial or failed.
     *
     * @param campaignId the campaign to send
     */
    public void dispatch(Long campaignId) {
        Campaign campaign = campaigns.findById(campaignId).orElse(null);
        if (campaign == null) {
            return;
        }
        EmailTemplate template = campaign.getTemplate();
        if (template == null) {
            closeCampaign(campaign, CampaignStatus.FAILED, "the campaign has no template");
            return;
        }
        String fromEmail;
        try {
            fromEmail = requireAccount(campaign.getOwnerUserId(), campaign.getFromAccountKind(), campaign.getFromAccountId());
        } catch (RuntimeException e) {
            closeCampaign(campaign, CampaignStatus.FAILED, "the sending account is no longer available");
            return;
        }

        List<Delivery> pending = deliveryRepository.findByCampaignIdAndStatus(campaignId, DeliveryStatus.QUEUED);
        int success = 0;
        int failure = 0;
        for (Delivery delivery : pending) {
            try {
                SendResult result = sendOne(campaign, template, fromEmail, delivery);
                if (result.success()) {
                    delivery.setStatus(DeliveryStatus.SENT);
                    delivery.setProviderMessageId(result.providerMessageId());
                    delivery.setSentAt(Instant.now());
                    delivery.setErrorDetail(null);
                    success++;
                } else {
                    delivery.setStatus(DeliveryStatus.FAILED);
                    delivery.setErrorDetail(result.errorMessage());
                    failure++;
                }
            } catch (RuntimeException e) {
                // The message of a mail failure can carry the name of the account, so only
                // the type of the failure is recorded.
                delivery.setStatus(DeliveryStatus.FAILED);
                delivery.setErrorDetail("the send failed (" + e.getClass().getSimpleName() + ")");
                failure++;
            }
            delivery.setUpdatedAt(Instant.now());
            deliveryRepository.save(delivery);
        }

        campaign.setSuccessCount(success);
        campaign.setFailureCount(failure);
        campaign.setFinishedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        if (failure == 0) {
            campaign.setStatus(CampaignStatus.SUCCESS);
        } else if (success > 0) {
            campaign.setStatus(CampaignStatus.PARTIAL);
        } else {
            campaign.setStatus(CampaignStatus.FAILED);
        }
        campaigns.save(campaign);
    }

    /**
     * Sends one recipient through the adapter that matches the sending account.
     */
    private SendResult sendOne(Campaign campaign, EmailTemplate template, String fromEmail, Delivery delivery) {
        Map<String, String> variables = new HashMap<>();
        variables.put("Email", delivery.getRecipientEmail());
        variables.put("Domain", domainOf(delivery.getRecipientEmail()));
        String body = template.getContentHtml() == null ? "" : template.getContentHtml();

        SendRequest request = new SendRequest(
                fromEmail,
                delivery.getRecipientEmail(),
                template.getSubject(),
                body,
                null,
                variables,
                null,
                campaign.getOwnerUserId(),
                campaign.getFromAccountId());
        return campaign.getFromAccountKind() == AccountKind.GMAIL
                ? gmailSender.send(request)
                : smtpSender.send(request);
    }

    /**
     * The domain part of an address, used for the {@code {{Domain}}} placeholder.
     */
    private static String domainOf(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return at < 0 ? "" : email.substring(at + 1);
    }

    /**
     * Ends a campaign that cannot be sent at all.
     */
    private void closeCampaign(Campaign campaign, CampaignStatus status, String detail) {
        campaign.setStatus(status);
        campaign.setErrorDetail(detail);
        campaign.setFinishedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaigns.save(campaign);
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
