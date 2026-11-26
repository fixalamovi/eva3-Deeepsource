param([switch]$SinCompilar)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tools.ps1')
$runtime = Join-Path $PSScriptRoot 'runtime'
$logs = Join-Path $runtime 'logs'
$stateFile = Join-Path $runtime 'processes.json'
if (Test-Path -LiteralPath $stateFile) {
    foreach ($old in @(Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json)) {
        $process = Get-Process -Id $old.id -ErrorAction SilentlyContinue
        if ($process -and $process.StartTime.ToUniversalTime().Ticks.ToString() -eq $old.startedTicks) {
            throw 'Los servicios ya están iniciados. Usa detener.ps1 antes de reiniciarlos.'
        }
    }
}
if (-not $SinCompilar) { & (Join-Path $PSScriptRoot 'compilar.ps1') }
$java = Find-JavaExecutable
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$services = @(
    @{ name='proyectos-service'; port=8081; peer='--app.records-url=http://127.0.0.1:8082' },
    @{ name='registros-service'; port=8082; peer='--app.projects-url=http://127.0.0.1:8081' }
)
$started = @()
try {
    foreach ($service in $services) {
        $jar = Join-Path $PSScriptRoot ($service.name + '\target\' + $service.name + '-1.0.0.jar')
        if (-not (Test-Path -LiteralPath $jar)) { throw 'Primero ejecuta compilar.ps1.' }
        $arguments = @('-jar', ('"' + $jar + '"'), ('--server.port=' + $service.port), $service.peer)
        $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs ($service.name + '.out.log')) -RedirectStandardError (Join-Path $logs ($service.name + '.err.log'))
        $started += @{ id=$process.Id; startedTicks=$process.StartTime.ToUniversalTime().Ticks.ToString(); name=$service.name }
    }
    $started | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding utf8
    foreach ($service in $services) {
        $ready = $false
        for ($attempt=0; $attempt -lt 45; $attempt++) {
            $own = $started | Where-Object { $_.name -eq $service.name }
            if (-not (Get-Process -Id $own.id -ErrorAction SilentlyContinue)) { throw ('No pudo iniciar ' + $service.name + '. Revisa runtime/logs.') }
            try {
                $health = Invoke-RestMethod -Uri ('http://127.0.0.1:' + $service.port + '/api/health') -TimeoutSec 1
                if ($health.ok) { $ready=$true; break }
            } catch { }
            Start-Sleep -Milliseconds 500
        }
        if (-not $ready) { throw ('No respondió ' + $service.name + '. Revisa runtime/logs.') }
    }
    Write-Host 'Servicios listos: proyectos 8081 y registros 8082. Ejecuta serverDebug en Android Studio.'
} catch {
    foreach ($item in $started) {
        $own = Get-Process -Id $item.id -ErrorAction SilentlyContinue
        if ($own -and $own.StartTime.ToUniversalTime().Ticks.ToString() -eq $item.startedTicks) { Stop-Process -Id $item.id }
    }
    throw
}
