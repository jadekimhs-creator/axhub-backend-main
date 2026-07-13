import urllib.request
import json

url = "http://localhost:8281/mcp/api/v1/tools/call"
data = {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
        "name": "send_email",
        "arguments": {
            "emailAddress": "kimhs@shinhanlife.io",
            "subject": "AX HUB MCP 연동 테스트 메일",
            "body": "안녕하세요 김형식님, AI 어시스턴트가 정상적으로 연동되어 발송하는 테스트 이메일입니다."
        }
    }
}

req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except urllib.error.URLError as e:
    print(f"Error: {e}")
