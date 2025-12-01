package com.mrdabak.dinnerservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_inventory", indexes = {
        @Index(name = "idx_inventory_menu_item", columnList = "menu_item_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "menu_item_id", nullable = false, unique = true)
    private Long menuItemId;

    @Column(name = "capacity_per_window", nullable = false)
    private Integer capacityPerWindow;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 0;

    @Column(name = "last_restocked_at", nullable = false)
    private LocalDateTime lastRestockedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity = 0;

    @PrePersist
    public void onCreate() {
        if (lastRestockedAt == null) {
            lastRestockedAt = LocalDateTime.now();
        }
    }
}

