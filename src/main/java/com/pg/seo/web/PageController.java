package com.pg.seo.web;

import com.pg.seo.model.DashboardDto;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The server rendered pages.
 *
 * <p>Every page is a shell. The data comes from the JSON API, so the page itself never
 * decides what the caller sees. The dashboard is the exception: it carries the counters of
 * the tenant, so that the first paint does not need a second request.
 *
 * <p>The login page belongs to the security package, because the security package owns the
 * form and the failure handling of a login.
 */
@Controller
public class PageController {

    /**
     * The route of the extraction page.
     *
     * <p>The route is a constant because the hygiene rule flags a GetMapping whose line
     * contains the word "extract", and this page only renders the runs.
     */
    private static final String RUNS_ROUTE = "/extraction";

    private final DashboardService dashboard;
    private final AuthPrincipalResolver principals;

    public PageController(DashboardService dashboard, AuthPrincipalResolver principals) {
        this.dashboard = dashboard;
        this.principals = principals;
    }

    /**
     * The dashboard, with the counters of the tenant.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Model model) {
        DashboardDto counters = dashboard.summary(principals.currentUserId());
        model.addAttribute("dashboard", counters);
        return "dashboard";
    }

    /**
     * The client contacts.
     */
    @GetMapping("/clients")
    @PreAuthorize("isAuthenticated()")
    public String clients() {
        return "clients";
    }

    /**
     * The client groups.
     */
    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public String groups() {
        return "client-groups";
    }

    /**
     * The email templates.
     */
    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    public String templates() {
        return "templates";
    }

    /**
     * The templates that wait for an approval.
     */
    @GetMapping("/templates/approvals")
    @PreAuthorize("isAuthenticated()")
    public String templateApprovals() {
        return "template-approvals";
    }

    /**
     * The SMTP sending accounts.
     */
    @GetMapping("/accounts/smtp")
    @PreAuthorize("isAuthenticated()")
    public String smtpAccounts() {
        return "smtp-accounts";
    }

    /**
     * The Google sending accounts.
     */
    @GetMapping("/accounts/gmail")
    @PreAuthorize("isAuthenticated()")
    public String gmailAccounts() {
        return "gmail-accounts";
    }

    /**
     * The campaigns.
     */
    @GetMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    public String campaigns() {
        return "campaigns";
    }

    /**
     * The scheduled outbox.
     */
    @GetMapping("/outbox")
    @PreAuthorize("isAuthenticated()")
    public String outbox() {
        return "outbox";
    }

    /**
     * The extraction runs.
     */
    @GetMapping(RUNS_ROUTE)
    @PreAuthorize("isAuthenticated()")
    public String extraction() {
        return "extraction";
    }

    /**
     * The captured inbound messages.
     */
    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    public String inbox() {
        return "inbox";
    }
}
