import os
import glob
import re

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        with open(filepath, 'r', encoding='cp949') as f:
            content = f.read()

    # Remove @Data
    content = re.sub(r'^\s*@Data\s*\n', '', content, flags=re.MULTILINE)

    # Skip interfaces and enums
    if 'public interface ' in content or 'public enum ' in content:
        return
        
    if 'public abstract class ' in content:
        return

    # Add imports
    imports = ["lombok.Getter", "lombok.Setter", "lombok.Builder", "lombok.NoArgsConstructor", "lombok.AllArgsConstructor"]
    import_block = ""
    for imp in imports:
        if f"import {imp};" not in content:
            import_block += f"import {imp};\n"

    if import_block:
        content = re.sub(r'^(package\s+[^;]+;)\s*\n', f"\\1\n\n{import_block}", content, count=1, flags=re.MULTILINE)

    # Add annotations
    annotations = []
    if '@Getter' not in content: annotations.append('@Getter')
    if '@Setter' not in content: annotations.append('@Setter')
    if '@Builder' not in content: annotations.append('@Builder')
    if '@NoArgsConstructor' not in content: annotations.append('@NoArgsConstructor')
    if '@AllArgsConstructor' not in content: annotations.append('@AllArgsConstructor')

    if annotations:
        anno_str = '\n'.join(annotations) + '\n'
        content = re.sub(r'^(public\s+class\s+)', f"{anno_str}\\1", content, count=1, flags=re.MULTILINE)

    # Remove manual ToolMetadata() and ZtUsacInDto() constructors
    content = re.sub(r'^\s*public\s+ToolMetadata\(\)\s*\{\}\s*\n', '', content, flags=re.MULTILINE)
    content = re.sub(r'^\s*public\s+ZtUsacInDto\(\)\s*\{\s*setPuseYn\("Y"\);\s*\}\s*\n', '', content, flags=re.MULTILINE)
    
    # Fix puseYn builder default
    if 'private String puseYn;' in content and 'ZtUsacInDto' in filepath:
        content = content.replace('private String puseYn;', '@Builder.Default\n    private String puseYn = "Y";')
        
    # Remove @Builder from AuditInfo (to prevent inheritance clash)
    if 'AuditInfo.java' in filepath:
        content = content.replace('@Builder\n', '')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Processed: {filepath}")

# Find all DTOs
for root, dirs, files in os.walk('.'):
    if '\\dto\\' in root or '/dto/' in root:
        for file in files:
            if file.endswith('.java') and not file.endswith('Request.java') and not file.endswith('Response.java') and file != 'SampleGlowMessage.java':
                process_file(os.path.join(root, file))
