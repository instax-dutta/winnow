package com.pg.seo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pg.seo.domain.AppUser;
import com.pg.seo.domain.Client;
import com.pg.seo.domain.ClientGroup;
import com.pg.seo.domain.ExtractionResult;
import com.pg.seo.domain.ExtractionRun;
import com.pg.seo.domain.ExtractionStatus;
import com.pg.seo.domain.UserStatus;
import com.pg.seo.extraction.ExtractionSink;
import com.pg.seo.extraction.ExtractionWorker;
import com.pg.seo.repository.AppUserRepository;
import com.pg.seo.repository.ClientGroupRepository;
import com.pg.seo.repository.ClientRepository;
import com.pg.seo.repository.ExtractionResultRepository;
import com.pg.seo.repository.ExtractionRunRepository;
import com.pg.seo.service.ClientService;
import com.pg.seo.service.ExtractionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * The queries of the repositories against the schema Flyway created.
 *
 * <p>These are the three properties the data access layer rests on: the migration applies
 * to a clean schema, the soft delete predicate hides a row without destroying it, and the
 * unique index on (run_id, email_hash) keeps a run free of duplicates.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryQueryIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientGroupRepository clientGroups;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private ExtractionRunRepository extractionRuns;

    @Autowired
    private ExtractionResultRepository extractionResults;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ExtractionService extractionService;

    private Long ownerUserId;
    private ClientGroup group;
    private Client client;

    @BeforeEach
    void createFixtures() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();

        AppUser user = new AppUser();
        user.setUsername("repository-user-" + token);
        user.setPasswordHash(passwordEncoder.encode("test-password-12345"));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        ownerUserId = users.save(user).getId();

        ClientGroup created = new ClientGroup();
        created.setOwnerUserId(ownerUserId);
        created.setCode("REPO-" + token);
        created.setName("Repository group");
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        group = clientGroups.save(created);

        Client createdClient = new Client();
        createdClient.setOwnerUserId(ownerUserId);
        createdClient.setGroup(group);
        createdClient.setName("Repository client");
        createdClient.setEmail("repository-" + token + "@example.invalid");
        createdClient.setCreatedAt(now);
        createdClient.setUpdatedAt(now);
        client = clients.save(createdClient);
    }

    @Test
    void theFlywayMigrationAppliedToTheSchema() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, description, success from flyway_schema_history"
                        + " where version is not null order by installed_rank");

        assertThat(history).isNotEmpty();
        assertThat(history.get(0).get("version")).isEqualTo("1");
        assertThat(history.get(0).get("success")).isEqualTo(Boolean.TRUE);
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(Boolean.TRUE));
    }

    @Test
    void theSchemaWasCreatedByFlywayAndNotByHibernate() {
        Integer historyTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'flyway_schema_history'",
                Integer.class);
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

        assertThat(historyTable).isEqualTo(1);
        assertThat(ddlAuto).isEqualTo("none");
    }

    @Test
    void everyTableOfTheBaselineMigrationExists() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where lower(table_schema) = 'public'",
                String.class);

        assertThat(tables).contains(
                "app_user",
                "app_role",
                "app_user_role",
                "client_group",
                "client",
                "email_template",
                "smtp_account",
                "gmail_account",
                "campaign",
                "delivery",
                "outbox",
                "extraction_run",
                "extraction_result",
                "inbox_message");
    }

    @Test
    void aSoftDeletedClientIsHiddenFromTheListing() {
        clientService.delete(ownerUserId, client.getId());

        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(ownerUserId)).isEmpty();
    }

    @Test
    void aSoftDeletedClientIsHiddenFromTheCount() {
        clientService.delete(ownerUserId, client.getId());

        assertThat(clients.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId)).isZero();
    }

    @Test
    void aSoftDeletedClientCannotBeReadById() {
        clientService.delete(ownerUserId, client.getId());

        assertThat(clients.findByIdAndOwnerUserIdAndDeletedAtIsNull(client.getId(), ownerUserId)).isEmpty();
    }

    @Test
    void aSoftDeletedClientIsStillInTheTable() {
        clientService.delete(ownerUserId, client.getId());
        // The update is written before the raw query reads the row.
        clients.flush();

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from client where id = ? and deleted_at is not null",
                Integer.class,
                client.getId());

        assertThat(rows).isEqualTo(1);
    }

    @Test
    void aSoftDeletedClientIsHiddenFromThePagedListing() {
        clientService.delete(ownerUserId, client.getId());

        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNull(
                        ownerUserId, PageRequest.of(0, 10))
                .getContent())
                .isEmpty();
    }

    @Test
    void aBatchInsertWithADuplicateEmailHashIsRejectedByTheUniqueIndex() {
        ExtractionRun run = newRun();
        byte[] hash = sha256("duplicate@example.invalid");
        ExtractionResult first = newResult(run, "duplicate@example.invalid", hash);
        ExtractionResult second = newResult(run, "another-address@example.invalid", hash);

        assertThatThrownBy(() -> extractionResults.saveAllAndFlush(List.of(first, second)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theSameEmailHashMayBeStoredInAnotherRun() {
        ExtractionRun first = newRun();
        ExtractionRun second = newRun();
        byte[] hash = sha256("shared@example.invalid");

        extractionResults.saveAllAndFlush(List.of(
                newResult(first, "shared@example.invalid", hash),
                newResult(second, "shared@example.invalid", hash)));

        assertThat(extractionResults.findByRunId(first.getId())).hasSize(1);
        assertThat(extractionResults.findByRunId(second.getId())).hasSize(1);
    }

    @Test
    void theSinkStoresAnAddressThatIsAlreadyInTheRunOnlyOnce() {
        ExtractionRun run = newRun();
        byte[] hash = sha256("once@example.invalid");
        ExtractionSink sink = extractionService.sinkFor(run);

        sink.storeAddresses(List.of(
                new ExtractionWorker.FoundAddress("example.invalid", "once@example.invalid", hash, 1),
                new ExtractionWorker.FoundAddress("example.invalid", "once@example.invalid", hash, 2)));

        assertThat(extractionResults.findByRunId(run.getId())).hasSize(1);
    }

    @Test
    void theSinkSkipsAnAddressThatTheRunAlreadyHolds() {
        ExtractionRun run = newRun();
        byte[] hash = sha256("already@example.invalid");
        extractionResults.save(newResult(run, "already@example.invalid", hash));
        ExtractionSink sink = extractionService.sinkFor(run);

        sink.storeAddresses(List.of(
                new ExtractionWorker.FoundAddress("example.invalid", "already@example.invalid", hash, 1)));

        assertThat(extractionResults.findByRunId(run.getId())).hasSize(1);
    }

    @Test
    void theDistinctDomainsOfARunAreTheDomainsOfThatRun() {
        ExtractionRun run = newRun();
        extractionResults.saveAllAndFlush(List.of(
                newResult(run, "one@example.invalid", sha256("one@example.invalid")),
                newResult(run, "two@example.invalid", sha256("two@example.invalid"))));

        assertThat(extractionResults.findDistinctDomainsByRunId(run.getId())).containsExactly("example.invalid");
    }

    private ExtractionRun newRun() {
        Instant now = Instant.now();
        ExtractionRun run = new ExtractionRun();
        run.setOwnerUserId(ownerUserId);
        run.setStatus(ExtractionStatus.SUCCESS);
        run.setDomainCount(1);
        run.setStartedAt(now);
        run.setFinishedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        return extractionRuns.save(run);
    }

    private ExtractionResult newResult(ExtractionRun run, String email, byte[] hash) {
        Instant now = Instant.now();
        ExtractionResult result = new ExtractionResult();
        result.setRun(run);
        result.setDomain("example.invalid");
        result.setEmail(email);
        result.setEmailHash(hash);
        result.setSourceRow(1);
        result.setCreatedAt(now);
        return result;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }
}
