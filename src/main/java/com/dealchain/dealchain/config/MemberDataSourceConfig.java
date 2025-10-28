package com.dealchain.dealchain.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary; // Member DB를 주 데이터소스로 지정
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
        basePackages = "com.dealchain.dealchain.domain.member", // Member Repositories 경로
        entityManagerFactoryRef = "memberEntityManagerFactory",
        transactionManagerRef = "memberTransactionManager"
)
public class MemberDataSourceConfig {

    // 1. DataSource 정의 (application.properties의 spring.datasource.member 접두사 사용)
    @Primary
    @Bean(name = "memberDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.member")
    public DataSource memberDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 2. EntityManagerFactory 정의 (엔티티 패키지 스캔)
    @Primary
    @Bean(name = "memberEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean memberEntityManagerFactory(
            @Qualifier("memberDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        // Member.java가 com.dealchain.dealchain.domain.member 패키지 바로 아래에 있으므로,
        // 해당 패키지를 스캔하거나, 더 넓은 범위를 스캔해야 합니다.
        factory.setPackagesToScan("com.dealchain.dealchain.domain.member");
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

        // JPA 속성 설정 (application.properties의 spring.jpa.member 설정 사용)
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        factory.setJpaPropertyMap(properties);

        return factory;
    }

    // 3. TransactionManager 정의
    @Primary
    @Bean(name = "memberTransactionManager")
    public PlatformTransactionManager memberTransactionManager(
            @Qualifier("memberEntityManagerFactory") LocalContainerEntityManagerFactoryBean memberEntityManagerFactory) {
        return new JpaTransactionManager(memberEntityManagerFactory.getObject());
    }
}