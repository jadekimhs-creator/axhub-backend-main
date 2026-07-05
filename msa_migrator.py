import os
import shutil
from pathlib import Path
import re

def create_module_structure(base_dir, module_name):
    os.makedirs(os.path.join(base_dir, module_name, "src", "main", "java"), exist_ok=True)
    os.makedirs(os.path.join(base_dir, module_name, "src", "main", "resources"), exist_ok=True)
    
def move_file_to_module(src_path, dest_module, dest_package):
    if not os.path.exists(src_path):
        return
    
    dest_dir = os.path.join(dest_module, "src", "main", "java", *dest_package.split("."))
    os.makedirs(dest_dir, exist_ok=True)
    
    file_name = os.path.basename(src_path)
    dest_path = os.path.join(dest_dir, file_name)
    shutil.move(src_path, dest_path)
    
def move_dir_to_module(src_dir, dest_module, dest_package):
    if not os.path.exists(src_dir):
        return
    
    dest_dir = os.path.join(dest_module, "src", "main", "java", *dest_package.split("."))
    os.makedirs(dest_dir, exist_ok=True)
    
    for item in os.listdir(src_dir):
        s = os.path.join(src_dir, item)
        d = os.path.join(dest_dir, item)
        if os.path.isdir(s):
            shutil.copytree(s, d, dirs_exist_ok=True)
            shutil.rmtree(s)
        else:
            shutil.move(s, d)
            
if __name__ == "__main__":
    print("Starting MSA Migration...")
    
    modules = ["axhub-common", "axhub-gateway", "axhub-tool-core", "axhub-tool-sms", "axhub-tool-email", "axhub-tool-other"]
    for m in modules:
        create_module_structure(".", m)
        
    base_pkg = "io.shinhanlife.axhub"
    biz_pkg = f"{base_pkg}.biz.mcp"
    src_base = os.path.join("src", "main", "java", "io", "shinhanlife", "axhub")
    src_biz = os.path.join(src_base, "biz", "mcp")
    
    move_dir_to_module(os.path.join(src_base, "common"), "axhub-common", f"{base_pkg}.common")
    move_file_to_module(os.path.join(src_biz, "adapter", "dto", "JsonRpcRequest.java"), "axhub-common", f"{biz_pkg}.adapter.dto")
    move_file_to_module(os.path.join(src_biz, "adapter", "dto", "JsonRpcResponse.java"), "axhub-common", f"{biz_pkg}.adapter.dto")
    move_file_to_module(os.path.join(src_biz, "adapter", "dto", "ErrorDetail.java"), "axhub-common", f"{biz_pkg}.adapter.dto")
    move_file_to_module(os.path.join(src_biz, "adapter", "dto", "Params.java"), "axhub-common", f"{biz_pkg}.adapter.dto")
    move_file_to_module(os.path.join(src_biz, "gateway", "dto", "ToolMetadata.java"), "axhub-common", f"{biz_pkg}.gateway.dto")
    
    move_dir_to_module(os.path.join(src_biz, "gateway", "controller"), "axhub-gateway", f"{biz_pkg}.gateway.controller")
    move_dir_to_module(os.path.join(src_biz, "gateway", "service"), "axhub-gateway", f"{biz_pkg}.gateway.service")
    move_dir_to_module(os.path.join(src_biz, "gateway", "config"), "axhub-gateway", f"{biz_pkg}.gateway.config")
    
    move_dir_to_module(os.path.join(src_biz, "adapter", "connector"), "axhub-tool-core", f"{biz_pkg}.adapter.connector")
    move_dir_to_module(os.path.join(src_biz, "adapter", "sender"), "axhub-tool-core", f"{biz_pkg}.adapter.sender")
    move_dir_to_module(os.path.join(src_biz, "adapter", "aop"), "axhub-tool-core", f"{biz_pkg}.adapter.aop")
    move_dir_to_module(os.path.join(src_biz, "adapter", "util"), "axhub-tool-core", f"{biz_pkg}.adapter.util")
    move_dir_to_module(os.path.join(src_biz, "adapter", "test"), "axhub-tool-core", f"{biz_pkg}.adapter.test")
    move_dir_to_module(os.path.join(src_biz, "tool", "annotation"), "axhub-tool-core", f"{biz_pkg}.tool.annotation")
    move_file_to_module(os.path.join(src_biz, "tool", "service", "AbstractMcpToolService.java"), "axhub-tool-core", f"{biz_pkg}.tool.service")
    move_dir_to_module(os.path.join(src_biz, "tool", "controller"), "axhub-tool-core", f"{biz_pkg}.tool.controller")
    
    move_file_to_module(os.path.join(src_biz, "tool", "service", "SmsSendService.java"), "axhub-tool-sms", f"{biz_pkg}.tool.service")
    move_file_to_module(os.path.join(src_biz, "tool", "dto", "SmsSendReq.java"), "axhub-tool-sms", f"{biz_pkg}.tool.dto")
    
    move_file_to_module(os.path.join(src_biz, "tool", "service", "EmailSendService.java"), "axhub-tool-email", f"{biz_pkg}.tool.service")
    move_file_to_module(os.path.join(src_biz, "tool", "dto", "EmailSendReq.java"), "axhub-tool-email", f"{biz_pkg}.tool.dto")
    
    move_dir_to_module(os.path.join(src_biz, "tool", "service"), "axhub-tool-other", f"{biz_pkg}.tool.service")
    move_dir_to_module(os.path.join(src_biz, "tool", "dto"), "axhub-tool-other", f"{biz_pkg}.tool.dto")
    move_dir_to_module(os.path.join(src_biz, "tool", "util"), "axhub-tool-other", f"{biz_pkg}.tool.util")

    move_file_to_module(os.path.join(src_base, "AxHubAdminApplication.java"), "axhub-gateway", base_pkg)

    print("Migration complete.")
