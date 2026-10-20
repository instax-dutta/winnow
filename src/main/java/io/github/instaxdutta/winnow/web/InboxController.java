package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.domain.ValidationException;
import io.github.instaxdutta.winnow.model.InboxMessageDto;
import io.github.instaxdutta.winnow.security.AuthPrincipalResolver;
import io.github.instaxdutta.winnow.service.InboxService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The captured inbound messages.
 */
@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final InboxService inbox;
    private final AuthPrincipalResolver principals;

    public InboxController(InboxService inbox, AuthPrincipalResolver principals) {
        this.inbox = inbox;
        this.principals = principals;
    }

    /**
     * The messages of one account of the tenant in a window.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<InboxMessageDto>> list(
            @RequestParam("accountId") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        if (accountId == null) {
            throw new ValidationException("The request does not name a mailbox");
        }
        Long ownerUserId = principals.currentUserId();
        return ApiResponse.ok(inbox.list(ownerUserId, accountId, from == null ? Instant.EPOCH : from,
                        to == null ? Instant.now() : to)
                .data());
    }
}
