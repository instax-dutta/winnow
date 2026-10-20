package io.github.instaxdutta.winnow.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * The HTTP security policy.
 *
 * <p>CSRF stays enabled. A state changing endpoint on {@code @GetMapping} is exempt from
 * CSRF and could be triggered from any other site with an image tag. Every mutation is on
 * a verb that CSRF protects.
 *
 * <p>Sessions are {@link SessionCreationPolicy#IF_REQUIRED} with at most one session per
 * user. Without a {@link SessionRegistry} and an {@link HttpSessionEventPublisher} expired
 * sessions are never evicted from the registry and users are spuriously logged out over
 * time. Both beans are declared here.
 *
 * <p>Session fixation protection is left at the Spring default, which changes the session
 * id on login.
 *
 * <p>A request that is refused because the caller holds no role is answered with 403 and
 * nothing else. A forward to an {@code /accessdenied} page leaks the refusal, and the
 * Spring default answers an authenticated caller's refusal with a redirect to the login
 * page, which sends the caller back to the login form for no reason.
 *
 * <p>A request with a missing or a wrong CSRF token is answered with a redirect to the
 * login page. That is Spring Security's own behaviour: because an invalid session URL is
 * configured, a missing token is read as a lost session, the session is dropped and the
 * caller is sent to the login form. The mutating endpoint is never executed either way.
 *
 * <p>The chain carries the coarse path rules only. The fine grained role rules live on the
 * controllers and the services as {@code @PreAuthorize}, expressed with the vocabulary in
 * {@link RoleNames} so both layers speak the same language.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Paths an anonymous caller may reach. */
    static final String[] PUBLIC_PATHS = {"/login", "/assets/**", "/favicon.ico", "/error"};

    /** Paths that need an authenticated caller. */
    static final String[] AUTHENTICATED_PATHS = {
        "/dashboard",
        "/clients/**",
        "/groups/**",
        "/templates/**",
        "/accounts/**",
        "/campaigns/**",
        "/outbox/**",
        "/extraction/**",
        "/inbox/**",
        "/api/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthPrincipalAuthenticationSuccessHandler successHandler,
            LoginFailureHandler failureHandler)
            throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(PUBLIC_PATHS)
                .permitAll()
                .requestMatchers(AUTHENTICATED_PATHS)
                .authenticated()
                .anyRequest()
                .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl(AuthPrincipalAuthenticationSuccessHandler.SUCCESS_URL)
                        .failureUrl("/login?" + LoginFailureHandler.ERROR_PARAMETER)
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login")
                        .maximumSessions(1));
        // The access denied handler is set eagerly. A customizer passed to
        // exceptionHandling() is only applied when the configurer is initialised, which
        // happens after the CSRF filter has resolved the handler it uses.
        accessDeniedHandler(http);
        // CSRF is deliberately not configured: it stays enabled at the Spring default.
        return http.build();
    }

    /** A refusal is answered with 403 and nothing else. */
    @SuppressWarnings("rawtypes")
    private void accessDeniedHandler(HttpSecurity http) {
        ExceptionHandlingConfigurer exceptions = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (exceptions != null) {
            exceptions.accessDeniedHandler(
                    (request, response, denied) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));
        }
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
