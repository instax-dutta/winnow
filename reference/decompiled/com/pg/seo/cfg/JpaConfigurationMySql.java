/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zaxxer.hikari.HikariDataSource
 *  jakarta.persistence.EntityManagerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Primary
 *  org.springframework.core.env.Environment
 *  org.springframework.data.jpa.repository.config.EnableJpaRepositories
 *  org.springframework.orm.jpa.JpaTransactionManager
 *  org.springframework.orm.jpa.JpaVendorAdapter
 *  org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
 *  org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
 *  org.springframework.transaction.PlatformTransactionManager
 *  org.springframework.transaction.annotation.EnableTransactionManagement
 */
package com.pg.seo.cfg;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages={"com.pg.*"}, entityManagerFactoryRef="EntityManagerMySql", transactionManagerRef="transactionManagerMySql")
@EnableTransactionManagement
public class JpaConfigurationMySql {
    @Autowired
    private Environment env;

    @Primary
    @Bean
    public DataSource mysqlDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(this.env.getProperty("app.datasource.mysql.driver-class-name"));
        dataSource.setJdbcUrl(this.env.getProperty("app.datasource.mysql.url"));
        dataSource.setUsername(this.env.getProperty("app.datasource.mysql.username"));
        dataSource.setPassword(this.env.getProperty("app.datasource.mysql.password"));
        return dataSource;
    }

    @Primary
    @Bean(value={"EntityManagerMySql"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws NamingException {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(this.mysqlDataSource());
        factoryBean.setPackagesToScan(new String[]{"com.pg.*"});
        factoryBean.setJpaVendorAdapter(this.jpaVendorAdapter());
        factoryBean.setJpaProperties(this.jpaProperties());
        return factoryBean;
    }

    @Primary
    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        return hibernateJpaVendorAdapter;
    }

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.show_sql", "true");
        return properties;
    }

    @Primary
    @Bean
    @Autowired
    public PlatformTransactionManager transactionManagerMySql(EntityManagerFactory emf) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(emf);
        return txManager;
    }
}

