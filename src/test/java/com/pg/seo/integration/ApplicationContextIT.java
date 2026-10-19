package com.pg.seo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.pg.seo.config.FileSecretStore;
import com.pg.seo.config.SecretStore;
import com.pg.seo.extraction.DomainCrawler;
import com.pg.seo.extraction.UrlAllowList;
import com.pg.seo.mail.MailTemplateRenderer;
import com.pg.seo.security.SecurityConfig;
import com.pg.seo.service.CampaignService;
import com.pg.seo.service.ClientGroupService;
import com.pg.seo.service.ClientService;
import com.pg.seo.service.DashboardService;
import com.pg.seo.service.ExtractionService;
import com.pg.seo.service.GmailAccountService;
import com.pg.seo.service.InboxService;
import com.pg.seo.service.OutboxService;
import com.pg.seo.service.SmtpAccountService;
import com.pg.seo.service.TemplateService;
import com.pg.seo.service.UserService;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * The application context.
 *
 * <p>This test is the cheapest way to catch a broken build: an ambiguous bean, a missing
 * bean, an invalid property, a migration that does not apply and an entity whose mapping
 * does not match the schema all fail here, before any behaviour is tested.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void theContextLoads() {
        assertThat(context).isNotNull();
        assertThat(context.getBean(ClientService.class)).isNotNull();
        assertThat(context.getBean(ClientGroupService.class)).isNotNull();
        assertThat(context.getBean(TemplateService.class)).isNotNull();
        assertThat(context.getBean(SmtpAccountService.class)).isNotNull();
        assertThat(context.getBean(GmailAccountService.class)).isNotNull();
        assertThat(context.getBean(CampaignService.class)).isNotNull();
        assertThat(context.getBean(OutboxService.class)).isNotNull();
        assertThat(context.getBean(ExtractionService.class)).isNotNull();
        assertThat(context.getBean(InboxService.class)).isNotNull();
        assertThat(context.getBean(DashboardService.class)).isNotNull();
        assertThat(context.getBean(UserService.class)).isNotNull();
        assertThat(context.getBean(SecurityConfig.class)).isNotNull();
    }

    @Test
    void theContextHasExactlyOnePasswordEncoder() {
        Map<String, PasswordEncoder> encoders = context.getBeansOfType(PasswordEncoder.class);

        assertThat(encoders).hasSize(1);
    }

    @Test
    void theContextHasExactlyOneBcryptPasswordEncoder() {
        Map<String, BCryptPasswordEncoder> encoders = context.getBeansOfType(BCryptPasswordEncoder.class);

        assertThat(encoders).hasSize(1);
        assertThat(encoders.values().iterator().next()).isInstanceOf(PasswordEncoder.class);
    }

    @Test
    void thePasswordEncoderIsBcryptAtCostTwelve() {
        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        // The hash of BCrypt carries its cost factor, so the hash of the encoder proves
        // the cost the application uses.
        assertThat(encoder.encode("test-password-12345")).matches("^\\$2[aby]\\$12\\$.+");
        assertThat(encoder.matches("test-password-12345", encoder.encode("test-password-12345"))).isTrue();
    }

    @Test
    void theSecurityCriticalBeansArePresent() {
        assertThat(context.getBean(SecretStore.class)).isInstanceOf(FileSecretStore.class);
        assertThat(context.getBean(UrlAllowList.class)).isNotNull();
        assertThat(context.getBean(MailTemplateRenderer.class)).isNotNull();
        assertThat(context.getBean(DomainCrawler.class)).isNotNull();
    }

    @Test
    void theFlywayMigrationAppliedToTheSchema() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, description, success from flyway_schema_history"
                        + " where version is not null order by installed_rank");

        assertThat(history).isNotEmpty();
        assertThat(history.get(0).get("version")).isEqualTo("1");
        assertThat(history.get(0).get("description")).isEqualTo("baseline");
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(Boolean.TRUE));
    }

    @Test
    void noMigrationFailed() {
        Integer failed = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);

        assertThat(failed).isZero();
    }

    @Test
    void hibernateNeverCreatesTheSchema() {
        assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("none");
        assertThat(context.getEnvironment().getProperty("spring.flyway.enabled")).isEqualTo("true");
    }

    @Test
    void theTestProfileHoldsTheScheduledJobsBack() {
        // A scheduled job must never run while a test runs: the dispatch job would try to
        // send mail and the extraction job would close the runs a test created. The
        // delays come from the test profile in src/test/resources, which shadows the one
        // in src/main/resources.
        assertThat(context.getEnvironment().getProperty("seo.scheduler.outbox-delay-ms")).isEqualTo("3600000");
        assertThat(context.getEnvironment().getProperty("seo.scheduler.inbox-delay-ms")).isEqualTo("3600000");
        assertThat(context.getEnvironment().getProperty("seo.scheduler.extraction-delay-ms")).isEqualTo("3600000");
    }

    @Test
    void everyTableOfTheMigrationExists() {
        List<String> tables = tables();

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
    void everyMappedColumnExistsInTheSchema() {
        Metamodel metamodel = entityManagerFactory.getMetamodel();
        List<String> missing = new ArrayList<>();

        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> type = entityType.getJavaType();
            jakarta.persistence.Table table = type.getAnnotation(jakarta.persistence.Table.class);
            String tableName = table == null ? type.getSimpleName().toLowerCase(Locale.ROOT) : table.name();
            for (String column : mappedColumns(type)) {
                if (!columnExists(tableName, column)) {
                    missing.add(tableName + "." + column);
                }
            }
        }

        assertThat(missing).isEmpty();
    }

    /** The columns an entity maps, including the ones it inherits. */
    private static List<String> mappedColumns(Class<?> type) {
        List<String> columns = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && !column.name().isEmpty()) {
                    columns.add(column.name());
                }
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null && !joinColumn.name().isEmpty()) {
                    columns.add(joinColumn.name());
                }
            }
        }
        return columns;
    }

    private List<String> tables() {
        return jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where lower(table_schema) = 'public'",
                String.class);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = ? and column_name = ?",
                Integer.class,
                table.toLowerCase(Locale.ROOT),
                column.toLowerCase(Locale.ROOT));
        return count != null && count > 0;
    }

    @Test
    void theDataSourceAnswers() throws Exception {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }
}
