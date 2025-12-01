package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final OrderRepository orderRepository;

    public PaymentController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@Valid @RequestBody Map<String, Object> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Long orderId = Long.parseLong(request.get("order_id").toString());
        String paymentMethod = request.get("payment_method").toString();

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("paid".equals(order.getPaymentStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order already paid"));
        }

        // In a real application, integrate with payment gateway here
        // For now, simulate successful payment

        order.setPaymentStatus("paid");
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Payment processed successfully",
                "order_id", orderId,
                "payment_status", "paid"
        ));
    }
}




