import os

file_path = r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-tool-core\src\main\java\io\shinhanlife\axhub\biz\mcp\tool\presentation\BusinessToolController.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

old_block = """            Map<String, Object> innerResult = new HashMap<>();
            if (methodResult instanceof Map) {
                innerResult.putAll((Map<String, Object>) methodResult);
            }
            if (!innerResult.containsKey("contracts")) {
                List<Object> contracts = new ArrayList<>();
                if (methodResult != null) {
                    contracts.add(methodResult);
                }
                innerResult.put("contracts", contracts);
            }"""

new_block = """            Map<String, Object> innerResult = new HashMap<>();
            if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("contracts")) {
                innerResult.putAll((Map<String, Object>) methodResult);
            } else {
                List<Object> contracts = new ArrayList<>();
                if (methodResult != null) {
                    contracts.add(methodResult);
                }
                innerResult.put("contracts", contracts);
                
                if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("status")) {
                    innerResult.put("status", ((Map<?, ?>) methodResult).get("status"));
                } else {
                    innerResult.put("status", "SUCCESS");
                }
            }"""

old_size = 'resultPayload.put("original_size", 0);'

new_size = """            int originalSize = 0;
            try {
                if (methodResult instanceof Map && ((Map<?, ?>) methodResult).containsKey("legacy_response")) {
                    Object legacyResp = ((Map<?, ?>) methodResult).get("legacy_response");
                    if (legacyResp instanceof String) {
                        originalSize = ((String) legacyResp).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    }
                } else {
                    String jsonStr = objectMapper.writeValueAsString(innerResult);
                    originalSize = jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
            } catch (Exception e) {
                log.warn("Failed to calculate original_size", e);
            }
            resultPayload.put("original_size", originalSize);"""

content = content.replace(old_block, new_block)
content = content.replace(old_size, new_size)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied to BusinessToolController.java")
