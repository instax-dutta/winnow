package io.github.instaxdutta.winnow.security;

import io.github.instaxdutta.winnow.config.SecurityProperties;
import io.github.instaxdutta.winnow.domain.AppRole;
import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.domain.UserStatus;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the application user that is logging in and keeps the login counter.
 *
 * <p>An unknown user, a deleted user, a disabled user and a locked user all fail the same
 * way, so the login page cannot be used to find out which accounts exist.
 *
 * <p>Failed logins are counted. Once the count reaches {@code seo.security.max-failed-logins}
 * the account is locked for {@code seo.security.lock-duration}. A successful login clears
 * the counter and stamps the last login time. Without a lockout a stolen password can be
 * tried forever.
 *
 * <p>The password, the hash and the session id are never logged. Only the username and the
 * outcome are.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);

    /** The one message every rejected login gets. */
    static final String GENERIC_FAILURE = "Invalid credentials";

    private final AppUserRepository users;

    private final SecurityProperties securityProperties;

    public AppUserDetailsService(AppUserRepository users, SecurityProperties securityProperties) {
        this.users = users;
        this.securityProperties = securityProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users
                .findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException(GENERIC_FAILURE));
        if (user.getStatus() == UserStatus.DISABLED || user.isLocked()) {
            throw new UsernameNotFoundException(GENERIC_FAILURE);
        }
        Set<String> codes = user.getRoles().stream().map(AppRole::getCode).collect(Collectors.toUnmodifiableSet());
        List<GrantedAuthority> authorities = codes.stream().map(SimpleGrantedAuthority::new).map(a -> (GrantedAuthority) a).toList();
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), authorities);
    }

    /**
     * Counts a failed login and locks the account once the configured number of failures is
     * reached.
     *
     * <p>The lock is a window, not a state: the account unlocks itself when the window
     * passes. An unknown username has no row and is ignored.
     */
    @Transactional
    public void recordAuthenticationFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Optional<AppUser> found = users.findByUsernameAndDeletedAtIsNull(username);
        if (found.isEmpty()) {
            log.info("The login of the unknown user '{}' was rejected", username);
            return;
        }
        AppUser user = found.get();
        int failures = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failures);
        Instant now = Instant.now();
        if (!user.isLocked() && failures >= securityProperties.maxFailedLogins()) {
            user.setLockedUntil(now.plus(securityProperties.lockDuration()));
            log.info(
                    "The account '{}' is locked for {} after {} failed logins",
                    username,
                    securityProperties.lockDuration(),
                    failures);
        }
        user.setUpdatedAt(now);
        users.save(user);
        log.debug("Recorded a failed login for '{}'", username);
    }

    /** Clears the failure counter, releases the lock and stamps the last login time. */
    @Transactional
    public void recordAuthenticationSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Optional<AppUser> found = users.findByUsernameAndDeletedAtIsNull(username);
        if (found.isEmpty()) {
            return;
        }
        AppUser user = found.get();
        Instant now = Instant.now();
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        users.save(user);
        log.debug("Recorded a successful login for '{}'", username);
    }
}
