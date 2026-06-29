[CmdletBinding()]
param(
    [string]$DbHost = "localhost",
    [int]$Port = 3306,
    [string]$Database = "english_learning",
    [string]$Username = "root",
    [string]$Password = "123456",
    [string]$MysqlPath,
    [string]$OutputCsvPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-OutputCsvPath {
    param([string]$CandidatePath)

    if ($CandidatePath) {
        return $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($CandidatePath)
    }

    return (Join-Path $PSScriptRoot "all-levels-vocabulary.csv")
}

$csvPath = Resolve-OutputCsvPath -CandidatePath $OutputCsvPath
$csvDirectory = Split-Path -Path $csvPath -Parent
if ($csvDirectory -and -not (Test-Path -LiteralPath $csvDirectory)) {
    New-Item -ItemType Directory -Path $csvDirectory | Out-Null
}

$buildScript = Join-Path $PSScriptRoot "build-vocabulary-csv.js"
if (-not (Test-Path -LiteralPath $buildScript)) {
    throw "Build script not found: $buildScript"
}

$importScript = Join-Path $PSScriptRoot "import-vocabulary-from-csv.ps1"
if (-not (Test-Path -LiteralPath $importScript)) {
    throw "Import script not found: $importScript"
}

Write-Host "Building all-level vocabulary CSV..."
& node $buildScript $csvPath

if ($LASTEXITCODE -ne 0) {
    throw "CSV build failed with exit code: $LASTEXITCODE"
}

Write-Host "Importing CSV into MySQL..."
& $importScript `
    -CsvPath $csvPath `
    -DbHost $DbHost `
    -Port $Port `
    -Database $Database `
    -Username $Username `
    -Password $Password `
    -MysqlPath $MysqlPath `
    -ReplaceModules

if ($LASTEXITCODE -ne 0) {
    throw "CSV import failed with exit code: $LASTEXITCODE"
}
