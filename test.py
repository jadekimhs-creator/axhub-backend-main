import urllib.request
import json

url = 'http://localhost:8281/mcp'
data = {
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "other_register_vacation",
    "arguments": {
      "date": "2026-07-15",
      "employeeId": "010-9999-1111",
      "unknownData": "이건 차단되어야 해"
    }
  }
}

req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except Exception as e:
    print(e)
