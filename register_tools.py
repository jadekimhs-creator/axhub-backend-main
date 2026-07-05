import urllib.request
import urllib.parse
import json

tools = [
    {
      "toolName": "grade",
      "description": "고객등급 조회",
      "podUrl": "http://tool-other:8084",
      "domainGroup": "biz_support",
      "integrationType": "TCP",
      "mciServiceId": "CRM_001",
      "parametersSchema": {
        "type": "object",
        "properties": {
          "reqId": { "type": "string" }
        }
      },
      "actionPrompts": {
        "grade": "이 고객의 VIP 등급을 조회해줘."
      }
    },
    {
      "toolName": "detail",
      "description": "고객상세 정보 조회",
      "podUrl": "http://tool-other:8084",
      "domainGroup": "biz_support",
      "integrationType": "TCP",
      "mciServiceId": "CRM_002",
      "parametersSchema": {
        "type": "object",
        "properties": {
          "reqId": { "type": "string" }
        }
      },
      "actionPrompts": {
        "detail": "이 고객의 상세 기본정보(주소, 연락처 등)를 알려줘."
      }
    }
]

url = 'http://localhost:8081/mcp/api/v1/registry/register'
headers = {
    'Content-Type': 'application/json',
    'X-API-KEY': 'SHINHAN_MCP_TEST_KEY_9999'
}

for tool in tools:
    data = json.dumps(tool).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers=headers, method='POST')
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Registered {tool['toolName']}: {response.read().decode('utf-8')}")
    except Exception as e:
        print(f"Error registering {tool['toolName']}: {e}")
