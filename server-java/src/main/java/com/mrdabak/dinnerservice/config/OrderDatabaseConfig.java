package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.model.OrderChangeRequest;
import com.mrdabak.dinnerservice.model.OrderChangeRequestItem;
import com.mrdabak.dinnerservice.model.OrderItem;
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
    basePackages = "com.mrdabak.dinnerservice.repository.order",
    entityManagerFactoryRef = "orderEntityManagerFactory",
    transactionManagerRef = "orderTransactionManager"
)
public class OrderDatabaseConfig {

    @Bean(name = "orderDataSource")
    public DataSource orderDataSource() {
        // Ensure data directory exists
        ensureDataDirectory();
        
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(60_000); // Increase to 60 seconds
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        // Add WAL mode and busy timeout to URL as well for compatibility
        dataSource.setUrl("jdbc:sqlite:data/orders.db?journal_mode=WAL&busy_timeout=60000");
        return dataSource;
    }
    
    private void ensureDataDirectory() {
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[OrderDatabaseConfig] Created data directory");
            } else {
                System.err.println("[OrderDatabaseConfig] Failed to create data directory");
            }
        }
    }

    @Bean(name = "orderEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("orderDataSource") DataSource dataSource) {
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.id.new_generator_mappings", "false");
        properties.put("hibernate.jdbc.use_get_generated_keys", "false");

        return builder
            .dataSource(dataSource)
            .packages(Order.class, OrderItem.class, OrderChangeRequest.class, OrderChangeRequestItem.class)
            .persistenceUnit("order")
            .properties(properties)
            .build();
    }

    @Bean(name = "orderTransactionManager")
    public PlatformTransactionManager orderTransactionManager(
            @Qualifier("orderEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

