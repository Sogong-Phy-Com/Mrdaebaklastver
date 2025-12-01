package com.mrdabak.dinnerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull
    @JsonProperty("dinner_type_id")
    private Long dinnerTypeId;

    @NotBlank
    @JsonProperty("serving_style")
    private String servingStyle;

    @NotBlank
    @JsonProperty("delivery_time")
    private String deliveryTime;

    @NotBlank
    @JsonProperty("delivery_address")
    private String deliveryAddress;

    @NotNull
    private List<OrderItemDto> items;

    @JsonProperty("payment_method")
    private String paymentMethod;
}


