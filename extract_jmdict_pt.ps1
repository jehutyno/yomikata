# extract_jmdict_pt.ps1
# Downloads JMdict (full multilingual), extracts Portuguese glosses, updates asset DB.
# JMdict is maintained by Jim Breen / EDRDG, licensed CC BY-SA 4.0.

param(
    [string]$DbPath  = "C:\Users\valen\Repos\yomikata\app\src\main\assets\yomikataz.db",
    [string]$Sqlite3 = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe",
    [string]$TempDir = $env:TEMP
)

$JmdictUrl = "https://www.edrdg.org/pub/Nihongo/JMdict.gz"
$GzipPath  = Join-Path $TempDir "JMdict.gz"
$XmlPath   = Join-Path $TempDir "JMdict.xml"
$SqlPath   = Join-Path $TempDir "jmdict_pt_updates.sql"

# ── 1. Download & decompress ──────────────────────────────────────────────────
if (-not (Test-Path $XmlPath)) {
    Write-Host "Downloading JMdict (~55 MB)..."
    Invoke-WebRequest -Uri $JmdictUrl -OutFile $GzipPath -UseBasicParsing
    Write-Host "Decompressing..."
    $gzStream  = [System.IO.File]::OpenRead($GzipPath)
    $dcStream  = [System.IO.Compression.GZipStream]::new($gzStream, [System.IO.Compression.CompressionMode]::Decompress)
    $outStream = [System.IO.File]::Create($XmlPath)
    $dcStream.CopyTo($outStream)
    $outStream.Close(); $dcStream.Close(); $gzStream.Close()
    Write-Host "Decompressed to $XmlPath"
} else {
    Write-Host "Using cached $XmlPath"
}

# ── 2. Load words from DB ─────────────────────────────────────────────────────
Write-Host "Loading words from database..."
& $Sqlite3 $DbPath "UPDATE words SET portuguese = '';"

$rawRows = & $Sqlite3 $DbPath ".separator '|'" `
    "SELECT _id, japanese, reading, is_kana FROM words ORDER BY _id;"

$words = $rawRows | ForEach-Object {
    $p = $_ -split '\|'
    [PSCustomObject]@{ Id=[long]$p[0]; Jp=$p[1]; Reading=$p[2]; IsKana=[int]$p[3] }
}
Write-Host "  $($words.Count) words loaded."

$byKanji   = [System.Collections.Generic.Dictionary[string,object]]::new()
$byReading = [System.Collections.Generic.Dictionary[string,object]]::new()
foreach ($w in $words) {
    if (-not $byKanji.ContainsKey($w.Jp))     { $byKanji[$w.Jp]     = [System.Collections.Generic.List[object]]::new() }
    ($byKanji[$w.Jp]).Add($w)
    if ($w.Reading -and -not $byReading.ContainsKey($w.Reading)) {
        $byReading[$w.Reading] = [System.Collections.Generic.List[object]]::new()
    }
    if ($w.Reading) { ($byReading[$w.Reading]).Add($w) }
}

# ── 3. Stream-parse JMdict ────────────────────────────────────────────────────
Write-Host "Parsing JMdict XML (streaming, this may take 2-3 minutes)..."
$results = [System.Collections.Generic.Dictionary[long,string]]::new()

$settings = [System.Xml.XmlReaderSettings]::new()
$settings.DtdProcessing = [System.Xml.DtdProcessing]::Parse
$settings.MaxCharactersFromEntities = 0
$fileStream   = [System.IO.File]::OpenRead($XmlPath)
$streamReader = [System.IO.StreamReader]::new($fileStream, [System.Text.Encoding]::UTF8)
$reader       = [System.Xml.XmlReader]::Create($streamReader, $settings)

$kebList = [System.Collections.Generic.List[string]]::new()
$rebList = [System.Collections.Generic.List[string]]::new()
$ptList  = [System.Collections.Generic.List[string]]::new()
$inSense = $false; $inGloss = $false; $isPt = $false
$currentText = [System.Text.StringBuilder]::new()

function Match-And-Store {
    if ($ptList.Count -eq 0) { $kebList.Clear(); $rebList.Clear(); $ptList.Clear(); return }
    $ptStr = $ptList -join '; '

    $matched = [System.Collections.Generic.HashSet[long]]::new()
    foreach ($keb in $kebList) {
        if ($byKanji.ContainsKey($keb)) {
            foreach ($w in $byKanji[$keb]) { [void]$matched.Add($w.Id) }
        }
    }
    if ($matched.Count -eq 0) {
        foreach ($reb in $rebList) {
            if ($byReading.ContainsKey($reb)) {
                foreach ($w in $byReading[$reb]) { [void]$matched.Add($w.Id) }
            }
        }
    }
    foreach ($id in $matched) {
        if (-not $results.ContainsKey($id)) { $results[$id] = $ptStr }
    }
    $kebList.Clear(); $rebList.Clear(); $ptList.Clear()
}

while ($reader.Read()) {
    switch ($reader.NodeType) {
        ([System.Xml.XmlNodeType]::Element) {
            switch ($reader.LocalName) {
                'entry' { Match-And-Store; $inSense = $false }
                'sense' { $inSense = $true }
                'gloss' {
                    if ($inSense) {
                        $lang = $reader.GetAttribute('lang', 'http://www.w3.org/XML/1998/namespace')
                        if (-not $lang) { $lang = $reader.GetAttribute('xml:lang') }
                        $isPt = ($lang -eq 'por'); $inGloss = $true
                        [void]$currentText.Clear()
                    }
                }
            }
        }
        ([System.Xml.XmlNodeType]::Text) {
            [void]$currentText.Append($reader.Value)
        }
        ([System.Xml.XmlNodeType]::EndElement) {
            switch ($reader.LocalName) {
                'keb'   { $kebList.Add($currentText.ToString()); [void]$currentText.Clear() }
                'reb'   { $rebList.Add($currentText.ToString()); [void]$currentText.Clear() }
                'gloss' {
                    if ($inGloss -and $isPt) {
                        $t = $currentText.ToString().Trim()
                        if ($t) { $ptList.Add($t) }
                    }
                    $inGloss = $false; $isPt = $false; [void]$currentText.Clear()
                }
                'sense' { $inSense = $false }
            }
        }
    }
}
Match-And-Store
$reader.Close(); $streamReader.Close(); $fileStream.Close()
Write-Host "  Matched $($results.Count) words."

# ── 4. Write SQL to UTF-8 file and execute ────────────────────────────────────
Write-Host "Writing SQL file (UTF-8)..."
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$sqlWriter = [System.IO.StreamWriter]::new($SqlPath, $false, $utf8NoBom)
$sqlWriter.WriteLine("BEGIN TRANSACTION;")
foreach ($kv in $results.GetEnumerator()) {
    $escaped = $kv.Value.Replace("'", "''")
    $sqlWriter.WriteLine("UPDATE words SET portuguese = '$escaped' WHERE _id = $($kv.Key);")
}
$sqlWriter.WriteLine("COMMIT;")
$sqlWriter.Close()

Write-Host "Executing SQL..."
& $Sqlite3 $DbPath ".read $SqlPath"
Remove-Item $SqlPath -ErrorAction SilentlyContinue

# ── 5. Verify ─────────────────────────────────────────────────────────────────
$filled = & $Sqlite3 $DbPath "SELECT COUNT(*) FROM words WHERE portuguese != '';"
Write-Host "Done. Filled: $filled / 7503"
& $Sqlite3 $DbPath "SELECT japanese, english, portuguese FROM words WHERE portuguese != '' LIMIT 5;"
