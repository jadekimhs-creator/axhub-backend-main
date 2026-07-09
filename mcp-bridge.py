import sys
import json
import urllib.request
import urllib.error

def send_response(response: dict):
    print(json.dumps(response), flush=True)

def handle_message(msg: dict):
    if "method" in msg:
        if msg["method"] == "initialize":
            send_response({
                "jsonrpc": "2.0",
                "id": msg.get("id"),
                "result": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {"tools": {}},
                    "serverInfo": {"name": "axhub-gateway", "version": "1.0.0"}
                }
            })
        elif msg["method"] == "notifications/initialized":
            pass
        elif msg["method"] == "tools/list":
            try:
                req = urllib.request.Request("http://localhost:8081/mcp/api/v1/tools/list")
                with urllib.request.urlopen(req, timeout=5) as response:
                    data = json.loads(response.read().decode())
                    tools = []
                    for t in data.get("result", {}).get("tools", []):
                        schema = t.get("parametersSchema", {})
                        if "type" not in schema:
                            schema["type"] = "object"
                        tools.append({
                            "name": t["toolName"],
                            "description": t.get("description", "No description"),
                            "inputSchema": schema
                        })
                    send_response({
                        "jsonrpc": "2.0",
                        "id": msg.get("id"),
                        "result": {"tools": tools}
                    })
            except Exception as e:
                send_response({"jsonrpc": "2.0", "id": msg.get("id"), "error": {"code": -32603, "message": str(e)}})
        elif msg["method"] == "tools/call":
            try:
                params = msg.get("params", {})
                req_payload = {
                    "jsonrpc": "2.0",
                    "method": "tools/call",
                    "params": {
                        "name": params.get("name"),
                        "arguments": params.get("arguments", {})
                    },
                    "id": msg.get("id")
                }
                req_data = json.dumps(req_payload).encode()
                req = urllib.request.Request(
                    "http://localhost:8081/mcp/api/v1/tools/call",
                    data=req_data,
                    headers={"Content-Type": "application/json", "X-Agent-Id": "Antigravity"}
                )
                with urllib.request.urlopen(req, timeout=10) as response:
                    resp_data = json.loads(response.read().decode())
                    result_obj = resp_data.get("result", resp_data)
                    if "error" in resp_data:
                        result_obj = resp_data["error"]
                    result_text = json.dumps(result_obj, ensure_ascii=False, indent=2)
                    is_error = resp_data.get("error") is not None or (isinstance(result_obj, dict) and result_obj.get("status") == "ERROR")
                    
                    send_response({
                        "jsonrpc": "2.0",
                        "id": msg.get("id"),
                        "result": {
                            "content": [{"type": "text", "text": result_text}],
                            "isError": is_error
                        }
                    })
            except urllib.error.HTTPError as e:
                error_data = e.read().decode()
                send_response({
                    "jsonrpc": "2.0",
                    "id": msg.get("id"),
                    "result": {
                        "content": [{"type": "text", "text": f"HTTP Error {e.code}: {error_data}"}],
                        "isError": True
                    }
                })
            except Exception as e:
                send_response({
                    "jsonrpc": "2.0",
                    "id": msg.get("id"),
                    "result": {
                        "content": [{"type": "text", "text": f"Error calling tool: {e}"}],
                        "isError": True
                    }
                })
        else:
            if "id" in msg:
                send_response({"jsonrpc": "2.0", "id": msg.get("id"), "error": {"code": -32601, "message": "Method not found"}})

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        msg = json.loads(line)
        handle_message(msg)
    except Exception as e:
        pass
