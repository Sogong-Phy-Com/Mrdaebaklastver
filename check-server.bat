@echo off
echo ====================================
echo 백엔드 서버 상태 확인
echo ====================================
echo.

echo [1] 포트 5000 확인 중...
netstat -ano | findstr :5000
if %ERRORLEVEL% EQU 0 (
    echo [OK] 포트 5000이 사용 중입니다.
) else (
    echo [ERROR] 포트 5000이 사용되지 않고 있습니다.
    echo 백엔드 서버가 실행되지 않았습니다.
)
echo.

echo [2] 백엔드 서버 연결 테스트 중...
curl -s http://localhost:5000/api/health
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] 백엔드 서버가 정상적으로 응답합니다.
) else (
    echo.
    echo [ERROR] 백엔드 서버에 연결할 수 없습니다.
    echo.
    echo 해결 방법:
    echo 1. run-java-only.bat 파일을 실행하여 백엔드 서버를 시작하세요.
    echo 2. 또는 run.bat 파일을 실행하여 전체 서비스를 시작하세요.
)
echo.
echo ====================================
pause

