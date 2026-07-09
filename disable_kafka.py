import os

for root, _, files in os.walk('.'):
    for f in files:
        if f == 'application-local.properties':
            p = os.path.join(root, f)
            try:
                content = open(p, encoding='utf-8').read()
            except Exception:
                content = open(p, encoding='cp949').read()
                
            # Disable Kafka AutoConfiguration if not present
            if 'KafkaAutoConfiguration' not in content:
                content += '\n# Disable Kafka\nspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration\n'
                
            with open(p, 'w', encoding='utf-8') as out:
                out.write(content)
