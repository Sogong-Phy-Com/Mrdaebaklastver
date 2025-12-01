# 빠른 시작 가이드

## 프로그램 실행 방법

### 방법 1: 자동 실행 (가장 간단!)

1. 파일 탐색기에서 `C:\Users\pando\Desktop\MrDaeBak` 폴더 열기
2. **`run.bat`** 파일을 **더블클릭**

끝! 백엔드와 프론트엔드가 자동으로 시작됩니다.

---

### 방법 2: 수동 실행

#### 1단계: 백엔드 서버 실행

명령 프롬프트(cmd)를 열고:

```bash
cd C:\Users\pando\Desktop\MrDaeBak\server-java
mvn clean spring-boot:run
```

**중요**: `cd`와 `mvn`을 한 줄에 붙이지 마세요!
- 먼저 `cd C:\Users\pando\Desktop\MrDaeBak\server-java` 입력 후 Enter
- 그 다음 `mvn clean spring-boot:run` 입력 후 Enter

백엔드가 시작되면 "Started DinnerServiceApplication" 메시지가 보입니다.

#### 2단계: 프론트엔드 실행 (새 창)

**새 명령 프롬프트 창**을 열고:

```bash
cd C:\Users\pando\Desktop\MrDaeBak\client
npm start
```

프론트엔드가 시작되면 브라우저가 자동으로 열립니다.

---

## 프로그램 보는 방법

### 1. 브라우저에서 접속

프론트엔드가 시작되면 자동으로 브라우저가 열립니다.

또는 수동으로 접속:
- **http://localhost:3000** ← 여기로 접속!

### 2. 백엔드 확인

브라우저에서 다음 주소로 접속:
- **http://localhost:5000/api/health**

`{"status":"ok"}` 메시지가 보이면 정상 작동 중입니다.

---

## 사용 방법

1. **회원가입**: 웹사이트에서 이메일, 비밀번호, 이름, 주소, 전화번호 입력
2. **로그인**: 회원가입한 계정으로 로그인
3. **주문하기**: 
   - 디너 선택 (발렌타인, 프렌치, 잉글리시, 샴페인 축제)
   - 서빙 스타일 선택 (심플, 그랜드, 디럭스)
   - 배달 시간과 주소 입력
   - 주문 완료!

---

## 문제 해결

### ❌ ERR_CONNECTION_REFUSED 오류가 발생할 때

**증상**: 로그인/회원가입 시 "Failed to load resource: net::ERR_CONNECTION_REFUSED" 오류

**원인**: 백엔드 서버가 실행되지 않았습니다.

**해결 방법**:

1. **백엔드 서버 시작**:
   - `START_SERVER.bat` 파일을 더블클릭하거나
   - `run-java-only.bat` 파일을 실행하세요
   - 또는 명령 프롬프트에서:
     ```bash
     cd server-java
     mvn clean spring-boot:run
     ```

2. **서버 상태 확인**:
   - 브라우저에서 http://localhost:5000/api/health 접속
   - `{"status":"ok"}` 메시지가 보이면 정상 작동 중

3. **전체 서비스 시작**:
   - `run.bat` 파일을 실행하면 백엔드와 프론트엔드가 모두 시작됩니다

### ❌ 서버가 시작되다가 cmd 창이 꺼질 때

**증상**: `START_SERVER.bat` 실행 시 Spring Boot가 작동하다가 cmd 창이 갑자기 꺼짐

**원인**: 컴파일 오류 또는 런타임 오류로 인해 서버가 종료됨

**해결 방법**:

1. **컴파일 테스트**:
   - `TEST_COMPILE.bat` 파일을 실행하여 컴파일 오류 확인
   - 오류가 있으면 수정 후 다시 시도

2. **상세 로그 확인**:
   - `START_SERVER_DEBUG.bat` 파일을 실행
   - Java/Maven 버전 확인 및 상세 오류 메시지 확인

3. **일반적인 원인**:
   - Java 17 이상이 설치되지 않음
   - Maven이 설치되지 않음
   - 포트 5000이 이미 사용 중
   - 데이터베이스 파일 권한 문제
   - 컴파일 오류 (문법 오류, 누락된 의존성 등)

### 백엔드가 시작되지 않을 때
- Java 17 이상이 설치되어 있는지 확인
- Maven이 설치되어 있는지 확인
- `server-java/data` 폴더가 있는지 확인
- 포트 5000이 다른 프로그램에 의해 사용 중인지 확인

### 프론트엔드가 시작되지 않을 때
- `client` 폴더에서 `npm install --legacy-peer-deps` 실행
- Node.js가 설치되어 있는지 확인

### 브라우저가 열리지 않을 때
- 수동으로 http://localhost:3000 접속

---

## 서버 종료 방법

각 명령 프롬프트 창에서:
- **Ctrl + C** 누르기

또는 창을 닫으면 됩니다.




