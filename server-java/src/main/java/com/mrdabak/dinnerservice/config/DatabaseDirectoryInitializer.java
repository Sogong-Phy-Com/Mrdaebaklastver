package com.mrdabak.dinnerservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DatabaseDirectoryInitializer {

    @PostConstruct
    public void init() {
        // Create data directory if it doesn't exist
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[DatabaseDirectoryInitializer] Created data directory: " + dataDir.getAbsolutePath());
            } else {
                System.err.println("[DatabaseDirectoryInitializer] Failed to create data directory: " + dataDir.getAbsolutePath());
            }
        } else {
            System.out.println("[DatabaseDirectoryInitializer] Data directory already exists: " + dataDir.getAbsolutePath());
        }
    }
}

