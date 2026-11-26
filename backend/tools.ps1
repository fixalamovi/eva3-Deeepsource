function Find-MavenExecutable {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $cache = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
    if (Test-Path -LiteralPath $cache) {
        $candidate = Get-ChildItem -LiteralPath $cache -Filter mvn.cmd -Recurse -File |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($candidate) { return $candidate.FullName }
    }
    throw 'Instala Maven 3.6 o posterior, o abre backend/pom.xml en IntelliJ y ejecuta package.'
}
function Find-JavaExecutable {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw 'Configura un JDK 17 o posterior para ejecutar los servicios.'
}
