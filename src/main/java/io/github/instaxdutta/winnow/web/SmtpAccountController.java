package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.model.SmtpAccountDto;
import io.github.instaxdutta.winnow.model.SmtpAccountRequest;
import io.github.instaxdutta.winnow.security.AuthPrincipalResolver;
import io.github.instaxdutta.winnow.service.SmtpAccountService;
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
 * The SMTP sending accounts.
 *
 * <p>The answer carries no credential. The service returns a flag that says whether a
 * secret is stored and nothing else, and returning the plaintext password to any
 * logged-in user hands over the account.
 */
@RestController
@RequestMapping("/api/accounts/smtp")
public class SmtpAccountController {

    private final SmtpAccountService accounts;
    private final AuthPrincipalResolver principals;

    public SmtpAccountController(SmtpAccountService accounts, AuthPrincipalResolver principals) {
        this.accounts = accounts;
        this.principals = principals;
    }

    /**
     * The SMTP accounts of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<SmtpAccountDto>> list() {
        return ApiResponse.ok(accounts.list(principals.currentUserId()));
    }

    /**
     * Creates one account.
     */
    @PostMapping
    @PreAuthorize(Roles.ACCOUNTS)
    public ApiResponse<SmtpAccountDto> create(@Valid @RequestBody SmtpAccountRequest request) {
        return ApiResponse.ok(accounts.create(principals.currentUserId(), request));
    }

    /**
     * Changes one account.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.ACCOUNTS)
    public ApiResponse<SmtpAccountDto> update(
            @PathVariable("id") Long id, @Valid @RequestBody SmtpAccountRequest request) {
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
