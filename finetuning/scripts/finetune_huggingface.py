#!/usr/bin/env python3
"""
Hugging Face Transformers를 사용하여 모델 파인튜닝을 수행하는 스크립트
로컬 또는 클라우드에서 오픈소스 모델을 파인튜닝합니다.
"""

import os
import argparse
import json
from pathlib import Path
from dotenv import load_dotenv
from datasets import load_dataset
from transformers import (
    AutoTokenizer,
    AutoModelForCausalLM,
    TrainingArguments,
    Trainer,
    DataCollatorForLanguageModeling
)
from peft import LoraConfig, get_peft_model, TaskType

# 환경 변수 로드
load_dotenv()

def load_training_data(file_path: Path):
    """학습 데이터 로드"""
    print(f"학습 데이터 로드 중: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"로드된 데이터 샘플 수: {len(data)}")
    return data

def format_prompt(example):
    """프롬프트 포맷팅"""
    instruction = example.get("instruction", "")
    input_text = example.get("input", "")
    output = example.get("output", "")
    
    if input_text:
        prompt = f"### 지시사항:\n{instruction}\n\n### 입력:\n{input_text}\n\n### 응답:\n{output}"
    else:
        prompt = f"### 지시사항:\n{instruction}\n\n### 응답:\n{output}"
    
    return {"text": prompt}

def tokenize_function(examples, tokenizer, max_length=2048):
    """토큰화 함수"""
    return tokenizer(
        examples["text"],
        truncation=True,
        max_length=max_length,
        padding="max_length",
    )

def main():
    parser = argparse.ArgumentParser(description="Hugging Face 모델 파인튜닝")
    parser.add_argument("--file", type=str,
                        default="dataset/training_data.json",
                        help="학습 데이터 파일 경로 (기본값: dataset/training_data.json)")
    parser.add_argument("--model", type=str, default="meta-llama/Llama-3.2-3B-Instruct",
                        help="기본 모델 이름 (기본값: meta-llama/Llama-3.2-3B-Instruct)")
    parser.add_argument("--output-dir", type=str, default="finetuned_model",
                        help="파인튜닝된 모델 저장 디렉토리")
    parser.add_argument("--epochs", type=int, default=3,
                        help="학습 에포크 수 (기본값: 3)")
    parser.add_argument("--batch-size", type=int, default=4,
                        help="배치 크기 (기본값: 4)")
    parser.add_argument("--learning-rate", type=float, default=2e-4,
                        help="학습률 (기본값: 2e-4)")
    parser.add_argument("--use-lora", action="store_true",
                        help="LoRA (Low-Rank Adaptation) 사용")
    
    args = parser.parse_args()
    
    # 파일 경로 확인
    script_dir = Path(__file__).parent.parent
    file_path = script_dir / args.file
    
    if not file_path.exists():
        print(f"오류: 파일을 찾을 수 없습니다: {file_path}")
        print(f"먼저 데이터셋을 생성하세요:")
        print(f"python scripts/generate_dataset.py --format huggingface")
        return
    
    # 학습 데이터 로드
    data = load_training_data(file_path)
    
    # 데이터셋 준비
    print("\n데이터셋 준비 중...")
    formatted_data = [format_prompt(ex) for ex in data]
    
    # Hugging Face Dataset 형식으로 변환
    from datasets import Dataset
    dataset = Dataset.from_list(formatted_data)
    
    # 데이터셋 분할 (학습 90%, 검증 10%)
    dataset = dataset.train_test_split(test_size=0.1, seed=42)
    train_dataset = dataset["train"]
    eval_dataset = dataset["test"]
    
    print(f"학습 데이터: {len(train_dataset)}개")
    print(f"검증 데이터: {len(eval_dataset)}개")
    
    # 토크나이저 로드
    print(f"\n모델 및 토크나이저 로드 중: {args.model}")
    try:
        tokenizer = AutoTokenizer.from_pretrained(args.model)
        if tokenizer.pad_token is None:
            tokenizer.pad_token = tokenizer.eos_token
        
        model = AutoModelForCausalLM.from_pretrained(
            args.model,
            device_map="auto",
            torch_dtype="auto"
        )
    except Exception as e:
        print(f"모델 로드 실패: {e}")
        print(f"\n대안: 더 작은 모델을 사용하세요:")
        print(f"python scripts/finetune_huggingface.py --model microsoft/Phi-3-mini-4k-instruct")
        return
    
    # LoRA 설정 (선택적)
    if args.use_lora:
        print("\nLoRA 적용 중...")
        lora_config = LoraConfig(
            task_type=TaskType.CAUSAL_LM,
            inference_mode=False,
            r=8,
            lora_alpha=16,
            lora_dropout=0.1,
            target_modules=["q_proj", "v_proj", "k_proj", "o_proj"]
        )
        model = get_peft_model(model, lora_config)
        model.print_trainable_parameters()
    
    # 토큰화
    print("\n데이터 토큰화 중...")
    tokenized_train = train_dataset.map(
        lambda x: tokenize_function(x, tokenizer),
        batched=True,
        remove_columns=train_dataset.column_names
    )
    tokenized_eval = eval_dataset.map(
        lambda x: tokenize_function(x, tokenizer),
        batched=True,
        remove_columns=eval_dataset.column_names
    )
    
    # 학습 인자 설정
    output_dir = script_dir / args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        learning_rate=args.learning_rate,
        logging_dir=str(output_dir / "logs"),
        logging_steps=10,
        eval_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        save_total_limit=3,
        warmup_steps=100,
        fp16=True,
        gradient_accumulation_steps=4,
    )
    
    # 데이터 콜레이터
    data_collator = DataCollatorForLanguageModeling(
        tokenizer=tokenizer,
        mlm=False
    )
    
    # 트레이너 생성
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=tokenized_train,
        eval_dataset=tokenized_eval,
        data_collator=data_collator,
    )
    
    # 학습 시작
    print(f"\n{'='*50}")
    print(f"파인튜닝 시작")
    print(f"{'='*50}")
    print(f"모델: {args.model}")
    print(f"학습 데이터: {len(train_dataset)}개")
    print(f"검증 데이터: {len(eval_dataset)}개")
    print(f"에포크: {args.epochs}")
    print(f"배치 크기: {args.batch_size}")
    print(f"학습률: {args.learning_rate}")
    print(f"{'='*50}\n")
    
    trainer.train()
    
    # 모델 저장
    print(f"\n모델 저장 중: {output_dir}")
    trainer.save_model()
    tokenizer.save_pretrained(output_dir)
    
    print(f"\n✅ 파인튜닝 완료!")
    print(f"모델 저장 위치: {output_dir}")
    print(f"\n사용 방법:")
    print(f"로컬에서 모델을 서빙하거나 Hugging Face에 업로드하세요.")

if __name__ == "__main__":
    main()

