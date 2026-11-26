$ErrorActionPreference = 'Stop'
$stateFile = Join-Path $PSScriptRoot 'runtime\processes.json'
if (-not (Test-Path -LiteralPath $stateFile)) { Write-Host 'No hay servicios iniciados por este proyecto.'; return }
foreach ($item in @(Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json)) {
    $process = Get-Process -Id $item.id -ErrorAction SilentlyContinue
    if ($process -and $process.StartTime.ToUniversalTime().Ticks.ToString() -eq $item.startedTicks) {
        Stop-Process -Id $item.id
        Write-Host ('Detenido: ' + $item.name)
    }
}
Remove-Item -LiteralPath $stateFile
