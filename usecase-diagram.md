# 유즈케이스 다이어그램

## PlantUML 형식

```plantuml
@startuml
!theme plain
skinparam packageStyle rectangle
skinparam actorStyle awesome

left to right direction

actor "고객" as Customer
actor "직원" as Employee
actor "관리자" as Admin

rectangle "주문 관리" {
  usecase "주문 하기" as UC1
  usecase "주문 내역 조회" as UC2
  usecase "주문 관리" as UC5
  usecase "결제" as Payment
}

rectangle "예약 관리" {
  usecase "예약 변경 요청 관리" as UC6
}

rectangle "재고 관리" {
  usecase "재고 확인" as UC3
  usecase "재고 보충 요청" as Replenish
}

rectangle "스케줄 관리" {
  usecase "스케줄 조회" as UC4
}

rectangle "계정 관리" {
  usecase "계정 관리" as UC7
}

' 액터 연결
Customer --> UC1
Customer --> UC2
Customer --> UC6
Employee --> UC3
Employee --> UC4
Employee --> UC5
Admin --> UC3
Admin --> UC4
Admin --> UC5
Admin --> UC6
Admin --> UC7

' include 관계 (필수)
UC1 ..> Payment : <<include>>
UC2 ..> UC5 : <<include>>

' extend 관계 (선택적)
UC3 ..> Replenish : <<extend>>
UC2 ..> UC6 : <<extend>>

@enduml
```

## 유즈케이스 설명

### 기능별 그룹화

**주문 관리**
- 주문 하기 (고객)
- 주문 내역 조회 (고객)
- 주문 관리 (직원, 관리자)
- 결제 (주문 하기에 포함)

**예약 관리**
- 예약 변경 요청 관리 (고객, 관리자)

**재고 관리**
- 재고 확인 (직원, 관리자)
- 재고 보충 요청 (재고 확인에서 확장)

**스케줄 관리**
- 스케줄 조회 (직원, 관리자)

**계정 관리**
- 계정 관리 (관리자)

### 관계 설명

**Include 관계 (필수)**
- 주문 하기 → 결제: 주문 시 결제는 필수
- 주문 내역 조회 → 주문 관리: 내역 조회 시 관리 기능 포함

**Extend 관계 (선택적)**
- 재고 확인 → 재고 보충 요청: 재고 확인 중 필요 시 보충 요청 가능
- 주문 내역 조회 → 예약 변경 요청 관리: 내역 조회 중 변경 요청 가능

