# 변경 요청 수정 기능 구현 문서

## 개요
고객이 주문 변경 요청을 올린 후, 관리자가 승인하기 전(PENDING 상태)까지는 고객이 그 변경 요청 내용을 다시 수정할 수 있는 기능을 구현했습니다.

## 주요 변경 사항

### 1. 백엔드 변경

#### 1.1 Repository 레이어
- **OrderChangeRequestRepository.java**
  - `findByOrderIdAndUserIdAndStatusIn`: 주문 ID와 사용자 ID로 활성 상태의 변경 요청을 조회하는 메소드 추가

- **OrderChangeRequestItemRepository.java**
  - `deleteByChangeRequestId`: 변경 요청 수정 시 기존 items를 삭제하기 위한 메소드 추가

#### 1.2 Service 레이어
- **OrderChangeRequestService.java**
  - `createChangeRequest`: 활성 상태의 변경 요청이 이미 존재하면 새로 생성하지 않고 기존 요청을 수정하도록 변경
  - `updateChangeRequest` (신규): PENDING 상태의 변경 요청을 수정하는 메소드 추가
    - 비즈니스 규칙:
      - PENDING 상태(REQUESTED, PAYMENT_FAILED, REFUND_FAILED)의 변경 요청만 수정 가능
      - 원래 주문이 변경 불가 시점을 지나지 않았을 것
      - 변경 수수료는 최종 승인 시점에 한 번만 적용되므로, 수정 시에도 재계산
      - 재고/예약 상태를 다시 검증

#### 1.3 Controller 레이어
- **ReservationChangeRequestController.java**
  - `PUT /api/reservations/{reservationId}/change-requests/{changeRequestId}`: 변경 요청 수정 엔드포인트 추가
  - 예외 처리 개선: 클라이언트 오류는 400, 서버 오류는 500으로 반환

### 2. 프론트엔드 변경

#### 2.1 Orders.tsx
- 변경 요청 모달에서 PENDING 상태의 변경 요청이 있을 때 "변경 요청 편집하기" 버튼 표시
- 버튼 클릭 시 `/order?modify={orderId}&editRequest={changeRequestId}`로 이동

#### 2.2 Order.tsx
- `editRequestId` 파라미터 추가: URL에서 변경 요청 ID 읽기
- `isEditingChangeRequest` 상태 추가: 변경 요청 수정 모드인지 구분
- `fetchChangeRequestForEditing`: 기존 변경 요청 정보를 불러와서 폼에 prefilling하는 함수 추가
- `handleConfirmOrder`: 변경 요청 수정 시 PUT API 호출, 새로 생성 시 POST API 호출로 분기

## 비즈니스 규칙

### 1. 변경 요청 수정 가능 조건
- 변경 요청 상태가 PENDING (REQUESTED, PAYMENT_FAILED, REFUND_FAILED)이어야 함
- 원래 주문이 변경 불가 시점(배달 1일 전 00:00 이후)을 지나지 않았을 것
- 관리자에 의해 APPROVED / REJECTED 된 변경 요청은 수정 불가

### 2. 가격/수수료 처리
- 변경 수수료(3만원)는 최종 승인 시점에 한 번만 적용
- 변경 요청을 수정할 때마다 가격 차액과 수수료를 재계산하되, 수수료는 중복 누적되지 않음

### 3. 재고/예약 상태
- 변경 요청 수정 시 새롭게 요청된 변경 내용(메뉴/인원/날짜/시간)이 재고와 스케줄 제약을 만족하는지 다시 검증
- 실제 재고 확정 차감은 관리자 승인 시점에 이루어짐

## API 엔드포인트

### PUT /api/reservations/{reservationId}/change-requests/{changeRequestId}
변경 요청을 수정합니다.

**Request Body:**
```json
{
  "dinner_type_id": 1,
  "serving_style": "grand",
  "delivery_time": "2025-12-20T18:00",
  "delivery_address": "서울시 강남구 테스트로 123",
  "items": [
    {
      "menu_item_id": 1,
      "quantity": 2
    }
  ],
  "reason": "변경 사유"
}
```

**Response (200 OK):**
```json
{
  "message": "변경 요청이 수정되었습니다. 관리자 승인 전까지는 다시 변경할 수 있습니다.",
  "change_request": {
    "id": 1,
    "order_id": 1,
    "status": "REQUESTED",
    ...
  }
}
```

**Error Responses:**
- 400 Bad Request: 이미 처리된 변경 요청, 변경 불가 시점 경과 등
- 500 Internal Server Error: 서버 오류

## 테스트 시나리오

### 시나리오 A: 기본 플로우
1. 고객이 주문 생성
2. 고객이 최초 변경 요청 생성 (예: 인원 변경 + 일부 메뉴 변경)
3. 고객이 승인 전 상태에서 다시 변경 요청 수정 (메뉴/인원/시간 다시 조정)
4. 관리자 화면에서 해당 변경 요청을 승인
→ 결과: 최종 주문 내용이 "마지막으로 수정된 변경 요청" 기준으로 반영됨

### 시나리오 B: 승인 이후 수정 시도
1. 변경 요청이 이미 APPROVED 인 상태에서
2. 고객이 다시 변경 요청 수정 API 호출
→ 결과: 400 에러와 함께 "이미 처리된 변경 요청은 수정할 수 없습니다" 메시지 반환

### 시나리오 C: 변경 불가 시점 이후 수정 시도
1. 예약 1일 전 이후에
2. PENDING 변경 요청을 수정하려고 할 때
→ 결과: 400 에러와 함께 "예약 변경 가능 기한이 지났습니다" 메시지 반환

## 변경된 파일 목록

### 백엔드
1. `server-java/src/main/java/com/mrdabak/dinnerservice/repository/order/OrderChangeRequestRepository.java`
2. `server-java/src/main/java/com/mrdabak/dinnerservice/repository/order/OrderChangeRequestItemRepository.java`
3. `server-java/src/main/java/com/mrdabak/dinnerservice/service/OrderChangeRequestService.java`
4. `server-java/src/main/java/com/mrdabak/dinnerservice/controller/ReservationChangeRequestController.java`

### 프론트엔드
1. `client/src/pages/Orders.tsx`
2. `client/src/pages/Order.tsx`

## 주의사항

1. **한 주문당 PENDING 상태의 변경 요청은 최대 1개만 존재**
   - `createChangeRequest`에서 활성 요청이 있으면 자동으로 수정하도록 구현됨
   - 따라서 고객이 "새 변경 요청 만들기"를 클릭해도 기존 요청이 수정됨

2. **변경 수수료 중복 누적 방지**
   - 수수료는 최종 승인 시점에 한 번만 적용되도록 설계됨
   - 변경 요청을 여러 번 수정해도 수수료는 한 번만 부과됨

3. **재고 검증**
   - 변경 요청 수정 시마다 재고를 다시 검증하므로, 재고가 부족하면 수정이 실패함

