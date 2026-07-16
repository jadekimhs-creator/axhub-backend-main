import requests
import json
import threading
import time

URL = "https://axhub-gateway.onrender.com/mcp/custom/common"

def read_sse(response):
    print("\n[SSE Stream Open]")
    for line in response.iter_lines():
        if line:
            decoded_line = line.decode('utf-8')
            print(f"[SSE] {decoded_line}")
            if decoded_line.startswith("data:"):
                try:
                    # Ignore the endpoint URL event which is not json
                    if "sessionId=" in decoded_line:
                        continue
                    data = json.loads(decoded_line[5:])
                    if "id" in data:
                        print(f"      => Received response for ID: {data['id']}")
                except json.JSONDecodeError:
                    pass

# 1. Initialize
init_payload = {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
        "protocolVersion": "2025-06-18",
        "capabilities": {},
        "clientInfo": {"name": "toolbox-executor", "version": "0.1.0"}
    }
}
print("1. Sending initialize request...")
resp1 = requests.post(URL, json=init_payload, stream=True)
print("HTTP Status:", resp1.status_code)
session_id = resp1.headers.get("Mcp-Session-Id")
print("Received Mcp-Session-Id:", session_id)

# Start reading SSE in a background thread
sse_thread = threading.Thread(target=read_sse, args=(resp1,), daemon=True)
sse_thread.start()

time.sleep(2) # Wait for init response to arrive via SSE

# 2. notifications/initialized
print("\n2. Sending notifications/initialized...")
notif_payload = {
    "jsonrpc": "2.0",
    "method": "notifications/initialized"
}
headers = {"Mcp-Session-Id": session_id, "MCP-Protocol-Version": "2025-06-18"}
resp2 = requests.post(URL, json=notif_payload, headers=headers)
print("HTTP Status:", resp2.status_code)

time.sleep(1)

# 3. tools/list
print("\n3. Sending tools/list...")
list_payload = {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
}
resp3 = requests.post(URL, json=list_payload, headers=headers)
print("HTTP Status:", resp3.status_code)

time.sleep(2)

print("\nFinished Deep Builder Test")
