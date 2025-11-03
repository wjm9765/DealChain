package com.dealchain.dealchain.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.dealchain.dealchain.domain.contract", // Contract Repositories 스캔 경로
        entityManagerFactoryRef = "contractEntityManagerFactory",
        transactionManagerRef = "contractTransactionManager"
)
public class ContractDataSourceConfig {

    // 1. DataSource 정의 (spring.datasource.contract 접두사 사용)
    @Bean(name = "contractDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.contract")
    public DataSource contractDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 2. EntityManagerFactory 정의 (Contract Entity 스캔)
    @Bean(name = "contractEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean contractEntityManagerFactory(
            @Qualifier("contractDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.dealchain.dealchain.domain.contract.entity"); // Contract Entities 스캔 경로

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

        // JPA 속성 설정
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        factory.setJpaPropertyMap(properties);

        return factory;
    }

    // 3. TransactionManager 정의
    @Bean(name = "contractTransactionManager")
    public PlatformTransactionManager contractTransactionManager(
            @Qualifier("contractEntityManagerFactory") LocalContainerEntityManagerFactoryBean contractEntityManagerFactory) {
        return new JpaTransactionManager(contractEntityManagerFactory.getObject());
    }
}
