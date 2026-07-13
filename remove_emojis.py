import os
import re

pattern = re.compile(r'[\u2600-\u27BF\U0001F000-\U0001F9FF]+')

root_dir = "."
target_exts = {'.java', '.html', '.md', '.yml', '.properties', '.xml', '.json', '.js', '.css'}

count = 0
for subdir, dirs, files in os.walk(root_dir):
    if '.git' in dirs: dirs.remove('.git')
    if 'build' in dirs: dirs.remove('build')
    if '.gradle' in dirs: dirs.remove('.gradle')
    
    for file in files:
        ext = os.path.splitext(file)[1].lower()
        if ext in target_exts:
            filepath = os.path.join(subdir, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = pattern.sub('', content)
                new_content = new_content.replace('\uFE0F', '').replace('\u200D', '')
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Cleaned: {filepath}")
                    count += 1
            except Exception as e:
                pass

print(f"\nTotal files cleaned: {count}")
