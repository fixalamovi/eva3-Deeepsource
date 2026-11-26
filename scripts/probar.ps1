param([ValidateSet('server','local')][string]$Modo = 'server', [switch]$Dispositivo, [switch]$Exigir80)
$ErrorActionPreference = 'Stop'
$variant = (Get-Culture).TextInfo.ToTitleCase($Modo)
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\gradlew.bat ":app:test${variant}DebugUnitTest" ":app:logic${variant}DebugCoverage"
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas o la generación del informe de cobertura' }
    if ($Exigir80) {
        & (Join-Path $PSScriptRoot 'comprobar-cobertura.ps1') -Informe "app/build/reports/coverage/logic/${Modo}Debug/jacoco.xml"
    }
    if ($Dispositivo) {
        & .\gradlew.bat ":app:connected${variant}DebugAndroidTest"
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas instrumentadas' }
    }
    Write-Host "Resultados: app/build/reports/tests/test${variant}DebugUnitTest/index.html"
    Write-Host "Cobertura lógica: app/build/reports/coverage/logic/${Modo}Debug/index.html"
} finally { Pop-Location }
