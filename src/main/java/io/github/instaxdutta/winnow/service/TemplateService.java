package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.domain.ConflictException;
import io.github.instaxdutta.winnow.domain.EmailTemplate;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import io.github.instaxdutta.winnow.model.EmailTemplateDto;
import io.github.instaxdutta.winnow.model.EmailTemplateRequest;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import io.github.instaxdutta.winnow.repository.EmailTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email templates and the approval workflow.
 *
 * <p>A template is created as a draft. Editing an approved template sends it back to a
 * draft, because a template that was approved in one form must not be sent in another.
 */
@Service
public class TemplateService {

    private final EmailTemplateRepository templates;
    private final AppUserRepository users;

    private final HtmlSanitiser sanitiser;

    public TemplateService(EmailTemplateRepository templates, AppUserRepository users, HtmlSanitiser sanitiser) {
        this.templates = templates;
        this.users = users;
        this.sanitiser = sanitiser;
    }

    /**
     * One template of the tenant.
     *
     * @param ownerUserId the tenant
     * @param id          the template
     * @return the template
     * @throws NotFoundException when the template does not exist or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public EmailTemplateDto get(Long ownerUserId, Long id) {
        return toDto(require(ownerUserId, id));
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> list(Long ownerUserId) {
        return templates.findByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ownerUserId).stream()
                .map(TemplateService::toDto)
                .toList();
    }

    @Transactional
    public EmailTemplateDto create(Long ownerUserId, EmailTemplateRequest request) {
        Instant now = Instant.now();
        EmailTemplate template = new EmailTemplate();
        template.setOwnerUserId(ownerUserId);
        template.setName(request.name().trim());
        template.setSubject(request.subject().trim());
        // The body is rendered without escaping, so it is sanitised on write.
        template.setContentHtml(sanitiser.clean(request.contentHtml()));
        template.setStatus(TemplateStatus.DRAFT);
        template.setVersion(1);
        template.setCreatedAt(now);
        template.setCreatedBy(ownerUserId);
        template.setUpdatedAt(now);
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
        return toDto(template);
    }

    @Transactional
    public EmailTemplateDto update(Long ownerUserId, Long id, EmailTemplateRequest request) {
        EmailTemplate template = require(ownerUserId, id);
        String name = request.name().trim();
        String subject = request.subject().trim();
        String content = sanitiser.clean(request.contentHtml());
        if (Objects.equals(name, template.getName())
                && Objects.equals(subject, template.getSubject())
                && Objects.equals(content, template.getContentHtml())) {
            return toDto(template);
        }
        template.setName(name);
        template.setSubject(subject);
        template.setContentHtml(content);
        template.setVersion(template.getVersion() + 1);
        template.setStatus(TemplateStatus.DRAFT);
        template.setApprovedBy(null);
        template.setApprovedAt(null);
        template.setUpdatedAt(Instant.now());
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
        return toDto(template);
    }

    @Transactional
    public void delete(Long ownerUserId, Long id) {
        EmailTemplate template = require(ownerUserId, id);
        Instant now = Instant.now();
        template.setDeletedAt(now);
        template.setUpdatedAt(now);
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
    }

    @Transactional
    public EmailTemplateDto submitForApproval(Long ownerUserId, Long id) {
        EmailTemplate template = require(ownerUserId, id);
        if (template.getStatus() != TemplateStatus.DRAFT && template.getStatus() != TemplateStatus.REJECTED) {
            throw new ConflictException(
                    "A template in the state " + template.getStatus() + " cannot be submitted for approval");
        }
        template.setStatus(TemplateStatus.PENDING_APPROVAL);
        template.setUpdatedAt(Instant.now());
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
        return toDto(template);
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> listPendingApproval(Long ownerUserId) {
        return templates
                .findByOwnerUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        ownerUserId, TemplateStatus.PENDING_APPROVAL)
                .stream()
                .map(TemplateService::toDto)
                .toList();
    }

    @Transactional
    public EmailTemplateDto approve(Long ownerUserId, Long id) {
        EmailTemplate template = require(ownerUserId, id);
        if (template.getStatus() != TemplateStatus.PENDING_APPROVAL) {
            throw new ConflictException(
                    "Only a template in the state PENDING_APPROVAL can be approved, not " + template.getStatus());
        }
        Instant now = Instant.now();
        template.setStatus(TemplateStatus.APPROVED);
        template.setApprovedBy(users.findByIdAndDeletedAtIsNull(ownerUserId).orElse(null));
        template.setApprovedAt(now);
        template.setUpdatedAt(now);
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
        return toDto(template);
    }

    @Transactional
    public void reject(Long ownerUserId, Long id) {
        EmailTemplate template = require(ownerUserId, id);
        if (template.getStatus() != TemplateStatus.PENDING_APPROVAL) {
            throw new ConflictException(
                    "Only a template in the state PENDING_APPROVAL can be rejected, not " + template.getStatus());
        }
        template.setStatus(TemplateStatus.REJECTED);
        template.setUpdatedAt(Instant.now());
        template.setUpdatedBy(ownerUserId);
        templates.save(template);
    }

    private EmailTemplate require(Long ownerUserId, Long id) {
        return templates
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Email template", id));
    }

    private static EmailTemplateDto toDto(EmailTemplate template) {
        AppUser approver = template.getApprovedBy();
        return new EmailTemplateDto(
                template.getId(),
                template.getName(),
                template.getSubject(),
                template.getContentHtml(),
                template.getStatus(),
                template.getVersion(),
                approver == null ? null : approver.getUsername(),
                template.getApprovedAt(),
                template.getUpdatedAt());
    }
}
