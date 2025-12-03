package com.mrdabak.dinnerservice.voice.service;

import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.repository.UserRepository;
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
    private final UserRepository userRepository;

    public VoiceOrderCheckoutService(VoiceOrderMapper voiceOrderMapper,
                                     OrderService orderService,
                                     OrderRepository orderRepository,
                                     UserRepository userRepository) {
        this.voiceOrderMapper = voiceOrderMapper;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order finalizeVoiceOrder(VoiceOrderSession session) {
        if (session.isOrderPlaced()) {
            throw new VoiceOrderException("이미 주문이 완료되었습니다.");
        }
        
        // 카드 정보 확인
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new VoiceOrderException("사용자 정보를 찾을 수 없습니다."));
        
        if (user.getCardNumber() == null || user.getCardNumber().trim().isEmpty()) {
            throw new VoiceOrderException("주문을 하려면 카드 정보가 필요합니다. 내 정보에서 카드 정보를 등록해주세요.");
        }
        
        OrderRequest request = voiceOrderMapper.toOrderRequest(session);
        System.out.println("[VoiceOrderCheckoutService] 음성 주문 생성 시작 - 사용자 ID: " + session.getUserId());
        System.out.println("[VoiceOrderCheckoutService] 주문 항목 수: " + (request.getItems() != null ? request.getItems().size() : 0));
        
        // 일반 주문과 동일한 로직으로 주문 생성 (재고 예약 포함)
        // 비밀번호 검증 후부터는 일반 주문과 동일하게 취급
        Order order = orderService.createOrder(session.getUserId(), request);
        System.out.println("[VoiceOrderCheckoutService] 주문 생성 완료 - 주문 ID: " + order.getId());
        System.out.println("[VoiceOrderCheckoutService] 음성 주문이 일반 주문과 동일하게 처리됩니다. (재고 예약 포함, adminApprovalStatus: PENDING)");
        
        // 일반 주문과 동일하게 처리 (paymentStatus는 createOrder에서 "pending"으로 설정됨)
        // paymentMethod도 일반 주문과 동일하게 유지
        Order saved = orderRepository.save(order);
        System.out.println("[VoiceOrderCheckoutService] 음성 주문 저장 완료 - 주문 ID: " + saved.getId() + 
                ", paymentStatus: " + saved.getPaymentStatus() + 
                ", adminApprovalStatus: " + saved.getAdminApprovalStatus() + 
                ", 재고 예약은 createOrder에서 처리됨");
        session.markOrderPlaced(saved.getId());
        return saved;
    }
}


