package com.mrdabak.dinnerservice.model;

import com.mrdabak.dinnerservice.util.DeliveryTimeUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "dinner_type_id", nullable = false)
    private Long dinnerTypeId;

    @Column(name = "serving_style", nullable = false)
    private String servingStyle;

    @Column(name = "delivery_time", nullable = false)
    private String deliveryTime;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private String status = "pending";

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "pending";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "cooking_employee_id")
    private Long cookingEmployeeId;

    @Column(name = "delivery_employee_id")
    private Long deliveryEmployeeId;

    @Column(name = "admin_approval_status")
    private String adminApprovalStatus = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public LocalDateTime getDeliveryDateTime() {
        return DeliveryTimeUtils.parseDeliveryTime(this.deliveryTime);
    }

    public LocalDate getReservationDate() {
        return getDeliveryDateTime().toLocalDate();
    }

    public boolean canModify(LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now(DEFAULT_ZONE);
        if (!"APPROVED".equalsIgnoreCase(adminApprovalStatus)) {
            return false;
        }
        if ("delivered".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            return false;
        }
        return !isChangeWindowClosed(today);
    }

    public boolean requiresChangeFee(LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now(DEFAULT_ZONE);
        LocalDate reservationDate = getReservationDate();
        LocalDate feeWindowStart = reservationDate.minusDays(3);
        LocalDate feeWindowEndExclusive = reservationDate.minusDays(1);
        return today.isAfter(feeWindowStart) && today.isBefore(feeWindowEndExclusive);
    }

    public boolean isChangeWindowClosed(LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now(DEFAULT_ZONE);
        LocalDate deadline = getReservationDate().minusDays(1);
        return !today.isBefore(deadline);
    }
}

