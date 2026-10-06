param([string]$Binario = '')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$taskClusterStarted = $false
try {
    . (Join-Path $PSScriptRoot 'scripts/configurar-java.ps1')
    & (Join-Path $PSScriptRoot 'compilar.ps1')
    if (-not $Binario) { $Binario = Join-Path $PSScriptRoot 'artifacts/pg-tools/extracted/pgsql/bin' }
    $taskPgBin = [IO.Path]::GetFullPath($Binario)
    if (-not (Test-Path -LiteralPath (Join-Path $taskPgBin 'initdb.exe'))) {
        throw 'Informe -Binario com a pasta bin dos binarios PostgreSQL. Veja docs/08_operacao_java.md.'
    }
    $taskProbe = [Net.Sockets.TcpClient]::new()
    try {
        $taskConnect = $taskProbe.BeginConnect('127.0.0.1', 55439, $null, $null)
        if ($taskConnect.AsyncWaitHandle.WaitOne(1000) -and $taskProbe.Connected) {
            throw 'Porta 55439 ja ocupada. Nenhum servidor existente sera encerrado por este script.'
        }
    } finally { $taskProbe.Dispose() }
    $taskArtifacts = Join-Path $PSScriptRoot 'artifacts'
    New-Item -ItemType Directory -Path $taskArtifacts -Force | Out-Null
    $taskPgData = Join-Path $taskArtifacts ('pg-teste-' + [Guid]::NewGuid().ToString('N'))
    & (Join-Path $taskPgBin 'initdb.exe') -D $taskPgData -U aeroporto_teste -A trust -E UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'Falha na criacao do cluster de teste.' }
    & (Join-Path $taskPgBin 'pg_ctl.exe') -D $taskPgData -l (Join-Path $taskPgData 'servidor.log') -o '-h 127.0.0.1 -p 55439' -w start
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar o cluster de teste.' }
    $taskClusterStarted = $true
    & java -cp 'lib/*' scripts/PrepararBancoTeste.java
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar o schema de teste.' }
    & (Join-Path $PSScriptRoot 'testar-java.ps1') -Integracao
    if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes de integracao.' }
} finally {
    if ($taskClusterStarted) {
        & (Join-Path $taskPgBin 'pg_ctl.exe') -D $taskPgData -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning ('Servidor de teste requer encerramento: ' + $taskPgData) }
    }
    Pop-Location
}
