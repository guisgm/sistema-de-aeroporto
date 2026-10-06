$taskJavaCompiler = Get-Command javac -ErrorAction SilentlyContinue
if (-not $taskJavaCompiler) {
    $taskJavaCandidates = @()
    if ($env:JAVA_HOME) { $taskJavaCandidates += (Join-Path $env:JAVA_HOME 'bin') }
    $taskJdkRoot = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path -LiteralPath $taskJdkRoot) {
        $taskJavaCandidates += @(Get-ChildItem -LiteralPath $taskJdkRoot -Directory | Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin' })
    }
    $taskJavaBin = $taskJavaCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'javac.exe') } | Select-Object -First 1
    if (-not $taskJavaBin) { throw 'Instale JDK 21 ou superior e configure JAVA_HOME ou o PATH.' }
    $env:PATH = $taskJavaBin + ';' + $env:PATH
}
