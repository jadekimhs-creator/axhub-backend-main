import os
base_dir = r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static"

files_to_patch = ["index.html", "playground.html"]
for f in files_to_patch:
    path = os.path.join(base_dir, f)
    with open(path, "r", encoding="utf-8") as file:
        content = file.read()
    content = content.replace("tool.toolName", "tool.name")
    with open(path, "w", encoding="utf-8") as file:
        file.write(content)

catalog_path = os.path.join(base_dir, "catalog.html")
with open(catalog_path, "r", encoding="utf-8") as file:
    catalog_content = file.read()
catalog_content = catalog_content.replace("${tool.name}", "${tool.displayName} <span class=\"text-xs text-slate-500 font-mono\">(${tool.name})</span>")
with open(catalog_path, "w", encoding="utf-8") as file:
    file.write(catalog_content)
print("Done")
