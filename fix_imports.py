import os
import re

root_dir = "."
target_ext = ".java"

# Regex to find fully qualified class names
fqcn_pattern = re.compile(r'\b(((?:java\.util|java\.time|java\.io|java\.math|java\.net)\.[a-z0-9.]*?([A-Z]\w+))|(org\.springframework\.[a-z0-9.]*?([A-Z]\w+))|(io\.shinhanlife\.[a-z0-9.]*?([A-Z]\w+)))\b')

count = 0

for subdir, dirs, files in os.walk(root_dir):
    if '.git' in dirs: dirs.remove('.git')
    if 'build' in dirs: dirs.remove('build')
    if '.gradle' in dirs: dirs.remove('.gradle')
    
    for file in files:
        if file.endswith(target_ext):
            filepath = os.path.join(subdir, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                lines = content.split('\n')
                new_imports = set()
                new_lines = []
                modified = False
                
                in_block_comment = False
                
                for line in lines:
                    stripped = line.strip()
                    
                    if stripped.startswith('/*'):
                        in_block_comment = True
                    
                    is_comment = in_block_comment or stripped.startswith('//') or stripped.startswith('*')
                    
                    if stripped.endswith('*/'):
                        in_block_comment = False
                    
                    if stripped.startswith('import ') or stripped.startswith('package '):
                        new_lines.append(line)
                        continue
                    
                    if is_comment:
                        new_lines.append(line)
                        continue
                    
                    def repl(match):
                        fqcn = match.group(1)
                        # Extract the simple class name
                        class_name = match.group(3) or match.group(5) or match.group(7)
                        
                        if class_name:
                            new_imports.add(f"import {fqcn};")
                            return class_name
                        return fqcn

                    new_line = fqcn_pattern.sub(repl, line)
                    if new_line != line:
                        modified = True
                    new_lines.append(new_line)
                
                if modified:
                    existing_imports = set()
                    insert_idx = 0
                    package_line = ""
                    
                    for i, line in enumerate(lines):
                        stripped = line.strip()
                        if stripped.startswith('package '):
                            package_line = line
                        elif stripped.startswith('import '):
                            existing_imports.add(stripped)
                        elif stripped == '' or stripped.startswith('//') or stripped.startswith('/*') or stripped.startswith('*'):
                            continue
                        elif package_line and not stripped.startswith('import '):
                            insert_idx = i
                            break

                    all_imports = existing_imports.union(new_imports)
                    sorted_imports = sorted(list(all_imports))
                    
                    res_lines = []
                    if package_line:
                        res_lines.append(package_line)
                        res_lines.append('')
                    
                    for imp in sorted_imports:
                        res_lines.append(imp)
                    
                    res_lines.append('')
                    
                    res_lines.extend(new_lines[insert_idx:])
                    
                    new_content = '\n'.join(res_lines)
                    
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Fixed imports in: {filepath}")
                    count += 1
            except Exception as e:
                print(f"Error processing {filepath}: {e}")

print(f"\nTotal files fixed: {count}")
