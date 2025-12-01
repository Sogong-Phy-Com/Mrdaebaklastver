package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.InventoryRestockRequest;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.service.InventoryService;
import com.mrdabak.dinnerservice.service.InventoryService.InventorySnapshot;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final MenuItemRepository menuItemRepository;

    public InventoryController(InventoryService inventoryService,
                               MenuItemRepository menuItemRepository) {
        this.inventoryService = inventoryService;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getInventory() {
        List<InventorySnapshot> snapshots = inventoryService.getInventorySnapshots();
        List<Map<String, Object>> response = snapshots.stream().map(snapshot -> {
            Map<String, Object> map = new HashMap<>();
            MenuItem menuItem = menuItemRepository.findById(snapshot.inventory().getMenuItemId()).orElse(null);
            map.put("menu_item_id", snapshot.inventory().getMenuItemId());
            map.put("capacity_per_window", snapshot.inventory().getCapacityPerWindow());
            map.put("reserved", snapshot.reserved());
            map.put("remaining", snapshot.remaining());
            map.put("ordered_quantity", snapshot.inventory().getOrderedQuantity() != null ? snapshot.inventory().getOrderedQuantity() : 0);
            map.put("window_start", snapshot.windowStart());
            map.put("window_end", snapshot.windowEnd());
            map.put("notes", snapshot.inventory().getNotes());
            if (menuItem != null) {
                map.put("menu_item_name", menuItem.getName());
                map.put("menu_item_name_en", menuItem.getNameEn());
                map.put("category", menuItem.getCategory());
            }
            // Calculate weekly reserved (sum of reservations for the week)
            map.put("weekly_reserved", snapshot.weeklyReserved());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{menuItemId}/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restock(@PathVariable Long menuItemId,
                                     @Valid @RequestBody InventoryRestockRequest request) {
        try {
            if (menuItemId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "메뉴 아이템 ID는 필수입니다."));
            }
            
            // Verify menu item exists
            if (!menuItemRepository.existsById(menuItemId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "메뉴 아이템을 찾을 수 없습니다: " + menuItemId));
            }
            
            var inventory = inventoryService.restock(menuItemId, request.getCapacityPerWindow(), request.getNotes());
            return ResponseEntity.ok(Map.of(
                    "menu_item_id", inventory.getMenuItemId(),
                    "capacity_per_window", inventory.getCapacityPerWindow(),
                    "notes", inventory.getNotes() != null ? inventory.getNotes() : "",
                    "last_restocked_at", inventory.getLastRestockedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "재고 보충 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(@RequestParam String menuItemIds,
                                               @RequestParam String deliveryTime) {
        try {
            java.time.LocalDateTime deliveryDateTime = java.time.LocalDateTime.parse(deliveryTime);
            Map<Long, Boolean> availability = new HashMap<>();
            
            // Parse comma-separated menu item IDs
            String[] ids = menuItemIds.split(",");
            for (String idStr : ids) {
                try {
                    Long menuItemId = Long.parseLong(idStr.trim());
                    inventoryService.prepareReservations(
                        List.of(new com.mrdabak.dinnerservice.dto.OrderItemDto(menuItemId, 1)),
                        deliveryDateTime
                    );
                    availability.put(menuItemId, true);
                } catch (Exception e) {
                    Long menuItemId = Long.parseLong(idStr.trim());
                    availability.put(menuItemId, false);
                }
            }
            
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "재고 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{menuItemId}/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> orderInventory(@PathVariable Long menuItemId,
                                           @RequestBody Map<String, Integer> request) {
        try {
            if (menuItemId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "메뉴 아이템 ID는 필수입니다."));
            }
            
            Integer orderedQuantity = request.get("ordered_quantity");
            if (orderedQuantity == null || orderedQuantity < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "주문 수량은 0 이상이어야 합니다."));
            }
            
            // Verify menu item exists
            if (!menuItemRepository.existsById(menuItemId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "메뉴 아이템을 찾을 수 없습니다: " + menuItemId));
            }
            
            var inventory = inventoryService.setOrderedQuantity(menuItemId, orderedQuantity);
            return ResponseEntity.ok(Map.of(
                    "menu_item_id", inventory.getMenuItemId(),
                    "ordered_quantity", inventory.getOrderedQuantity()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "주문 재고 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{menuItemId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> receiveInventory(@PathVariable Long menuItemId) {
        try {
            if (menuItemId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "메뉴 아이템 ID는 필수입니다."));
            }
            
            // Verify menu item exists
            if (!menuItemRepository.existsById(menuItemId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "메뉴 아이템을 찾을 수 없습니다: " + menuItemId));
            }
            
            var inventory = inventoryService.receiveOrderedInventory(menuItemId);
            return ResponseEntity.ok(Map.of(
                    "menu_item_id", inventory.getMenuItemId(),
                    "capacity_per_window", inventory.getCapacityPerWindow(),
                    "ordered_quantity", inventory.getOrderedQuantity(),
                    "message", "재고 수령이 완료되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "재고 수령 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

