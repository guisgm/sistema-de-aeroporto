param([switch]$Integracao)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    . (Join-Path $PSScriptRoot 'scripts/configurar-java.ps1')
    & (Join-Path $PSScriptRoot 'compilar.ps1')
    New-Item -ItemType Directory -Path 'target/test-classes' -Force | Out-Null
    $taskTestSources = @(Get-ChildItem -LiteralPath 'src/test/java' -Recurse -Filter '*.java' | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $taskTestList = Join-Path $PSScriptRoot 'target/fontes-testes.txt'
    [IO.File]::WriteAllLines($taskTestList, $taskTestSources, [Text.UTF8Encoding]::new($false))
    & javac --release 21 -encoding UTF-8 -cp 'target/classes;lib/*' -d 'target/test-classes' "@$taskTestList"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes.' }
    & java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/test-classes;lib/*' br.edu.aeroporto.UnidadeTeste
    if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes unitarios Java.' }
    if ($Integracao) {
        & java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/test-classes;lib/*' br.edu.aeroporto.IntegracaoTeste
        if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes PostgreSQL.' }
    }
} finally { Pop-Location }
