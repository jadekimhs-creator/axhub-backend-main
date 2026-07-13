import os
import re

# 1. Fix catalog.html JS bug and error box colors
catalog_path = r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\catalog.html"
with open(catalog_path, "r", encoding="utf-8") as f:
    catalog_html = f.read()

# Remove totalCount reference since we removed the element
catalog_html = catalog_html.replace("document.getElementById('totalCount').textContent = Total: ;", "")

# Fix the error box colors to be dark-theme compatible
catalog_html = catalog_html.replace("border-red-200 bg-red-50 text-red-600", "border-red-900 bg-red-950/30 text-red-400")
with open(catalog_path, "w", encoding="utf-8") as f:
    f.write(catalog_html)

# 2. Fix input field visibility in scaffold.html and playground.html
files = [
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\admin\scaffold.html",
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\playground.html"
]

for fp in files:
    if not os.path.exists(fp):
        continue
    with open(fp, "r", encoding="utf-8") as f:
        content = f.read()

    # Enhance input-base visibility
    content = content.replace("border: 1px solid rgba(255,255,255,0.1);", "border: 1px solid rgba(255,255,255,0.25);\n            background-color: rgba(255,255,255,0.05);")
    content = content.replace("background-color: #18181b;", "") # remove old background
    
    # In playground.html, also enhance select Tool and textarea
    # We already updated .input-base, which applies to textarea and select.
    
    with open(fp, "w", encoding="utf-8") as f:
        f.write(content)

print("Bugs and visibility fixed.")
