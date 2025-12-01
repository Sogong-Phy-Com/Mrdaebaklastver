#!/bin/bash
echo "npm 패키지 설치 중..."
npm install --legacy-peer-deps
if [ $? -ne 0 ]; then
    echo "[오류] npm 설치에 실패했습니다."
    exit 1
fi
echo "설치 완료!"




