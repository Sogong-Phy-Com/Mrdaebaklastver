# Render 배포 가이드

이 프로젝트를 Render에 배포하는 방법입니다.

## 사전 요구사항

1. GitHub 저장소에 코드가 푸시되어 있어야 합니다.
2. Render 계정이 필요합니다 (https://render.com)

## 배포 단계

### 1. Render에서 새 Web Service 생성

1. Render 대시보드에서 "New +" 버튼 클릭
2. "Web Service" 선택
3. GitHub 저장소 연결 및 선택

### 2. 서비스 설정

- **Name**: `mrdabak-dinner-service` (또는 원하는 이름)
- **Environment**: `Docker`
- **Region**: 가장 가까운 지역 선택
- **Branch**: `main` (또는 기본 브랜치)
- **Root Directory**: (비워두기 - 루트에서 빌드)
- **Dockerfile Path**: `server-java/Dockerfile`
- **Docker Context**: `.` (루트 디렉토리)

### 3. 환경 변수 설정

Render 대시보드의 "Environment" 섹션에서 다음 환경 변수를 설정:

- `SPRING_PROFILES_ACTIVE`: `production`
- `JWT_SECRET`: (자동 생성 또는 직접 설정 - 긴 랜덤 문자열)
- `PORT`: (Render가 자동으로 설정하므로 설정하지 않음)

### 4. 빌드 및 배포

Render는 자동으로:
1. 프론트엔드(React)를 빌드합니다
2. Spring Boot 애플리케이션을 빌드합니다
3. 프론트엔드를 Spring Boot의 static 리소스로 포함시킵니다
4. 애플리케이션을 시작합니다

### 5. 헬스 체크

배포 후 다음 URL로 헬스 체크:
- `https://your-service.onrender.com/api/health`

## 주의사항

### 데이터베이스

- 현재 SQLite를 사용하고 있으며, Render의 임시 파일 시스템에 저장됩니다.
- **중요**: Render의 무료 플랜에서는 서비스가 비활성화되면 데이터가 손실될 수 있습니다.
- 프로덕션 환경에서는 PostgreSQL 같은 영구 데이터베이스를 사용하는 것을 권장합니다.

### 포트

- Render는 `PORT` 환경 변수를 자동으로 설정합니다.
- `application.properties`에서 `${PORT:5000}`로 설정되어 있어 자동으로 감지됩니다.

### CORS

- 현재 모든 오리진을 허용하도록 설정되어 있습니다 (`spring.web.cors.allowed-origin-patterns=*`).
- 프로덕션에서는 특정 도메인으로 제한하는 것을 권장합니다.

## 문제 해결

### 빌드 실패

1. Render 로그를 확인하세요
2. 로컬에서 Docker 빌드 테스트:
   ```bash
   docker build -f server-java/Dockerfile -t mrdabak-test .
   ```

### 프론트엔드가 로드되지 않음

1. 프론트엔드 빌드가 성공했는지 확인
2. `server-java/src/main/resources/static` 폴더에 빌드된 파일이 있는지 확인
3. `WebConfig.java`의 정적 리소스 설정 확인

### API 요청 실패

1. 브라우저 콘솔에서 CORS 오류 확인
2. `SecurityConfig.java`의 CORS 설정 확인
3. API URL이 올바른지 확인 (프로덕션에서는 `/api` 사용)

## 로컬 테스트

Render에 배포하기 전에 로컬에서 Docker 빌드를 테스트할 수 있습니다:

```bash
# Docker 이미지 빌드
docker build -f server-java/Dockerfile -t mrdabak-test .

# 컨테이너 실행
docker run -p 5000:5000 -e JWT_SECRET=test-secret-key mrdabak-test
```

그런 다음 http://localhost:5000 에서 접속하여 테스트할 수 있습니다.

