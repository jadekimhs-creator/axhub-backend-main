import os

for root, _, files in os.walk('.'):
    for f in files:
        if f == 'application-local.properties' or f == 'application.properties':
            p = os.path.join(root, f)
            try:
                content = open(p, encoding='utf-8').read()
            except Exception:
                content = open(p, encoding='cp949').read()
                
            # Add logging level to suppress kafka warnings
            if 'logging.level.org.apache.kafka' not in content:
                content += '\n# Suppress Kafka Connection Logs\nlogging.level.org.apache.kafka=ERROR\n'
                
            with open(p, 'w', encoding='utf-8') as out:
                out.write(content)
