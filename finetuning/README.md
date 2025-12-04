# 파인튜닝 가이드

이 디렉토리는 미스터 대박 디너 서비스의 음성 주문 AI 모델을 파인튜닝하기 위한 도구와 데이터셋을 포함합니다.

## 빠른 시작

```bash
# 1. 환경 설정
pip install -r requirements.txt

# 2. 샘플 데이터셋 생성
python scripts/generate_dataset.py --format openai --sample

# 3. 파인튜닝 실행 (OpenAI)
python scripts/finetune_openai.py --file dataset/training_data.jsonl
```

자세한 내용은 [QUICKSTART.md](QUICKSTART.md)를 참조하세요.

## 구조

```
finetuning/
├── README.md                    # 이 파일
├── QUICKSTART.md                # 빠른 시작 가이드
├── FINETUNE_GUIDE.md            # 상세 파인튜닝 가이드
├── .env.example                 # 환경 변수 예제
├── requirements.txt             # Python 의존성
├── dataset/                     # 파인튜닝 데이터셋
│   ├── training_data.jsonl      # 학습 데이터 (생성됨)
│   └── examples/                # 예제 대화 데이터
│       └── sample_conversations.json
└── scripts/
    ├── generate_dataset.py      # 대화 데이터를 파인튜닝 형식으로 변환
    ├── finetune_openai.py       # OpenAI 파인튜닝 스크립트
    └── finetune_huggingface.py  # Hugging Face 파인튜닝 스크립트
```

## 파인튜닝 방법

### 방법 1: OpenAI 파인튜닝 (추천) ⭐

**장점**: 사용하기 쉬움, 빠른 학습, 클라우드 인프라 불필요  
**단점**: 비용 발생, OpenAI API 의존

```bash
# 1. 데이터셋 생성
python scripts/generate_dataset.py --format openai

# 2. 파인튜닝 실행
python scripts/finetune_openai.py --file dataset/training_data.jsonl
```

### 방법 2: Hugging Face 파인튜닝

**장점**: 완전한 제어, 오픈소스, 무료 (자체 인프라)  
**단점**: GPU 필요, 더 복잡한 설정

```bash
# 1. 데이터셋 생성
python scripts/generate_dataset.py --format huggingface

# 2. 파인튜닝 실행
python scripts/finetune_huggingface.py --model meta-llama/Llama-3.2-3B-Instruct
```

## 파인튜닝된 모델 사용

파인튜닝이 완료되면 모델 ID를 받게 됩니다:

```
ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
```

### application.properties 설정

```properties
voice.llm.api-url=https://api.openai.com/v1/chat/completions
voice.llm.model=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
voice.llm.api-key=${OPENAI_API_KEY}
```

### 환경 변수로 설정

```bash
export VOICE_LLM_API_URL=https://api.openai.com/v1/chat/completions
export VOICE_LLM_MODEL=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
export VOICE_LLM_API_KEY=your-openai-api-key
```

## 데이터 형식

### 입력 형식 (JSON)

실제 대화 데이터를 다음 형식으로 준비:

```json
[
  {
    "customer_name": "김민수",
    "messages": [
      {"role": "user", "content": "안녕하세요"},
      {"role": "assistant", "content": "안녕하세요, 김민수님!...", "order_state": {...}}
    ]
  }
]
```

### 출력 형식 (OpenAI JSONL)

자동으로 변환됩니다:

```json
{"messages": [
  {"role": "system", "content": "당신은 미스터 대박 디너 서비스의..."},
  {"role": "user", "content": "안녕하세요"},
  {"role": "assistant", "content": "안녕하세요, 김민수님!..."}
]}
```

## 문서

- **[QUICKSTART.md](QUICKSTART.md)**: 5분 안에 시작하기
- **[FINETUNE_GUIDE.md](FINETUNE_GUIDE.md)**: 완전한 파인튜닝 가이드

## 데이터 수집 방법

1. **실제 대화 데이터 수집**
   - 프로덕션 환경에서 실제 고객과의 대화 로그 수집
   - `VoiceConversationMessage` 데이터를 JSON 형식으로 변환

2. **샘플 데이터 생성**
   - `scripts/generate_dataset.py`를 사용하여 다양한 시나리오 생성

3. **데이터 품질**
   - 최소 50개 이상의 완전한 대화 세션 권장
   - 다양한 디너 타입, 서빙 스타일, 상황 포함

## 주의사항

- **API 키 보안**: API 키를 코드에 하드코딩하지 마세요. `.env` 파일 사용
- **데이터 개인정보**: 실제 고객 데이터는 익명화 후 사용
- **비용**: OpenAI 파인튜닝은 비용이 발생할 수 있습니다 (약 $0.01-0.10)
- **모델 크기**: 모델 크기에 따라 학습 시간이 달라집니다

## 문제 해결

### "API 키가 설정되지 않았습니다"

`.env` 파일에 `OPENAI_API_KEY`를 설정했는지 확인하세요:

```bash
cp .env.example .env
# .env 파일 편집
```

### "파일을 찾을 수 없습니다"

먼저 데이터셋을 생성하세요:

```bash
python scripts/generate_dataset.py --format openai --sample
```

### 파인튜닝이 실패하는 경우

1. 데이터 형식 확인: 각 대화에 최소 1개의 user-assistant 쌍이 있어야 합니다
2. 데이터 양: 최소 10개 이상의 대화를 권장합니다
3. API 할당량 확인: OpenAI API 사용량을 확인하세요

## 추가 리소스

- [OpenAI 파인튜닝 가이드](https://platform.openai.com/docs/guides/fine-tuning)
- [Hugging Face 파인튜닝 가이드](https://huggingface.co/docs/transformers/training)
