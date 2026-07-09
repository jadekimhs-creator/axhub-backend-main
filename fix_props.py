import os

for root, _, files in os.walk('.'):
    for f in files:
        if f == 'application-local.properties':
            p = os.path.join(root, f)
            try:
                content = open(p, encoding='utf-8').read()
            except Exception:
                content = open(p, encoding='cp949').read()
                
            content = content.replace('localhost:/', 'localhost:${server.port}/')
            content = content.replace('localhost:\n', 'localhost:${server.port}\n')
            
            with open(p, 'w', encoding='utf-8') as out:
                out.write(content)
