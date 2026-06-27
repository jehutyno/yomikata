<#
.SYNOPSIS
  Génère un rapport de test versionné pour Yomikata Z, ancré à un commit / une version.

.DESCRIPTION
  Agrège les résultats JUnit XML des suites DÉJÀ exécutées et les estampille avec
  commit + versionName/versionCode + date + device. Écrit un rapport Markdown dans
  build/reports/ (dossier gitignored : c'est un artefact de run, pas une référence versionnée).

  Le script NE lance PAS les tests. Exécuter d'abord les suites voulues, p.ex. :
      $env:JAVA_HOME = "C:\Users\valen\AppData\Local\Programs\Android Studio\jbr"
      .\gradlew.bat test verifyRoborazziDebug          # couches 1 + 3 (JVM)
      .\gradlew.bat connectedDebugAndroidTest          # couches 2 + 4 (émulateur/appareil)
  puis :
      .\scripts\test-report.ps1

  Pour un rapport de RELEASE : committer/taguer d'abord (arbre propre), lancer les suites
  sur ce commit, puis générer le rapport — il sera ancré au tag.

.PARAMETER Device
  Étiquette du device testé. Si omis, prend le 1er device adb connecté.
#>
param(
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot   # scripts/ est sous la racine du repo

# --- Version (depuis app/build.gradle) -----------------------------------------------------------
$gradle = Get-Content (Join-Path $repo "app/build.gradle") -Raw
$versionName = if ($gradle -match 'versionName\s+"([^"]+)"') { $Matches[1] } else { "?" }
$versionCode = if ($gradle -match 'versionCode\s+(\d+)')     { $Matches[1] } else { "?" }

# --- Git ----------------------------------------------------------------------------------------
Push-Location $repo
$sha      = (git rev-parse HEAD).Trim()
$shortSha = (git rev-parse --short HEAD).Trim()
$branch   = (git rev-parse --abbrev-ref HEAD).Trim()
$dirty    = if (git status --porcelain) { "oui (arbre de travail modifié)" } else { "non" }
$baseline = (git log -1 --format="%h %ci" -- app/src/test/screenshots/ 2>$null)
Pop-Location

# --- Device -------------------------------------------------------------------------------------
if (-not $Device) {
    $adb = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $adb) {
        $line = (& $adb devices) | Select-String -Pattern "\sdevice$" | Select-Object -First 1
        if ($line) { $Device = ($line.ToString() -split "\s+")[0] }
    }
    if (-not $Device) { $Device = "n/a" }
}

# --- Agrégation JUnit XML -----------------------------------------------------------------------
function Sum-JUnit([string]$dir) {
    $res = [ordered]@{ tests = 0; failures = 0; skipped = 0; fails = @() }
    if (-not (Test-Path $dir)) { return $res }
    Get-ChildItem $dir -Recurse -Filter "*.xml" | ForEach-Object {
        try { $x = [xml](Get-Content $_.FullName -Raw) } catch { return }
        # //testsuite couvre les deux formats : racine <testsuite> (JVM Gradle, un fichier par
        # classe) ET racine <testsuites> qui enveloppe plusieurs <testsuite> (androidTest).
        foreach ($ts in $x.SelectNodes("//testsuite")) {
            $res.tests    += [int]$ts.tests
            $res.failures += [int]$ts.failures + [int]$ts.errors
            $res.skipped  += [int]$ts.skipped
            foreach ($tc in $ts.SelectNodes("testcase")) {
                if ($tc.failure -or $tc.error) {
                    $msg = @($tc.failure.message, $tc.error.message | Where-Object { $_ })[0]
                    $res.fails += ("{0}.{1} - {2}" -f $tc.classname, $tc.name, $msg)
                }
            }
        }
    }
    return $res
}

$jvm  = Sum-JUnit (Join-Path $repo "app/build/test-results/testDebugUnitTest")
$inst = Sum-JUnit (Join-Path $repo "app/build/outputs/androidTest-results")

$total     = $jvm.tests + $inst.tests
$totalFail = $jvm.failures + $inst.failures
$totalSkip = $jvm.skipped + $inst.skipped
$status    = if ($totalFail -eq 0) { "✅ VERT" } else { "❌ ROUGE ($totalFail échec(s))" }
$date      = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# --- Rendu Markdown -----------------------------------------------------------------------------
$lines = @()
$lines += "# Rapport de test — Yomikata Z"
$lines += ""
$lines += "| | |"
$lines += "|---|---|"
$lines += "| **Version** | $versionName (code $versionCode) |"
$lines += "| **Commit** | ``$shortSha`` ($branch) |"
$lines += "| **SHA complet** | ``$sha`` |"
$lines += "| **Arbre modifié** | $dirty |"
$lines += "| **Date** | $date |"
$lines += "| **Device** | $Device |"
$lines += "| **Statut** | $status |"
$lines += ""
$lines += "## Résultats par suite"
$lines += ""
$lines += "| Couches | Suite | Tests | Échecs | Ignorés |"
$lines += "|---|---|---|---|---|"
$lines += "| 1 + 3 | Unitaires JVM + screenshots (``test`` / ``verifyRoborazziDebug``) | $($jvm.tests) | $($jvm.failures) | $($jvm.skipped) |"
$lines += "| 2 + 4 | Instrumentation Room + Compose (``connectedDebugAndroidTest``) | $($inst.tests) | $($inst.failures) | $($inst.skipped) |"
$lines += "| | **Total** | **$total** | **$totalFail** | **$totalSkip** |"
$lines += ""
if ($totalFail -gt 0) {
    $lines += "## Échecs"
    $lines += ""
    foreach ($f in (@($jvm.fails) + @($inst.fails))) { $lines += "- $f" }
    $lines += ""
}
$lines += "## Baselines screenshot"
$lines += ""
$lines += "Dernier commit ayant touché ``app/src/test/screenshots/`` : ``$baseline``"
$lines += ""
$lines += "> Si **Arbre modifié = oui**, ce rapport ne correspond pas exactement au commit ``$shortSha`` — committer/taguer avant un rapport de release."

$outDir = Join-Path $repo "build/reports"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
$out = Join-Path $outDir "test-report-$versionName-$shortSha.md"
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($out, ($lines -join "`r`n"), $utf8)

Write-Host "Rapport écrit : $out"
Write-Host ("Total : {0} tests, {1} échec(s), {2} ignoré(s) — {3}" -f $total, $totalFail, $totalSkip, $status)
