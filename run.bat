@echo off
REM Change to batch file directory
cd /d "%~dp0"

echo ====================================
echo Mr. DaeBak Dinner Service Starting
echo ====================================
echo Current directory: %CD%
echo.

REM Check working directories
if not exist "server-java" (
    echo [ERROR] server-java folder not found.
    echo Current location: %CD%
    echo.
    pause
    exit /b 1
)

if not exist "client" (
    echo [ERROR] client folder not found.
    echo Current location: %CD%
    echo.
    pause
    exit /b 1
)

REM Check Java
echo Checking Java installation...
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed.
    echo Please install Java 17 or higher.
    echo.
    pause
    exit /b 1
)
echo [OK] Java check complete

REM Check Maven
echo Checking Maven installation...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed.
    echo Please install Maven or use Maven Wrapper.
    echo.
    pause
    exit /b 1
)
echo [OK] Maven check complete

REM Check Node.js
echo Checking Node.js installation...
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed.
    echo Please install Node.js.
    echo.
    pause
    exit /b 1
)
echo [OK] Node.js check complete
echo.

echo [1/2] Starting backend server...
cd server-java
if not exist "data" mkdir data
echo Starting Spring Boot application...
start "Mr. DaeBak Backend" cmd /k "mvn clean spring-boot:run"
cd ..

echo [INFO] Waiting for backend server initialization... (15 seconds)
timeout /t 15 /nobreak >nul

echo [2/2] Starting frontend...
cd client
if not exist "node_modules" (
    echo Installing npm packages...
    call npm install --legacy-peer-deps
    if errorlevel 1 (
        echo [ERROR] npm installation failed.
        cd ..
        echo.
        pause
        exit /b 1
    )
)
echo Starting React development server...
start "Mr. DaeBak Frontend" cmd /k "npm start"
cd ..

echo.
echo ====================================
echo Services started successfully!
echo ====================================
echo Backend: http://localhost:5000
echo Frontend: http://localhost:3000
echo.
echo Opening browser...
timeout /t 15 /nobreak >nul

REM Open browser
start http://localhost:3000

echo.
echo ====================================
echo Browser opened!
echo ====================================
echo.
echo To stop the services:
echo   1. Close the backend/frontend windows, or
echo   2. Close this window
echo.
echo Do not close this window. You can check server status here.
echo ====================================
echo.
pause
