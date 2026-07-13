import os
import re

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

    # Change background and foreground colors in tailwind config
    content = content.replace("background: '#ffffff',", "background: '#f8fafc',")
    content = content.replace("foreground: '#000000',", "foreground: '#0f172a',")

    # Add primary colors to tailwind config
    if "primaryHover: '#4338ca'," not in content:
        content = content.replace("border: '#e2e8f0',", "border: '#e2e8f0',\n                        primary: '#4f46e5',\n                        primaryHover: '#4338ca',")

    # Change btn-black to use primary color (indigo) instead of pure black for primary actions
    # In scaffold.html, btn-black is used for the generate button. Let's make it btn-primary.
    content = content.replace(".btn-black {", ".btn-black {\n            background-color: theme('colors.primary');")
    content = content.replace("background-color: #000000;", "")
    content = content.replace(".btn-black:hover {\n            background-color: #333333;\n        }", ".btn-black:hover {\n            background-color: theme('colors.primaryHover');\n        }")
    
    # Change btn-black references to btn-primary
    content = content.replace("btn-black", "btn-primary")
    content = content.replace(".btn-primary {", ".btn-primary {\n            color: #ffffff;\n            font-weight: 500;\n            padding: 0.5rem 1rem;\n            border-radius: 4px;\n            font-size: 0.875rem;\n            transition: background-color 0.15s;\n        }")
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Patch applied.")
