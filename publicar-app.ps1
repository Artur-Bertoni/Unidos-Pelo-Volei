param(
    [string]$Notas,
    [string]$Versao,
    [string]$Credencial = "$env:USERPROFILE\chaves\play-publicador.json"
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$raiz = $PSScriptRoot
$gradleKts = Join-Path $raiz 'app\build.gradle.kts'
$aab = Join-Path $raiz 'app\build\outputs\bundle\prodRelease\app-prod-release.aab'
$servidor = Join-Path $raiz 'server'

function Write-Passo([string]$mensagem) {
    Write-Host ''
    Write-Host "==> $mensagem" -ForegroundColor Cyan
}

function Stop-Publicacao([string]$mensagem) {
    Write-Host ''
    Write-Host $mensagem -ForegroundColor Red
    exit 1
}

function Restore-Versao {
    & git -C $raiz checkout -- app/build.gradle.kts
}

function Save-Commit([string]$mensagem) {
    $arquivo = [IO.Path]::GetTempFileName()
    [IO.File]::WriteAllText($arquivo, $mensagem, (New-Object System.Text.UTF8Encoding($false)))
    try {
        & git -C $raiz commit -F $arquivo | Out-Null
        return ($LASTEXITCODE -eq 0)
    } finally {
        Remove-Item $arquivo -ErrorAction SilentlyContinue
    }
}

function Invoke-Play([string[]]$argumentos) {
    Push-Location $servidor
    try {
        $env:NODE_OPTIONS = '--use-system-ca'
        $saida = & npx --no-install tsx scripts/play-store.ts @argumentos --credencial $Credencial
        $codigo = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($codigo -ne 0) { return $null }
    return (($saida -join "`n") | ConvertFrom-Json)
}

Write-Host 'Publicar o UP Vôlei na Play Store' -ForegroundColor White

Write-Passo 'Conferindo o ambiente'

if (-not (Test-Path $Credencial)) {
    Stop-Publicacao "Não achei a credencial da Play Store em $Credencial. Veja 'Publicando com um comando' no README para criá-la."
}
if (-not (Test-Path (Join-Path $raiz 'keystore.properties'))) {
    Stop-Publicacao 'Falta o keystore.properties na raiz do projeto: sem ele o pacote sai sem assinatura.'
}
$propriedades = Get-Content (Join-Path $raiz 'local.properties') -Raw
if ($propriedades -notmatch '(?m)^prod\.API_URL=\S+' -or $propriedades -notmatch '(?m)^prod\.POWERSYNC_URL=\S+') {
    Stop-Publicacao 'prod.API_URL e prod.POWERSYNC_URL precisam estar preenchidos no local.properties.'
}

$houveCommit = $false
$pendente = & git -C $raiz status --porcelain
if ($pendente) {
    Write-Host ''
    Write-Host 'Alterações que ainda não estão no Git e vão junto nesta publicação (app, site e servidor):'
    & git -C $raiz status --short
    $mensagem = Read-Host 'Mensagem do commit (deixe vazio para cancelar)'
    if (-not $mensagem -or -not $mensagem.Trim()) { Stop-Publicacao 'Publicação cancelada. Nada foi alterado.' }
    & git -C $raiz add -A
    if (-not (Save-Commit $mensagem.Trim())) { Stop-Publicacao 'O commit falhou (mensagem acima).' }
    $houveCommit = $true
}
$ramo = (& git -C $raiz rev-parse --abbrev-ref HEAD).Trim()
if ($ramo -ne 'main') {
    $seguir = Read-Host "Você está na branch '$ramo', não na main. Publicar mesmo assim? (s/n)"
    if ($seguir -ne 's') { Stop-Publicacao 'Publicação cancelada.' }
}

if (-not (Test-Path (Join-Path $servidor 'node_modules'))) {
    Push-Location $servidor
    & npm install --no-audit --no-fund
    Pop-Location
}

Write-Passo 'Consultando a Play Store'
$info = Invoke-Play @('faixas')
if ($null -eq $info) { Stop-Publicacao 'Não consegui falar com a Play Store (mensagem acima).' }

$atuais = @{}
foreach ($faixa in $info.faixas) { $atuais[$faixa.faixa] = $faixa.versaoAtual }

$opcoes = New-Object System.Collections.ArrayList
[void]$opcoes.Add(@{ Rotulo = 'Produção'; Faixa = 'production' })
[void]$opcoes.Add(@{ Rotulo = 'Teste aberto'; Faixa = 'beta' })
$fechadas = @($info.faixas | Where-Object { @('production', 'beta', 'internal') -notcontains $_.faixa } | ForEach-Object { $_.faixa })
if ($fechadas.Count -eq 0) { $fechadas = @('alpha') }
foreach ($nome in $fechadas) { [void]$opcoes.Add(@{ Rotulo = "Teste fechado ($nome)"; Faixa = $nome }) }
[void]$opcoes.Add(@{ Rotulo = 'Teste interno'; Faixa = 'internal' })
[void]$opcoes.Add(@{ Rotulo = 'Compartilhamento interno de apps (só gera um link, não muda a versão)'; Faixa = '' })

Write-Host ''
Write-Host 'Para onde enviar?'
for ($i = 0; $i -lt $opcoes.Count; $i++) {
    $opcao = $opcoes[$i]
    $atual = ''
    if ($opcao.Faixa -and $atuais.ContainsKey($opcao.Faixa) -and $atuais[$opcao.Faixa]) {
        $atual = "   [hoje: $($atuais[$opcao.Faixa])]"
    }
    Write-Host ("  {0}) {1}{2}" -f ($i + 1), $opcao.Rotulo, $atual)
}
$resposta = Read-Host 'Número da opção'
$indice = 0
if (-not [int]::TryParse($resposta, [ref]$indice) -or $indice -lt 1 -or $indice -gt $opcoes.Count) {
    Stop-Publicacao 'Opção inválida.'
}
$escolha = $opcoes[$indice - 1]
$compartilhamento = -not $escolha.Faixa

if ($escolha.Faixa -eq 'production') {
    $confirma = Read-Host 'Isso manda a versão para TODO MUNDO depois da análise do Google. Confirma? (s/n)'
    if ($confirma -ne 's') { Stop-Publicacao 'Publicação cancelada.' }
}

if (-not $compartilhamento) {
    if (-not $Notas) { $Notas = Read-Host 'Notas da versão (o que muda para quem usa, até 500 caracteres)' }
    if (-not $Notas.Trim()) { Stop-Publicacao 'As notas da versão são obrigatórias.' }
    if ($Notas.Length -gt 500) { Stop-Publicacao 'As notas passam de 500 caracteres.' }

    $texto = [IO.File]::ReadAllText($gradleKts)
    $codigoLocal = [int]([regex]::Match($texto, 'versionCode = (\d+)').Groups[1].Value)
    $nomeLocal = [regex]::Match($texto, 'versionName = "([^"]+)"').Groups[1].Value
    $novoCodigo = [Math]::Max($codigoLocal, [int]$info.maiorVersionCode) + 1

    if ($Versao) {
        $novoNome = $Versao
    } else {
        $partes = $nomeLocal.Split('.')
        $partes[$partes.Length - 1] = [string]([int]$partes[$partes.Length - 1] + 1)
        $novoNome = $partes -join '.'
    }

    Write-Passo "Versão $novoNome (código $novoCodigo) para $($escolha.Rotulo)"
    $texto = [regex]::Replace($texto, 'versionCode = \d+', "versionCode = $novoCodigo")
    $texto = [regex]::Replace($texto, 'versionName = "[^"]+"', "versionName = ""$novoNome""")
    [IO.File]::WriteAllText($gradleKts, $texto, (New-Object System.Text.UTF8Encoding($false)))
}

Write-Passo 'Gerando o pacote assinado (leva alguns minutos)'
$inicio = Get-Date
Push-Location $raiz
try {
    & .\gradlew.bat --stop | Out-Null
    $env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
    & .\gradlew.bat :app:bundleProdRelease --console=plain
    $codigoBuild = $LASTEXITCODE
} finally {
    Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    Pop-Location
}
if ($codigoBuild -ne 0 -or -not (Test-Path $aab) -or (Get-Item $aab).LastWriteTime -lt $inicio) {
    Restore-Versao
    $aviso = ''
    if ($houveCommit) { $aviso = ' O seu commit ficou só na sua máquina: corrija e rode de novo, ou envie com git push.' }
    Stop-Publicacao "O build falhou (mensagem acima). A versão no build.gradle.kts foi desfeita.$aviso"
}

if ($compartilhamento) {
    Write-Passo 'Enviando para o compartilhamento interno'
    $resultado = Invoke-Play @('compartilhar', '--aab', $aab)
    if ($null -eq $resultado) { Stop-Publicacao 'O envio falhou (mensagem acima).' }
    Write-Host ''
    Write-Host 'Pronto. Link para instalar (só funciona para quem está liberado no compartilhamento interno):' -ForegroundColor Green
    Write-Host $resultado.downloadUrl
    if ($houveCommit) {
        Write-Passo 'Enviando o commit (o Railway publica o site e o servidor)'
        & git -C $raiz push origin HEAD
    }
    exit 0
}

Write-Passo 'Enviando para a Play Store'
$env:PLAY_NOTAS = $Notas
$env:PLAY_FAIXA = $escolha.Faixa
try {
    $resultado = Invoke-Play @('enviar', '--aab', $aab, '--nome', $novoNome)
} finally {
    Remove-Item Env:PLAY_NOTAS -ErrorAction SilentlyContinue
    Remove-Item Env:PLAY_FAIXA -ErrorAction SilentlyContinue
}
if ($null -eq $resultado) {
    Restore-Versao
    $aviso = ''
    if ($houveCommit) { $aviso = ' O seu commit ficou só na sua máquina: rode de novo, ou envie com git push.' }
    Stop-Publicacao "O envio falhou (mensagem acima). Nada foi publicado e a versão no build.gradle.kts foi desfeita.$aviso"
}

Write-Passo 'Registrando a versão no Git e enviando (o Railway publica o site e o servidor)'
$tag = "v$novoNome"
& git -C $raiz add app/build.gradle.kts
[void](Save-Commit "Publica a versão $novoNome ($novoCodigo) na Play Store: $($escolha.Rotulo)")
$existe = & git -C $raiz tag --list $tag
if (-not $existe) { & git -C $raiz tag $tag }
& git -C $raiz push origin HEAD
if (-not $existe) { & git -C $raiz push origin $tag }

Write-Host ''
Write-Host "Pronto: versão $novoNome (código $novoCodigo) enviada para $($escolha.Rotulo)." -ForegroundColor Green
if ($resultado.rascunho) {
    Write-Host 'A Play Store só aceitou como RASCUNHO nessa faixa. Termine no Play Console: Testar e lançar > a faixa > Editar versão > Revisar > Lançar.' -ForegroundColor Yellow
} elseif (-not $resultado.enviadoParaAnalise) {
    Write-Host 'Falta um clique: no Play Console, abra Visão geral da publicação e clique em Enviar alterações para análise.' -ForegroundColor Yellow
} else {
    Write-Host 'Já foi para a análise do Google. Acompanhe em Visão geral da publicação no Play Console.'
}
