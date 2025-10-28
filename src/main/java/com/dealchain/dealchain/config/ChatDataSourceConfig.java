package com.dealchain.dealchain.config;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "chatEntityManagerFactory", // 이 설정 클래스가 관리할 EntityManagerFactory 빈의 이름
        transactionManagerRef = "chatTransactionManager",     // 이 설정 클래스가 관리할 TransactionManager 빈의 이름
        basePackages = {"com.dealchain.dealchain.domain.chat.repository"} // Chat 관련 Repository 위치
)
public class ChatDataSourceConfig {
    // 1. DataSource 정의 (application.properties의 spring.datasource.chat 접두사 사용)
    @Bean(name = "chatDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.chat") // chat 접두사 사용
    public DataSource chatDataSource() {
        // DataSourceBuilder를 사용하여 DataSource 생성
        return DataSourceBuilder.create().build();
    }

    // 2. EntityManagerFactory 정의 (엔티티 패키지 스캔)
    @Bean(name = "chatEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean chatEntityManagerFactory(
            @Qualifier("chatDataSource") DataSource dataSource) { // chatDataSource 주입

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.dealchain.dealchain.domain.chat.entity"); // Chat Entity 경로
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

        // JPA 속성 설정 (필요에 따라 application.properties 값 참조 또는 직접 설정)
        Map<String, Object> properties = new HashMap<>();
        // properties.put("hibernate.hbm2ddl.auto", "update"); // Chat DB에 맞는 전략 설정
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect"); // MySQL Dialect 설정
        // properties.put("hibernate.show_sql", "true");
        // properties.put("hibernate.format_sql", "true");
        factory.setJpaPropertyMap(properties);
        factory.setPersistenceUnitName("chat"); // Persistence Unit 이름 설정 (선택적)


        return factory;
    }

    // 3. TransactionManager 정의
    @Bean(name = "chatTransactionManager")
    public PlatformTransactionManager chatTransactionManager(
            @Qualifier("chatEntityManagerFactory") LocalContainerEntityManagerFactoryBean chatEntityManagerFactory) { // chatEntityManagerFactory 주입
        return new JpaTransactionManager(chatEntityManagerFactory.getObject());
    }
}
