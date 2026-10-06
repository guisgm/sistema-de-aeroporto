param([switch]$Inicializar, [switch]$Ajuda, [switch]$Migrar, [switch]$Diagnostico)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    . (Join-Path $PSScriptRoot 'scripts/configurar-java.ps1')
    & (Join-Path $PSScriptRoot 'compilar.ps1')
    $taskJavaArgs = @()
    if ($Inicializar) { $taskJavaArgs += '--inicializar' }
    if ($Ajuda) { $taskJavaArgs += '--ajuda' }
    if ($Migrar) { $taskJavaArgs += '--migrar' }
    if ($Diagnostico) { $taskJavaArgs += '--diagnostico' }
    & java '-Dfile.encoding=UTF-8' -cp 'target/classes;lib/*' br.edu.aeroporto.Main @taskJavaArgs
    if ($LASTEXITCODE -ne 0) { throw 'A aplicação foi encerrada com erro. Confira a mensagem acima.' }
} finally {
    Pop-Location
}
