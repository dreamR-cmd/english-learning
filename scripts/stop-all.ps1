$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$PidDir = Join-Path $Root ".run"

$Services = @(
    "frontend",
    "gateway",
    "shop-service",
    "learning-service",
    "user-service",
    "auth-service"
)

foreach ($name in $Services) {
    $pidFile = Join-Path $PidDir "$name.pid"
    if (!(Test-Path $pidFile)) {
        Write-Host "$name is not recorded as running"
        continue
    }

    $pidValue = Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)) {
        Write-Host "Stopping $name, pid=$pidValue..."
        Stop-Process -Id $pidValue -Force
    } else {
        Write-Host "$name process not found, removing stale pid file"
    }

    Remove-Item -LiteralPath $pidFile -Force
}

Write-Host "Stop commands completed."
