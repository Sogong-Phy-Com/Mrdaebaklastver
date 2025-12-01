# 재고 관리 API 및 예약 수량 계산 가이드

## 개요
이 문서는 재고 관리 API에서 예약 수량을 어떻게 계산하고 응답하는지 설명합니다.

## API 엔드포인트

### GET /api/inventory
관리자와 직원 모두 사용하는 공통 재고 조회 API입니다.

**권한**: 인증된 사용자 (관리자/직원/고객)

**응답 구조**:
```json
[
  {
    "menu_item_id": 1,
    "menu_item_name": "메뉴 이름",
    "menu_item_name_en": "Menu Name",
    "category": "카테고리",
    "capacity_per_window": 100,        // 현재 보유량 (실제 재고)
    "reserved": 20,                     // 현재 날짜의 예약 수량 (windowStart 기준)
    "remaining": 80,                    // 남은 재고 (capacity_per_window - reserved)
    "weekly_reserved": 50,              // 이번주 예약 수량 (deliveryTime 기준, consumed=false만)
    "ordered_quantity": 10,             // 주문한 재고 (관리자만 사용)
    "window_start": "2025-12-01T17:00:00",
    "window_end": "2025-12-01T21:00:00",
    "notes": "비고"
  }
]
```

## 필드 의미

### capacity_per_window (현재 보유량)
- **의미**: 실제 보유하고 있는 재고 수량
- **변경 시점**: 
  - 관리자가 재고 보충 시 증가
  - 조리 시작 시점에 주문 수량만큼 차감
- **사용**: 관리자/직원 화면 모두 표시

### reserved (현재 날짜의 예약 수량)
- **의미**: 현재 날짜/시간대(windowStart)에 예약된 수량
- **계산 기준**: `InventoryReservation.windowStart == 현재 시간대의 windowStart`
- **제한**: 현재 시간대에만 해당하는 예약만 포함
- **사용**: 주로 현재 시간대의 예약 현황 확인용

### weekly_reserved (이번주 예약 수량) ⭐ **직원 화면에서 사용**
- **의미**: 이번 주(일요일~토요일)에 배달 예정인 미소진 예약 수량
- **계산 기준**: 
  - `InventoryReservation.deliveryTime >= 이번주 일요일 00:00`
  - `InventoryReservation.deliveryTime < 다음주 일요일 00:00`
  - `InventoryReservation.consumed = false` (아직 소진되지 않은 예약만)
- **제한**: 이번 주 전체 기간의 예약을 합산
- **사용**: 직원/관리자 화면 모두 표시 (이번주 전체 예약 현황 파악용)

### remaining (남은 재고)
- **의미**: 현재 보유량에서 현재 날짜 예약 수량을 뺀 값
- **계산**: `capacity_per_window - reserved`
- **사용**: 현재 시간대 기준 사용 가능한 재고

### availableQuantity (가용 재고) - 프론트엔드에서 계산
- **의미**: 현재 보유량에서 이번주 예약 수량을 뺀 값
- **계산**: `capacity_per_window - weekly_reserved`
- **사용**: 직원 화면에서 이번주 전체 기준 사용 가능한 재고 표시

## 예약 수량 계산 로직

### 1. 현재 날짜 예약 수량 (reserved)
```java
// InventoryService.getInventorySnapshots()
Integer reserved = inventoryReservationRepository
    .sumQuantityByMenuItemIdAndWindowStart(menuItemId, currentWindow.start());
```
- **쿼리**: `InventoryReservation.windowStart == 현재 시간대의 windowStart`
- **필터**: windowStart 기준으로 현재 시간대에만 해당하는 예약

### 2. 이번주 예약 수량 (weekly_reserved)
```java
// InventoryService.getInventorySnapshots()
LocalDate weekStart = today.minusDays(dayOfWeek); // 이번 주 일요일
LocalDate weekEnd = weekStart.plusWeeks(1); // 다음 주 일요일
Integer weeklyReserved = inventoryReservationRepository
    .sumWeeklyReservedByMenuItemId(menuItemId, weekStartDateTime, weekEndDateTime);
```
- **쿼리**: 
  ```sql
  SELECT COALESCE(SUM(r.quantity), 0) 
  FROM InventoryReservation r 
  WHERE r.menuItemId = :menuItemId 
    AND r.deliveryTime >= :weekStart 
    AND r.deliveryTime < :weekEnd 
    AND r.consumed = false
  ```
- **필터**: 
  - `deliveryTime` 기준으로 이번 주(일요일~토요일) 범위
  - `consumed = false` (아직 소진되지 않은 예약만)

## 예약 상태에 따른 포함 여부

### consumed 필드의 역할
- **consumed = false**: 아직 소진되지 않은 예약 (예약 수량에 포함)
- **consumed = true**: 이미 소진된 예약 (예약 수량에서 제외)

### consumed가 true로 변경되는 시점
- 조리 시작 시점 (`status = "cooking"`으로 변경될 때)
- `InventoryService.consumeReservationsForOrder(orderId)` 호출 시
- 해당 주문의 모든 `InventoryReservation`의 `consumed`가 `true`로 설정됨

## 프론트엔드 사용 가이드

### 관리자 화면 (AdminInventoryManagement.tsx)
```typescript
interface InventoryItem {
  capacity_per_window: number;      // 현재 보유량
  weekly_reserved?: number;         // 이번주 예약 수량
  reserved: number;                 // 현재 날짜 예약 수량
  remaining: number;                // 남은 재고
  // ...
}

// 표시 예시
const weeklyReserved = item.weekly_reserved || item.reserved || 0;
const spareQuantity = capacity_per_window - weeklyReserved;
```

### 직원 화면 (EmployeeInventoryManagement.tsx)
```typescript
interface InventoryItem {
  capacity_per_window: number;      // 현재 보유량
  weekly_reserved?: number;         // 이번주 예약 수량 ⭐ 필수
  reserved: number;                 // 현재 날짜 예약 수량
  remaining: number;                // 남은 재고
  // ...
}

// 표시 예시
const weeklyReserved = item.weekly_reserved !== undefined 
  ? item.weekly_reserved 
  : (item.reserved || 0);
const availableQuantity = item.capacity_per_window - weeklyReserved;
```

## 주의사항

1. **weekly_reserved는 consumed=false인 예약만 포함**
   - 조리 시작된 주문은 consumed=true로 변경되어 예약 수량에서 제외됨
   - 따라서 weekly_reserved는 "앞으로 소화해야 할 예약 수량"을 정확히 반영

2. **reserved vs weekly_reserved**
   - `reserved`: 현재 날짜/시간대에만 해당하는 예약 (좁은 범위)
   - `weekly_reserved`: 이번 주 전체 기간의 예약 (넓은 범위)
   - 직원 화면에서는 **weekly_reserved를 사용**하여 이번주 전체 예약 현황을 파악

3. **남은 재고 계산**
   - 현재 시간대 기준: `remaining = capacity_per_window - reserved`
   - 이번주 전체 기준: `availableQuantity = capacity_per_window - weekly_reserved`
   - 직원 화면에서는 이번주 전체 기준 계산을 사용하는 것이 더 유용

## 변경 이력

### 2025-12-01: 직원 화면 예약 수량 표시 버그 수정
- **문제**: 직원 화면에서 `weekly_reserved` 필드를 받지 못하여 예약 수량이 0으로 표시됨
- **해결**: 
  - `EmployeeInventoryManagement.tsx` 인터페이스에 `weekly_reserved` 필드 추가
  - "예약됨" 컬럼에 `weekly_reserved`를 표시하도록 수정
  - "남은 재고" 계산을 `capacity_per_window - weekly_reserved`로 수정
- **결과**: 관리자/직원 화면 모두 동일한 예약 수량을 표시

