package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.dto.ReservationChangeRequestCreateDto;
import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.DinnerTypeRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.repository.order.OrderChangeRequestItemRepository;
import com.mrdabak.dinnerservice.repository.order.OrderChangeRequestRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderChangeRequestServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderChangeRequestRepository changeRequestRepository;
    @Mock
    private OrderChangeRequestItemRepository changeRequestItemRepository;
    @Mock
    private DinnerTypeRepository dinnerTypeRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private DeliverySchedulingService deliverySchedulingService;

    @InjectMocks
    private OrderChangeRequestService service;

    private Order baseOrder;
    private User baseUser;

    @BeforeEach
    void setup() {
        LocalDateTime deliveryTime = LocalDateTime.now().plusDays(2);
        baseOrder = new Order();
        baseOrder.setId(1L);
        baseOrder.setUserId(10L);
        baseOrder.setDinnerTypeId(5L);
        baseOrder.setServingStyle("simple");
        baseOrder.setDeliveryTime(deliveryTime.toString());
        baseOrder.setDeliveryAddress("서울시");
        baseOrder.setTotalPrice(100_000);
        baseOrder.setPaymentStatus("paid");
        baseOrder.setAdminApprovalStatus("APPROVED");
        baseOrder.setStatus("pending");

        baseUser = new User();
        baseUser.setId(10L);
        baseUser.setLoyaltyConsent(Boolean.FALSE);
    }

    @Test
    void createChangeRequestAppliesChangeFeeInsideWindow() {
        ReservationChangeRequestCreateDto dto = new ReservationChangeRequestCreateDto();
        dto.setDinnerTypeId(5L);
        dto.setServingStyle("grand");
        dto.setDeliveryTime(LocalDateTime.now().plusDays(2).toString());
        dto.setDeliveryAddress("서울시");
        dto.setItems(List.of(new OrderItemDto(100L, 2)));
        dto.setReason("인원 추가");

        DinnerType dinnerType = new DinnerType();
        dinnerType.setId(5L);
        dinnerType.setBasePrice(80_000);
        dinnerType.setName("샴페인 디너");

        MenuItem menuItem = new MenuItem();
        menuItem.setId(100L);
        menuItem.setPrice(10_000);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(baseOrder));
        when(changeRequestRepository.existsByOrderIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(dinnerTypeRepository.findById(5L)).thenReturn(Optional.of(dinnerType));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(menuItem));
        when(userRepository.findById(10L)).thenReturn(Optional.of(baseUser));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(changeRequestRepository.save(any())).thenAnswer(invocation -> {
            OrderChangeRequest request = invocation.getArgument(0);
            request.setId(99L);
            return request;
        });

        var response = service.createChangeRequest(1L, 10L, dto);
        assertEquals(99L, response.getId());
        assertTrue(response.isRequiresAdditionalPayment());
        assertEquals(30_000, response.getChangeFeeAmount());
    }

    @Test
    void approveMarksPaymentFailureWhenGatewayFails() {
        OrderChangeRequest request = new OrderChangeRequest();
        request.setId(5L);
        request.setOrderId(1L);
        request.setUserId(10L);
        request.setStatus(OrderChangeRequestStatus.REQUESTED);
        request.setNewDeliveryTime(LocalDateTime.now().plusDays(2).toString());
        request.setNewDeliveryAddress("서울시");
        request.setNewServingStyle("grand");
        request.setNewDinnerTypeId(5L);
        request.setOriginalTotalAmount(100_000);
        request.setRecalculatedAmount(120_000);
        request.setChangeFeeAmount(30_000);
        request.setNewTotalAmount(150_000);
        request.setAlreadyPaidAmount(100_000);
        request.setExtraChargeAmount(50_000);

        OrderChangeRequestItem item = new OrderChangeRequestItem();
        item.setMenuItemId(100L);
        item.setQuantity(2);
        item.setUnitPrice(10_000);

        when(changeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(baseOrder));
        when(changeRequestItemRepository.findByChangeRequestId(5L)).thenReturn(List.of(item));
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(new MenuItem()));

        doThrow(new PaymentException("결제 실패")).when(paymentService).chargeAdditionalAmount(baseOrder, 50_000);

        assertThrows(PaymentException.class, () ->
                service.approve(5L, null));

        verify(changeRequestRepository).save(argThat(saved ->
                saved.getStatus() == OrderChangeRequestStatus.PAYMENT_FAILED));
    }
}

