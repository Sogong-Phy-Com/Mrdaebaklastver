@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ====================================
echo Backend Server Start
echo ====================================
echo.
echo Starting backend server...
echo When server starts, you will see "Started DinnerServiceApplication" message.
echo.
echo Do not close this window!
echo.

cd /d "%~dp0server-java"
if not exist "data" mkdir data

echo.
echo [INFO] Maven build and server start...
echo [INFO] Errors will be displayed in this window.
echo [INFO] When server starts, logs will be displayed here.
echo.
echo ====================================
echo.

call mvn clean spring-boot:run
set EXIT_CODE=!ERRORLEVEL!

echo.
echo ====================================

if !EXIT_CODE! NEQ 0 (
    echo [ERROR] Server start failed! Exit code: !EXIT_CODE!
    echo ====================================
    echo.
    echo Possible causes:
    echo 1. Java not installed or wrong version (Java 17+ required)
    echo 2. Maven not installed
    echo 3. Port 5000 already in use
    echo 4. Database file permission issue
    echo 5. Compilation error
    echo.
    echo Solutions:
    echo 1. Run TEST_COMPILE.bat to check compilation errors
    echo 2. Run START_SERVER_DEBUG.bat to see detailed logs
    echo 3. Check the error messages above
    echo.
) else (
    echo [INFO] Server stopped normally.
    echo ====================================
    echo.
)

echo.
echo Press any key to close...
pause >nul
