import os

files_to_patch = [
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\admin\scaffold.html",
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\catalog.html",
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\playground.html"
]

for file_path in files_to_patch:
    if not os.path.exists(file_path):
        continue
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Fix the links
    content = content.replace('href="/admin/catalog.html"', 'href="/catalog.html"')
    content = content.replace('href="/admin/playground.html"', 'href="/playground.html"')
    content = content.replace('href="/admin/scaffold.html"', 'href="/admin/scaffold.html"') # Keep scaffold as /admin/scaffold.html
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Links fixed.")
