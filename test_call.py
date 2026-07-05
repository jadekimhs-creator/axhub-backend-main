import urllib.request
import json

payload = {
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "grade",
    "arguments": { "cust_id": "C001" }
  },
  "id": 1
}

url = 'http://localhost:8081/mcp/api/v1/tools/call'
headers = {
    'Content-Type': 'application/json',
    'X-API-KEY': 'SHINHAN_MCP_TEST_KEY_9999'
}

data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(url, data=data, headers=headers, method='POST')

try:
    with urllib.request.urlopen(req) as response:
        print(f"Response: {response.read().decode('utf-8')}")
except Exception as e:
    print(f"Error: {e}")
