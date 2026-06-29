[CmdletBinding()]
param(
    [string]$CsvPath = "",
    [string]$DbHost = "localhost",
    [int]$Port = 3306,
    [string]$Database = "english_learning",
    [string]$Username = "root",
    [string]$Password = "123456",
    [string]$MysqlPath,
    [switch]$ReplaceModules
)

$ErrorActionPreference = "Stop"

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

if (-not $CsvPath) {
    $CsvPath = Join-Path $PSScriptRoot "vocabulary-full-template.csv"
}

if (-not (Test-Path -LiteralPath $CsvPath)) {
    throw "CSV file not found: $CsvPath"
}

$mysqlPath = Resolve-MysqlPath -CandidatePath $MysqlPath
$resolvedCsvPath = (Resolve-Path -LiteralPath $CsvPath).Path
$rows = Import-Csv -LiteralPath $resolvedCsvPath -Encoding UTF8

if (-not $rows -or $rows.Count -eq 0) {
    throw "CSV file is empty: $resolvedCsvPath"
}

$requiredColumns = @("module_code", "word", "phonetic", "meaning", "example")
$headers = $rows[0].PSObject.Properties.Name
foreach ($column in $requiredColumns) {
    if ($headers -notcontains $column) {
        throw "Missing required CSV column: $column"
    }
}

function Escape-SqlValue {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) {
        return "NULL"
    }

    $text = [string]$Value
    $text = $text.Replace("\", "\\")
    $text = $text.Replace("'", "''")
    $text = $text.Replace("`r", "\r")
    $text = $text.Replace("`n", "\n")
    return "'$text'"
}

function Normalize-CellValue {
    param([AllowNull()][string]$Value, [int]$MaxLength = 0)

    if ($null -eq $Value) {
        return $null
    }

    $text = [string]$Value
    if ($MaxLength -gt 0 -and $text.Length -gt $MaxLength) {
        return $text.Substring(0, $MaxLength)
    }

    return $text
}

$moduleMap = [ordered]@{
    cet4   = @{ name = "CET-4"; description = "College English Test Band 4"; sort_order = 1 }
    cet6   = @{ name = "CET-6"; description = "College English Test Band 6"; sort_order = 2 }
    toefl  = @{ name = "TOEFL"; description = "Test of English as a Foreign Language"; sort_order = 3 }
    ielts  = @{ name = "IELTS"; description = "International English Language Testing System"; sort_order = 4 }
    kaoyan = @{ name = "Kaoyan"; description = "Postgraduate Entrance English"; sort_order = 5 }
    gre    = @{ name = "GRE"; description = "Graduate Record Examination"; sort_order = 6 }
}

$builder = New-Object System.Text.StringBuilder
[void]$builder.AppendLine("SET NAMES utf8mb4;")
[void]$builder.AppendLine("USE $Database;")
[void]$builder.AppendLine("")

foreach ($moduleCode in $moduleMap.Keys) {
    $module = $moduleMap[$moduleCode]
    [void]$builder.AppendLine("INSERT INTO exam_modules (name, code, description, icon, sort_order)")
    [void]$builder.AppendLine("SELECT $(Escape-SqlValue $module.name), $(Escape-SqlValue $moduleCode), $(Escape-SqlValue $module.description), $(Escape-SqlValue $moduleCode), $($module.sort_order)")
    [void]$builder.AppendLine("WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = $(Escape-SqlValue $moduleCode));")
    [void]$builder.AppendLine("")
}

[void]$builder.AppendLine("CREATE TEMPORARY TABLE tmp_vocab_import (")
[void]$builder.AppendLine("    module_code VARCHAR(32) NOT NULL,")
[void]$builder.AppendLine("    word VARCHAR(255) NOT NULL,")
[void]$builder.AppendLine("    phonetic VARCHAR(255),")
[void]$builder.AppendLine("    meaning TEXT NOT NULL,")
[void]$builder.AppendLine("    example TEXT")
[void]$builder.AppendLine(");")
[void]$builder.AppendLine("")
[void]$builder.AppendLine("INSERT INTO tmp_vocab_import (module_code, word, phonetic, meaning, example) VALUES")

for ($i = 0; $i -lt $rows.Count; $i++) {
    $row = $rows[$i]
    $moduleCode = ([string]$row.module_code).Trim().ToLowerInvariant()

    if (-not $moduleMap.Contains($moduleCode)) {
        throw "Unsupported module_code '$moduleCode' in row $($i + 2). Allowed values: $($moduleMap.Keys -join ', ')"
    }

    $wordValue = Normalize-CellValue -Value $row.word -MaxLength 255
    $phoneticValue = Normalize-CellValue -Value $row.phonetic -MaxLength 255

    $line = "({0}, {1}, {2}, {3}, {4})" -f `
        (Escape-SqlValue $moduleCode), `
        (Escape-SqlValue $wordValue), `
        (Escape-SqlValue $phoneticValue), `
        (Escape-SqlValue $row.meaning), `
        (Escape-SqlValue $row.example)

    if ($i -lt $rows.Count - 1) {
        $line += ","
    }
    else {
        $line += ";"
    }

    [void]$builder.AppendLine($line)
}

[void]$builder.AppendLine("")
[void]$builder.AppendLine("START TRANSACTION;")
[void]$builder.AppendLine("")

if ($ReplaceModules) {
    [void]$builder.AppendLine("DELETE w")
    [void]$builder.AppendLine("FROM words w")
    [void]$builder.AppendLine("JOIN exam_modules m ON m.id = w.module_id")
    [void]$builder.AppendLine("JOIN (SELECT DISTINCT module_code FROM tmp_vocab_import) t ON t.module_code = m.code;")
    [void]$builder.AppendLine("")
}

[void]$builder.AppendLine("INSERT INTO words (word, phonetic, meaning, example, module_id)")
[void]$builder.AppendLine("SELECT t.word, t.phonetic, t.meaning, t.example, m.id")
[void]$builder.AppendLine("FROM tmp_vocab_import t")
[void]$builder.AppendLine("JOIN exam_modules m ON m.code = t.module_code")
[void]$builder.AppendLine("LEFT JOIN words w ON w.module_id = m.id AND w.word = t.word")
[void]$builder.AppendLine("WHERE w.id IS NULL")
[void]$builder.AppendLine("ORDER BY m.sort_order, t.word;")
[void]$builder.AppendLine("")
[void]$builder.AppendLine("COMMIT;")
[void]$builder.AppendLine("")
[void]$builder.AppendLine("DROP TEMPORARY TABLE tmp_vocab_import;")
[void]$builder.AppendLine("")
[void]$builder.AppendLine("SELECT m.code AS module_code, COUNT(*) AS total_words")
[void]$builder.AppendLine("FROM words w")
[void]$builder.AppendLine("JOIN exam_modules m ON m.id = w.module_id")
[void]$builder.AppendLine("WHERE m.code IN ('cet4', 'cet6', 'toefl', 'ielts', 'kaoyan', 'gre')")
[void]$builder.AppendLine("GROUP BY m.code, m.sort_order")
[void]$builder.AppendLine("ORDER BY m.sort_order;")

$tempSqlFile = Join-Path $env:TEMP ("vocab-import-" + [guid]::NewGuid().ToString("N") + ".sql")
[System.IO.File]::WriteAllText($tempSqlFile, $builder.ToString(), [System.Text.UTF8Encoding]::new($false))

try {
    $sourcePath = $tempSqlFile -replace "\\", "/"

    Write-Host "Starting CSV vocabulary import..."
    Write-Host "CSV file: $resolvedCsvPath"
    Write-Host "Rows: $($rows.Count)"
    Write-Host "Database: $Database@$DbHost`:$Port"
    Write-Host "MySQL: $mysqlPath"

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

    Write-Host "CSV vocabulary import completed."
}
finally {
    if (Test-Path -LiteralPath $tempSqlFile) {
        Remove-Item -LiteralPath $tempSqlFile -Force
    }
}
