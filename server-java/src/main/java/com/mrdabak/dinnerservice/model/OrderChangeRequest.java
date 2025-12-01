package com.mrdabak.dinnerservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_change_requests", indexes = {
        @Index(name = "idx_change_request_order", columnList = "order_id"),
        @Index(name = "idx_change_request_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderChangeRequestStatus status = OrderChangeRequestStatus.REQUESTED;

    @Column(name = "new_dinner_type_id", nullable = false)
    private Long newDinnerTypeId;

    @Column(name = "new_serving_style", nullable = false)
    private String newServingStyle;

    @Column(name = "new_delivery_time", nullable = false)
    private String newDeliveryTime;

    @Column(name = "new_delivery_address", nullable = false)
    private String newDeliveryAddress;

    @Column(name = "original_total_amount", nullable = false)
    private Integer originalTotalAmount;

    @Column(name = "recalculated_amount", nullable = false)
    private Integer recalculatedAmount;

    @Column(name = "change_fee_amount", nullable = false)
    private Integer changeFeeAmount;

    @Column(name = "new_total_amount", nullable = false)
    private Integer newTotalAmount;

    @Column(name = "already_paid_amount", nullable = false)
    private Integer alreadyPaidAmount;

    @Column(name = "extra_charge_amount", nullable = false)
    private Integer extraChargeAmount;

    @Column(name = "change_fee_applied")
    private Boolean changeFeeApplied = Boolean.FALSE;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderChangeRequestItem> items = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        updatedAt = requestedAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderChangeRequestItem item) {
        items.add(item);
        item.setChangeRequest(this);
    }

    public boolean requiresAdditionalCharge() {
        return extraChargeAmount != null && extraChargeAmount > 0;
    }

    public boolean requiresRefund() {
        return extraChargeAmount != null && extraChargeAmount < 0;
    }
}

