param([string]$Informe = 'docs/evidencias/cobertura-logica/jacoco.xml', [double]$Minimo = 80)
$ErrorActionPreference = 'Stop'
$reportPath = if ([IO.Path]::IsPathRooted($Informe)) { $Informe } else { Join-Path (Split-Path $PSScriptRoot -Parent) $Informe }
$settings = [Xml.XmlReaderSettings]::new()
$settings.DtdProcessing = [Xml.DtdProcessing]::Ignore
$settings.XmlResolver = $null
$reader = [Xml.XmlReader]::Create($reportPath, $settings)
try {
    $report = [Xml.XmlDocument]::new()
    $report.XmlResolver = $null
    $report.Load($reader)
} finally { $reader.Dispose() }
$counter = $report.SelectSingleNode('/report/counter[@type="LINE"]')
if (-not $counter) { throw 'El informe no contiene un contador global LINE' }
$covered = [double]$counter.GetAttribute('covered')
$missed = [double]$counter.GetAttribute('missed')
if ($covered + $missed -le 0) { throw 'El informe no contiene líneas medibles' }
$percentage = 100 * $covered / ($covered + $missed)
Write-Host ('Cobertura lógica de líneas: {0:F2}% ({1}/{2})' -f $percentage, $covered, ($covered + $missed))
if ($percentage -lt $Minimo) { throw "No se alcanza el mínimo de $Minimo%. Revisa las clases pendientes en el informe HTML." }
