#!/bin/bash
# Spring Boot 로그 파일 경로 (기본 설정: ./logs/spring-boot-logger.log)
LOG_FILE="./logs/spring-boot-logger.log"
SEARCH_TARGET=$1

if [ -z "$SEARCH_TARGET" ]; then
  echo "추적할 Request ID나 Tool 이름을 입력해주세요."
  exit 1
fi

if [ ! -f "$LOG_FILE" ]; then
  echo "오류: 로그 파일($LOG_FILE)을 찾을 수 없습니다. 경로를 확인해주세요."
  exit 1
fi

echo "[$SEARCH_TARGET] 라우팅 추적 시작..."
# Spring Boot 로그에서 대상 검색하여 주요 필드 출력
grep -i "$SEARCH_TARGET" "$LOG_FILE" | awk '{print $1, $2, $3, $5, $NF}'
