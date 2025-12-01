package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.model.DeliverySchedule;
import com.mrdabak.dinnerservice.model.EmployeeWorkAssignment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.mrdabak.dinnerservice.repository.schedule",
    entityManagerFactoryRef = "scheduleEntityManagerFactory",
    transactionManagerRef = "scheduleTransactionManager"
)
public class ScheduleDatabaseConfig {

    @Bean(name = "scheduleDataSource")
    public DataSource scheduleDataSource() {
        // Ensure data directory exists
        ensureDataDirectory();
        
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(60_000);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl("jdbc:sqlite:data/schedule.db?journal_mode=WAL&busy_timeout=60000");
        return dataSource;
    }
    
    private void ensureDataDirectory() {
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[ScheduleDatabaseConfig] Created data directory");
            } else {
                System.err.println("[ScheduleDatabaseConfig] Failed to create data directory");
            }
        }
    }

    @Bean(name = "scheduleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean scheduleEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("scheduleDataSource") DataSource dataSource) {
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.id.new_generator_mappings", "false");
        properties.put("hibernate.jdbc.use_get_generated_keys", "false");

        return builder
            .dataSource(dataSource)
            .packages(DeliverySchedule.class, EmployeeWorkAssignment.class)
            .persistenceUnit("schedule")
            .properties(properties)
            .build();
    }

    @Bean(name = "scheduleTransactionManager")
    public PlatformTransactionManager scheduleTransactionManager(
            @Qualifier("scheduleEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

