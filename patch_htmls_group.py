import os
base_dir = r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static"

files_to_patch = ["index.html", "playground.html"]
for f in files_to_patch:
    path = os.path.join(base_dir, f)
    with open(path, "r", encoding="utf-8") as file:
        content = file.read()
    content = content.replace("tool.domainGroup", "tool.categoryKey")
    with open(path, "w", encoding="utf-8") as file:
        file.write(content)

print("Done")
