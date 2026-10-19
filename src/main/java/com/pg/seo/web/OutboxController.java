package com.pg.seo.web;

import com.pg.seo.domain.NotFoundException;
import com.pg.seo.model.OutboxDto;
import com.pg.seo.model.OutboxRequest;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.OutboxService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The scheduled outbox.
 */
@RestController
@RequestMapping("/api/outbox")
public class OutboxController {

    /** The window the list uses when the caller does not name one. */
    private static final Instant DEFAULT_FROM = Instant.EPOCH;

    private final OutboxService outbox;
    private final AuthPrincipalResolver principals;

    public OutboxController(OutboxService outbox, AuthPrincipalResolver principals) {
        this.outbox = outbox;
        this.principals = principals;
    }

    /**
     * The rows of the tenant in a window.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<OutboxDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Long ownerUserId = principals.currentUserId();
        return ApiResponse.ok(outbox.list(ownerUserId, from == null ? DEFAULT_FROM : from, to == null ? defaultTo() : to));
    }

    /**
     * Schedules one row.
     */
    @PostMapping
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<OutboxDto> schedule(@Valid @RequestBody OutboxRequest request) {
        return ApiResponse.ok(outbox.schedule(principals.currentUserId(), request));
    }

    /**
     * Cancels one row.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<OutboxDto> cancel(@PathVariable("id") Long id) {
        Long ownerUserId = principals.currentUserId();
        outbox.cancel(ownerUserId, id);
        return ApiResponse.ok(find(ownerUserId, id));
    }

    /**
     * The row as it is now.
     *
     * <p>The service reads the rows as a list and never on their own, so the row is picked
     * out of the list of the tenant. A row of another tenant is not in it.
     */
    private OutboxDto find(Long ownerUserId, Long id) {
        for (OutboxDto row : outbox.list(ownerUserId, DEFAULT_FROM, defaultTo())) {
            if (row.id().equals(id)) {
                return row;
            }
        }
        throw NotFoundException.of("Outbox row", id);
    }

    /**
     * The end of the default window. A row that is scheduled for later is still worth
     * showing.
     */
    private static Instant defaultTo() {
        return Instant.now().plus(365, ChronoUnit.DAYS);
    }
}
