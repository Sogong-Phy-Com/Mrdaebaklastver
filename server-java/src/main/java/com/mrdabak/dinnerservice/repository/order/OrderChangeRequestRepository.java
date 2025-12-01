package com.mrdabak.dinnerservice.repository.order;

import com.mrdabak.dinnerservice.model.OrderChangeRequest;
import com.mrdabak.dinnerservice.model.OrderChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderChangeRequestRepository extends JpaRepository<OrderChangeRequest, Long> {

    List<OrderChangeRequest> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    Optional<OrderChangeRequest> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<OrderChangeRequestStatus> statuses);

    List<OrderChangeRequest> findByStatusOrderByRequestedAtDesc(OrderChangeRequestStatus status);

    List<OrderChangeRequest> findByStatusInAndRequestedAtBetween(Collection<OrderChangeRequestStatus> statuses,
                                                                 LocalDateTime from,
                                                                 LocalDateTime to);

    List<OrderChangeRequest> findByRequestedAtBetween(LocalDateTime from, LocalDateTime to);

    List<OrderChangeRequest> findAllByOrderByRequestedAtDesc();

    /**
     * 주문 ID와 사용자 ID로 활성 상태(REQUESTED, PAYMENT_FAILED, REFUND_FAILED)의 변경 요청을 조회합니다.
     * PENDING 상태의 변경 요청은 한 주문당 최대 1개만 존재해야 하므로, Optional로 반환합니다.
     */
    Optional<OrderChangeRequest> findByOrderIdAndUserIdAndStatusIn(Long orderId, Long userId, Collection<OrderChangeRequestStatus> statuses);
}

