import os

apps = {
    "axhub-gateway": {
        "pkg": "io.shinhanlife.axhub.biz.mcp.gateway",
        "cls": "AxHubGatewayApplication",
        "path": "axhub-gateway/src/main/java/io/shinhanlife/axhub/biz/mcp/gateway/AxHubGatewayApplication.java"
    },
    "axhub-tool-sms": {
        "pkg": "io.shinhanlife.axhub.biz.mcp.tool.sms",
        "cls": "AxHubToolSmsApplication",
        "path": "axhub-tool-sms/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/sms/AxHubToolSmsApplication.java"
    },
    "axhub-tool-email": {
        "pkg": "io.shinhanlife.axhub.biz.mcp.tool.email",
        "cls": "AxHubToolEmailApplication",
        "path": "axhub-tool-email/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/email/AxHubToolEmailApplication.java"
    },
    "axhub-tool-other": {
        "pkg": "io.shinhanlife.axhub.biz.mcp.tool.other",
        "cls": "AxHubToolOtherApplication",
        "path": "axhub-tool-other/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/other/AxHubToolOtherApplication.java"
    }
}

for module, info in apps.items():
    dir_path = os.path.dirname(info["path"])
    os.makedirs(dir_path, exist_ok=True)
    content = f"""package {info['pkg']};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class {info['cls']} {{
    public static void main(String[] args) {{
        SpringApplication.run({info['cls']}.class, args);
    }}
}}
"""
    with open(info["path"], "w", encoding="utf-8") as f:
        f.write(content)

print("Application classes recreated.")
