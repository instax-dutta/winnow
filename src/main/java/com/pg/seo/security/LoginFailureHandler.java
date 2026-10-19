package com.pg.seo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * The login failure handler.
 *
 * <p>Every failure gets the same message and the same redirect. An unknown user, a deleted
 * user, a disabled user, a locked account and a wrong password are indistinguishable from
 * the outside, so the login page cannot be used to find out which accounts exist.
 *
 * <p>The reason for the failure is never put in the response and never logged here. The
 * counter and the lock live in {@link AppUserDetailsService}.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    /** The one message every rejected login gets. */
    public static final String GENERIC_MESSAGE = "Your username and password is invalid.";

    /** The query parameter that tells the login page a login failed. */
    public static final String ERROR_PARAMETER = "error";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login?" + ERROR_PARAMETER);
    }
}
