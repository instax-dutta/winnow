package com.pg.seo.web;

import com.pg.seo.domain.NotFoundException;
import com.pg.seo.model.EmailTemplateDto;
import com.pg.seo.model.EmailTemplateRequest;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The email templates and their approval workflow.
 *
 * <p>The approval steps are a state change and are therefore POSTs. The original approved
 * a template over GET, which is exempt from CSRF and triggerable from any site.
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templates;
    private final AuthPrincipalResolver principals;

    public TemplateController(TemplateService templates, AuthPrincipalResolver principals) {
        this.templates = templates;
        this.principals = principals;
    }

    /**
     * The templates of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<EmailTemplateDto>> list() {
        return ApiResponse.ok(templates.list(principals.currentUserId()));
    }

    /**
     * The templates of the tenant that wait for an approval.
     */
    @GetMapping("/pending")
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<EmailTemplateDto>> pending() {
        return ApiResponse.ok(templates.listPendingApproval(principals.currentUserId()));
    }

    /**
     * Creates one template.
     */
    @PostMapping
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<EmailTemplateDto> create(@Valid @RequestBody EmailTemplateRequest request) {
        return ApiResponse.ok(templates.create(principals.currentUserId(), request));
    }

    /**
     * Changes one template.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<EmailTemplateDto> update(
            @PathVariable("id") Long id, @Valid @RequestBody EmailTemplateRequest request) {
        return ApiResponse.ok(templates.update(principals.currentUserId(), id, request));
    }

    /**
     * Removes one template.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        templates.delete(principals.currentUserId(), id);
        return ApiResponse.ok("The template was removed", null);
    }

    /**
     * Sends one template for approval.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<EmailTemplateDto> submit(@PathVariable("id") Long id) {
        return ApiResponse.ok(templates.submitForApproval(principals.currentUserId(), id));
    }

    /**
     * Approves one template.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.APPROVE)
    public ApiResponse<EmailTemplateDto> approve(@PathVariable("id") Long id) {
        Long ownerUserId = principals.currentUserId();
        templates.approve(ownerUserId, id);
        return ApiResponse.ok(find(ownerUserId, id));
    }

    /**
     * Rejects one template.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.APPROVE)
    public ApiResponse<EmailTemplateDto> reject(@PathVariable("id") Long id) {
        Long ownerUserId = principals.currentUserId();
        templates.reject(ownerUserId, id);
        return ApiResponse.ok(find(ownerUserId, id));
    }

    /**
     * The template as it is now.
     *
     * <p>The service reads a template as part of a list and never on its own, so the row is
     * picked out of the list of the tenant. A template of another tenant is not in it.
     */
    private EmailTemplateDto find(Long ownerUserId, Long id) {
        for (EmailTemplateDto template : templates.list(ownerUserId)) {
            if (template.id().equals(id)) {
                return template;
            }
        }
        throw NotFoundException.of("Email template", id);
    }
}
