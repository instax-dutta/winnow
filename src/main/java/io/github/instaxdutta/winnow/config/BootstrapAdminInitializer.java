package io.github.instaxdutta.winnow.config;

import io.github.instaxdutta.winnow.domain.AppRole;
import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.domain.UserStatus;
import io.github.instaxdutta.winnow.repository.AppRoleRepository;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator on a fresh install.
 *
 * <p>When {@code seo.bootstrap.password} is set, the named user gets that password and is
 * activated. When the user does not exist the boot carries on with a warning, because a
 * missing row is an operator problem, not a reason to refuse to start.
 *
 * <p>The password is never logged. This is a deployment convenience and not an activation
 * mechanism: the application carries no licence, no token and no machine check.
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AppUserRepository users;

    private final AppRoleRepository roles;

    private final PasswordEncoder passwordEncoder;

    private final BootstrapProperties properties;

    public BootstrapAdminInitializer(
            AppUserRepository users,
            AppRoleRepository roles,
            PasswordEncoder passwordEncoder,
            BootstrapProperties properties) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String password = properties.password();
        if (password == null || password.isBlank()) {
            log.info("No bootstrap password is configured, the administrator is left untouched");
            return;
        }
        String username = properties.username();
        Optional<AppUser> found = users.findByUsernameAndDeletedAtIsNull(username);
        if (found.isEmpty()) {
            log.warn("The bootstrap user '{}' does not exist, the administrator was not created", username);
            return;
        }
        AppUser user = found.get();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        grantAdministrator(user);
        users.save(user);
        log.info("The bootstrap user '{}' was given the configured password and activated", username);
    }

    /**
     * Makes sure the bootstrap user holds the administrator role.
     *
     * <p>Without a role the account can sign in but can do nothing, which looks like a
     * broken install. The role is added only when it is missing.
     */
    private void grantAdministrator(AppUser user) {
        boolean hasRole = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals(AppRoleNames.ADMIN));
        if (hasRole) {
            return;
        }
        roles.findByCode(AppRoleNames.ADMIN).ifPresent(role -> user.getRoles().add(role));
    }
}
