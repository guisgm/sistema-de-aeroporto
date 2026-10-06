$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    . (Join-Path $PSScriptRoot 'scripts/configurar-java.ps1')
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        throw 'Instale um JDK 21 ou superior e configure javac no PATH.'
    }
    New-Item -ItemType Directory -Path 'target/classes', 'lib' -Force | Out-Null
    $taskDriverPath = Join-Path $PSScriptRoot 'lib/postgresql-42.7.13.jar'
    # SHA-256 conferido nos arquivos oficiais do pgJDBC e do Maven Central.
    $taskExpectedHash = '6e0e4cc2d8cae902084f8a2b18728b073a6fd9d1f87c9d8bff8f298c18185b93'
    if (-not (Test-Path -LiteralPath $taskDriverPath)) {
        Write-Host 'Baixando o driver JDBC do PostgreSQL...'
        $taskDriverUrl = 'https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar'
        $taskDownloadPath = $taskDriverPath + '.download'
        Invoke-WebRequest -Uri $taskDriverUrl -OutFile $taskDownloadPath
        $taskActualHash = (Get-FileHash -LiteralPath $taskDownloadPath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($taskExpectedHash -ne $taskActualHash) {
            Remove-Item -LiteralPath $taskDownloadPath
            throw 'O download do driver não passou na conferência de integridade.'
        }
        Move-Item -LiteralPath $taskDownloadPath -Destination $taskDriverPath
    }
    if ((Get-FileHash -LiteralPath $taskDriverPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $taskExpectedHash) {
        throw 'O driver JDBC local não corresponde à versão oficial esperada.'
    }
    $taskSources = @(Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $taskSourceList = Join-Path $PSScriptRoot 'target/fontes.txt'
    [System.IO.File]::WriteAllLines($taskSourceList, $taskSources, [System.Text.UTF8Encoding]::new($false))
    & javac --release 21 -encoding UTF-8 -d 'target/classes' "@$taskSourceList"
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação Java.' }
    Write-Host 'Compilação concluída.'
} finally {
    Pop-Location
}
