param([switch]$Inicializar, [switch]$Ajuda)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'compilar.ps1')
    $taskJavaArgs = @()
    if ($Inicializar) { $taskJavaArgs += '--inicializar' }
    if ($Ajuda) { $taskJavaArgs += '--ajuda' }
    & java '-Dfile.encoding=UTF-8' -cp 'target/classes;lib/*' br.edu.aeroporto.Main @taskJavaArgs
    if ($LASTEXITCODE -ne 0) { throw 'A aplicação foi encerrada com erro. Confira a mensagem acima.' }
} finally {
    Pop-Location
}
