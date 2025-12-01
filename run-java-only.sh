#!/bin/bash

echo "===================================="
echo "미스터 대박 백엔드 서버 시작"
echo "===================================="
echo ""

cd server-java
mkdir -p data
echo "Building and starting Spring Boot application..."
mvn clean spring-boot:run




