[CmdletBinding()]
param(
    [string]$DbHost = "localhost",
    [int]$Port = 3306,
    [string]$Database = "english_learning",
    [string]$Username = "root",
    [string]$Password = "123456",
    [string]$MysqlPath
)

$ErrorActionPreference = "Stop"

$sqlFile = Join-Path $PSScriptRoot "src\main\resources\vocabulary_import.sql"
if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "SQL file not found: $sqlFile"
}

function Resolve-MysqlPath {
    param([string]$CandidatePath)

    if ($CandidatePath) {
        if (-not (Test-Path -LiteralPath $CandidatePath)) {
            throw "Specified mysql path does not exist: $CandidatePath"
        }
        return (Resolve-Path -LiteralPath $CandidatePath).Path
    }

    $mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysqlCommand) {
        return $mysqlCommand.Source
    }

    $commonPaths = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    )

    foreach ($path in $commonPaths) {
        if (Test-Path -LiteralPath $path) {
            return $path
        }
    }

    $found = Get-ChildItem -Path "C:\Program Files\MySQL" -Recurse -Filter "mysql.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName

    if ($found) {
        return $found
    }

    throw "mysql client was not found. Install MySQL Client or pass -MysqlPath with the full mysql.exe path."
}

$mysqlPath = Resolve-MysqlPath -CandidatePath $MysqlPath
$sourcePath = $sqlFile -replace "\\", "/"

Write-Host "Starting vocabulary import..."
Write-Host "Database: $Database@$DbHost`:$Port"
Write-Host "MySQL: $mysqlPath"
Write-Host "SQL file: $sqlFile"

& $mysqlPath `
    "--host=$DbHost" `
    "--port=$Port" `
    "--user=$Username" `
    "--password=$Password" `
    "--default-character-set=utf8mb4" `
    $Database `
    "--execute=source $sourcePath"

if ($LASTEXITCODE -ne 0) {
    throw "Import failed. mysql exit code: $LASTEXITCODE"
}

Write-Host "Vocabulary import completed."
