import os
import re

pattern = re.compile(r'name\s*=\s*"[^"]*",\s*description\s*=\s*"[^"]*",\s*group\s*=\s*"[^"]*",?\s*')

for root, _, files in os.walk('.'):
    for f in files:
        if f.endswith('.java'):
            p = os.path.join(root, f)
            try:
                content = open(p, encoding='utf-8').read()
            except Exception:
                content = open(p, encoding='cp949').read()
                
            new_content = pattern.sub('', content)
            
            # Clean up empty parens like @McpTool()
            new_content = new_content.replace('@McpTool(\n    \n)', '@McpTool')
            new_content = new_content.replace('@McpTool()', '@McpTool')
            
            if content != new_content:
                with open(p, 'w', encoding='utf-8') as out:
                    out.write(new_content)
                print(f"Updated {p}")
