package com.mrdabak.dinnerservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "dinner_menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DinnerMenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "dinner_type_id", nullable = false)
    private Long dinnerTypeId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private Integer quantity = 1;
}

