param(
    [ValidateSet('start', 'stop', 'restart', 'status')]
    [string]$Action = 'start'
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $Root '.service-pids.json'

$Services = @(
    @{ Name = 'auth-service';     Path = 'backend-services/auth-service';     Command = 'mvn spring-boot:run' },
    @{ Name = 'user-service';     Path = 'backend-services/user-service';     Command = 'mvn spring-boot:run' },
    @{ Name = 'learning-service'; Path = 'backend-services/learning-service'; Command = 'mvn spring-boot:run' },
    @{ Name = 'shop-service';     Path = 'backend-services/shop-service';     Command = 'mvn spring-boot:run' },
    @{ Name = 'admin-service';    Path = 'backend-services/admin-service';    Command = 'mvn spring-boot:run' },
    @{ Name = 'gateway';          Path = 'backend-services/gateway';          Command = 'mvn spring-boot:run' },
    @{ Name = 'frontend';         Path = 'frontend';                          Command = 'npm run dev' }
)

function Read-PidState {
    if (-not (Test-Path $PidFile)) {
        return @{}
    }

    $raw = Get-Content $PidFile -Raw
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return @{}
    }

    $json = $raw | ConvertFrom-Json
    $state = @{}
    foreach ($property in $json.PSObject.Properties) {
        $state[$property.Name] = [int]$property.Value
    }
    return $state
}

function Write-PidState($state) {
    $object = [ordered]@{}
    foreach ($key in $state.Keys | Sort-Object) {
        $object[$key] = $state[$key]
    }
    $object | ConvertTo-Json | Set-Content -Path $PidFile -Encoding UTF8
}

function Test-ProcessAlive([int]$processId) {
    return [bool](Get-Process -Id $processId -ErrorAction SilentlyContinue)
}

function Stop-ProcessTree([int]$processId) {
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId=$processId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -processId ([int]$child.ProcessId)
    }
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

function Start-OneService($service, $state) {
    $name = $service.Name
    if ($state.ContainsKey($name) -and (Test-ProcessAlive $state[$name])) {
        Write-Host "[$name] already running, pid=$($state[$name])"
        return
    }

    $serviceDir = Join-Path $Root $service.Path
    if (-not (Test-Path $serviceDir)) {
        throw "[$name] directory not found: $serviceDir"
    }

    $script = @"
`$Host.UI.RawUI.WindowTitle = 'english-learning - $name'
Set-Location -LiteralPath '$serviceDir'
Write-Host 'Starting $name in $serviceDir'
Write-Host 'Command: $($service.Command)'
$($service.Command)
"@
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
    $process = Start-Process -FilePath 'powershell.exe' `
        -ArgumentList @('-NoProfile', '-NoExit', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded) `
        -PassThru `
        -WindowStyle Normal

    $state[$name] = $process.Id
    Write-Host "[$name] started, pid=$($process.Id)"
}

function Start-AllServices {
    $state = Read-PidState
    foreach ($service in $Services) {
        Start-OneService $service $state
        Start-Sleep -Seconds 1
    }
    Write-PidState $state
    Write-Host "All services have been requested to start. PID file: $PidFile"
}

function Stop-AllServices {
    $state = Read-PidState
    if ($state.Count -eq 0) {
        Write-Host "No PID file found or no services recorded."
        return
    }

    foreach ($service in ($Services | Sort-Object { $_.Name } -Descending)) {
        $name = $service.Name
        if (-not $state.ContainsKey($name)) {
            continue
        }

        $processId = [int]$state[$name]
        if (Test-ProcessAlive $processId) {
            Write-Host "[$name] stopping, pid=$processId"
            Stop-ProcessTree $processId
        } else {
            Write-Host "[$name] not running, pid=$processId"
        }
    }

    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Write-Host "All recorded services have been requested to stop."
}

function Show-Status {
    $state = Read-PidState
    foreach ($service in $Services) {
        $name = $service.Name
        if ($state.ContainsKey($name) -and (Test-ProcessAlive $state[$name])) {
            Write-Host "[$name] running, pid=$($state[$name])"
        } else {
            Write-Host "[$name] stopped"
        }
    }
}

switch ($Action) {
    'start' { Start-AllServices }
    'stop' { Stop-AllServices }
    'restart' {
        Stop-AllServices
        Start-Sleep -Seconds 2
        Start-AllServices
    }
    'status' { Show-Status }
}
