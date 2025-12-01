package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.model.MenuInventory;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.repository.inventory.InventoryReservationRepository;
import com.mrdabak.dinnerservice.repository.inventory.MenuInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceWeeklyReservedTest {

    @Mock
    private MenuInventoryRepository menuInventoryRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private InventoryService inventoryService;

    private MenuInventory inventory;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                menuInventoryRepository,
                inventoryReservationRepository,
                menuItemRepository,
                "MONDAY,FRIDAY",
                "06:00",
                20
        );
        inventory = new MenuInventory();
        inventory.setMenuItemId(1L);
        inventory.setCapacityPerWindow(50);
        menuItem = new MenuItem();
        menuItem.setId(1L);
        menuItem.setName("Test Dish");
        menuItem.setCategory("food");
    }

    @Test
    void getInventorySnapshotsUsesWeeklyReservedQuantity() {
        when(menuItemRepository.findAll()).thenReturn(List.of(menuItem));
        when(menuInventoryRepository.findByMenuItemId(1L)).thenReturn(Optional.of(inventory));
        when(menuInventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(inventoryReservationRepository.sumQuantityByMenuItemIdAndWindowStart(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0);
        when(inventoryReservationRepository.sumWeeklyReservedByMenuItemId(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(12);

        List<InventoryService.InventorySnapshot> snapshots = inventoryService.getInventorySnapshots();

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).weeklyReserved()).isEqualTo(12);
    }
}

