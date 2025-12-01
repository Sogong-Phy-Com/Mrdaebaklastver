package com.mrdabak.dinnerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationChangeRequestItemResponseDto {

    @JsonProperty("menu_item_id")
    private Long menuItemId;

    private Integer quantity;

    private String name;

    @JsonProperty("unit_price")
    private Integer unitPrice;

    @JsonProperty("line_total")
    private Integer lineTotal;
}

