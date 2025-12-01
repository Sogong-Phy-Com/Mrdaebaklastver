package com.mrdabak.dinnerservice.service;

public record ReservationChangeQuote(int originalPaidAmount,
                                     int recalculatedAmount,
                                     int changeFeeAmount) {

    public int newTotalAmount() {
        return recalculatedAmount + changeFeeAmount;
    }

    public int extraChargeAmount() {
        return newTotalAmount() - originalPaidAmount;
    }

    public int expectedRefundAmount() {
        int delta = extraChargeAmount();
        return delta < 0 ? Math.abs(delta) : 0;
    }

    public boolean requiresAdditionalPayment() {
        return extraChargeAmount() > 0;
    }

    public boolean requiresRefund() {
        return extraChargeAmount() < 0;
    }
}

