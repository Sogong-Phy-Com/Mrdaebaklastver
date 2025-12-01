#!/bin/bash

echo "===================================="
echo "미스터 대박 디너 서비스 시작"
echo "===================================="
echo ""

# Java와 Maven이 설치되어 있는지 확인
if ! command -v java &> /dev/null; then
    echo "[오류] Java가 설치되어 있지 않습니다."
    echo "Java 17 이상을 설치해주세요."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "[오류] Maven이 설치되어 있지 않습니다."
    echo "Maven을 설치하거나 Maven Wrapper를 사용하세요."
    exit 1
fi

echo "[1/2] 백엔드 서버 시작 중..."
cd server-java
mkdir -p data
echo "Building and starting Spring Boot application..."
mvn clean spring-boot:run &
BACKEND_PID=$!
cd ..

echo "[INFO] Waiting for backend server initialization... (15 seconds)"
sleep 15

echo "[2/2] 프론트엔드 시작 중..."
cd client
if [ ! -d "node_modules" ]; then
    echo "npm 패키지 설치 중..."
    npm install --legacy-peer-deps
    if [ $? -ne 0 ]; then
        echo "[오류] npm 설치에 실패했습니다."
        exit 1
    fi
fi
npm start &
FRONTEND_PID=$!
cd ..

echo ""
echo "===================================="
echo "서비스가 시작되었습니다!"
echo "===================================="
echo "백엔드: http://localhost:5000"
echo "프론트엔드: http://localhost:3000"
echo ""
echo "서비스를 종료하려면 Ctrl+C를 누르세요."
echo "===================================="

# 종료 시그널 처리
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM

wait

