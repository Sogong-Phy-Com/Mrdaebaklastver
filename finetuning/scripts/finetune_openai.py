#!/usr/bin/env python3
"""
OpenAI API를 사용하여 모델 파인튜닝을 수행하는 스크립트
"""

import os
import argparse
import time
from pathlib import Path
from dotenv import load_dotenv
from openai import OpenAI

# 환경 변수 로드
load_dotenv()

def upload_training_file(client: OpenAI, file_path: Path) -> str:
    """학습 파일을 OpenAI에 업로드"""
    print(f"파일 업로드 중: {file_path}")
    
    with open(file_path, 'rb') as f:
        file = client.files.create(
            file=f,
            purpose='fine-tune'
        )
    
    print(f"파일 업로드 완료. File ID: {file.id}")
    return file.id

def create_finetune_job(client: OpenAI, file_id: str, model: str = "gpt-4o-mini", suffix: str = None) -> str:
    """파인튜닝 작업 생성"""
    print(f"파인튜닝 작업 생성 중...")
    
    job_params = {
        "training_file": file_id,
        "model": model,
    }
    
    if suffix:
        job_params["suffix"] = suffix
    
    job = client.fine_tuning.jobs.create(**job_params)
    
    print(f"파인튜닝 작업 생성 완료. Job ID: {job.id}")
    print(f"상태 확인: https://platform.openai.com/finetune")
    return job.id

def check_job_status(client: OpenAI, job_id: str):
    """파인튜닝 작업 상태 확인"""
    while True:
        job = client.fine_tuning.jobs.retrieve(job_id)
        
        status = job.status
        print(f"\n작업 상태: {status}")
        
        if status == "succeeded":
            print(f"\n✅ 파인튜닝 완료!")
            print(f"모델 ID: {job.fine_tuned_model}")
            print(f"\n사용 방법:")
            print(f"application.properties에 다음을 추가하세요:")
            print(f"voice.llm.api-url=https://api.openai.com/v1/chat/completions")
            print(f"voice.llm.model={job.fine_tuned_model}")
            print(f"voice.llm.api-key=your-openai-api-key")
            return job.fine_tuned_model
        elif status == "failed":
            print(f"\n❌ 파인튜닝 실패")
            if hasattr(job, 'error') and job.error:
                print(f"오류: {job.error}")
            return None
        elif status in ["validating_files", "queued", "running"]:
            print(f"진행 중... (약 1분 후 다시 확인)")
            time.sleep(60)
        else:
            print(f"알 수 없는 상태: {status}")
            time.sleep(30)

def list_finetune_jobs(client: OpenAI):
    """파인튜닝 작업 목록 조회"""
    jobs = client.fine_tuning.jobs.list(limit=10)
    
    print("\n=== 최근 파인튜닝 작업 목록 ===")
    for job in jobs.data:
        status_emoji = "✅" if job.status == "succeeded" else "⏳" if job.status == "running" else "❌"
        print(f"{status_emoji} [{job.status}] {job.id}")
        if job.fine_tuned_model:
            print(f"   모델: {job.fine_tuned_model}")
        if job.created_at:
            from datetime import datetime
            created = datetime.fromtimestamp(job.created_at)
            print(f"   생성일: {created}")

def main():
    parser = argparse.ArgumentParser(description="OpenAI 모델 파인튜닝")
    parser.add_argument("--file", type=str, 
                        default="dataset/training_data.jsonl",
                        help="학습 데이터 파일 경로 (기본값: dataset/training_data.jsonl)")
    parser.add_argument("--model", type=str, default="gpt-4o-mini",
                        choices=["gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"],
                        help="기본 모델 선택 (기본값: gpt-4o-mini)")
    parser.add_argument("--suffix", type=str,
                        help="파인튜닝된 모델 이름 접미사")
    parser.add_argument("--upload-only", action="store_true",
                        help="파일만 업로드하고 파인튜닝은 시작하지 않음")
    parser.add_argument("--check", type=str,
                        help="파인튜닝 작업 상태 확인 (Job ID)")
    parser.add_argument("--list", action="store_true",
                        help="파인튜닝 작업 목록 조회")
    
    args = parser.parse_args()
    
    # API 키 확인
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("오류: OPENAI_API_KEY 환경 변수가 설정되지 않았습니다.")
        print("설정 방법:")
        print("1. .env 파일에 OPENAI_API_KEY=your-key 추가")
        print("2. 또는 환경 변수로 설정: export OPENAI_API_KEY=your-key")
        return
    
    client = OpenAI(api_key=api_key)
    
    # 작업 목록 조회
    if args.list:
        list_finetune_jobs(client)
        return
    
    # 상태 확인
    if args.check:
        check_job_status(client, args.check)
        return
    
    # 파일 경로 확인
    script_dir = Path(__file__).parent.parent
    file_path = script_dir / args.file
    
    if not file_path.exists():
        print(f"오류: 파일을 찾을 수 없습니다: {file_path}")
        print(f"먼저 데이터셋을 생성하세요:")
        print(f"python scripts/generate_dataset.py --format openai")
        return
    
    # 파일 업로드
    file_id = upload_training_file(client, file_path)
    
    if args.upload_only:
        print(f"\n파일 업로드 완료. File ID: {file_id}")
        print(f"나중에 파인튜닝을 시작하려면:")
        print(f"python scripts/finetune_openai.py --check {file_id}")
        return
    
    # 파인튜닝 작업 생성
    suffix = args.suffix or "mrdabak-voice-order"
    job_id = create_finetune_job(client, file_id, args.model, suffix)
    
    # 상태 확인
    print(f"\n파인튜닝 작업이 시작되었습니다.")
    print(f"진행 상황을 확인하려면:")
    print(f"python scripts/finetune_openai.py --check {job_id}")
    print(f"\n또는 자동으로 상태를 모니터링합니다...")
    
    check_job_status(client, job_id)

if __name__ == "__main__":
    main()

