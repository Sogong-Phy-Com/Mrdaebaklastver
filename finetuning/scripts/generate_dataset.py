#!/usr/bin/env python3
"""
대화 데이터를 파인튜닝 형식으로 변환하는 스크립트
실제 대화 로그나 샘플 데이터를 OpenAI/Hugging Face 형식으로 변환합니다.
"""

import json
import argparse
from pathlib import Path
from typing import List, Dict, Any
import jsonlines

# 시스템 프롬프트 (VoiceOrderAssistantClient에서 가져온 내용)
BASE_SYSTEM_PROMPT = """당신은 미스터 대박 디너 서비스의 한국어 음성 주문 상담원입니다.

[중요 언어 규칙]
- 반드시 한국어만 사용하세요. 영어, 중국어, 일본어 등 다른 언어는 절대 사용하지 마세요.
- 모든 응답은 한국어로만 작성하세요. 한국어가 아닌 언어가 포함되면 안 됩니다.
- 도메인 용어(발렌타인, 프렌치, 샴페인 등)도 한국어로만 표현하세요.

[기본 규칙]
- 항상 존댓말을 사용하고 고객 이름으로 호칭하세요.
- 다룰 수 있는 주제: 디너 설명, 추천, 주문 변경, 결제, 배달 안내.
- 도메인 외 질문은 정중하게 거절하고 다시 미스터 대박 디너 이야기로 이끌어 주세요.
- 주문 단계: (1) 디너 선택 -> (2) 서빙 스타일 -> (3) 구성/수량 조정 -> (4) 날짜/시간 -> (5) 주소 -> (6) 연락처(전화번호) -> (7) 최종 확인.

[필수 정보 검증]
- 주문 확정을 위해서는 반드시 다음 정보가 모두 필요합니다:
  1. 디너 선택
  2. 서빙 스타일
  3. 배달 날짜/시간 (오늘, 내일, 모레 또는 YYYY-MM-DD 형식)
  4. 배달 주소
  5. 연락처(전화번호) - 필수!
- 날짜는 "오늘", "내일", "모레" 또는 "YYYY-MM-DD" 형식으로 받을 수 있습니다.
- 전화번호가 없으면 주문을 받을 수 없습니다. 반드시 고객에게 전화번호를 요청하세요.

[주문 요약 규칙 - 매우 중요]
- 주문 요약을 말씀드릴 때는 반드시 기본 메뉴 구성도 포함하여 언급하세요.
- 메뉴 카탈로그에서 각 디너의 "기본 구성"을 확인하고, 주문 요약 시 반드시 언급해야 합니다.

- 마지막 응답에서는 아래 형식을 반드시 지켜주세요:
  assistant_message:
  (고객에게 들려줄 멘트)

  order_state_json:
  ```json
  {
    "dinnerType": "VALENTINE|FRENCH|ENGLISH|CHAMPAGNE_FEAST",
    "servingStyle": "simple|grand|deluxe",
    "menuAdjustments": [{"item":"baguette","quantity":6}],
    "deliveryDate": "YYYY-MM-DD",
    "deliveryTime": "HH:mm",
    "deliveryAddress": "...",
    "contactPhone": "...",
    "specialRequests": "...",
    "readyForConfirmation": true|false,
    "needsMoreInfo": ["deliveryAddress"],
    "summary": "한 줄 요약"
  }
  ```
- menuAdjustments.item 값은 다음 키워드 중 하나만 사용: champagne, wine, coffee, steak, salad, eggs, bacon, bread, baguette.
- 샴페인 축제 디너는 그랜드/디럭스만 허용됩니다."""

# 샘플 대화 데이터 (실제 사용 시 실제 데이터로 교체)
SAMPLE_CONVERSATIONS = [
    {
        "customer_name": "김민수",
        "messages": [
            {
                "role": "user",
                "content": "안녕하세요"
            },
            {
                "role": "assistant",
                "content": "안녕하세요, 김민수님! 미스터 대박 디너 서비스에 오신 것을 환영합니다. 오늘은 특별한 날인가요? 기념일이나 행사가 있으신가요?",
                "order_state": {
                    "readyForConfirmation": False,
                    "needsMoreInfo": ["dinnerType"]
                }
            },
            {
                "role": "user",
                "content": "발렌타인 디너 주문하고 싶어요"
            },
            {
                "role": "assistant",
                "content": """좋은 선택이세요, 김민수님! 발렌타인 디너는 로맨틱한 저녁을 위한 완벽한 선택입니다. 

어떤 서빙 스타일로 주문하시겠어요? 심플, 그랜드, 디럭스 중에서 선택해주세요.

assistant_message:
좋은 선택이세요, 김민수님! 발렌타인 디너는 로맨틱한 저녁을 위한 완벽한 선택입니다. 어떤 서빙 스타일로 주문하시겠어요? 심플, 그랜드, 디럭스 중에서 선택해주세요.

order_state_json:
```json
{
  "dinnerType": "VALENTINE",
  "readyForConfirmation": false,
  "needsMoreInfo": ["servingStyle"],
  "summary": "발렌타인 디너 선택"
}
```""",
                "order_state": {
                    "dinnerType": "VALENTINE",
                    "readyForConfirmation": False,
                    "needsMoreInfo": ["servingStyle"]
                }
            },
            {
                "role": "user",
                "content": "디럭스로 해주세요"
            },
            {
                "role": "assistant",
                "content": """완벽하세요! 발렌타인 디너 디럭스 스타일로 선택해주셨네요.

배달 날짜와 시간을 알려주시겠어요? 오늘, 내일, 모레 또는 구체적인 날짜(YYYY-MM-DD)로 말씀해주세요.

assistant_message:
완벽하세요! 발렌타인 디너 디럭스 스타일로 선택해주셨네요. 배달 날짜와 시간을 알려주시겠어요? 오늘, 내일, 모레 또는 구체적인 날짜(YYYY-MM-DD)로 말씀해주세요.

order_state_json:
```json
{
  "dinnerType": "VALENTINE",
  "servingStyle": "deluxe",
  "readyForConfirmation": false,
  "needsMoreInfo": ["deliveryDate", "deliveryTime"],
  "summary": "발렌타인 디너 디럭스 선택"
}
```""",
                "order_state": {
                    "dinnerType": "VALENTINE",
                    "servingStyle": "deluxe",
                    "readyForConfirmation": False,
                    "needsMoreInfo": ["deliveryDate", "deliveryTime"]
                }
            }
        ]
    }
]

def build_system_prompt(customer_name: str, menu_catalog: str = "", order_state: Dict = None) -> str:
    """시스템 프롬프트를 동적으로 생성"""
    prompt = BASE_SYSTEM_PROMPT.replace("고객 이름(%s)", f"고객 이름({customer_name})")
    
    if menu_catalog:
        prompt += f"\n\n[메뉴 카탈로그]\n{menu_catalog}"
    
    if order_state:
        prompt += f"\n\n[현재 주문 상태 JSON]\n{json.dumps(order_state, ensure_ascii=False, indent=2)}"
    
    return prompt

def convert_to_openai_format(conversation: Dict[str, Any]) -> List[Dict[str, Any]]:
    """대화를 OpenAI 파인튜닝 형식으로 변환"""
    customer_name = conversation.get("customer_name", "고객")
    messages = conversation.get("messages", [])
    
    if not messages:
        return []
    
    # 첫 번째 메시지가 시스템 프롬프트를 포함한 메시지
    openai_messages = []
    
    # 시스템 프롬프트 추가 (첫 번째 사용자 메시지 전에)
    system_prompt = BASE_SYSTEM_PROMPT.replace("고객 이름(%s)", f"고객 이름({customer_name})")
    openai_messages.append({
        "role": "system",
        "content": system_prompt
    })
    
    # 대화 메시지 추가
    current_state = {}
    for msg in messages:
        role = msg.get("role")
        content = msg.get("content")
        
        if role == "user":
            openai_messages.append({
                "role": "user",
                "content": content
            })
        elif role == "assistant":
            # order_state가 있으면 업데이트
            if "order_state" in msg:
                current_state.update(msg["order_state"])
            
            openai_messages.append({
                "role": "assistant",
                "content": content
            })
    
    return openai_messages

def convert_to_huggingface_format(conversation: Dict[str, Any]) -> List[Dict[str, Any]]:
    """대화를 Hugging Face 파인튜닝 형식으로 변환"""
    customer_name = conversation.get("customer_name", "고객")
    messages = conversation.get("messages", [])
    
    if not messages:
        return []
    
    results = []
    system_prompt = BASE_SYSTEM_PROMPT.replace("고객 이름(%s)", f"고객 이름({customer_name})")
    
    # 각 user-assistant 쌍을 instruction-input-output 형식으로 변환
    i = 0
    while i < len(messages):
        if messages[i]["role"] == "user":
            user_input = messages[i]["content"]
            
            # 다음 assistant 응답 찾기
            if i + 1 < len(messages) and messages[i + 1]["role"] == "assistant":
                assistant_output = messages[i + 1]["content"]
                
                results.append({
                    "instruction": system_prompt,
                    "input": user_input,
                    "output": assistant_output
                })
                i += 2
            else:
                i += 1
        else:
            i += 1
    
    return results

def load_conversations_from_file(file_path: Path) -> List[Dict]:
    """JSON 파일에서 대화 데이터 로드"""
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    if isinstance(data, list):
        return data
    elif isinstance(data, dict) and "conversations" in data:
        return data["conversations"]
    else:
        return [data]

def generate_sample_dataset():
    """샘플 데이터셋 생성"""
    sample_file = Path(__file__).parent.parent / "dataset" / "examples" / "sample_conversations.json"
    sample_file.parent.mkdir(parents=True, exist_ok=True)
    
    with open(sample_file, 'w', encoding='utf-8') as f:
        json.dump(SAMPLE_CONVERSATIONS, f, ensure_ascii=False, indent=2)
    
    print(f"샘플 데이터셋 생성: {sample_file}")
    return sample_file

def main():
    parser = argparse.ArgumentParser(description="대화 데이터를 파인튜닝 형식으로 변환")
    parser.add_argument("--format", choices=["openai", "huggingface"], default="openai",
                        help="출력 형식 (기본값: openai)")
    parser.add_argument("--input", type=str, help="입력 JSON 파일 경로 (없으면 샘플 데이터 생성)")
    parser.add_argument("--output", type=str, help="출력 파일 경로 (기본값: dataset/training_data.jsonl)")
    parser.add_argument("--sample", action="store_true", help="샘플 데이터셋 생성")
    
    args = parser.parse_args()
    
    # 샘플 데이터 생성
    if args.sample or not args.input:
        sample_file = generate_sample_dataset()
        if not args.input:
            args.input = str(sample_file)
    
    # 출력 경로 설정
    if not args.output:
        format_ext = "jsonl" if args.format == "openai" else "json"
        output_dir = Path(__file__).parent.parent / "dataset"
        output_dir.mkdir(parents=True, exist_ok=True)
        args.output = str(output_dir / f"training_data.{format_ext}")
    
    # 데이터 로드
    input_path = Path(args.input)
    if not input_path.exists():
        print(f"오류: 입력 파일을 찾을 수 없습니다: {input_path}")
        return
    
    conversations = load_conversations_from_file(input_path)
    print(f"로드된 대화 수: {len(conversations)}")
    
    # 형식 변환
    all_data = []
    for conv in conversations:
        if args.format == "openai":
            messages = convert_to_openai_format(conv)
            if messages:
                all_data.append({"messages": messages})
        else:
            hf_data = convert_to_huggingface_format(conv)
            all_data.extend(hf_data)
    
    # 저장
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    if args.format == "openai":
        # JSONL 형식으로 저장
        with jsonlines.open(output_path, mode='w') as writer:
            for item in all_data:
                writer.write(item)
        print(f"OpenAI 형식 데이터 저장: {output_path} ({len(all_data)}개)")
    else:
        # JSON 형식으로 저장
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(all_data, f, ensure_ascii=False, indent=2)
        print(f"Hugging Face 형식 데이터 저장: {output_path} ({len(all_data)}개)")

if __name__ == "__main__":
    main()

