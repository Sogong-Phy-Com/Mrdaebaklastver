package com.mrdabak.dinnerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationChangeRequestResponseDto {

    private Long id;

    @JsonProperty("order_id")
    private Long orderId;

    private String status;

    @JsonProperty("original_total_amount")
    private Integer originalTotalAmount;

    @JsonProperty("recalculated_amount")
    private Integer recalculatedAmount;

    @JsonProperty("change_fee_amount")
    private Integer changeFeeAmount;

    @JsonProperty("new_total_amount")
    private Integer newTotalAmount;

    @JsonProperty("already_paid_amount")
    private Integer alreadyPaidAmount;

    @JsonProperty("extra_charge_amount")
    private Integer extraChargeAmount;

    @JsonProperty("expected_refund_amount")
    private Integer expectedRefundAmount;

    @JsonProperty("requires_additional_payment")
    private boolean requiresAdditionalPayment;

    @JsonProperty("requires_refund")
    private boolean requiresRefund;

    @JsonProperty("change_fee_applied")
    private boolean changeFeeApplied;

    @JsonProperty("new_dinner_type_id")
    private Long newDinnerTypeId;

    @JsonProperty("new_serving_style")
    private String newServingStyle;

    @JsonProperty("new_delivery_time")
    private String newDeliveryTime;

    @JsonProperty("new_delivery_address")
    private String newDeliveryAddress;

    private String reason;

    @JsonProperty("admin_comment")
    private String adminComment;

    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;

    @JsonProperty("rejected_at")
    private LocalDateTime rejectedAt;

    private List<ReservationChangeRequestItemResponseDto> items;
}

