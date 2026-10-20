package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.model.GmailAccountDto;
import io.github.instaxdutta.winnow.model.GmailAccountRequest;
import io.github.instaxdutta.winnow.security.AuthPrincipalResolver;
import io.github.instaxdutta.winnow.service.GmailAccountService;
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
 * The Google OAuth sending accounts.
 *
 * <p>The answer carries no client secret and no refresh token. Returning all four OAuth
 * fields, including a live refresh token, to any logged-in user hands over the account.
 */
@RestController
@RequestMapping("/api/accounts/gmail")
public class GmailAccountController {

    private final GmailAccountService accounts;
    private final AuthPrincipalResolver principals;

    public GmailAccountController(GmailAccountService accounts, AuthPrincipalResolver principals) {
        this.accounts = accounts;
        this.principals = principals;
    }

    /**
     * The Google accounts of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<GmailAccountDto>> list() {
        return ApiResponse.ok(accounts.list(principals.currentUserId()));
    }

    /**
     * Creates one account.
     */
    @PostMapping
    @PreAuthorize(Roles.ACCOUNTS)
    public ApiResponse<GmailAccountDto> create(@Valid @RequestBody GmailAccountRequest request) {
        return ApiResponse.ok(accounts.create(principals.currentUserId(), request));
    }

    /**
     * Changes one account.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.ACCOUNTS)
    public ApiResponse<GmailAccountDto> update(
            @PathVariable("id") Long id, @Valid @RequestBody GmailAccountRequest request) {
        return ApiResponse.ok(accounts.update(principals.currentUserId(), id, request));
    }

    /**
     * Removes one account.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ACCOUNTS)
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        accounts.delete(principals.currentUserId(), id);
        return ApiResponse.ok("The account was removed", null);
    }
}
