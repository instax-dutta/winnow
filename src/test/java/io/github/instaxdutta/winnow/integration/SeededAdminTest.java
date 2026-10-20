package io.github.instaxdutta.winnow.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * The seeded administrator.
 *
 * <p>The migration seeds one administrator so that a fresh install is usable, and the
 * README documents the password of that account. Two things have to hold: the seeded hash
 * really has to verify against the documented password, and the account really has to hold
 * the administrator role.
 *
 * <p>The hash is read out of the migration rather than out of the database, because the
 * bootstrap initializer replaces the seeded password with the configured one on the first
 * start. Reading it from the database would therefore test the bootstrap configuration
 * rather than the migration.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeededAdminTest {

    /** The password the README documents for the seeded administrator. */
    private static final String DOCUMENTED_PASSWORD = "change-me-now";

    /** The seeded hash in the migration. */
    private static final Pattern SEEDED_HASH =
            Pattern.compile("VALUES\\s*\\(\\s*'admin'\\s*,\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    @DisplayName("the hash the migration seeds really verifies against the documented password")
    void theSeededHashVerifiesAgainstTheDocumentedPassword() throws IOException {
        String migration = new String(
                new ClassPathResource("db/migration/V1__baseline.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        Matcher matcher = SEEDED_HASH.matcher(migration);
        assertThat(matcher.find()).as("the migration seeds the administrator").isTrue();
        String seededHash = matcher.group(1);

        assertThat(encoder.matches(DOCUMENTED_PASSWORD, seededHash))
                .as("the seeded hash verifies against '%s'", DOCUMENTED_PASSWORD)
                .isTrue();
    }

    @Test
    @DisplayName("the seeded administrator holds the administrator role")
    void theSeededAdministratorHoldsTheAdministratorRole() {
        AppUser admin = users.findByUsernameAndDeletedAtIsNull("admin").orElse(null);
        assertThat(admin).as("the migration seeds an administrator").isNotNull();
        assertThat(admin.getRoles())
                .as("the migration grants ROLE_ADMIN to the seeded administrator")
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getCode()));
    }
}
