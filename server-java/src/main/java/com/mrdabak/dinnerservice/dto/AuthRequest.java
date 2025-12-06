package com.mrdabak.dinnerservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    private String name;
    private String address;
    private String phone;
    private String role; // customer, employee, admin
    private String securityQuestion; // For password recovery
    private String securityAnswer; // For password recovery
    private Boolean consent;
    private Boolean loyaltyConsent;
}

