import os
import re

files = [
    "axhub-tool-sms/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/sms/AxHubToolSmsApplication.java",
    "axhub-tool-email/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/email/AxHubToolEmailApplication.java",
    "axhub-tool-other/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/other/AxHubToolOtherApplication.java"
]

for file_path in files:
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Replace the basePackages
    content = content.replace(
        '{"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}',
        '{"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.biz.mcp.adapter", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}'
    )
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    print(f"Fixed {file_path}")
