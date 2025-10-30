package com.dealchain.dealchain.config;


import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.dealchain.dealchain.domain.DealTracking.repository",   // deal 리포지토리 패키지 추가해야됨
        entityManagerFactoryRef = "dealEntityManager",
        transactionManagerRef = "dealTransactionManager"
)
public class DealTrackingDataSourceConfig {

    @Bean(name = "dealDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.deal")
    public DataSource dealDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dealEntityManager")
    public LocalContainerEntityManagerFactoryBean dealEntityManager(@Qualifier("dealDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.dealchain.dealchain.domain.DealTracking.entity"); // deal 엔티티 패키지 추가해야됨

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        // 필요시 추가 JPA 속성 설정
        em.setJpaPropertyMap(props);

        return em;
    }

    @Bean(name = "dealTransactionManager")
    public PlatformTransactionManager dealTransactionManager(@Qualifier("dealEntityManager") LocalContainerEntityManagerFactoryBean dealEntityManager) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(dealEntityManager.getObject());
        return tm;
    }
}
