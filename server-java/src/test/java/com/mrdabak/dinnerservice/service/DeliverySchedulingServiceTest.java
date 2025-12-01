package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.model.DeliverySchedule;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.repository.schedule.DeliveryScheduleRepository;
import com.mrdabak.dinnerservice.service.TravelTimeEstimator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliverySchedulingServiceTest {

    @Mock
    private DeliveryScheduleRepository deliveryScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelTimeEstimator travelTimeEstimator;

    private DeliverySchedulingService deliverySchedulingService;

    @BeforeEach
    void setUp() {
        deliverySchedulingService = new DeliverySchedulingService(
                deliveryScheduleRepository,
                userRepository,
                travelTimeEstimator,
                "15:00",
                "22:00"
        );
    }

    @Test
    void commitAssignmentForOrderPreventsOverlappingSchedules() {
        Long orderId = 1L;
        Long employeeId = 99L;
        LocalDateTime deliveryTime = LocalDateTime.of(2025, 5, 10, 18, 0);

        when(travelTimeEstimator.estimateOneWayMinutes(anyString(), any())).thenReturn(30);
        when(deliveryScheduleRepository.findByOrderId(orderId)).thenReturn(Optional.of(new DeliverySchedule()));
        when(deliveryScheduleRepository.existsActiveOverlap(eq(employeeId), eq(orderId), any(), any()))
                .thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                deliverySchedulingService.commitAssignmentForOrder(orderId, employeeId, deliveryTime, "서울시 성동구 테스트로 1"));

        verify(deliveryScheduleRepository, never()).save(any());
    }
}

