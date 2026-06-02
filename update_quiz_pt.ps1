$db = "C:\Users\valen\Repos\yomikata\app\src\main\assets\yomikataz.db"
$sq  = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"

# Unicode char codes to avoid encoding issues with PS 5.1 (reads scripts as Windows-1252)
$a  = [char]0x00E1  # á
$aa = [char]0x00E3  # ã
$e  = [char]0x00E9  # é
$i  = [char]0x00ED  # í
$o  = [char]0x00F3  # ó
$oo = [char]0x00F5  # õ
$u  = [char]0x00FA  # ú
$c  = [char]0x00E7  # ç

function Translate-QuizName($nameEn) {
    $before = $nameEn -replace '%.*',''
    $after  = if ($nameEn -match '%') { '%' + ($nameEn -replace '^[^%]*%','') } else { '' }

    $pt = $before `
        -replace 'Vocabulary',          "Vocabul${a}rio" `
        -replace 'Part(\d)',            "Parte`$1" `
        -replace 'The vowels',          'As vogais' `
        -replace 'Vowels',              'Vogais' `
        -replace 'Column',              'Coluna' `
        -replace 'Modified',            'Modificado' `
        -replace 'Composed',            'Composto' `
        -replace 'Country Name',        "Nomes de pa${i}ses" `
        -replace 'Basic Numbers',       "N${u}meros b${a}sicos" `
        -replace 'Advanced Numbers',    "N${u}meros avan${c}ados" `
        -replace 'Basic Word',          "Palavras b${a}sicas" `
        -replace 'Days',                'Dias da semana' `
        -replace 'Adjectives',          'Adjetivos' `
        -replace 'Verbs',               'Verbos' `
        -replace 'Words',               'Palavras' `
        -replace 'Numbers',             "N${u}meros"

    return $pt + $after
}

$rows = & $sq $db ".separator '|'" "SELECT _id, name_en FROM quiz;" | ForEach-Object {
    $parts = $_ -split '\|', 2
    [PSCustomObject]@{ Id = $parts[0]; NameEn = $parts[1] }
}

$sqlPath = Join-Path $env:TEMP "quiz_pt_updates.sql"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$writer = [System.IO.StreamWriter]::new($sqlPath, $false, $utf8NoBom)

foreach ($row in $rows) {
    $pt = Translate-QuizName $row.NameEn
    $ptSql = $pt -replace "'","''"
    $writer.WriteLine("UPDATE quiz SET name_pt = '$ptSql' WHERE _id = $($row.Id);")
}
$writer.WriteLine("PRAGMA user_version;")
$writer.Close()

& $sq $db ".read $sqlPath"
Remove-Item $sqlPath -ErrorAction SilentlyContinue

Write-Output "Done. Sample check:"
& $sq $db "SELECT _id, name_en, name_pt FROM quiz LIMIT 5;"
& $sq $db "SELECT _id, name_en, name_pt FROM quiz WHERE _id IN (5,57);"
