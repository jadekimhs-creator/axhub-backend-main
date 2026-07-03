$ErrorActionPreference = "Stop"

$source = "C:\Users\Admin\.gemini\antigravity\scratch\shinhan-mcp-prebuild\src\main\java\com\shinhan\mcp"
$dest = "C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\src\main\java\io\shinhanlife\axhub\biz\mcp"
$commonDest = "C:\Users\Admin\.gemini\antigravity\scratch\axhub-backend-main\src\main\java\io\shinhanlife\axhub\common\mcp"

New-Item -ItemType Directory -Force -Path $dest | Out-Null
New-Item -ItemType Directory -Force -Path $commonDest | Out-Null

Copy-Item -Path "$source\gateway" -Destination $dest -Recurse -Force
Copy-Item -Path "$source\tool" -Destination $dest -Recurse -Force
Copy-Item -Path "$source\adapter" -Destination $dest -Recurse -Force
Copy-Item -Path "$source\common\*" -Destination $commonDest -Recurse -Force

# We also need to delete the leftover Application classes from shinhan-mcp-prebuild
Remove-Item -Path "$dest\gateway\GatewayApplication.java" -ErrorAction SilentlyContinue
Remove-Item -Path "$dest\tool\ToolApplication.java" -ErrorAction SilentlyContinue

# Replace package names safely with UTF-8
$utf8 = New-Object System.Text.UTF8Encoding($false)
Get-ChildItem -Path $dest, $commonDest -Filter *.java -Recurse | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName, $utf8)
    
    $content = $content -replace "package com.shinhan.mcp.common", "package io.shinhanlife.axhub.common.mcp"
    $content = $content -replace "import com.shinhan.mcp.common", "import io.shinhanlife.axhub.common.mcp"
    
    $content = $content -replace "package com.shinhan.mcp", "package io.shinhanlife.axhub.biz.mcp"
    $content = $content -replace "import com.shinhan.mcp", "import io.shinhanlife.axhub.biz.mcp"
    
    # Do NOT do wild regex replacements for service/util/etc.
    
    [System.IO.File]::WriteAllText($_.FullName, $content, $utf8)
}
