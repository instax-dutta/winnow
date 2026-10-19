package com.pg.seo.web;

import com.pg.seo.model.DashboardDto;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The counters of the dashboard.
 *
 * <p>Every counter is scoped to the tenant. The original counted every row of every table,
 * so one operator saw the numbers of another tenant.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final AuthPrincipalResolver principals;

    public DashboardController(DashboardService dashboard, AuthPrincipalResolver principals) {
        this.dashboard = dashboard;
        this.principals = principals;
    }

    /**
     * The counters of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<DashboardDto> summary() {
        return ApiResponse.ok(dashboard.summary(principals.currentUserId()));
    }
}
