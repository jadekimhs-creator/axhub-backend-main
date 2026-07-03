import os

project_root = r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main"

replacements = {
    "package io.shinhanlife.axhub.biz.mcp.tool.dto;": "package io.shinhanlife.axhub.common.mcp.tool.dto;",
    "package io.shinhanlife.axhub.biz.mcp.adapter.dto;": "package io.shinhanlife.axhub.common.mcp.adapter.dto;",
    "package io.shinhanlife.axhub.biz.mcp.gateway.dto;": "package io.shinhanlife.axhub.common.mcp.gateway.dto;",
    "import io.shinhanlife.axhub.biz.mcp.tool.dto": "import io.shinhanlife.axhub.common.mcp.tool.dto",
    "import io.shinhanlife.axhub.biz.mcp.adapter.dto": "import io.shinhanlife.axhub.common.mcp.adapter.dto",
    "import io.shinhanlife.axhub.biz.mcp.gateway.dto": "import io.shinhanlife.axhub.common.mcp.gateway.dto"
}

for root, dirs, files in os.walk(project_root):
    for file in files:
        if file.endswith(".java"):
            file_path = os.path.join(root, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            modified = False
            for old, new in replacements.items():
                if old in content:
                    content = content.replace(old, new)
                    modified = True
            
            if modified:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {file_path}")
