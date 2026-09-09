param(
    [string]$NacosBaseUrl = 'http://localhost:8848',
    [string]$Group = 'DEFAULT_GROUP'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ConfigDir = Join-Path $RepoRoot 'docs\nacos-config'
$ReadinessUrl = "$NacosBaseUrl/nacos/v1/console/health/readiness"

Write-Host "Waiting for Nacos at $NacosBaseUrl ..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri $ReadinessUrl -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) { $ready = $true; break }
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $ready) {
    throw "Nacos is not ready at $NacosBaseUrl after 60s."
}
Write-Host "Nacos is ready."

if (-not (Test-Path $ConfigDir)) {
    throw "Config directory not found: $ConfigDir"
}

$files = Get-ChildItem -Path $ConfigDir -Filter '*.yaml' | Sort-Object Name
foreach ($file in $files) {
    $dataId = $file.Name
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $body = @{
        dataId  = $dataId
        group   = $Group
        type    = 'yaml'
        content = $content
    }
    try {
        $result = Invoke-RestMethod -Method Post `
            -Uri "$NacosBaseUrl/nacos/v1/cs/configs" `
            -Body $body `
            -ContentType 'application/x-www-form-urlencoded' `
            -TimeoutSec 15
        Write-Host "[OK] $dataId -> $result"
    } catch {
        Write-Warning "[FAILED] $dataId : $($_.Exception.Message)"
    }
}

Write-Host "Import finished. Verify at $NacosBaseUrl/nacos"