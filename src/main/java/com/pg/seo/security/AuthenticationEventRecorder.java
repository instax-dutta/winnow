package com.pg.seo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Keeps the login counter of the application user.
 *
 * <p>The framework publishes an event for every authentication attempt that succeeded and
 * for every attempt that was rejected. Listening to those events means the counter is kept
 * for every way into the application, not only for the login form.
 *
 * <p>The username comes from the authentication token the framework built, not from a
 * request parameter, so a caller cannot make the counter move for an account they did not
 * try to log into.
 */
@Component
public class AuthenticationEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventRecorder.class);

    private final AppUserDetailsService userDetailsService;

    public AuthenticationEventRecorder(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = usernameOf(event.getAuthentication());
        log.info("The login of '{}' was rejected", username);
        userDetailsService.recordAuthenticationFailure(username);
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        String username = authentication.getName();
        userDetailsService.recordAuthenticationSuccess(username);
    }

    private static String usernameOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? null : name;
    }
}
