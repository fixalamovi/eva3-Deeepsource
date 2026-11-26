param([ValidateSet('server','local')][string]$Modo = 'server')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$variant = (Get-Culture).TextInfo.ToTitleCase($Modo)
$propertiesPath = Join-Path $projectRoot 'firma/keystore.properties'
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw 'Falta firma/keystore.properties. Consulta docs/APK_FIRMADO.md.'
}
$signing = ConvertFrom-StringData (Get-Content -LiteralPath $propertiesPath -Raw)
foreach ($field in @('STORE_FILE','STORE_PASSWORD','KEY_ALIAS','KEY_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace($signing[$field])) { throw "Falta $field en la configuración de firma" }
}
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $signing.STORE_FILE))) { throw 'No se encontró la clave release' }

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    $studioJdk = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
    if (Test-Path -LiteralPath (Join-Path $studioJdk 'bin/java.exe')) { $env:JAVA_HOME = $studioJdk }
    else { throw 'Configura JAVA_HOME o ejecuta desde la terminal de Android Studio' }
}
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$buildToolsRoot = Join-Path $sdkRoot 'build-tools'
if (-not (Test-Path -LiteralPath $buildToolsRoot)) { throw 'No se encontró el SDK. Configura ANDROID_HOME con la ruta del SDK de Android Studio' }
$tools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
    Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $tools) { throw 'Instala Android SDK Build-Tools en SDK Manager' }
$signer = Join-Path $tools.FullName 'apksigner.bat'
if (-not (Test-Path -LiteralPath $signer)) { throw 'No se encontró apksigner en Build-Tools' }

Push-Location $projectRoot
try {
    & .\gradlew.bat ":app:test${variant}DebugUnitTest" ":app:logic${variant}DebugCoverage" ":app:assemble${variant}Release"
    if ($LASTEXITCODE -ne 0) { throw 'Falló Gradle; no se generó una entrega nueva' }
    $apkPath = Join-Path $projectRoot "app/build/outputs/apk/$Modo/release/app-$Modo-release.apk"
    if (-not (Test-Path -LiteralPath $apkPath)) { throw "Gradle no produjo el APK esperado: $apkPath" }
    $verification = & $signer verify --verbose --print-certs $apkPath 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'El APK no superó la verificación de firma' }
    $outputDirectory = Join-Path $projectRoot 'entregables'
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    $destination = Join-Path $outputDirectory "Deepsource-$Modo-release.apk"
    Copy-Item -LiteralPath $apkPath -Destination $destination -Force
    $verification | Set-Content -LiteralPath (Join-Path $outputDirectory "firma-$Modo.txt") -Encoding UTF8
    $hash = Get-FileHash -LiteralPath $destination -Algorithm SHA256
    "$($hash.Hash)  Deepsource-$Modo-release.apk" | Set-Content -LiteralPath (Join-Path $outputDirectory "SHA256-$Modo.txt") -Encoding ASCII
    Write-Host "APK firmado y verificado: $destination"
    Write-Host "Cobertura: app/build/reports/coverage/logic/${Modo}Debug/index.html"
} finally { Pop-Location }
