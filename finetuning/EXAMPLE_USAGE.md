# 실제 예시 대화 데이터 사용법

제공하신 실제 대화 예시가 `sample_conversations.json`에 추가되었습니다.

## 추가된 대화 예시

### 1. 정수진님 대화
- 기념일: 내일이 어머님 생신
- 디너: 샴페인 축제 디너 디럭스
- 메뉴 조정: 바게트빵 6개, 샴페인 2병
- 배달: 내일

### 2. 이동호님 대화
- 기념일: 모레가 어머님 생신
- 디너: 샴페인 축제 디너 디럭스
- 메뉴 조정: 바게트빵 6개, 샴페인 2병
- 배달: 모레

## 데이터셋 생성

```bash
cd finetuning

# OpenAI 형식으로 생성
python scripts/generate_dataset.py --format openai

# 또는 특정 파일 사용
python scripts/generate_dataset.py --format openai --input dataset/examples/sample_conversations.json
```

## 데이터 구조

각 대화는 다음 구조를 가집니다:

```json
{
  "customer_name": "정수진",
  "messages": [
    {
      "role": "user",
      "content": "맛있는 디너 추천해주세요"
    },
    {
      "role": "assistant",
      "content": "무슨 기념일인가요?",
      "order_state": {
        "readyForConfirmation": false,
        "needsMoreInfo": ["dinnerType"]
      }
    }
  ]
}
```

## 추가 예시 만들기

더 많은 실제 대화 예시를 추가하려면:

1. `dataset/examples/sample_conversations.json` 파일을 엽니다
2. 새로운 대화 객체를 배열에 추가합니다
3. 데이터셋을 다시 생성합니다

### 대화 작성 팁

- **자연스러운 흐름**: 실제 고객이 사용할 법한 표현 사용
- **다양한 시나리오**: 
  - 다양한 디너 타입
  - 다양한 기념일 상황
  - 다양한 메뉴 조정
  - 다양한 날짜 표현
- **주문 상태 정확성**: 각 단계에서 필요한 정보와 현재 상태를 정확히 반영

## 파인튜닝 실행

데이터셋 생성 후:

```bash
# OpenAI 파인튜닝
python scripts/finetune_openai.py --file dataset/training_data.jsonl
```

## 데이터 검증

생성된 데이터가 올바른지 확인:

```bash
# JSONL 파일 확인
head -n 5 dataset/training_data.jsonl

# OpenAI 형식 검증
openai tools fine_tunes.prepare_data -f dataset/training_data.jsonl
```

## 다음 단계

1. 실제 프로덕션 대화 로그 수집
2. 더 다양한 시나리오 추가
3. 데이터 품질 검증
4. 파인튜닝 실행
5. 성능 평가 및 개선

