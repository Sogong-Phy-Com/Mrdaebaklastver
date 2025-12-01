package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.service.OrderService;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoiceOrderCheckoutService {

    private final VoiceOrderMapper voiceOrderMapper;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public VoiceOrderCheckoutService(VoiceOrderMapper voiceOrderMapper,
                                     OrderService orderService,
                                     OrderRepository orderRepository) {
        this.voiceOrderMapper = voiceOrderMapper;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order finalizeVoiceOrder(VoiceOrderSession session) {
        if (session.isOrderPlaced()) {
            throw new VoiceOrderException("이미 주문이 완료되었습니다.");
        }
        OrderRequest request = voiceOrderMapper.toOrderRequest(session);
        Order order = orderService.createOrder(session.getUserId(), request);
        order.setPaymentStatus("paid");
        order.setPaymentMethod("voice-bot-card");
        Order saved = orderRepository.save(order);
        session.markOrderPlaced(saved.getId());
        return saved;
    }
}


