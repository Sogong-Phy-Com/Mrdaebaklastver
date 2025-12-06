package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    public DataInitializer(DinnerTypeRepository dinnerTypeRepository, MenuItemRepository menuItemRepository,
                          DinnerMenuItemRepository dinnerMenuItemRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Qualifier("dataSource") DataSource dataSource) {
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.dinnerMenuItemRepository = dinnerMenuItemRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("[DataInitializer] Starting data initialization...");
        // Wait for Hibernate to finish initializing
        try {
            System.out.println("[DataInitializer] Waiting for Hibernate to initialize...");
            Thread.sleep(3000);
            System.out.println("[DataInitializer] Hibernate initialization wait complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Database migration: Add columns to inventory_reservations table
        // Do this outside of transaction to avoid lock issues
        int retries = 3;
        boolean migrationSuccess = false;
        while (retries > 0 && !migrationSuccess) {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                
                // Check if table exists
                boolean tableExists = false;
                try (ResultSet tables = connection.getMetaData().getTables(null, null, "inventory_reservations", null)) {
                    tableExists = tables.next();
                }
                
                if (!tableExists) {
                    System.out.println("[DataInitializer] inventory_reservations table does not exist yet. Migration skipped.");
                    migrationSuccess = true;
                } else {
                    DatabaseMetaData metaData = connection.getMetaData();
                    
                    // Check and add consumed column
                    boolean hasConsumed = false;
                    boolean hasExpiresAt = false;
                    try (ResultSet columns = metaData.getColumns(null, null, "inventory_reservations", null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            if ("consumed".equalsIgnoreCase(columnName)) {
                                hasConsumed = true;
                            }
                            if ("expires_at".equalsIgnoreCase(columnName)) {
                                hasExpiresAt = true;
                            }
                        }
                    }
                    
                    // Check menu_inventory table for ordered_quantity column
                    boolean hasOrderedQuantity = false;
                    try (ResultSet columns = metaData.getColumns(null, null, "menu_inventory", null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            if ("ordered_quantity".equalsIgnoreCase(columnName)) {
                                hasOrderedQuantity = true;
                            }
                        }
                    }
                    
                    try (Statement stmt = connection.createStatement()) {
                        if (!hasConsumed) {
                            stmt.execute("ALTER TABLE inventory_reservations ADD COLUMN consumed INTEGER DEFAULT 0");
                            System.out.println("[DataInitializer] Added 'consumed' column to inventory_reservations table");
                        }
                        if (!hasExpiresAt) {
                            stmt.execute("ALTER TABLE inventory_reservations ADD COLUMN expires_at TEXT");
                            System.out.println("[DataInitializer] Added 'expires_at' column to inventory_reservations table");
                        }
                        if (!hasOrderedQuantity) {
                            stmt.execute("ALTER TABLE menu_inventory ADD COLUMN ordered_quantity INTEGER DEFAULT 0");
                            System.out.println("[DataInitializer] Added 'ordered_quantity' column to menu_inventory table");
                        }
                    }
                    migrationSuccess = true;
                }
            } catch (Exception e) {
                retries--;
                if (retries > 0) {
                    System.out.println("[DataInitializer] Database migration retry (" + retries + " attempts remaining)...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("[DataInitializer] Database migration error: " + e.getMessage());
                    e.printStackTrace();
                    // Continue even if migration fails (table might not exist yet)
                }
            }
        }
        // Wait a bit to ensure Hibernate has finished initializing
        try {
            System.out.println("[DataInitializer] Waiting before user data initialization...");
            Thread.sleep(2000);
            System.out.println("[DataInitializer] Wait complete, starting user data initialization");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Ensure consent and admin approval columns exist, and migrate schema
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            DatabaseMetaData metaData = connection.getMetaData();
            try (Statement stmt = connection.createStatement()) {
                // Add consent column (unified)
                if (!hasColumn(metaData, "users", "consent")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN consent INTEGER DEFAULT 0");
                    System.out.println("[DataInitializer] Added 'consent' column to users table");
                }
                
                // Migrate from old consent columns to new unified consent
                if (hasColumn(metaData, "users", "consent_name") && hasColumn(metaData, "users", "consent")) {
                    // Update consent based on old columns (if all three were true, set consent to true)
                    stmt.execute("UPDATE users SET consent = 1 WHERE consent_name = 1 AND consent_address = 1 AND consent_phone = 1");
                    System.out.println("[DataInitializer] Migrated old consent columns to unified consent");
                }
                
                if (!hasColumn(metaData, "users", "loyalty_consent")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN loyalty_consent INTEGER DEFAULT 0");
                    System.out.println("[DataInitializer] Added 'loyalty_consent' column to users table");
                }
                if (!hasColumn(metaData, "orders", "admin_approval_status")) {
                    stmt.execute("ALTER TABLE orders ADD COLUMN admin_approval_status TEXT DEFAULT 'APPROVED'");
                    System.out.println("[DataInitializer] Added 'admin_approval_status' column to orders table");
                }
                stmt.execute("UPDATE orders SET admin_approval_status = 'APPROVED' WHERE admin_approval_status IS NULL OR admin_approval_status = ''");
                
                // Fix invalid created_at values (milliseconds timestamps or invalid formats)
                try {
                    // Update rows where created_at is a number (milliseconds) or invalid format
                    stmt.execute("""
                        UPDATE users 
                        SET created_at = NULL 
                        WHERE created_at IS NOT NULL 
                        AND (typeof(created_at) = 'integer' 
                             OR (typeof(created_at) = 'text' AND created_at GLOB '[0-9]*' AND length(created_at) > 10)
                             OR (typeof(created_at) = 'text' AND created_at NOT LIKE '%-%-% %:%:%'))
                        """);
                    System.out.println("[DataInitializer] Fixed invalid created_at values in users table");
                } catch (Exception fixError) {
                    System.err.println("[DataInitializer] Error fixing created_at values: " + fixError.getMessage());
                    // Continue - not critical
                }
                
                // Make name, address, phone nullable by recreating table (SQLite limitation)
                // Check if columns are already nullable by checking if we can insert NULL
                try {
                    // Try to update a test row with NULL to see if constraint exists
                    // If this fails, we need to recreate the table
                    boolean needsMigration = false;
                    try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
                        while (rs.next()) {
                            String colName = rs.getString("name");
                            int notNull = rs.getInt("notnull");
                            if (("name".equals(colName) || "address".equals(colName) || "phone".equals(colName)) && notNull == 1) {
                                needsMigration = true;
                                break;
                            }
                        }
                    }
                    
                    if (needsMigration) {
                        System.out.println("[DataInitializer] Migrating users table to make name, address, phone nullable...");
                        // Create new table with nullable columns
                        stmt.execute("""
                            CREATE TABLE users_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                email TEXT UNIQUE NOT NULL,
                                password TEXT NOT NULL,
                                name TEXT,
                                address TEXT,
                                phone TEXT,
                                consent INTEGER DEFAULT 0,
                                loyalty_consent INTEGER DEFAULT 0,
                                role TEXT NOT NULL DEFAULT 'customer',
                                approval_status TEXT DEFAULT 'approved',
                                employee_type TEXT,
                                security_question TEXT,
                                security_answer TEXT,
                                card_number TEXT,
                                card_expiry TEXT,
                                card_cvv TEXT,
                                card_holder_name TEXT,
                                created_at TEXT
                            )
                            """);
                        
                        // Copy data - handle created_at conversion
                        // Fix created_at: if it's a number (milliseconds), convert to NULL or current timestamp
                        stmt.execute("""
                            INSERT INTO users_new 
                            (id, email, password, name, address, phone, consent, loyalty_consent, role, approval_status, 
                             employee_type, security_question, security_answer, card_number, card_expiry, card_cvv, 
                             card_holder_name, created_at)
                            SELECT 
                                id, email, password, name, address, phone, 
                                COALESCE(consent, CASE WHEN consent_name = 1 AND consent_address = 1 AND consent_phone = 1 THEN 1 ELSE 0 END) as consent,
                                COALESCE(loyalty_consent, 0) as loyalty_consent,
                                role, approval_status, employee_type, security_question, security_answer,
                                card_number, card_expiry, card_cvv, card_holder_name,
                                CASE 
                                    WHEN created_at IS NULL THEN NULL
                                    WHEN typeof(created_at) = 'integer' THEN NULL
                                    WHEN typeof(created_at) = 'text' AND created_at GLOB '[0-9]*' AND length(created_at) > 10 THEN NULL
                                    WHEN typeof(created_at) = 'text' AND created_at NOT LIKE '%-%-% %:%:%' THEN NULL
                                    ELSE created_at
                                END as created_at
                            FROM users
                            """);
                        
                        // Drop old table
                        stmt.execute("DROP TABLE users");
                        
                        // Rename new table
                        stmt.execute("ALTER TABLE users_new RENAME TO users");
                        
                        System.out.println("[DataInitializer] Successfully migrated users table - name, address, phone are now nullable");
                    }
                } catch (Exception migrationError) {
                    System.err.println("[DataInitializer] Error during table migration: " + migrationError.getMessage());
                    // Continue - Hibernate might handle this
                }
            }
        } catch (Exception e) {
            System.err.println("[DataInitializer] Error ensuring consent/admin approval columns: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Update existing users' approvalStatus if null
        try {
            System.out.println("[DataInitializer] Updating user approval status...");
            updateUserApprovalStatus();
            System.out.println("[DataInitializer] Creating default accounts...");
            createDefaultAccounts();
            System.out.println("[DataInitializer] User data initialization complete");
        } catch (Exception e) {
            System.err.println("[DataInitializer] Error initializing user data: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("[DataInitializer] Checking if data already exists...");
            if (dinnerTypeRepository.count() > 0) {
                System.out.println("[DataInitializer] Data already initialized, skipping seed data");
                return; // Data already initialized
            }
            System.out.println("[DataInitializer] No existing data found, starting seed data insertion");
        } catch (Exception e) {
            System.err.println("[DataInitializer] Error checking dinner types: " + e.getMessage());
            // Continue to initialize data
        }

        // Insert menu items
        System.out.println("[DataInitializer] Inserting menu items...");
        MenuItem wine = new MenuItem(null, "와인", "Wine", 15000, "drink");
        MenuItem champagneItem = new MenuItem(null, "샴페인", "Champagne", 50000, "drink");
        MenuItem coffee = new MenuItem(null, "커피", "Coffee", 5000, "drink");
        MenuItem steak = new MenuItem(null, "스테이크", "Steak", 35000, "food");
        MenuItem salad = new MenuItem(null, "샐러드", "Salad", 12000, "food");
        MenuItem eggs = new MenuItem(null, "에그 스크램블", "Scrambled Eggs", 8000, "food");
        MenuItem bacon = new MenuItem(null, "베이컨", "Bacon", 10000, "food");
        MenuItem bread = new MenuItem(null, "빵", "Bread", 5000, "food");
        MenuItem baguette = new MenuItem(null, "바게트빵", "Baguette", 6000, "food");
        
        wine = menuItemRepository.save(wine);
        champagneItem = menuItemRepository.save(champagneItem);
        coffee = menuItemRepository.save(coffee);
        steak = menuItemRepository.save(steak);
        salad = menuItemRepository.save(salad);
        eggs = menuItemRepository.save(eggs);
        bacon = menuItemRepository.save(bacon);
        bread = menuItemRepository.save(bread);
        baguette = menuItemRepository.save(baguette);

        // Insert dinner types
        DinnerType valentine = new DinnerType(null, "발렌타인 디너", "Valentine Dinner", 60000,
                "와인과 스테이크가 하트 모양 접시와 큐피드 장식과 함께 제공");
        DinnerType french = new DinnerType(null, "프렌치 디너", "French Dinner", 70000,
                "커피, 와인, 샐러드, 스테이크 제공");
        DinnerType english = new DinnerType(null, "잉글리시 디너", "English Dinner", 65000,
                "에그 스크램블, 베이컨, 빵, 스테이크 제공");
        DinnerType champagneDinner = new DinnerType(null, "샴페인 축제 디너", "Champagne Feast Dinner", 120000,
                "2인 식사, 샴페인 1병, 바게트빵 4개, 커피 포트 1개, 와인, 스테이크");
        
        valentine = dinnerTypeRepository.save(valentine);
        french = dinnerTypeRepository.save(french);
        english = dinnerTypeRepository.save(english);
        champagneDinner = dinnerTypeRepository.save(champagneDinner);

        // Insert dinner menu items
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, valentine.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, valentine.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), coffee.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), salad.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), eggs.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), bacon.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), bread.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), champagneItem.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), baguette.getId(), 4));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), coffee.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), steak.getId(), 1));

        System.out.println("[DataInitializer] Initial data seeded successfully");
        System.out.println("[DataInitializer] Data initialization complete");
    }

    @Transactional("transactionManager")
    private void updateUserApprovalStatus() {
        userRepository.findAll().forEach(user -> {
            if (user.getApprovalStatus() == null || user.getApprovalStatus().isEmpty()) {
                user.setApprovalStatus("approved");
                if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
                    user.setSecurityQuestion("내 어릴적 별명은?");
                    user.setSecurityAnswer("asd");
                }
                userRepository.save(user);
            }
        });
    }
    
    @Transactional("transactionManager")
    private void createDefaultAccounts() {
        // Delete and recreate admin account
        userRepository.findByEmail("admin@mrdabak.com").ifPresent(user -> userRepository.delete(user));
        createEmployeeAccount("admin@mrdabak.com", "admin123", "Admin", "Seoul", "010-0000-0000", "admin");
        
        // Create 10 employee accounts
        // First 5 are for cooking, last 5 are for delivery (can be changed by admin)
        for (int i = 1; i <= 10; i++) {
            String email = "emp" + i + "@emp.com";
            String name = "직원" + i;
            String phone = "010-" + String.format("%04d", i * 1111);
            createEmployeeAccount(email, "emp123", name, "Seoul", phone, "employee");
        }
        
        // Create test customer account
        createCustomerAccount("rptmxm@rptmxm.com", "rptmxm", "테스트 고객", "Seoul", "010-1234-5678");
    }
    
    private void createCustomerAccount(String email, String password, String name, String address, String phone) {
        if (!userRepository.existsByEmail(email)) {
            User customer = new User();
            customer.setEmail(email);
            customer.setPassword(passwordEncoder.encode(password));
            customer.setName(name);
            customer.setAddress(address);
            customer.setPhone(phone);
            customer.setRole("customer");
            customer.setApprovalStatus("approved");
            customer.setSecurityQuestion("내 어릴적 별명은?");
            customer.setSecurityAnswer("asd");
            // 테스트용 더미 카드 정보 추가
            customer.setCardNumber("1234-5678-9012-3456");
            customer.setCardExpiry("12/25");
            customer.setCardCvv("123");
            customer.setCardHolderName(name);
            userRepository.save(customer);
            System.out.println("[DataInitializer] 고객 계정 생성: " + email);
        } else {
            // 기존 계정에 카드 정보가 없으면 더미 카드 정보 추가
            userRepository.findByEmail(email).ifPresent(user -> {
                if (user.getCardNumber() == null || user.getCardNumber().isEmpty()) {
                    user.setCardNumber("1234-5678-9012-3456");
                    user.setCardExpiry("12/25");
                    user.setCardCvv("123");
                    user.setCardHolderName(user.getName() != null ? user.getName() : "Test User");
                    userRepository.save(user);
                    System.out.println("[DataInitializer] 기존 고객 계정에 더미 카드 정보 추가: " + email);
                }
            });
            System.out.println("[DataInitializer] 고객 계정이 이미 존재합니다: " + email);
        }
    }

    private void createEmployeeAccount(String email, String password, String name, String address, String phone, String role) {
        if (!userRepository.existsByEmail(email)) {
            User employee = new User();
            employee.setEmail(email);
            employee.setPassword(passwordEncoder.encode(password));
            employee.setName(name);
            employee.setAddress(address);
            employee.setPhone(phone);
            employee.setRole(role);
            employee.setApprovalStatus("approved"); // 관리자/직원 계정은 자동 승인
            employee.setSecurityQuestion("내 어릴적 별명은?");
            employee.setSecurityAnswer("asd");
            // 직원/관리자는 모든 개인정보 동의 자동 설정
            employee.setConsent(true);
            employee.setLoyaltyConsent(true);
            // 테스트용 더미 카드 정보 추가
            employee.setCardNumber("1234-5678-9012-3456");
            employee.setCardExpiry("12/25");
            employee.setCardCvv("123");
            employee.setCardHolderName(name);
            userRepository.save(employee);
        } else {
            // 기존 사용자의 approvalStatus 및 보안 질문 업데이트
            userRepository.findByEmail(email).ifPresent(user -> {
                boolean updated = false;
                if (user.getApprovalStatus() == null || user.getApprovalStatus().isEmpty()) {
                    user.setApprovalStatus("approved");
                    updated = true;
                }
                if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
                    user.setSecurityQuestion("내 어릴적 별명은?");
                    user.setSecurityAnswer("asd");
                    updated = true;
                }
                // 카드 정보가 없으면 더미 카드 정보 추가
                if (user.getCardNumber() == null || user.getCardNumber().isEmpty()) {
                    user.setCardNumber("1234-5678-9012-3456");
                    user.setCardExpiry("12/25");
                    user.setCardCvv("123");
                    user.setCardHolderName(user.getName() != null ? user.getName() : "Test User");
                    updated = true;
                }
                if (updated) {
                    userRepository.save(user);
                }
            });
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}

