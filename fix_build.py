import os

lombok_deps = """
dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.32'
    annotationProcessor 'org.projectlombok:lombok:1.18.32'
}
"""

modules = ["axhub-common", "axhub-gateway", "axhub-tool-core", "axhub-tool-sms", "axhub-tool-email", "axhub-tool-other"]

for m in modules:
    build_gradle_path = os.path.join(m, "build.gradle")
    if os.path.exists(build_gradle_path):
        with open(build_gradle_path, "a", encoding="utf-8") as f:
            f.write(lombok_deps)

print("Lombok added to all modules.")
