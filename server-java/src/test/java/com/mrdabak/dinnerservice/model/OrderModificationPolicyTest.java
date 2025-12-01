package com.mrdabak.dinnerservice.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OrderModificationPolicyTest {

    @Test
    void allowsModificationWithoutFeeBeforeThreeDays() {
        Order order = baseOrder();
        LocalDate reference = LocalDate.of(2025, 12, 14);
        assertTrue(order.canModify(reference));
        assertFalse(order.requiresChangeFee(reference));
    }

    @Test
    void appliesChangeFeeBetweenThreeAndOneDay() {
        Order order = baseOrder();
        LocalDate reference = LocalDate.of(2025, 12, 15);
        assertTrue(order.canModify(reference));
        assertTrue(order.requiresChangeFee(reference));
    }

    @Test
    void blocksModificationInsideOneDayWindow() {
        Order order = baseOrder();
        LocalDate reference = LocalDate.of(2025, 12, 16);
        assertTrue(order.requiresChangeFee(LocalDate.of(2025, 12, 15)));
        assertFalse(order.canModify(reference));
    }

    private Order baseOrder() {
        Order order = new Order();
        order.setAdminApprovalStatus("APPROVED");
        order.setStatus("pending");
        order.setDeliveryTime("2025-12-17T18:00");
        return order;
    }
}

