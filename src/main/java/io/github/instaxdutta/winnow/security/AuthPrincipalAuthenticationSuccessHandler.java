package io.github.instaxdutta.winnow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * The login success handler.
 *
 * <p>The principal is put on the session so the page can show who is logged in, and the
 * caller is sent to the dashboard.
 *
 * <p>The user id comes from the {@link org.springframework.security.core.userdetails.UserDetails}
 * the authentication carries, never from a request parameter. Reading the submitted
 * username and password out of the request and storing them on a transient entity means a
 * typo silently produces a session without a tenant.
 */
@Component
public class AuthPrincipalAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** The session attribute the principal is stored under. */
    public static final String SESSION_ATTRIBUTE = "seo.auth.principal";

    /** Where the caller lands after a successful login. */
    public static final String SUCCESS_URL = "/dashboard";

    private static final Logger log = LoggerFactory.getLogger(AuthPrincipalAuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        AppUserPrincipal principal = principalOf(authentication);
        AuthPrincipal authPrincipal = principal.authPrincipal();
        request.getSession().setAttribute(SESSION_ATTRIBUTE, authPrincipal);
        log.info("The login of '{}' succeeded", principal.getUsername());
        response.sendRedirect(request.getContextPath() + SUCCESS_URL);
    }

    private static AppUserPrincipal principalOf(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal;
        }
        throw new IllegalStateException("The authenticated principal is not an application user");
    }
}
