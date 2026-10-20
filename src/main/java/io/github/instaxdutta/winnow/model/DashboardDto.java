package io.github.instaxdutta.winnow.model;

/** The counters on the dashboard. Every one of them is tenant scoped. */
public record DashboardDto(
        long clients,
        long clientGroups,
        long templates,
        long pendingTemplates,
        long smtpAccounts,
        long gmailAccounts,
        long campaigns,
        long emailsSent,
        long emailsFailed,
        long extractionRuns,
        long inboxMessages) {}
