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
        basePackages = "com.dealchain.dealchain.domain.product", // Product Repositories 스캔 경로
        entityManagerFactoryRef = "productEntityManagerFactory",
        transactionManagerRef = "productTransactionManager"
)
public class ProductDataSourceConfig {

    // 1. DataSource 정의 (spring.datasource.product 접두사 사용)
    @Bean(name = "productDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.product")
    public DataSource productDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 2. EntityManagerFactory 정의 (Product Entity 스캔)
    @Bean(name = "productEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean productEntityManagerFactory(
            @Qualifier("productDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.dealchain.dealchain.domain.product"); // Product Entities 스캔 경로

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
    @Bean(name = "productTransactionManager")
    public PlatformTransactionManager productTransactionManager(
            @Qualifier("productEntityManagerFactory") LocalContainerEntityManagerFactoryBean productEntityManagerFactory) {
        return new JpaTransactionManager(productEntityManagerFactory.getObject());
    }
}