package com.mrdabak.dinnerservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "dinner_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DinnerType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    private String description;
}

