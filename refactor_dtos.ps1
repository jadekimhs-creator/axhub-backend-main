$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False

$files = Get-ChildItem -Recurse -Filter *.java | Where-Object { $_.FullName -match "\\dto\\" -and $_.Name -notmatch "(Request|Response)\.java" -and $_.Name -ne "SampleGlowMessage.java" }

foreach ($file in $files) {
    # Read as UTF8 (it usually handles CP949 okay if characters don't conflict, but let's be safe and use raw bytes if needed. Actually Get-Content is fine if the file is already UTF-8 or CP949)
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8

    # 1. Remove @Data if it exists
    $content = $content -replace '(?m)^\s*@Data\s*\r?\n', ''

    # 2. Check if it's an interface or enum, skip if so
    if ($content -match 'public interface ' -or $content -match 'public enum ') {
        continue
    }

    # 3. Add imports if they don't exist
    $imports = @("lombok.Getter", "lombok.Setter", "lombok.Builder", "lombok.NoArgsConstructor", "lombok.AllArgsConstructor")
    $importBlock = ""
    foreach ($imp in $imports) {
        if (-not ($content -match "import $imp;")) {
            $importBlock += "import $imp;`r`n"
        }
    }

    if ($importBlock -ne "") {
        # Insert imports after package declaration
        $content = $content -replace '(?m)^(package\s+[^;]+;)\r?\n', "`$1`r`n`r`n$importBlock"
    }

    # 4. Add annotations if they don't exist
    $annotations = @()
    if (-not ($content -match '@Getter')) { $annotations += "@Getter" }
    if (-not ($content -match '@Setter')) { $annotations += "@Setter" }
    if (-not ($content -match '@Builder')) { $annotations += "@Builder" }
    if (-not ($content -match '@NoArgsConstructor')) { $annotations += "@NoArgsConstructor" }
    if (-not ($content -match '@AllArgsConstructor')) { $annotations += "@AllArgsConstructor" }

    if ($annotations.Count -gt 0) {
        $annoStr = ($annotations -join "`r`n") + "`r`n"
        # Insert annotations before public class
        $content = $content -replace '(?m)^(public\s+class\s+)', "$annoStr`$1"
    }
    
    # 5. Fix manual constructors
    $content = $content -replace '(?m)^\s*public\s+ToolMetadata\(\)\s*\{\}\s*\r?\n', ''
    $content = $content -replace '(?m)^\s*public\s+ZtUsacInDto\(\)\s*\{\s*setPuseYn\("Y"\);\s*\}\s*\r?\n', ''
    
    # Fix puseYn default
    if ($file.Name -eq "ZtUsacInDto.java") {
        $content = $content -replace 'private String puseYn;', '@Builder.Default`r`n    private String puseYn = "Y";'
    }
    
    # Fix AuditInfo Builder conflict
    if ($file.Name -eq "AuditInfo.java") {
        $content = $content -replace '(?m)^@Builder\r?\n', ''
    }

    # Write without BOM
    [System.IO.File]::WriteAllText($file.FullName, $content, $Utf8NoBomEncoding)
    Write-Host "Processed: $($file.Name)"
}
