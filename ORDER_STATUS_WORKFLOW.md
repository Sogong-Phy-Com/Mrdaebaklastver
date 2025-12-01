# 주문 상태 전환 및 재고 차감 워크플로우

## 개요
예약/주문 상태 흐름과 재고 차감 시점을 명확히 정의하고 구현했습니다.

## 상태 전환 규칙

### 1. 승인 완료 → 조리 중
- **전제 조건**: 
  - `adminApprovalStatus == "APPROVED"`
  - `status == "pending"`
- **권한**: 조리 담당 직원 (employeeType == "cooking" 또는 COOKING 작업 할당)
- **액션**: 
  - `status → "cooking"`
  - **재고 차감 발생** (이 시점에서 실제 보유 재고에서 차감)
- **엔드포인트**: `POST /api/employee/orders/{orderId}/start-cooking`

### 2. 조리 중 → 배달 중
- **전제 조건**: 
  - `status == "cooking"`
- **권한**: 배달 담당 직원 (employeeType == "delivery" 또는 DELIVERY 작업 할당)
- **액션**: 
  - `status → "out_for_delivery"`
  - **재고 차감 없음** (이미 조리 시작 시점에 차감 완료)
- **엔드포인트**: `POST /api/employee/orders/{orderId}/start-delivery`

### 3. 배달 중 → 배달 완료
- **전제 조건**: 
  - `status == "out_for_delivery"`
- **권한**: 조리원 또는 배달 담당 직원 (요구사항에 따라 조리원이 배달 완료를 누름)
- **액션**: 
  - `status → "delivered"`
  - **재고 차감 없음**
- **엔드포인트**: `POST /api/employee/orders/{orderId}/complete-delivery`

## 재고 차감 로직

### 승인 완료 시점
- 재고 부족 여부를 검증
- `InventoryReservation`으로 "예약(hold)"만 설정
  - `consumed = false`
  - 실제 보유 재고(`capacity_per_window`)는 아직 차감하지 않음

### 조리 시작 시점 (승인 완료 → 조리 중)
- 해당 주문의 `OrderItem`을 기반으로 항목별 quantity 합산
- `InventoryService.consumeReservationsForOrder(orderId)` 호출:
  1. `InventoryReservation`의 `consumed = true`로 설정
  2. `MenuInventory.capacity_per_window -= 주문 수량` (실제 보유 재고 차감)
  3. 이번주 예약 수량(`weekly_reserved`)에서 자동으로 제외됨 (consumed=false만 포함)

### 트랜잭션 처리
- 재고 차감 과정 전체가 트랜잭션 내에서 처리됨
- 재고 차감 중 예외 발생 시, 주문 상태를 `cooking`으로 변경하지 않도록 롤백 처리

## 잘못된 상태 전환 방지

다음 경우에는 400 또는 409 에러를 반환합니다:

1. **승인 완료가 아닌 상태에서 조리 시작 시도**
   - `adminApprovalStatus != "APPROVED"` 또는 `status != "pending"`
   - 에러 메시지: "관리자 승인 완료된 주문만 조리를 시작할 수 있습니다."

2. **조리 중이 아닌 상태에서 배달 시작 시도**
   - `status != "cooking"`
   - 에러 메시지: "조리 중인 주문만 배달을 시작할 수 있습니다."

3. **배달 중이 아닌 상태에서 배달 완료 시도**
   - `status != "out_for_delivery"`
   - 에러 메시지: "배달 중인 주문만 배달 완료 처리할 수 있습니다."

## 권한 검증

### 조리 시작
- 조리 담당 직원만 가능
- 검증 방법:
  1. 해당 날짜에 COOKING 작업 할당 확인
  2. 또는 `employeeType == "cooking"` 확인

### 배달 시작
- 배달 담당 직원만 가능
- 검증 방법:
  1. 해당 날짜에 DELIVERY 작업 할당 확인
  2. 또는 `employeeType == "delivery"` 확인

### 배달 완료
- 조리원 또는 배달 담당 직원 가능
- 검증 방법:
  1. 해당 날짜에 COOKING 또는 DELIVERY 작업 할당 확인
  2. 또는 `employeeType == "cooking"` 또는 `employeeType == "delivery"` 확인

## 재고 표시 (직원/관리자 화면)

### API 엔드포인트
- **공통 API**: `GET /api/inventory`
- 직원과 관리자 모두 동일한 API를 사용하여 일관된 데이터를 받습니다.

### 응답 필드
```json
{
  "menu_item_id": 1,
  "menu_item_name": "메뉴 이름",
  "menu_item_name_en": "Menu Name",
  "category": "카테고리",
  "capacity_per_window": 100,      // 현재 보유량 (실제 재고)
  "reserved": 20,                   // 예약된 수량 (consumed=false인 예약)
  "remaining": 80,                  // 남은 재고 (capacity_per_window - reserved)
  "weekly_reserved": 50,            // 이번주 예약 수량 (consumed=false만 포함)
  "ordered_quantity": 10,           // 주문한 재고 (관리자만 표시)
  "window_start": "2025-12-01T17:00:00",
  "window_end": "2025-12-01T21:00:00",
  "notes": "비고"
}
```

### 필드 의미
- **capacity_per_window**: 현재 보유량 (실제 재고)
  - 조리 시작 시점에 차감됨
  - 관리자/직원 화면에서 동일하게 표시됨
- **reserved**: 예약된 수량 (consumed=false인 예약)
  - 승인 완료된 주문의 예약 수량
  - 조리 시작 시 consumed=true로 변경되어 reserved에서 제외됨
- **remaining**: 남은 재고 (capacity_per_window - reserved)
  - 실제 사용 가능한 재고
- **weekly_reserved**: 이번주 예약 수량 (consumed=false만 포함)
  - 주간 예약 현황 파악용

## 변경된 파일 목록

### 백엔드
1. `server-java/src/main/java/com/mrdabak/dinnerservice/controller/EmployeeController.java`
   - `startCooking()`: 조리 시작 엔드포인트 추가
   - `startDelivery()`: 배달 시작 엔드포인트 추가
   - `completeDelivery()`: 배달 완료 엔드포인트 추가
   - 각 엔드포인트에서 상태 전환 검증 및 권한 검증 수행

### 프론트엔드
1. `client/src/pages/ScheduleCalendar.tsx`
   - 상태별 버튼 표시 로직 수정
   - 승인 완료 → 조리 중: "조리 시작" 버튼 (조리원만)
   - 조리 중 → 배달 중: "배달 시작" 버튼 (배달원만)
   - 배달 중 → 배달 완료: "배달 완료" 버튼 (조리원 또는 배달원)
   - 새로운 엔드포인트 사용 (`/start-cooking`, `/start-delivery`, `/complete-delivery`)

## 테스트 시나리오

### 시나리오 A: 정상 플로우
1. 주문 생성 → 관리자 승인 (`status="pending"`, `adminApprovalStatus="APPROVED"`)
2. 조리원 계정으로 로그인 → "조리 시작" 클릭
   - `status == "cooking"`
   - 재고 `capacity_per_window`가 주문 수량만큼 감소
   - `InventoryReservation.consumed = true`
3. 배달원 계정으로 로그인 → "배달 시작" 클릭
   - `status == "out_for_delivery"`
   - 재고 추가 변경 없음
4. 조리원 계정으로 로그인 → "배달 완료" 클릭
   - `status == "delivered"`

### 시나리오 B: 잘못된 상태에서 호출
1. `status="pending"`이고 `adminApprovalStatus="PENDING"`인 상태에서 조리 시작 시도
   → 400 에러: "관리자 승인 완료된 주문만 조리를 시작할 수 있습니다."
2. `status="delivered"`인 상태에서 다시 배달 완료 시도
   → 400 에러: "배달 중인 주문만 배달 완료 처리할 수 있습니다."

### 시나리오 C: 재고 확인
1. 특정 메뉴의 재고 수량 확인 (관리자/직원 화면 모두)
   - `capacity_per_window = 100`
   - `reserved = 20`
   - `remaining = 80`
2. 해당 메뉴를 포함하는 주문을 승인 → 조리 시작
3. 관리자/직원 화면에서 재고 수량 확인
   - `capacity_per_window = 80` (20개 차감됨)
   - `reserved = 0` (consumed=true로 변경되어 제외됨)
   - `remaining = 80`

## 주의사항

1. **재고 차감은 조리 시작 시점에만 발생**
   - 승인 완료 시점에는 예약(hold)만 설정
   - 조리 시작 시점에 실제 보유 재고에서 차감
   - 배달 시작/완료 시점에는 재고 변경 없음

2. **트랜잭션 처리**
   - 재고 차감과 주문 상태 변경이 트랜잭션 내에서 처리됨
   - 재고 차감 실패 시 주문 상태 변경도 롤백됨

3. **권한 검증**
   - 각 상태 전환은 해당 작업을 할당받은 직원만 가능
   - 작업 할당은 `EmployeeWorkAssignment` 테이블에서 확인

4. **재고 표시 일관성**
   - 직원과 관리자 모두 동일한 API(`/api/inventory`)를 사용
   - 동일한 필드 이름과 의미를 사용하여 일관된 데이터 표시

