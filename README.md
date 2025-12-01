# 미스터 대박 디너 서비스 (Mr. DaeBak Dinner Service)

특별한 날에 집에서 편안히 보내면서 당신의 남편, 아내, 엄마, 아버지, 또는 친구를 감동시킬 수 있는 프리미엄 디너 배달 서비스입니다.

## 주요 기능

### 고객 기능
- **회원가입 및 로그인**: 안전한 JWT 기반 인증 시스템
- **디너 주문**: 4가지 프리미엄 디너 메뉴 선택
  - 발렌타인 디너
  - 프렌치 디너
  - 잉글리시 디너
  - 샴페인 축제 디너
- **서빙 스타일 선택**: 심플, 그랜드, 디럭스 중 선택
- **주문 수정**: 주문 후 음식 항목 추가/삭제/수량 변경 가능
- **음성 주문**: Web Speech API를 활용한 음성 인식 주문
- **주문 내역 조회**: 이전 주문 목록 최신순으로 조회
- **단골 고객 할인**: 재주문 시 10% 자동 할인

### 예약 변경 및 재승인 (2025-11 추가)
- **승인 완료 예약 수정 요청**: 배달 1일 전 00:00 이전까지 고객이 메뉴/인원/스타일을 변경 요청할 수 있습니다.
- **변경 수수료 로직**: 배달 3~1일 전에는 30,000원 변경 수수료가 자동 부과되며, 배달 1일 전 이후에는 변경이 차단됩니다.
- **차액 결제/환불 자동 처리**: 관리자 승인 시 추가 금액은 재결제하고 금액이 줄어들면 환불을 시도합니다.
- **재고 검증**: 기존 예약 재고를 반납한 것으로 가정하고 증감분만 검증하여 부족 재고를 고객에게 안내합니다.
- **관리자 재승인**: 요청 상태(REQUESTED, PAYMENT_FAILED 등)를 확인하고 승인/거절하며 결제 실패 시 재시도할 수 있습니다.
- **주요 API**
  - 고객: `POST /api/reservations/{reservationId}/change-requests`, `GET /api/reservations/{reservationId}/change-requests`, `GET /api/change-requests/{id}`
  - 관리자: `GET /api/admin/change-requests`, `POST /api/admin/change-requests/{id}/approve`, `POST /api/admin/change-requests/{id}/reject`

### 직원 기능
- **주문 관리**: 모든 주문 상태 확인 및 관리
- **상태 업데이트**: 주문 상태 변경 (대기 → 조리 → 준비 완료 → 배달 중 → 배달 완료)
- **필터링**: 주문 상태별 필터링

## 기술 스택

### Backend
- **Node.js** + **Express** + **TypeScript**
- **SQLite** 데이터베이스
- **JWT** 인증
- **bcryptjs** 비밀번호 암호화

### Frontend
- **React** + **TypeScript**
- **React Router** 라우팅
- **Axios** HTTP 클라이언트
- **Web Speech API** 음성 인식

## 설치 및 실행

### 사전 요구사항
- Node.js 16 이상
- npm 또는 yarn

### 설치

1. 저장소 클론 또는 다운로드
2. 루트 디렉토리에서 의존성 설치:
```bash
npm run install-all
```

또는 개별적으로:
```bash
npm install
cd server && npm install
cd ../client && npm install
```

### 환경 변수 설정

`server/.env` 파일 생성 (선택사항):
```
PORT=5000
JWT_SECRET=your-secret-key-change-in-production
NODE_ENV=development
```

`client/.env` 파일 생성 (선택사항):
```
REACT_APP_API_URL=http://localhost:5000/api
```

### 실행

개발 모드 (백엔드 + 프론트엔드 동시 실행):
```bash
npm run dev
```

또는 개별적으로:

백엔드 서버:
```bash
cd server
npm run dev
```

프론트엔드:
```bash
cd client
npm start
```

### 프로덕션 빌드

프론트엔드 빌드:
```bash
cd client
npm run build
```

백엔드 빌드:
```bash
cd server
npm run build
npm start
```

## 프로젝트 구조

```
MrDaeBak/
├── server/                 # 백엔드 서버
│   ├── src/
│   │   ├── index.ts       # 서버 진입점
│   │   ├── database.ts    # 데이터베이스 설정
│   │   ├── middleware/    # 미들웨어 (인증 등)
│   │   └── routes/        # API 라우트
│   ├── data/              # SQLite 데이터베이스 파일
│   └── package.json
├── client/                 # 프론트엔드 React 앱
│   ├── src/
│   │   ├── pages/         # 페이지 컴포넌트
│   │   ├── components/    # 재사용 컴포넌트
│   │   ├── contexts/      # React Context
│   │   └── App.tsx
│   └── package.json
└── package.json
```

## API 엔드포인트

### 인증
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인

### 메뉴
- `GET /api/menu/dinners` - 디너 목록 조회
- `GET /api/menu/items` - 메뉴 항목 조회
- `GET /api/menu/serving-styles` - 서빙 스타일 조회

### 주문
- `GET /api/orders` - 주문 목록 조회 (인증 필요)
- `GET /api/orders/:id` - 주문 상세 조회 (인증 필요)
- `POST /api/orders` - 주문 생성 (인증 필요)
- `PUT /api/orders/:id` - 주문 수정 (인증 필요)
- `DELETE /api/orders/:id` - 주문 취소 (인증 필요)

### 결제
- `POST /api/payment/process` - 결제 처리 (인증 필요)

### 직원
- `GET /api/employee/orders` - 모든 주문 조회 (직원 권한 필요)
- `PATCH /api/employee/orders/:id/status` - 주문 상태 업데이트 (직원 권한 필요)
- `POST /api/employee/orders/:id/assign` - 주문 배정 (직원 권한 필요)

## 데이터베이스 스키마

- **users**: 사용자 정보 (고객 및 직원)
- **dinner_types**: 디너 타입
- **menu_items**: 메뉴 항목
- **dinner_menu_items**: 디너-메뉴 항목 관계
- **orders**: 주문 정보
- **order_items**: 주문 항목 (수정된 항목 포함)
- **employees**: 직원 정보
- **order_assignments**: 주문 배정 정보

## 음성 인식 사용법

1. 주문 페이지에서 "음성으로 주문하기" 버튼 클릭
2. 마이크 권한 허용
3. 한국어로 명령어 말하기:
   - "발렌타인 디너", "프렌치 디너", "잉글리시 디너", "샴페인 축제 디너"
   - "심플", "그랜드", "디럭스"
   - "와인 추가" 등

## 보안 고려사항

- 프로덕션 환경에서는 반드시 `.env` 파일에서 `JWT_SECRET`을 변경하세요
- 실제 결제 시스템 통합 시 Stripe, PayPal 등의 안전한 결제 게이트웨이를 사용하세요
- HTTPS를 사용하여 통신을 암호화하세요
- SQL Injection 방지를 위해 파라미터화된 쿼리를 사용했습니다

## 라이선스

이 프로젝트는 교육 목적으로 제작되었습니다.

## 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.




