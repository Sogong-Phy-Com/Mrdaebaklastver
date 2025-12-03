# 로컬 환경변수 설정 가이드

## 방법 1: application.properties에 직접 추가 (가장 간단)

`src/main/resources/application.properties` 파일을 열고 다음을 추가하세요:

```properties
voice.llm.api-key=your-groq-api-key-here
```

## 방법 2: 시스템 환경변수로 설정 (Windows)

### PowerShell에서:
```powershell
$env:VOICE_LLM_API_KEY="your-groq-api-key-here"
```

### 명령 프롬프트(CMD)에서:
```cmd
set VOICE_LLM_API_KEY=your-groq-api-key-here
```

### 영구적으로 설정 (시스템 환경변수):
1. Windows 검색에서 "환경 변수" 검색
2. "시스템 환경 변수 편집" 선택
3. "환경 변수" 버튼 클릭
4. "새로 만들기" 클릭
5. 변수 이름: `VOICE_LLM_API_KEY`
6. 변수 값: `your-groq-api-key-here`
7. 확인 클릭

## 방법 3: IDE 실행 설정에서 환경변수 추가

### IntelliJ IDEA:
1. Run/Debug Configurations 열기
2. Environment variables 섹션에서 추가
3. `VOICE_LLM_API_KEY=your-groq-api-key-here` 추가

### VS Code:
`.vscode/launch.json` 파일에 추가:
```json
{
  "configurations": [
    {
      "env": {
        "VOICE_LLM_API_KEY": "your-groq-api-key-here"
      }
    }
  ]
}
```

### Eclipse:
1. Run > Run Configurations
2. Environment 탭에서 환경변수 추가

## 방법 4: Maven 실행 시 환경변수 전달

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--voice.llm.api-key=your-groq-api-key-here"
```

## 필요한 환경변수

- `VOICE_LLM_API_KEY`: Groq API 키 (필수)
- `VOICE_LLM_API_URL`: API URL (선택, 기본값: https://api.groq.com/openai/v1/chat/completions)
- `VOICE_LLM_MODEL`: 모델 이름 (선택, 기본값: llama-3.1-8b-instant)

## Groq API 키 발급 방법

1. https://console.groq.com 접속
2. 회원가입/로그인
3. API Keys 메뉴에서 새 API 키 생성
4. 생성된 키를 환경변수에 설정

## 주의사항

- **절대 Git에 API 키를 커밋하지 마세요!**
- `.gitignore`에 `.env` 파일이 포함되어 있는지 확인하세요
- 프로덕션에서는 반드시 환경변수로 설정하세요

