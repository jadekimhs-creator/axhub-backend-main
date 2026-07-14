import os

# Fix ToolAuthorizationService
auth_path = r"C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\axhub-gateway\src\main\java\io\shinhanlife\axhub\biz\mcp\gateway\security\ToolAuthorizationService.java"
with open(auth_path, "r", encoding="utf-8") as f:
    auth_content = f.read()

auth_content = auth_content.replace("metadata.name()", "metadata.getName()")
auth_content = auth_content.replace("metadata.operationType()", "metadata.getOperationType()")
auth_content = auth_content.replace("metadata.requiresApproval()", "(metadata.getRequiresApproval() != null && metadata.getRequiresApproval())")

with open(auth_path, "w", encoding="utf-8") as f:
    f.write(auth_content)

# Fix RedisToolTraceService
redis_path = r"C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\axhub-gateway\src\main\java\io\shinhanlife\axhub\biz\mcp\gateway\redis\RedisToolTraceService.java"
with open(redis_path, "r", encoding="utf-8") as f:
    redis_content = f.read()

redis_content = redis_content.replace("metadata.name()", "metadata.getName()")
redis_content = redis_content.replace("metadata.operationType()", "metadata.getOperationType()")

arg_names_old = 'trace.put("argumentNames", arguments.propertyNames());'
arg_names_new = '''        java.util.List<String> argNames = new java.util.ArrayList<>();
        arguments.fieldNames().forEachRemaining(argNames::add);
        trace.put("argumentNames", argNames);'''
redis_content = redis_content.replace(arg_names_old, arg_names_new)

with open(redis_path, "w", encoding="utf-8") as f:
    f.write(redis_content)

print("Files fixed!")
