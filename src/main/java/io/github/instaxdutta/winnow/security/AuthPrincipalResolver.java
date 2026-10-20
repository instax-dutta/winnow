package io.github.instaxdutta.winnow.security;

import io.github.instaxdutta.winnow.domain.ForbiddenException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The authenticated caller, read from the security context.
 *
 * <p>The web layer asks this component for the tenant. A request parameter can never
 * choose the tenant, because the tenant is the authenticated user.
 */
@Component
public class AuthPrincipalResolver {

    /**
     * The authenticated caller.
     *
     * @throws ForbiddenException when there is no authenticated application user
     */
    public AuthPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthPrincipal authPrincipal) {
                return authPrincipal;
            }
            if (principal instanceof AppUserPrincipal appUserPrincipal) {
                return appUserPrincipal.authPrincipal();
            }
        }
        throw new ForbiddenException("An authenticated user is required");
    }

    /**
     * The tenant key of the authenticated caller.
     *
     * @throws ForbiddenException when there is no authenticated application user
     */
    public Long currentUserId() {
        return current().userId();
    }
}
