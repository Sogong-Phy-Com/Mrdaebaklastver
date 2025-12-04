# 파인튜닝 완전 가이드

## 목차

1. [파인튜닝이란?](#파인튜닝이란)
2. [데이터 준비](#데이터-준비)
3. [OpenAI 파인튜닝](#openai-파인튜닝)
4. [Hugging Face 파인튜닝](#hugging-face-파인튜닝)
5. [파인튜닝된 모델 사용](#파인튜닝된-모델-사용)
6. [성능 최적화](#성능-최적화)

## 파인튜닝이란?

파인튜닝(Fine-tuning)은 이미 학습된 대규모 언어 모델(LLM)을 특정 작업에 맞게 추가 학습시키는 과정입니다.

### 현재 시스템

현재 시스템은 **프롬프트 엔지니어링** 방식으로 작동합니다:
- 시스템 프롬프트를 통해 모델에게 역할과 규칙을 지시
- 매 요청마다 전체 프롬프트를 전송
- 모델이 프롬프트를 기반으로 응답 생성

### 파인튜닝 후

파인튜닝을 하면:
- 모델이 도메인 특화된 응답 패턴을 학습
- 더 일관되고 정확한 응답
- 프롬프트 길이 감소 가능
- 더 빠른 응답 시간 (일부 경우)

## 데이터 준비

### 필요한 데이터

최소 **50개 이상의 완전한 대화 세션**을 권장합니다.

각 대화는 다음을 포함해야 합니다:
- 사용자 입력 (user messages)
- 상담원 응답 (assistant messages)
- 주문 상태 정보 (order_state, 선택적)

### 데이터 형식

#### 입력 형식 (JSON)

```json
[
  {
    "customer_name": "김민수",
    "messages": [
      {
        "role": "user",
        "content": "안녕하세요"
      },
      {
        "role": "assistant",
        "content": "안녕하세요, 김민수님! 미스터 대박 디너 서비스에 오신 것을 환영합니다...",
        "order_state": {
          "readyForConfirmation": false,
          "needsMoreInfo": ["dinnerType"]
        }
      }
    ]
  }
]
```

#### 출력 형식 (OpenAI JSONL)

```json
{"messages": [
  {"role": "system", "content": "당신은 미스터 대박 디너 서비스의..."},
  {"role": "user", "content": "안녕하세요"},
  {"role": "assistant", "content": "안녕하세요, 김민수님!..."}
]}
```

### 데이터 수집 방법

#### 방법 1: 실제 대화 로그 수집

프로덕션 환경에서 실제 고객과의 대화를 수집:

```python
# 예시: 데이터베이스에서 대화 로그 추출
# VoiceConversationMessage를 JSON 형식으로 변환
```

#### 방법 2: 샘플 데이터 생성

다양한 시나리오를 포함한 샘플 데이터 생성:

```bash
python scripts/generate_dataset.py --format openai --sample
```

#### 방법 3: 데이터 보강

기존 데이터에 변형을 추가하여 데이터 양 늘리기:
- 다양한 고객 이름
- 다양한 날짜/시간 표현
- 다양한 주소 형식

### 데이터 품질 체크리스트

- [ ] 최소 50개 대화 세션
- [ ] 다양한 디너 타입 포함 (발렌타인, 프렌치, 잉글리시, 샴페인)
- [ ] 다양한 서빙 스타일 포함 (심플, 그랜드, 디럭스)
- [ ] 완전한 주문 흐름 포함 (인사 → 주문 → 확인)
- [ ] 오류 처리 예제 포함 (잘못된 입력, 도메인 외 질문 등)

## OpenAI 파인튜닝

### 장점

- ✅ 사용하기 쉬움
- ✅ 클라우드 인프라 불필요
- ✅ 자동 모델 관리
- ✅ 빠른 학습 (10-30분)

### 단점

- ❌ 비용 발생 (약 $3-10/파인튜닝, 토큰 사용량에 따라)
- ❌ 모델 수정 불가
- ❌ OpenAI API 의존

### 단계별 가이드

#### 1. 환경 설정

```bash
cd finetuning
pip install -r requirements.txt
```

#### 2. API 키 설정

`.env` 파일 생성:

```bash
OPENAI_API_KEY=sk-...
```

또는 환경 변수:

```bash
export OPENAI_API_KEY=sk-...
```

#### 3. 데이터셋 생성

```bash
python scripts/generate_dataset.py --format openai --sample
```

#### 4. 데이터 검증

OpenAI CLI로 데이터 형식 검증:

```bash
openai tools fine_tunes.prepare_data -f dataset/training_data.jsonl
```

#### 5. 파인튜닝 실행

```bash
python scripts/finetune_openai.py \
    --file dataset/training_data.jsonl \
    --model gpt-4o-mini \
    --suffix mrdabak-voice-order
```

#### 6. 상태 확인

```bash
# 작업 목록
python scripts/finetune_openai.py --list

# 특정 작업 상태
python scripts/finetune_openai.py --check <job-id>
```

#### 7. 모델 사용

파인튜닝 완료 후 받은 모델 ID를 사용:

```properties
voice.llm.api-url=https://api.openai.com/v1/chat/completions
voice.llm.model=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
voice.llm.api-key=${OPENAI_API_KEY}
```

### 비용 예상

- **gpt-4o-mini**: 약 $0.50-1.00 / 1M 토큰 (학습), $0.15-0.60 / 1M 토큰 (추론)
- **gpt-4o**: 약 $2.50-5.00 / 1M 토큰 (학습), $2.50-10.00 / 1M 토큰 (추론)
- **gpt-3.5-turbo**: 약 $0.80 / 1M 토큰 (학습), $0.50-1.50 / 1M 토큰 (추론)

예상: 50개 대화 → 약 10K 토큰 → 약 $0.01-0.05 학습 비용

## Hugging Face 파인튜닝

### 장점

- ✅ 완전한 제어
- ✅ 오픈소스 모델 사용
- ✅ 무료 (자체 인프라 사용 시)
- ✅ 모델 커스터마이징 가능

### 단점

- ❌ GPU 필요 (보통)
- ❌ 더 복잡한 설정
- ❌ 모델 서빙 인프라 필요

### 단계별 가이드

#### 1. 환경 설정

```bash
pip install -r requirements.txt
```

GPU 사용을 위해 CUDA 설치 필요 (선택적).

#### 2. 데이터셋 생성

```bash
python scripts/generate_dataset.py --format huggingface
```

#### 3. 파인튜닝 실행

```bash
python scripts/finetune_huggingface.py \
    --model meta-llama/Llama-3.2-3B-Instruct \
    --epochs 3 \
    --batch-size 4 \
    --use-lora
```

#### 4. 모델 사용

파인튜닝된 모델을 로컬에서 서빙하거나 Hugging Face에 업로드.

### 추천 모델

1. **meta-llama/Llama-3.2-3B-Instruct** (권장)
   - 3B 파라미터, 한국어 지원 양호
   - GPU 메모리: ~6GB

2. **microsoft/Phi-3-mini-4k-instruct**
   - 더 작은 모델 (3.8B), 빠른 추론
   - GPU 메모리: ~4GB

3. **beomi/KoAlpaca-Polyglot-5.8B**
   - 한국어 특화 모델
   - GPU 메모리: ~12GB

### LoRA (Low-Rank Adaptation)

메모리 효율적인 파인튜닝 방법:

```bash
python scripts/finetune_huggingface.py --use-lora
```

장점:
- 메모리 사용량 감소 (약 50-70%)
- 학습 속도 향상
- 원본 모델과 병합 가능

## 파인튜닝된 모델 사용

### application.properties 설정

```properties
# OpenAI 파인튜닝된 모델
voice.llm.api-url=https://api.openai.com/v1/chat/completions
voice.llm.model=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
voice.llm.api-key=${OPENAI_API_KEY}

# 또는 Groq 계속 사용
voice.llm.api-url=${VOICE_LLM_API_URL:https://api.groq.com/openai/v1/chat/completions}
voice.llm.model=${VOICE_LLM_MODEL:llama-3.1-8b-instant}
voice.llm.api-key=${VOICE_LLM_API_KEY:}
```

### 환경 변수로 설정

```bash
export VOICE_LLM_API_URL=https://api.openai.com/v1/chat/completions
export VOICE_LLM_MODEL=ft:gpt-4o-mini:your-org:mrdabak-voice-order:xxxxx
export VOICE_LLM_API_KEY=sk-...
```

### 코드 변경 없음

기존 코드는 그대로 사용 가능합니다. `VoiceOrderAssistantClient`가 설정된 API URL과 모델을 자동으로 사용합니다.

## 성능 최적화

### 데이터 최적화

1. **다양성 증가**
   - 다양한 시나리오 포함
   - 다양한 표현 방식 포함

2. **품질 향상**
   - 명확하고 일관된 응답
   - 올바른 JSON 형식

3. **데이터 양**
   - 최소 50개, 권장 100-200개
   - 더 많은 데이터 = 더 나은 성능

### 하이퍼파라미터 튜닝

#### OpenAI

- 모델 선택: `gpt-4o-mini` (비용 효율), `gpt-4o` (성능)

#### Hugging Face

```bash
python scripts/finetune_huggingface.py \
    --epochs 5 \              # 더 많은 에포크
    --batch-size 8 \          # 더 큰 배치
    --learning-rate 1e-4 \    # 학습률 조정
    --use-lora                # LoRA 사용
```

### 평가

파인튜닝 후 성능 평가:

1. **정확도**: 주문 정보 추출 정확도
2. **일관성**: 응답 형식 일관성
3. **자연스러움**: 대화 흐름 자연스러움
4. **JSON 형식**: 올바른 JSON 생성 비율

## 문제 해결

### 파인튜닝이 실패하는 경우

1. 데이터 형식 확인
2. 데이터 양 확인 (최소 10개)
3. API 키 유효성 확인
4. OpenAI 할당량 확인

### 성능이 기대에 못 미치는 경우

1. 더 많은 데이터 추가
2. 데이터 품질 개선
3. 하이퍼파라미터 조정
4. 다른 모델 시도

### 비용이 걱정되는 경우

1. 작은 모델 사용 (`gpt-4o-mini`)
2. 데이터 양 최적화
3. Hugging Face 오픈소스 모델 고려

## 다음 단계

1. ✅ 파인튜닝 데이터셋 준비
2. ✅ 첫 파인튜닝 실행
3. ✅ 성능 평가
4. ✅ 추가 데이터 수집 및 개선
5. ✅ 프로덕션 배포

## 참고 자료

- [OpenAI 파인튜닝 가이드](https://platform.openai.com/docs/guides/fine-tuning)
- [Hugging Face Transformers 문서](https://huggingface.co/docs/transformers/training)
- [LoRA 논문](https://arxiv.org/abs/2106.09685)

