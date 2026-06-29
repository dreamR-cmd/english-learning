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

function New-MeaningText {
    param($TransList)

    if (-not $TransList) {
        return ""
    }

    $parts = New-Object System.Collections.ArrayList
    foreach ($item in $TransList) {
        $tran = ""
        $pos = ""

        if ($item.tranCn) {
            $tran = ([string]$item.tranCn).Trim()
        }
        if ($item.pos) {
            $pos = ([string]$item.pos).Trim()
        }
        if (-not $tran) {
            continue
        }

        if ($pos) {
            [void]$parts.Add($pos + ". " + $tran)
        }
        else {
            [void]$parts.Add($tran)
        }
    }

    if ($parts.Count -eq 0) {
        return ""
    }

    return (($parts | Select-Object -Unique) -join "; ")
}

function New-PhoneticText {
    param($Content)

    $candidates = New-Object System.Collections.ArrayList

    if ($Content.phone) {
        [void]$candidates.Add(([string]$Content.phone).Trim())
    }
    if ($Content.ukphone) {
        [void]$candidates.Add(([string]$Content.ukphone).Trim())
    }
    if ($Content.usphone) {
        [void]$candidates.Add(([string]$Content.usphone).Trim())
    }

    $unique = $candidates | Where-Object { $_ } | Select-Object -Unique
    if (-not $unique) {
        return ""
    }

    $joined = ($unique -join " | ")
    return ("/" + $joined + "/")
}

function New-ExampleText {
    param($Content)

    if ($Content.sentence -and $Content.sentence.sentences) {
        $first = $Content.sentence.sentences | Select-Object -First 1
        if ($first -and $first.sContent) {
            return ([string]$first.sContent).Trim()
        }
    }

    if ($Content.realExamSentence -and $Content.realExamSentence.sentences) {
        $first = $Content.realExamSentence.sentences | Select-Object -First 1
        if ($first -and $first.sContent) {
            return ([string]$first.sContent).Trim()
        }
    }

    return ""
}

function Resolve-OutputCsvPath {
    param([string]$CandidatePath)

    if ($CandidatePath) {
        return $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($CandidatePath)
    }

    return (Join-Path $PSScriptRoot "cet4-kylebing-full.csv")
}

$mysqlPath = Resolve-MysqlPath -CandidatePath $MysqlPath
$csvPath = Resolve-OutputCsvPath -CandidatePath $OutputCsvPath
$csvDirectory = Split-Path -Path $csvPath -Parent
if ($csvDirectory -and -not (Test-Path -LiteralPath $csvDirectory)) {
    New-Item -ItemType Directory -Path $csvDirectory | Out-Null
}

$sourceUrls = @(
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_1.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_3.json"
)

$rowList = New-Object System.Collections.ArrayList
$seenWords = @{}

Write-Host "Downloading CET4 source files..."

foreach ($url in $sourceUrls) {
    Write-Host ("Source: " + $url)
    $response = Invoke-WebRequest -Headers @{ 'User-Agent' = 'Codex' } -Uri $url
    $items = $response.Content | ConvertFrom-Json

    foreach ($entry in $items) {
        $word = ""
        if ($entry.headWord) {
            $word = ([string]$entry.headWord).Trim()
        }
        if ((-not $word) -and $entry.content -and $entry.content.word -and $entry.content.word.wordHead) {
            $word = ([string]$entry.content.word.wordHead).Trim()
        }
        if (-not $word) {
            continue
        }

        $wordKey = $word.ToLowerInvariant()
        if ($seenWords.ContainsKey($wordKey)) {
            continue
        }

        if (-not $entry.content -or -not $entry.content.word -or -not $entry.content.word.content) {
            continue
        }

        $wordContent = $entry.content.word.content
        $meaning = New-MeaningText -TransList $wordContent.trans
        if (-not $meaning) {
            continue
        }

        $row = [pscustomobject]@{
            module_code = "cet4"
            word        = $word
            phonetic    = (New-PhoneticText -Content $wordContent)
            meaning     = $meaning
            example     = (New-ExampleText -Content $wordContent)
        }

        [void]$rowList.Add($row)
        $seenWords[$wordKey] = $true
    }
}

if ($rowList.Count -eq 0) {
    throw "No CET4 rows were parsed from the source files."
}

$rowList |
    Sort-Object word |
    Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding UTF8

Write-Host ("CSV generated: " + $csvPath)
Write-Host ("Unique CET4 rows: " + $rowList.Count)

$importScript = Join-Path $PSScriptRoot "import-vocabulary-from-csv.ps1"
if (-not (Test-Path -LiteralPath $importScript)) {
    throw "Import script not found: $importScript"
}

& $importScript `
    -CsvPath $csvPath `
    -DbHost $DbHost `
    -Port $Port `
    -Database $Database `
    -Username $Username `
    -Password $Password `
    -MysqlPath $mysqlPath

if ($LASTEXITCODE -ne 0) {
    throw "CSV import script failed with exit code: $LASTEXITCODE"
}
