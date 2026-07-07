$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$PidDir = Join-Path $Root ".run"
$LogDir = Join-Path $Root "logs"
$BackendServicesDir = Join-Path $Root "backend-services"

New-Item -ItemType Directory -Force -Path $PidDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$Services = @(
    @{ Name = "auth-service"; Directory = (Join-Path $BackendServicesDir "auth-service"); Command = "mvn"; Arguments = @("spring-boot:run"); Port = 8082 },
    @{ Name = "user-service"; Directory = (Join-Path $BackendServicesDir "user-service"); Command = "mvn"; Arguments = @("spring-boot:run"); Port = 8083 },
    @{ Name = "learning-service"; Directory = (Join-Path $BackendServicesDir "learning-service"); Command = "mvn"; Arguments = @("spring-boot:run"); Port = 8084 },
    @{ Name = "shop-service"; Directory = (Join-Path $BackendServicesDir "shop-service"); Command = "mvn"; Arguments = @("spring-boot:run"); Port = 8085 },
    @{ Name = "gateway"; Directory = (Join-Path $BackendServicesDir "gateway"); Command = "mvn"; Arguments = @("spring-boot:run"); Port = 8080 },
    @{ Name = "frontend"; Directory = (Join-Path $Root "frontend"); Command = "npm"; Arguments = @("run", "dev"); Port = 3000 }
)

function Test-PortInUse {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connection
}

function Start-ServiceProcess {
    param([hashtable]$Service)

    $name = $Service.Name
    $workDir = $Service.Directory
    $pidFile = Join-Path $PidDir "$name.pid"
    $outFile = Join-Path $LogDir "$name.out.log"
    $errFile = Join-Path $LogDir "$name.err.log"

    if (Test-Path $pidFile) {
        $existingPid = Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
            Write-Host "$name already running, pid=$existingPid"
            return
        }
        Remove-Item -LiteralPath $pidFile -Force
    }

    if (Test-PortInUse -Port $Service.Port) {
        Write-Host "$name port $($Service.Port) is already in use, skip starting"
        return
    }

    Write-Host "Starting $name on port $($Service.Port)..."
    $process = Start-Process `
        -FilePath $Service.Command `
        -ArgumentList $Service.Arguments `
        -WorkingDirectory $workDir `
        -RedirectStandardOutput $outFile `
        -RedirectStandardError $errFile `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -Path $pidFile -Value $process.Id
    Write-Host "$name started, pid=$($process.Id), logs=$LogDir"
}

foreach ($service in $Services) {
    Start-ServiceProcess -Service $service
}

Write-Host ""
Write-Host "All startup commands have been issued."
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Frontend: http://localhost:3000"
Write-Host "Logs:     $LogDir"
