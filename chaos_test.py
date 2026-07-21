import requests
import json
import threading
import time

BASE_URL = "https://axhub-gateway.onrender.com/mcp/custom/common"
TOOL_NAME = "other_get_current_weather"

results = []

def run_test(test_name, payload_args):
    print(f"\n[{test_name}] 시작...")

    try:
        # 1. Initialize (서버 슬립 해제 포함, 타임아웃 넉넉하게)
        init_resp = requests.post(BASE_URL, json={
            "jsonrpc": "2.0", "id": 1, "method": "initialize",
            "params": {
                "protocolVersion": "2025-06-18",
                "capabilities": {},
                "clientInfo": {"name": "chaos-tester", "version": "1.0.0"}
            }
        }, stream=True, timeout=60)

        session_id = init_resp.headers.get("Mcp-Session-Id")
        if not session_id:
            results.append({"name": test_name, "payload": str(payload_args), "result": "세션 수립 실패", "safety": "알 수 없음"})
            print(f"  세션 수립 실패")
            return

        print(f"  세션 ID: {session_id[:16]}...")
        headers = {"Mcp-Session-Id": session_id}

        sse_result = {"data": None, "raw": []}

        def read_sse(resp):
            for line in resp.iter_lines():
                if line:
                    decoded = line.decode("utf-8")
                    sse_result["raw"].append(decoded)
                    if decoded.startswith("data:"):
                        raw = decoded[5:].strip()
                        if raw and not raw.startswith("/mcp"):
                            try:
                                parsed = json.loads(raw)
                                # tools/call 응답 (id=2)만 저장
                                if parsed.get("id") == 2:
                                    sse_result["data"] = parsed
                            except:
                                pass

        sse_thread = threading.Thread(target=read_sse, args=(init_resp,), daemon=True)
        sse_thread.start()
        time.sleep(1)

        # 2. notifications/initialized
        requests.post(BASE_URL, json={"jsonrpc": "2.0", "method": "notifications/initialized"},
                      headers=headers, timeout=30)
        time.sleep(0.5)

        # 3. tools/call with chaos payload
        print(f"  페이로드 주입 중: {json.dumps(payload_args, ensure_ascii=False)[:80]}")
        call_resp = requests.post(BASE_URL, json={
            "jsonrpc": "2.0", "id": 2, "method": "tools/call",
            "params": {"name": TOOL_NAME, "arguments": payload_args}
        }, headers=headers, timeout=30)

        print(f"  HTTP Status: {call_resp.status_code}")

        # SSE 응답 대기 (최대 5초)
        for _ in range(10):
            time.sleep(0.5)
            if sse_result["data"]:
                break

        tool_result = sse_result["data"]
        http_status = call_resp.status_code

        # 결과 분석
        server_died = False
        is_error = False
        response_summary = "SSE 응답 없음 (타임아웃)"

        if tool_result:
            result_body = tool_result.get("result", {})
            error_body  = tool_result.get("error", {})

            if error_body:
                is_error = True
                response_summary = f"MCP Error: {json.dumps(error_body, ensure_ascii=False)[:100]}"
            elif isinstance(result_body, dict):
                is_err_flag = result_body.get("isError", False)
                content = result_body.get("content", [])
                text = content[0].get("text", "") if content else str(result_body)
                if is_err_flag or "500" in text or "Exception" in text:
                    server_died = True
                    response_summary = f"서버 에러: {text[:100]}"
                else:
                    response_summary = f"정상 응답: {text[:100]}"
        elif http_status >= 500:
            server_died = True
            response_summary = f"HTTP {http_status} 서버 에러"

        if server_died:
            safety = "위험 - 서버 500 에러"
        elif is_error:
            safety = "안전 - Validation 에러 반환"
        else:
            safety = f"실행됨 (HTTP {http_status})"

        results.append({
            "name": test_name,
            "payload": json.dumps(payload_args, ensure_ascii=False)[:60],
            "result": response_summary,
            "safety": safety
        })

        print(f"  결과: {response_summary}")
        print(f"  판정: {safety}")

    except requests.exceptions.ReadTimeout:
        results.append({"name": test_name, "payload": str(payload_args), "result": "서버 응답 타임아웃", "safety": "판정 불가"})
        print(f"  타임아웃 발생")
    except Exception as e:
        results.append({"name": test_name, "payload": str(payload_args), "result": f"오류: {str(e)[:80]}", "safety": "판정 불가"})
        print(f"  오류: {e}")


# ===== 서버 웨이크업 =====
print("=" * 60)
print("서버 웨이크업 중... (최초 응답에 시간이 걸릴 수 있습니다)")
try:
    r = requests.get("https://axhub-gateway.onrender.com/actuator/health", timeout=60)
    print(f"서버 상태: {r.status_code}")
except:
    print("헬스체크 엔드포인트 없음 - 바로 테스트 진행")

print("=" * 60)
print(f"CHAOS TEST: {TOOL_NAME}")
print("=" * 60)

# 1. 타입 브레이커: city에 10000자 문자열
run_test("1. 타입 브레이커 (10000자 문자열)", {"city": "A" * 10000})
time.sleep(2)

# 2. 필수 파라미터 누락
run_test("2. 필수 파라미터 누락 (city 없음)", {})
time.sleep(2)

# 3. XSS + SQL Injection
run_test("3. 악의적 페이로드 (XSS + SQL Injection)", {"city": "<script>alert(1)</script>' OR 1=1 --"})

# ===== 결과 리포트 =====
print("\n\n" + "=" * 80)
print("CHAOS TEST 결과 리포트 - other_get_current_weather")
print("=" * 80)
print(f"{'테스트 시나리오':<35} {'판정':<30} {'서버 응답'}")
print("-" * 100)
for r in results:
    print(f"{r['name']:<35} {r['safety']:<30} {r['result'][:40]}")
print("=" * 80)
