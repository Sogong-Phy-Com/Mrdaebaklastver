# 파인튜닝 빠른 시작 가이드

## 1단계: 환경 설정

```bash
# Python 가상 환경 생성 (권장)
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 패키지 설치
pip install -r requirements.txt

# 환경 변수 설정
cp .env.example .env
# .env 파일을 열어서 OPENAI_API_KEY를 설정하세요
```

## 2단계: 데이터셋 생성

### 방법 A: 샘플 데이터 사용 (테스트용)

```bash
cd finetuning
python scripts/generate_dataset.py --format openai --sample
```

이 명령은 `dataset/training_data.jsonl` 파일을 생성합니다.

### 방법 B: 실제 대화 데이터 사용

실제 대화 데이터가 있다면:

1. `dataset/examples/your_conversations.json` 파일을 만드세요
2. 다음 형식을 따르세요:

```json
[
  {
    "customer_name": "고객이름",
    "messages": [
      {"role": "user", "content": "사용자 말"},
      {"role": "assistant", "content": "상담원 응답", "order_state": {...}}
    ]
  }
]
```

3. 데이터셋 생성:

```bash
python scripts/generate_dataset.py --format openai --input dataset/examples/your_conversations.json
```

## 3단계: 데이터 검증 (OpenAI)

OpenAI CLI를 사용하여 데이터 검증:

```bash
openai tools fine_tunes.prepare_data -f dataset/training_data.jsonl
```

## 4단계: 파인튜닝 실행

### OpenAI 파인튜닝

```bash
# 파일 업로드 및 파인튜닝 시작
python scripts/finetune_openai.py --file dataset/training_data.jsonl

# 또는 단계별로:
# 1. 파일만 업로드
python scripts/finetune_openai.py --file dataset/training_data.jsonl --upload-only

# 2. 나중에 파인튜닝 시작 (파일 ID 필요)
python scripts/finetune_openai.py --check <job-id>
```

### 진행 상황 확인

```bash
# 작업 목록 확인
python scripts/finetune_openai.py --list

# 특정 작업 상태 확인
python scripts/finetune_openai.py --check <job-id>
```

파인튜닝은 보통 10-30분 정도 소요됩니다.

## 5단계: 파인튜닝된 모델 사용

파인튜닝이 완료되면 모델 ID를 받게 됩니다. 예:

```
ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
```

### application.properties 설정

`server-java/src/main/resources/application.properties` 파일을 열고:

```properties
# OpenAI 파인튜닝된 모델 사용
voice.llm.api-url=https://api.openai.com/v1/chat/completions
voice.llm.model=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
voice.llm.api-key=${OPENAI_API_KEY}
```

또는 환경 변수로 설정:

```bash
export VOICE_LLM_API_URL=https://api.openai.com/v1/chat/completions
export VOICE_LLM_MODEL=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
export VOICE_LLM_API_KEY=your-openai-api-key
```

## Hugging Face 파인튜닝 (고급)

로컬에서 오픈소스 모델을 파인튜닝하려면:

```bash
# Hugging Face 형식 데이터 생성
python scripts/generate_dataset.py --format huggingface

# 파인튜닝 실행
python scripts/finetune_huggingface.py \
    --model meta-llama/Llama-3.2-3B-Instruct \
    --epochs 3 \
    --batch-size 4 \
    --use-lora

# 더 작은 모델 사용 (GPU 메모리가 부족한 경우)
python scripts/finetune_huggingface.py \
    --model microsoft/Phi-3-mini-4k-instruct \
    --epochs 3 \
    --batch-size 2
```

## 문제 해결

### "API 키가 설정되지 않았습니다"

`.env` 파일에 `OPENAI_API_KEY`를 설정했는지 확인하세요.

### "파일을 찾을 수 없습니다"

먼저 데이터셋을 생성하세요:

```bash
python scripts/generate_dataset.py --format openai --sample
```

### 파인튜닝이 실패하는 경우

1. 데이터 형식 확인: 각 대화에 최소 1개의 user-assistant 쌍이 있어야 합니다
2. 데이터 양: 최소 10개 이상의 대화를 권장합니다
3. API 할당량 확인: OpenAI API 사용량을 확인하세요

## 다음 단계

- 더 많은 실제 대화 데이터 수집
- 다양한 시나리오 추가 (취소, 변경, 문의 등)
- 파인튜닝된 모델 성능 평가 및 개선

## 참고 자료

- [OpenAI 파인튜닝 가이드](https://platform.openai.com/docs/guides/fine-tuning)
- [Hugging Face Transformers 문서](https://huggingface.co/docs/transformers)

