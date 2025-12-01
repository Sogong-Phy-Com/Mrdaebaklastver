@echo off
echo ====================================
echo Mr. DaeBak Backend Server Starting
echo ====================================
echo.

cd server-java
if not exist "data" mkdir data
echo Building and starting Spring Boot application...
echo.
echo [INFO] 서버가 시작되면 "Started DinnerServiceApplication" 메시지가 보입니다.
echo [INFO] 이 창을 닫지 마세요!
echo.
echo ====================================
echo.

call mvn clean spring-boot:run
set EXIT_CODE=%ERRORLEVEL%

echo.
echo ====================================
if %EXIT_CODE% NEQ 0 (
    echo [ERROR] 서버 시작 실패! (종료 코드: %EXIT_CODE%)
    echo 위의 오류 메시지를 확인하세요.
) else (
    echo [INFO] 서버가 정상적으로 종료되었습니다.
)
echo ====================================
echo.

pause
