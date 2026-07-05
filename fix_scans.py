import os

files_to_fix = {
    "axhub-gateway/src/main/java/io/shinhanlife/axhub/biz/mcp/gateway/AxHubGatewayApplication.java": '{"io.shinhanlife.axhub.biz.mcp.gateway", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}',
    "axhub-tool-sms/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/sms/AxHubToolSmsApplication.java": '{"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}',
    "axhub-tool-email/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/email/AxHubToolEmailApplication.java": '{"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}',
    "axhub-tool-other/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/other/AxHubToolOtherApplication.java": '{"io.shinhanlife.axhub.biz.mcp.tool", "io.shinhanlife.axhub.common.mcp", "io.shinhanlife.axhub.common.config"}',
}

for filepath, packages in files_to_fix.items():
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # regex replace the content in the string
    import re
    content = re.sub(r'scanBasePackages = \{.*?\}', f'scanBasePackages = {packages}', content)
    content = re.sub(r'basePackages = \{.*?\}', f'basePackages = {packages}', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Fixed component scans")
