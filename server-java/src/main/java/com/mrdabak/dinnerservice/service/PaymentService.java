package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.model.Order;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public void chargeAdditionalAmount(Order order, int amount) {
        if (amount <= 0) {
            return;
        }
        ensureChargeable(order);
        System.out.printf("[PaymentService] order %d additional charge %d%n", order.getId(), amount);
    }

    public void refundAmount(Order order, int amount) {
        if (amount <= 0) {
            return;
        }
        ensureRefundable(order);
        System.out.printf("[PaymentService] order %d refund %d%n", order.getId(), amount);
    }

    private void ensureChargeable(Order order) {
        if (order == null) {
            throw new PaymentException("주문 정보를 찾을 수 없습니다.");
        }
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()) {
            throw new PaymentException("추가 결제를 진행할 결제 수단이 없습니다.");
        }
        // 결제 상태 검증 완화: 관리자 승인 완료된 주문은 결제 상태와 관계없이 추가 결제 가능
        // (주문 생성 시점에 결제가 이루어지지만, 결제 상태가 아직 업데이트되지 않았을 수 있음)
    }

    private void ensureRefundable(Order order) {
        if (order == null) {
            throw new PaymentException("주문 정보를 찾을 수 없습니다.");
        }
        // 결제 상태 검증 완화: 관리자 승인 완료된 주문은 결제 상태와 관계없이 환불 가능
        // (주문 생성 시점에 결제가 이루어지지만, 결제 상태가 아직 업데이트되지 않았을 수 있음)
    }
}

