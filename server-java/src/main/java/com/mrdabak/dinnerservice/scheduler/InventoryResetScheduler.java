package com.mrdabak.dinnerservice.scheduler;

import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.inventory.InventoryReservationRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.repository.inventory.MenuInventoryRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InventoryResetScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryResetScheduler.class);
    
    private final InventoryReservationRepository inventoryReservationRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuInventoryRepository menuInventoryRepository;
    private final InventoryService inventoryService;

    public InventoryResetScheduler(InventoryReservationRepository inventoryReservationRepository,
                                   OrderRepository orderRepository,
                                   OrderItemRepository orderItemRepository,
                                   MenuItemRepository menuItemRepository,
                                   MenuInventoryRepository menuInventoryRepository,
                                   InventoryService inventoryService) {
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuInventoryRepository = menuInventoryRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * 매일 자정(00:00:00)에 전날의 재고 예약을 삭제하고, 당일 예약량의 110% 재고 준비
     * 월요일/금요일에는 주문 재고를 현재 보유량에 더하고 주문 수량을 0으로 변경
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 0 * * ?" = 매일 자정
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional("inventoryTransactionManager")
    public void resetDailyInventory() {
        try {
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            boolean isRestockDay = (dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.FRIDAY);
            LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
            LocalDateTime tomorrowStart = todayStart.plusDays(1);
            
            logger.info("[InventoryResetScheduler] 매일 재고 초기화 시작 - 오늘 날짜: {}", today);
            
            // 1. 오늘 이전의 모든 재고 예약 삭제 (당일 재고는 유지)
            List<com.mrdabak.dinnerservice.model.InventoryReservation> oldReservations = 
                inventoryReservationRepository.findByWindowStartBetween(
                    LocalDateTime.of(2000, 1, 1, 0, 0), // 매우 오래된 날짜부터
                    todayStart.minusSeconds(1) // 오늘 00:00:00 이전까지
                );
            
            int count = oldReservations.size();
            if (count > 0) {
                for (com.mrdabak.dinnerservice.model.InventoryReservation reservation : oldReservations) {
                    inventoryReservationRepository.delete(reservation);
                }
                logger.info("[InventoryResetScheduler] 전날 재고 예약 {}개 삭제 완료", count);
            } else {
                logger.info("[InventoryResetScheduler] 삭제할 전날 예약이 없습니다.");
            }
            
            // 2. 당일 예약된 주문 확인하여 110% 재고 준비
            String todayStr = today.toString(); // "2025-11-22"
            List<com.mrdabak.dinnerservice.model.Order> todayOrders = 
                orderRepository.findByDeliveryTimeStartingWith(todayStr);
            
            if (!todayOrders.isEmpty()) {
                // 메뉴 아이템별로 수량 집계
                Map<Long, Integer> itemQuantities = todayOrders.stream()
                    .flatMap(order -> orderItemRepository.findByOrderId(order.getId()).stream())
                    .collect(Collectors.groupingBy(
                        com.mrdabak.dinnerservice.model.OrderItem::getMenuItemId,
                        Collectors.summingInt(com.mrdabak.dinnerservice.model.OrderItem::getQuantity)
                    ));
                
                // 각 메뉴 아이템에 대해 110% 재고 설정
                for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
                    Long menuItemId = entry.getKey();
                    Integer totalQuantity = entry.getValue();
                    int requiredCapacity = (int) Math.ceil(totalQuantity * 1.1); // 110%
                    
                    try {
                        inventoryService.restock(menuItemId, requiredCapacity, 
                            "당일 예약량(" + totalQuantity + "개)의 110% 자동 보충");
                        logger.info("[InventoryResetScheduler] 메뉴 아이템 {} 재고 {}개로 설정 완료", 
                            menuItemId, requiredCapacity);
                    } catch (Exception e) {
                        logger.error("[InventoryResetScheduler] 메뉴 아이템 {} 재고 설정 실패: {}", 
                            menuItemId, e.getMessage());
                    }
                }
            }
            
            // 3. 주류 제외 재료 중 3일 경과한 것 폐기
            List<com.mrdabak.dinnerservice.model.InventoryReservation> expiredReservations = 
                inventoryReservationRepository.findExpiredUnconsumed(LocalDateTime.now());
            
            int expiredCount = 0;
            for (com.mrdabak.dinnerservice.model.InventoryReservation reservation : expiredReservations) {
                MenuItem menuItem = menuItemRepository.findById(reservation.getMenuItemId()).orElse(null);
                if (menuItem != null && !isAlcoholCategory(menuItem.getCategory())) {
                    if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
                        inventoryReservationRepository.delete(reservation);
                        expiredCount++;
                    }
                }
            }
            
            if (expiredCount > 0) {
                logger.info("[InventoryResetScheduler] 3일 경과 재료 {}개 폐기 완료", expiredCount);
            }
            
            // 4. 재고 받는 날(월요일, 금요일)에 주문 재고를 현재 보유량에 더하고 주문 수량을 0으로 변경
            if (isRestockDay) {
                List<com.mrdabak.dinnerservice.model.MenuInventory> allInventories = 
                    menuInventoryRepository.findAll();
                
                for (com.mrdabak.dinnerservice.model.MenuInventory inventory : allInventories) {
                    int orderedQty = inventory.getOrderedQuantity() != null ? inventory.getOrderedQuantity() : 0;
                    if (orderedQty > 0) {
                        int newCapacity = inventory.getCapacityPerWindow() + orderedQty;
                        inventory.setCapacityPerWindow(newCapacity);
                        inventory.setOrderedQuantity(0);
                        menuInventoryRepository.save(inventory);
                        logger.info("[InventoryResetScheduler] 메뉴 아이템 {} 재고 수령 완료: 주문 재고 {}개가 현재 보유량에 추가됨", 
                            inventory.getMenuItemId(), orderedQty);
                    }
                }
            }
            
            logger.info("[InventoryResetScheduler] 매일 재고 초기화 완료 - 날짜: {}, 재고 수령일: {}", today, isRestockDay);
        } catch (Exception e) {
            logger.error("[InventoryResetScheduler] 재고 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    private boolean isAlcoholCategory(String category) {
        if (category == null) return false;
        String lowerCategory = category.toLowerCase();
        return lowerCategory.contains("주류") || lowerCategory.contains("alcohol") || 
               lowerCategory.contains("wine") || lowerCategory.contains("beer") ||
               lowerCategory.contains("drink") || lowerCategory.contains("음료");
    }
}

