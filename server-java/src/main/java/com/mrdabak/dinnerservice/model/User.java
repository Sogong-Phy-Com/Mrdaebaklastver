package com.mrdabak.dinnerservice.model;

import com.mrdabak.dinnerservice.util.LocalDateTimeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String name;

    @Column(nullable = true)
    private String address;

    @Column(nullable = true)
    private String phone;

    @Column(name = "consent")
    private Boolean consent = Boolean.FALSE;

    @Column(name = "loyalty_consent")
    private Boolean loyaltyConsent = Boolean.FALSE;

    @Column(nullable = false)
    private String role = "customer";

    @Column(name = "approval_status")
    private String approvalStatus = "approved"; // pending, approved, rejected

    @Column(name = "employee_type")
    private String employeeType; // cooking, delivery

    @Column(name = "security_question")
    private String securityQuestion; // For password recovery

    @Column(name = "security_answer")
    private String securityAnswer; // For password recovery

    @Column(name = "card_number")
    private String cardNumber; // Credit card number (encrypted in production)

    @Column(name = "card_expiry")
    private String cardExpiry; // Card expiry date (MM/YY)

    @Column(name = "card_cvv")
    private String cardCvv; // Card CVV (encrypted in production)

    @Column(name = "card_holder_name")
    private String cardHolderName; // Card holder name

    @Column(name = "created_at", nullable = true)
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

