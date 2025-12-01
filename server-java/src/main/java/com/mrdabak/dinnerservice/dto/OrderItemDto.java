package com.mrdabak.dinnerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    @JsonProperty("menu_item_id")
    private Long menuItemId;
    private Integer quantity;
}


