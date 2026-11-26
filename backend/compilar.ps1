$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tools.ps1')
$maven = Find-MavenExecutable
Push-Location $PSScriptRoot
try {
    & $maven -B verify
    if ($LASTEXITCODE -ne 0) { throw 'La compilación falló. Revisa el mensaje de Maven.' }
} finally { Pop-Location }
