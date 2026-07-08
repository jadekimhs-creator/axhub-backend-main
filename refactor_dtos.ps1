$files = Get-ChildItem -Recurse -Filter *.java | Where-Object { $_.FullName -match "\\dto\\" -and $_.Name -notmatch "(Request|Response)\.java" -and $_.Name -ne "SampleGlowMessage.java" }

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw

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

    Set-Content -Path $file.FullName -Value $content -Encoding UTF8
    Write-Host "Processed: $($file.Name)"
}
