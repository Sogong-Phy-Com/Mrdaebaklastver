package com.mrdabak.dinnerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String address;
    private String phone;
    private String role;
    private String approvalStatus;
    private String employeeType;
    
    public UserDto(Long id, String email, String name, String address, String phone, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.role = role;
        this.approvalStatus = "approved"; // 기본값
    }
    
    public UserDto(Long id, String email, String name, String address, String phone, String role, String approvalStatus) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.role = role;
        this.approvalStatus = approvalStatus;
    }
}




