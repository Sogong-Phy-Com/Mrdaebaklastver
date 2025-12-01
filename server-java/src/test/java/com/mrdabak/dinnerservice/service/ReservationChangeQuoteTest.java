package com.mrdabak.dinnerservice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservationChangeQuoteTest {

    @Test
    void calculatesExtraChargeWhenNewTotalIsGreater() {
        ReservationChangeQuote quote = new ReservationChangeQuote(100_000, 120_000, 0);
        assertEquals(120_000, quote.newTotalAmount());
        assertEquals(20_000, quote.extraChargeAmount());
        assertTrue(quote.requiresAdditionalPayment());
        assertFalse(quote.requiresRefund());
        assertEquals(0, quote.expectedRefundAmount());
    }

    @Test
    void calculatesRefundWhenNewTotalIsLower() {
        ReservationChangeQuote quote = new ReservationChangeQuote(150_000, 110_000, 0);
        assertEquals(110_000, quote.newTotalAmount());
        assertEquals(-40_000, quote.extraChargeAmount());
        assertFalse(quote.requiresAdditionalPayment());
        assertTrue(quote.requiresRefund());
        assertEquals(40_000, quote.expectedRefundAmount());
    }

    @Test
    void changeFeeIsIncludedInNewTotal() {
        ReservationChangeQuote quote = new ReservationChangeQuote(100_000, 110_000, 30_000);
        assertEquals(140_000, quote.newTotalAmount());
        assertEquals(40_000, quote.extraChargeAmount());
        assertTrue(quote.requiresAdditionalPayment());
    }
}

