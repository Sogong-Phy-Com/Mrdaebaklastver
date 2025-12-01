package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.DinnerType;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.repository.DinnerTypeRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderServiceTransactionIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private DinnerTypeRepository dinnerTypeRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private DeliverySchedulingService deliverySchedulingService;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void createOrderRetriesWithoutPersistingDuplicatesWhenInventoryLocked() {
        User user = new User();
        String uniqueEmail = java.util.UUID.randomUUID() + "@mrdabak.com";
        user.setEmail(uniqueEmail);
        user.setPassword("secret");
        user.setName("Integration Tester");
        user.setAddress("Seoul");
        user.setPhone("010-0000-0000");
        user.setRole("customer");
        user.setApprovalStatus("approved");
        User savedUser = userRepository.save(user);

        DinnerType dinnerType = new DinnerType();
        dinnerType.setName("Integration Dinner");
        dinnerType.setNameEn("Integration Dinner");
        dinnerType.setBasePrice(50000);
        dinnerType.setDescription("Test dinner");
        DinnerType savedDinner = dinnerTypeRepository.save(dinnerType);

        MenuItem item = new MenuItem();
        item.setName("테스트 요리");
        item.setNameEn("Test Dish");
        item.setCategory("food");
        item.setPrice(10000);
        MenuItem savedItem = menuItemRepository.save(item);

        OrderRequest request = new OrderRequest();
        request.setDinnerTypeId(savedDinner.getId());
        request.setServingStyle("simple");
        request.setDeliveryTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0).toString());
        request.setDeliveryAddress("서울시 테스트구");
        request.setPaymentMethod("card");
        request.setItems(List.of(new OrderItemDto(savedItem.getId(), 2)));

        InventoryService.InventoryReservationPlan plan = mock(InventoryService.InventoryReservationPlan.class);
        when(inventoryService.prepareReservations(any(), any())).thenReturn(plan);

        AtomicInteger commitAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (commitAttempts.getAndIncrement() < 2) {
                throw new RuntimeException("database is locked");
            }
            return null;
        }).when(inventoryService).commitReservations(anyLong(), eq(plan));

        orderService.createOrder(savedUser.getId(), request);

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(request.getItems().size());
        assertThat(commitAttempts.get()).isEqualTo(3);
    }
}

