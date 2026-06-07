param(
    [ValidateSet("de","es","pt","zh","all")]
    [string]$Lang = "all",
    [int]$BatchSize = 80,
    [int]$StartId = 1,
    [switch]$DryRun,
    [string]$SqlOutputFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$SQ  = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"
$DB  = "C:\Users\valen\Repos\yomikata\app\src\main\assets\yomikataz.db"
$API = "https://api.anthropic.com/v1/messages"
$MDL = "claude-haiku-4-5-20251001"

$LANGS = if ($Lang -eq "all") { @("de","es","pt","zh") } else { @($Lang) }

if (-not $env:ANTHROPIC_API_KEY) {
    Write-Error "ANTHROPIC_API_KEY non defini."
    exit 1
}

if ($SqlOutputFile -eq "") {
    $SqlOutputFile = ".\sentences_translations_$($Lang).sql"
}

$SYS = 'You are a translation assistant for a Japanese language learning app. Translate each sentence into German (de), Spanish (neutral Latin American, es), Brazilian Portuguese (pt), and Simplified Mandarin Chinese (zh). Be natural and idiomatic. Never leave a field empty. CRITICAL: Never use ASCII double-quote characters (") inside translation values — use language-appropriate alternatives instead (e.g. German: „…", Chinese: 「…」or «…», or simply omit quotes). Output ONLY a valid JSON array with no markdown, no explanation, no extra brackets. Format: [{"id":1,"de":"...","es":"...","pt":"...","zh":"..."}]'

function Run-Sql([string]$q) {
    $enc     = [System.Text.UTF8Encoding]::new($false)
    $inFile  = [System.IO.Path]::GetTempFileName() + ".sql"
    $outFile = [System.IO.Path]::GetTempFileName() + ".out"
    [System.IO.File]::WriteAllText($inFile, $q, $enc)
    $psi = [System.Diagnostics.ProcessStartInfo]::new($SQ)
    $psi.Arguments             = "`"$DB`""
    $psi.RedirectStandardInput  = $true
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute        = $false
    $psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $p = [System.Diagnostics.Process]::Start($psi)
    $p.StandardInput.WriteLine($q)
    $p.StandardInput.Close()
    $out = $p.StandardOutput.ReadToEnd()
    $p.WaitForExit()
    [System.IO.File]::Delete($inFile)
    return $out -split "`n" | Where-Object { $_ -ne "" }
}

function Call-Api([string]$userMsg) {
    $body = [ordered]@{
        model      = $MDL
        max_tokens = 8192
        system     = $SYS
        messages   = @(@{ role = "user"; content = $userMsg })
    } | ConvertTo-Json -Depth 10 -Compress

    $hdrs = @{
        "x-api-key"         = $env:ANTHROPIC_API_KEY
        "anthropic-version" = "2023-06-01"
        "content-type"      = "application/json; charset=utf-8"
    }

    # WebClient avec encodage UTF-8 explicite (evite les bugs PS5.1 avec Invoke-RestMethod)
    $wc = [System.Net.WebClient]::new()
    $wc.Headers.Add("x-api-key", $env:ANTHROPIC_API_KEY)
    $wc.Headers.Add("anthropic-version", "2023-06-01")
    $wc.Headers.Add("Content-Type", "application/json; charset=utf-8")
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $respBytes = $wc.UploadData($API, "POST", $bodyBytes)
    $text   = [System.Text.Encoding]::UTF8.GetString($respBytes)
    $parsed = $text | ConvertFrom-Json
    return $parsed.content[0].text
}

function Esc([string]$s) { return $s.Replace("'", "''") }

Write-Host "Chargement phrases..." -ForegroundColor Cyan
$rows = Run-Sql "SELECT _id, en, fr FROM sentences WHERE _id >= $StartId AND de = '' ORDER BY _id;"

$sentences = [System.Collections.Generic.List[hashtable]]::new()
foreach ($row in $rows) {
    $p = $row -split '\|'
    if ($p.Count -ge 3) {
        $sentences.Add(@{ id = [int]$p[0]; en = $p[1]; fr = $p[2] })
    }
}

Write-Host "$($sentences.Count) phrases a traduire" -ForegroundColor Green

$enc2 = [System.Text.UTF8Encoding]::new($false)
$sw   = [System.IO.StreamWriter]::new($SqlOutputFile, $false, $enc2)
$sw.WriteLine("BEGIN TRANSACTION;")
$sw.Flush()

$total  = [Math]::Ceiling($sentences.Count / $BatchSize)
$done   = 0
$errors = 0

Write-Host "Lancement - $total batches de $BatchSize" -ForegroundColor Cyan

for ($i = 0; $i -lt $total; $i++) {
    $batch = @($sentences | Select-Object -Skip ($i * $BatchSize) -First $BatchSize)
    $fid = $batch[0].id
    $lid = $batch[-1].id

    Write-Host "  Batch $($i+1)/$total (ids $fid-$lid)..." -NoNewline

    $items = $batch | ForEach-Object { @{ id = $_.id; en = $_.en; fr = $_.fr } }
    $msg   = "Translate. Output ONLY the JSON array.`n`n" + ($items | ConvertTo-Json -Depth 3 -Compress)

    $raw = $null
    for ($try = 1; $try -le 3; $try++) {
        try {
            $raw = Call-Api $msg
            break
        } catch {
            if ($try -lt 3) {
                Write-Host " [retry $try]" -NoNewline -ForegroundColor Yellow
                Start-Sleep -Seconds (5 * $try)
            } else {
                Write-Host " [ERREUR: $_]" -ForegroundColor Red
                $errors++
            }
        }
    }

    if ($null -eq $raw) { continue }

    $translations = $null
    try {
        if ($raw -match '(\[[\s\S]*\])') {
            $jsonStr = $Matches[1]
            # Nettoyer les doubles crochets finaux que Claude ajoute parfois
            $jsonStr = $jsonStr -replace '\]\s*\]$', ']'
            $translations = $jsonStr | ConvertFrom-Json
        } else {
            throw "Pas de tableau JSON"
        }
    } catch {
        Write-Host " [PARSE ERROR: $_]" -ForegroundColor Red
        $dbg = ".\debug_batch_$i.txt"
        [System.IO.File]::WriteAllText($dbg, $raw, $enc2)
        Write-Host "   -> $dbg" -ForegroundColor Yellow
        $errors++
        continue
    }

    foreach ($t in $translations) {
        $id   = $t.id
        $sets = @()
        foreach ($l in $LANGS) {
            $val = $t.$l
            if ($null -ne $val -and "$val" -ne "") {
                $sets += "$l='$(Esc $val)'"
            }
        }
        if ($sets.Count -gt 0) {
            $sw.WriteLine("UPDATE sentences SET $($sets -join ', ') WHERE _id=$id;")
        }
    }
    $sw.Flush()
    $done += $batch.Count

    Write-Host " OK ($done/$($sentences.Count))" -ForegroundColor Green

    if ($i -lt $total - 1) { Start-Sleep -Milliseconds 300 }
}

$sw.WriteLine("COMMIT;")
$sw.Close()

Write-Host ""
Write-Host "SQL genere : $SqlOutputFile" -ForegroundColor Cyan
Write-Host "Traduits   : $done / $($sentences.Count)" -ForegroundColor Green
if ($errors -gt 0) { Write-Host "Erreurs    : $errors batches" -ForegroundColor Red }

if ($DryRun) {
    Write-Host "DryRun - SQL non applique." -ForegroundColor Yellow
    exit 0
}

if ($errors -gt 0) {
    Write-Host "$errors batches en erreur - application quand meme (les IDs manquants pourront etre retraites)." -ForegroundColor Yellow
}

Write-Host "Application SQL..." -ForegroundColor Cyan
& $SQ $DB ".read $SqlOutputFile"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Succes !" -ForegroundColor Green
} else {
    Write-Host "Erreur SQL (code $LASTEXITCODE)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Verification :" -ForegroundColor Cyan
foreach ($l in $LANGS) {
    $cnt = (Run-Sql "SELECT COUNT(*) FROM sentences WHERE $l != '';").Trim()
    $col = if ([int]$cnt -gt 0) { "Green" } else { "Red" }
    Write-Host "  $l : $cnt / 7425" -ForegroundColor $col
}

Write-Host "Encodage (? parasites) :" -ForegroundColor Cyan
foreach ($l in $LANGS) {
    $n = [int](Run-Sql "SELECT COUNT(*) FROM sentences WHERE $l LIKE '%?%' AND $l != '';").Trim()
    if ($n -gt 0) {
        Write-Host "  $l : $n suspects" -ForegroundColor Yellow
    } else {
        Write-Host "  $l : OK" -ForegroundColor Green
    }
}
