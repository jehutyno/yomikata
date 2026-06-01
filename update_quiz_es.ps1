$db = "C:\Users\valen\Repos\yomikata\app\src\main\assets\yomikataz.db"
$sq  = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"

# Unicode char codes to avoid encoding issues with PS 5.1 (reads scripts as Windows-1252)
$a  = [char]0x00E1  # á
$e  = [char]0x00E9  # é
$i  = [char]0x00ED  # í
$o  = [char]0x00F3  # ó
$u  = [char]0x00FA  # ú
$ny = [char]0x00F1  # ñ

# Helper: translate the human-readable part of a quiz name (before the % separator)
function Translate-QuizName($nameEn) {
    $before = $nameEn -replace '%.*',''
    $after  = if ($nameEn -match '%') { '%' + ($nameEn -replace '^[^%]*%','') } else { '' }

    $es = $before `
        -replace 'Vocabulary',          'Vocabulario' `
        -replace 'Part(\d)',            "Parte`$1" `
        -replace 'The vowels',          'Las vocales' `
        -replace 'Vowels',              'Vocales' `
        -replace 'Column',              'Columna' `
        -replace 'Modified',            'Modificado' `
        -replace 'Composed',            'Compuesto' `
        -replace 'Country Name',        "Nombres de pa${i}ses" `
        -replace 'Basic Numbers',       "N${u}meros b${a}sicos" `
        -replace 'Advanced Numbers',    "N${u}meros avanzados" `
        -replace 'Basic Word',          "Palabras b${a}sicas" `
        -replace 'Days',                "D${i}as de la semana" `
        -replace 'Adjectives',          'Adjetivos' `
        -replace 'Verbs',               'Verbos' `
        -replace 'Words',               'Palabras' `
        -replace 'Numbers',             "N${u}meros"

    return $es + $after
}

# Load all quiz rows
$rows = & $sq $db ".separator '|'" "SELECT _id, name_en FROM quiz;" | ForEach-Object {
    $parts = $_ -split '\|', 2
    [PSCustomObject]@{ Id = $parts[0]; NameEn = $parts[1] }
}

# Build SQL file (UTF-8 NoBOM to handle accented chars safely)
$sqlPath = Join-Path $env:TEMP "quiz_es_updates.sql"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$writer = [System.IO.StreamWriter]::new($sqlPath, $false, $utf8NoBom)

foreach ($row in $rows) {
    $es = Translate-QuizName $row.NameEn
    $esSql = $es -replace "'","''"
    $writer.WriteLine("UPDATE quiz SET name_es = '$esSql' WHERE _id = $($row.Id);")
}
$writer.WriteLine("PRAGMA user_version;") # flush
$writer.Close()

& $sq $db ".read $sqlPath"
Remove-Item $sqlPath -ErrorAction SilentlyContinue

Write-Output "Done. Sample check:"
& $sq $db ".output STDOUT" "SELECT _id, name_en, name_es FROM quiz WHERE name_es LIKE '%mero%' LIMIT 3;"
