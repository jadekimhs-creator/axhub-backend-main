import os
import re

files = [
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\admin\scaffold.html",
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\catalog.html",
    r"C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\axhub-gateway\src\main\resources\static\playground.html"
]

tailwind_and_style = """<script>
        tailwind.config = {
            darkMode: 'class',
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Geist', 'sans-serif'],
                        mono: ['Geist Mono', 'monospace']
                    },
                    colors: {
                        background: '#09090b',
                        panel: '#18181b',
                        foreground: '#f4f4f5',
                        muted: '#a1a1aa',
                        border: 'rgba(255,255,255,0.1)',
                        primary: '#3b82f6',
                        primaryHover: '#2563eb',
                    }
                }
            }
        }
    </script>
    <style>
        body {
            background-color: theme('colors.background');
            color: theme('colors.foreground');
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }
        
        .geist-mono { font-family: 'Geist Mono', monospace; }
        
        .btn-primary {
            background: linear-gradient(135deg, #3b82f6 0%, #4f46e5 100%);
            color: #ffffff;
            font-weight: 500;
            padding: 0.5rem 1rem;
            border-radius: 6px;
            font-size: 0.875rem;
            transition: all 0.3s ease;
            border: 1px solid rgba(255,255,255,0.1);
            box-shadow: 0 0 10px rgba(59, 130, 246, 0.2);
        }
        
        .btn-primary:hover {
            box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
            transform: translateY(-1px);
        }

        .btn-primary:disabled {
            background: #27272a;
            color: #71717a;
            box-shadow: none;
            border-color: transparent;
            transform: none;
            cursor: not-allowed;
        }
        
        .input-base {
            width: 100%;
            padding: 0.5rem 0.75rem;
            font-size: 0.875rem;
            background-color: #18181b;
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 6px;
            color: #f4f4f5;
            transition: all 0.2s;
        }
        
        .input-base:focus {
            outline: none;
            border-color: #3b82f6;
            box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
        }
        
        select.input-base {
            appearance: none;
            background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%23a1a1aa' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
            background-position: right 0.5rem center;
            background-repeat: no-repeat;
            background-size: 1.5em 1.5em;
            padding-right: 2.5rem;
        }

        .card-panel {
            background: #18181b;
            border: 1px solid theme('colors.border');
            border-radius: 8px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.5);
        }
        
        /* For catalog schema / playground output */
        pre {
            background-color: #09090b !important;
            border: 1px solid rgba(255,255,255,0.05);
        }
    </style>"""

for fp in files:
    if not os.path.exists(fp):
        continue
    with open(fp, "r", encoding="utf-8") as f:
        content = f.read()
    
    # 1. Replace tailwind config and style
    content = re.sub(r'<script>\s*tailwind\.config = \{.*?</style>', tailwind_and_style, content, flags=re.DOTALL)
    
    # 2. Add class="dark" to html tag if not present
    content = re.sub(r'<html lang="ko">', '<html lang="ko" class="dark">', content)
    
    # 3. Fix colors in the HTML body
    # Replace background colors
    content = content.replace("bg-white", "bg-zinc-900")
    content = content.replace("bg-slate-50", "bg-zinc-950")
    content = content.replace("bg-slate-100", "bg-zinc-800")
    content = content.replace("bg-slate-200", "bg-zinc-700")
    content = content.replace("bg-gray-50", "bg-zinc-900")
    content = content.replace("bg-gray-100", "bg-zinc-800")
    
    # Replace text colors
    content = content.replace("text-black", "text-zinc-100")
    content = content.replace("text-slate-900", "text-zinc-100")
    content = content.replace("text-slate-800", "text-zinc-200")
    content = content.replace("text-slate-600", "text-zinc-400")
    content = content.replace("text-slate-500", "text-zinc-400")
    content = content.replace("text-gray-900", "text-zinc-100")
    content = content.replace("text-gray-800", "text-zinc-200")
    content = content.replace("text-gray-600", "text-zinc-400")
    content = content.replace("text-gray-500", "text-zinc-400")
    content = content.replace("text-gray-400", "text-zinc-500")
    
    # Replace borders
    content = content.replace("border-slate-200", "border-white/10")
    content = content.replace("border-slate-300", "border-white/20")
    content = content.replace("border-gray-200", "border-white/10")
    content = content.replace("border-gray-300", "border-white/20")
    content = content.replace("border-border", "border-white/10")
    
    # Update Header to match index.html
    header_html = """<header class="border-b border-white/10 sticky top-0 bg-[#09090b]/80 backdrop-blur-xl z-50">
        <div class="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
            <div class="flex items-center space-x-5">
                <a href="/index.html" class="flex items-center group">
                    <div class="w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.8)] mr-2 group-hover:shadow-[0_0_12px_rgba(59,130,246,1)] transition-all"></div>
                    <span class="font-semibold tracking-tight text-sm text-zinc-100 group-hover:text-white">AXHUB Gateway</span>
                </a>
                <div class="h-4 w-px bg-white/10"></div>
                <nav class="flex space-x-5 text-[13px] font-medium">
                    <a href="/admin/scaffold.html" class="text-zinc-400 hover:text-white transition-colors">Scaffold</a>
                    <a href="/catalog.html" class="text-zinc-400 hover:text-white transition-colors">Catalog</a>
                    <a href="/playground.html" class="text-zinc-400 hover:text-white transition-colors">Playground</a>
                </nav>
            </div>
            <div class="flex items-center">
                <span class="text-[10px] uppercase tracking-widest bg-blue-500/10 text-blue-400 px-2 py-1 rounded font-bold border border-blue-500/20">v0.0.1</span>
            </div>
        </div>
    </header>"""
    
    content = re.sub(r'<header.*?</header>', header_html, content, flags=re.DOTALL)
    
    # In playground.html, fix the text area and json output styling
    if "playground.html" in fp:
        content = content.replace('bg-slate-50', 'bg-[#09090b]')
    
    with open(fp, "w", encoding="utf-8") as f:
        f.write(content)

print("Dark theme applied.")
