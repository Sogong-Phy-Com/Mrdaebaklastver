package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.model.InventoryReservation;
import com.mrdabak.dinnerservice.model.MenuInventory;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.inventory.InventoryReservationRepository;
import com.mrdabak.dinnerservice.repository.inventory.MenuInventoryRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceChangePlanTest {

    @Mock
    private MenuInventoryRepository menuInventoryRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setup() {
        inventoryService = new InventoryService(menuInventoryRepository, inventoryReservationRepository, menuItemRepository,
                "MONDAY,FRIDAY", "06:00", 20);
    }

    @Test
    void validateChangePlanSubtractsCurrentOrderReservations() {
        Long orderId = 1L;
        Long menuItemId = 10L;
        LocalDateTime deliveryTime = LocalDateTime.now().plusDays(1);

        MenuInventory inventory = new MenuInventory();
        inventory.setMenuItemId(menuItemId);
        inventory.setCapacityPerWindow(20);

        InventoryReservation reservation = new InventoryReservation();
        reservation.setMenuItemId(menuItemId);
        reservation.setQuantity(5);
        reservation.setWindowStart(deliveryTime.toLocalDate().atStartOfDay());

        when(menuInventoryRepository.findByMenuItemId(menuItemId)).thenReturn(Optional.of(inventory));
        when(inventoryReservationRepository.findByOrderId(orderId)).thenReturn(List.of(reservation));
        when(inventoryReservationRepository.sumQuantityByMenuItemIdAndWindowStart(menuItemId, reservation.getWindowStart())).thenReturn(15);
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(new MenuItem()));

        OrderItemDto dto = new OrderItemDto(menuItemId, 10);
        inventoryService.validateChangePlan(orderId, List.of(dto), deliveryTime);
        // no exception
    }

    @Test
    void validateChangePlanThrowsWhenProjectedExceedsCapacity() {
        Long orderId = 1L;
        Long menuItemId = 20L;
        LocalDateTime deliveryTime = LocalDateTime.now().plusDays(1);

        MenuInventory inventory = new MenuInventory();
        inventory.setMenuItemId(menuItemId);
        inventory.setCapacityPerWindow(10);

        when(menuInventoryRepository.findByMenuItemId(menuItemId)).thenReturn(Optional.of(inventory));
        when(inventoryReservationRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(inventoryReservationRepository.sumQuantityByMenuItemIdAndWindowStart(menuItemId, deliveryTime.toLocalDate().atStartOfDay())).thenReturn(8);
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(new MenuItem()));

        OrderItemDto dto = new OrderItemDto(menuItemId, 5);
        assertThrows(RuntimeException.class, () ->
                inventoryService.validateChangePlan(orderId, List.of(dto), deliveryTime));
    }
}

