package io.github.instaxdutta.winnow.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * The persistence wiring.
 *
 * <p>The entity manager factory scans the two real packages that hold entities and
 * repositories. The wildcard {@code com.pg.*} picks up everything under the tree and
 * breaks as soon as a package is added.
 *
 * <p>Hibernate never creates the schema. Flyway owns it, and Hibernate only validates it in
 * the profiles that run against MySQL.
 */
@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, JpaProperties.class, HibernateProperties.class})
public class JpaConfig {

    /** The two packages that are scanned for entities and repositories. */
    static final String[] ENTITY_PACKAGES = {"io.github.instaxdutta.winnow.domain", "io.github.instaxdutta.winnow.repository"};

    private static final String H2_URL_PREFIX = "jdbc:h2:";

    private static final String H2_DIALECT = "org.hibernate.dialect.H2Dialect";

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, JpaProperties jpaProperties, HibernateProperties hibernateProperties, Environment environment) {
LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPersistenceUnitName("seo");
        factory.setPackagesToScan(ENTITY_PACKAGES);
        // The vendor adapter is what resolves the persistence provider.
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> jpa = new HashMap<>(jpaProperties.getProperties());
        if (environment.getProperty("spring.datasource.url", "").startsWith(H2_URL_PREFIX)) {
            // The test profile runs on H2, where the MySQL dialect and the MySQL DDL do not apply.
            jpa.put("hibernate.dialect", H2_DIALECT);
            jpa.put("hibernate.ddl-auto", "none");
        } else {
            jpa.put("hibernate.ddl-auto", ddlAuto(hibernateProperties));
        }
        jpa.put("hibernate.show_sql", environment.getProperty("seo.jpa.show-sql", Boolean.class, false));
        jpa.put("hibernate.jdbc.time_zone", "UTC");
        factory.setJpaPropertyMap(jpa);
        return factory;
    }

    /**
     * The one transaction manager of the application. It is a {@link org.springframework.transaction.PlatformTransactionManager},
     * so a bean of that type resolves to it and there is never a second candidate.
     */
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private static String ddlAuto(HibernateProperties hibernateProperties) {
        String configured = hibernateProperties.getDdlAuto();
        return configured == null || configured.isBlank() ? "none" : configured;
    }
}
