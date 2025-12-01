package com.mrdabak.dinnerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRestockRequest {

    @NotNull
    @Min(1)
    @JsonProperty("capacity_per_window")
    private Integer capacityPerWindow;

    private String notes;
}

