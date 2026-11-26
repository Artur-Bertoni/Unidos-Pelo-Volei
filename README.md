# Unidos Pelo Vôlei

Controle de jogadores, times, chaveamento, placar e histórico do grupo de vôlei de
sábado, com backend real, login Google, sincronização multiusuário em tempo real e
sorteio de times equilibrado por gênero e habilidade.

O app é **aberto para qualquer um baixar e fechado por grupo**. Quem entra não cai
em grupo nenhum: precisa de uma **chave de acesso** dada pela diretoria de um grupo,
ou cria o próprio grupo em dois toques. Tudo — jogadores, times, chaveamento,
placar, mural, agenda, regras e financeiro — pertence a um grupo, e a mesma conta
pode participar de vários e trocar entre eles pelo cabeçalho. O **Unidos Pelo Vôlei**
é só o primeiro grupo cadastrado. Isso está detalhado em
[Grupos de jogo e chave de acesso](#grupos-de-jogo-e-chave-de-acesso).

São **dois clientes sobre o mesmo backend**:

| Cliente | Pasta | Para quem |
|---|---|---|
| App Android nativo | [`app/`](app/) | Quem usa Android instala pela Play Store |
| Aplicação web (PWA) | [`web/`](web/) | Quem usa iPhone — ou qualquer navegador — acessa pelo link |

Os dois falam com a mesma API e o mesmo PowerSync, hospedados no Railway, então
presença, times e placar aparecem nos dois em segundos. Por isso **os dois clientes
vivem na mesma branch**: as migrations e as sync rules valem para ambos ao mesmo
tempo, e separá-los em branches faria toda mudança de schema precisar pousar duas
vezes.

---

## Sumário

- [O que o app faz](#o-que-o-app-faz)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Backend no Railway](#backend-no-railway)
  - [O que roda no Railway](#o-que-roda-no-railway)
  - [A infraestrutura está no repositório](#a-infraestrutura-está-no-repositório)
  - [Subindo do zero](#subindo-do-zero)
  - [Migrations](#migrations)
  - [Sync rules](#sync-rules)
  - [Login Google](#login-google)
  - [Trazendo os dados do Supabase](#trazendo-os-dados-do-supabase)
  - [Backups](#backups)
- [Chaves em local.properties](#chaves-em-localproperties)
- [Assinatura do release](#assinatura-do-release)
- [Publicando uma versão na Play Store](#publicando-uma-versão-na-play-store)
- [Rodando localmente para teste](#rodando-localmente-para-teste)
  - [Rodar no Android Studio](#rodar-no-android-studio)
  - [Backend local](#backend-local)
  - [Alternativa por linha de comando](#alternativa-por-linha-de-comando)
- [Aplicação web](#aplicação-web)
  - [Rodando a web localmente](#rodando-a-web-localmente)
  - [Publicando a web](#publicando-a-web)
  - [Páginas públicas](#páginas-públicas)
- [As cinco abas](#as-cinco-abas)
  - [A aba EU é um índice, não uma página](#a-aba-eu-é-um-índice-não-uma-página)
  - [O botão Voltar do aparelho](#o-botão-voltar-do-aparelho)
  - [Configurações, o índice da diretoria](#configurações-o-índice-da-diretoria)
- [Grupos de jogo e chave de acesso](#grupos-de-jogo-e-chave-de-acesso)
- [Quem é quem no app](#quem-é-quem-no-app)
- [Confirmação de presença e lembretes](#confirmação-de-presença-e-lembretes)
- [Mural, agenda e regras](#mural-agenda-e-regras)
- [Financeiro](#financeiro)
- [Nota por fundamento e avaliação entre colegas](#nota-por-fundamento-e-avaliação-entre-colegas)
- [Testes](#testes)
- [Decisões de projeto](#decisões-de-projeto)
- [Fora do escopo do MVP](#fora-do-escopo-do-mvp)
- [Solução de problemas](#solução-de-problemas)

---

## O que o app faz

| # | Funcionalidade | Onde fica |
|---|---|---|
| 1 | CRUD de jogadores com nota de 1 a 5 em cada um dos seis fundamentos e gênero — as notas **só a diretoria vê e edita**, e a estrela geral é a média delas | aba **TIMES** → botão **Jogadores** |
| 2 | Presença do dia: busca pelo nome, filtro de presentes/ausentes, chave **Presente?** na linha, **Marcar todos** e **Limpar presenças** — tudo só para a diretoria | aba **TIMES** → botão **Jogadores** |
| 3 | Gestão de times coloridos (nome, cor, sigla de 2 letras), com ativar/desativar o time do dia, **Ajustar** ao tanto de gente e excluir | aba **TIMES** → **Novo time** / lápis / chave no cartão |
| 4 | Sorteio dos times mirando 2 homens e 2 mulheres, equilibrando a força e evitando repetir duplas | aba **TIMES** → **Distribuir** |
| 5 | Chaveamento round-robin em que **toda rodada enche todas as quadras**, sem quadra ociosa | aba **JOGOS** → **Gerar chaveamento** |
| 6 | Placar ao vivo por partida, digitado ou ponto a ponto, com vencedor marcado automaticamente | aba **JOGOS** → toque em uma partida |
| 7 | Classificação geral com V, S e PP e desempate na ordem correta | aba **CLASSIFICAÇÃO** |
| 8 | Elenco vinculado e histórico de jogos por time (rodada, quadra, placar, resultado); nível e força do time **só para a diretoria** | aba **TIMES** → toque em um time |
| 9 | Encerrar o dia: guarda o desempenho e a presença de quem veio, desfaz os times, apaga o chaveamento e zera as presenças | aba **JOGOS** → **Encerrar dia** |
| 10 | Login com Google; usuário comum só lê, admin edita | tela inicial |
| 11 | Funciona offline e sincroniza ao reconectar | em todo o app |
| 12 | Cada pessoa vira dona da própria ficha: escolhe o nome na lista e a diretoria confirma — ou a diretoria liga jogador e conta na mão | aba **EU** |
| 13 | Ficha do membro com aniversário, telefone e contato de emergência | aba **EU** → **Detalhes da conta** → lápis |
| 13b | Cada um escolhe como paga: **Mensalista** ou **Diarista** | aba **EU** → **Detalhes da conta** → lápis |
| 14 | Confirmação antecipada do sábado (**Vou / Talvez / Não vou**), pela pessoa ou pela diretoria | aba **EU** e aba **SOCIAL** → **Chamada** |
| 15 | **Trazer confirmados**: quem respondeu que vem vira presença na lista de hoje | aba **SOCIAL** → **Chamada** (só diretoria) |
| 16 | Lembrete por notificação na sexta à noite e no sábado de manhã | push no Android e no PWA |
| 17 | Mural de recados da diretoria, com imagem anexada e emoji de reação escolhido por publicação, editável depois de publicado — é a tela que abre | aba **SOCIAL** → **Mural** |
| 18 | Agenda de eventos, mais aniversários e tempo de casa calculados sozinhos | aba **SOCIAL** → **Agenda** |
| 19 | Páginas de regras do vôlei de areia em quarteto, regras do grupo e campeonatos, editáveis pela diretoria | aba **SOCIAL** → **Regras** |
| 20 | Mensalidade e diária, extrato pessoal e Pix Copia e Cola | aba **EU** → **Financeiro**; painel do grupo em **Configurações** |
| 21 | Avaliação anônima entre companheiros de time e painel de evolução com dicas; bolinha verde na aba **EU** quando alguém espera a sua nota | aba **EU** → **Avaliar colegas** e **Histórico** |
| 22 | Mini tour no primeiro login, terminando na escolha do grupo | primeira entrada |
| 23 | **Grupos de jogo**: entre com uma chave de acesso, crie o seu grupo e troque entre os grupos que você participa | aba **EU** → **Grupos**, ou o nome do grupo no cabeçalho |
| 24 | **Chaves de acesso**: a diretoria cria códigos com validade, limite de usos e papel de entrada, e revoga quando quiser | aba **EU** → **Configurações** → **Chaves de acesso** |
| 25 | **Membros do grupo**: promover à diretoria, rebaixar a atleta e tirar quem saiu | aba **EU** → **Configurações** → **Membros do grupo** |
| 26 | **Nome, cidade e logo do grupo** editáveis pela diretoria, com as iniciais como reserva | aba **EU** → **Configurações** → **Nome, cidade e logo** |

O indicador **Online / Conectando / Offline** no cabeçalho mostra o estado do sync.

### Sábados com mais ou menos gente

O grupo é grande e nunca vem todo mundo. Quem organiza abre o app na quadra e
prepara o dia em três passos, sem precisar entrar jogador por jogador.

1. *Marcar quem veio.* Na listagem de jogadores, a coluna **Presente?** liga e
   desliga cada um direto na linha. A busca no topo acha o nome no meio de uma
   lista longa e os filtros **Todos / Presentes / Ausentes** separam quem já foi
   marcado de quem falta conferir. Para as pontas: **Marcar todos** deixa o grupo
   inteiro presente (sábado cheio, você só desmarca os poucos que faltaram) e
   **Limpar presenças** desmarca todo mundo de uma vez. O cabeçalho mostra
   quantos estão presentes e a divisão entre homens e mulheres.
2. *Ajustar os times do dia.* Cada cartão da aba **TIMES** tem uma chave no canto
   que tira o time do dia sem apagar nada: ele some do sorteio, do chaveamento e
   da contagem, e volta na semana que vem com um toque. O resumo no topo da aba
   compara os presentes com os times ativos e sugere quantos times de 4 cabem
   hoje; o **Ajustar** ao lado da sugestão liga e desliga os times sozinho até
   bater com o número de presentes, mantendo os que já estavam ativos e
   preenchendo o resto pela ordem. O lápis abre a edição, onde fica o **Excluir
   time** — esse sim apaga o time e os jogos dele.
3. *Sortear e jogar.* Só entram no sorteio os jogadores presentes e os times
   ativos, então o resto do fluxo — **Distribuir**, **Gerar chaveamento**,
   placar e **Encerrar dia** — segue igual em qualquer tamanho de sábado.

Presença é a mesma flag `ativo` do jogador, e o time do dia é a flag `ativo` do
time: nada é apagado ao desmarcar, e o histórico dos dias já encerrados continua
inteiro.

O **Encerrar dia** fecha o ciclo: guarda uma linha em `player_day_stats` para
cada presente — com time e desempenho para quem jogou, sem time para quem só
apareceu — e zera as presenças, para o próximo sábado começar em branco. É daí
que sai o `X dias` na linha de cada jogador: quantos sábados a pessoa veio, não
quantos ela jogou.

### Como os algoritmos funcionam

**Sorteio dos times** ([`TeamDraft.kt`](app/src/main/java/com/unidospelovolei/domain/scheduling/TeamDraft.kt)):
três critérios, nesta ordem de importância.

1. *Gênero.* Cada time mira 2 homens e 2 mulheres, independente de habilidade.
   Quando o grupo do dia não permite, as cotas são ajustadas time a time sempre
   pelo extremo, para a sobra ficar espalhada em vez de concentrada num time só.
2. *Histórico.* Duplas que já jogaram juntas pesam contra. O histórico vem dos
   dias já encerrados (com peso decaindo pela recência), do elenco que está
   valendo agora e dos sorteios que acabaram de aparecer na tela.
3. *Habilidade.* Dentro do que sobra, o jogador vai para o time mais fraco até
   ali, o que aproxima a força total dos elencos. A força de um jogador é a média
   das seis notas por fundamento (`nota_media`), em ponto flutuante — ela se move
   sozinha com as avaliações dos colegas, como conta a seção
   [Nota por fundamento](#nota-por-fundamento-e-avaliação-entre-colegas).

Cada tentativa é uma montagem gulosa com desempate aleatório, e entre 240
tentativas fica a de menor custo. Por isso dois toques em **Sortear de novo**
dão times diferentes, e o sorteio de amanhã não repete o de hoje.

**Chaveamento** ([`RoundRobinScheduler.kt`](app/src/main/java/com/unidospelovolei/domain/scheduling/RoundRobinScheduler.kt)):
a regra que manda é **nenhuma quadra parada**. Toda rodada tem exatamente uma
partida por quadra selecionada, ou seja, `2 × quadras` times em jogo — com 3
quadras são sempre 6 times na areia e o resto folgando. As rodadas se sucedem
até fechar o round-robin completo.

Com 9 times e 3 quadras:

```
rodada 1   quadra 1: 1 x 2   quadra 2: 3 x 4   quadra 3: 5 x 6   folgam 7, 8, 9
rodada 2   quadra 1: 7 x 8   quadra 2: 9 x 1   quadra 3: 2 x 3   folgam 4, 5, 6
...
rodada 12  a última quadra fecha o 36º confronto
```

São 36 confrontos e 3 por rodada: 12 rodadas exatas, todas cheias, nenhum
confronto repetido. Cada time joga 8 partidas e folga 4.

Cada rodada é montada escolhendo `quadras` confrontos que não compartilham time.
Os candidatos são ordenados por, nesta ordem:

1. *Confronto inédito primeiro.* Fechar o round-robin vem antes de tudo.
2. *Quem já jogou demais seguido vai para o fim da fila.* O limite de partidas
   emendadas sai da própria aritmética do formato: com `n` times e `q` quadras,
   `n − 2q` folgam por rodada, então dá para alternar a cada
   `⌈2q ÷ (n − 2q)⌉` jogos. Com 9 times e 3 quadras isso dá 2.
3. *Confronto menos repetido, depois quem está descansado há mais tempo, depois
   quem ainda tem mais adversários pela frente.* O último critério é o que evita
   o beco sem saída no fim, quando sobram confrontos que dividem os mesmos times.

Uma busca com backtracking pega o melhor conjunto que cabe na rodada, e a geração
inteira é repetida 12 vezes com sorteios diferentes: fica a tentativa com menos
revanches, menos rodadas e sequências mais curtas.

Quando a conta não fecha redonda — 8 times em 3 quadras dão 28 confrontos, e 28
não divide por 3 — a última rodada completa a quadra que sobraria vazia com uma
**revanche** de quem menos se repetiu. Quadra parada, nunca.

O selo **Fase** agrupa rodadas em que todo time joga a mesma quantidade de vezes
(`n ÷ mdc(n, 2q)` rodadas). Com 9 times e 3 quadras são 4 fases de 3 rodadas, com
2 jogos por time em cada. Quando a fase tem uma rodada só — porque todo mundo
joga toda rodada — o selo some, que aí ele não diria nada.

A habilidade **não** entra no chaveamento, apenas na formação dos times.

---

## Stack e arquitetura

| Camada | Tecnologia |
|---|---|
| App Android | Kotlin + Jetpack Compose, MVVM |
| Build Android | Gradle 9.7.1 + Android Gradle Plugin 9.3.2, `compileSdk` 37, `minSdk` 26 |
| Aplicação web | React + TypeScript sobre Vite, PWA instalável, servida por Caddy |
| API | Node 24 + TypeScript com [Hono](https://hono.dev), em [`server/`](server/) |
| Banco | PostgreSQL gerenciado pelo Railway |
| Sync offline | PowerSync self-hosted (`journeyapps/powersync-service`), usando o próprio Postgres como armazenamento; nos clientes, `com.powersync` e `@powersync/web` |
| Autenticação | Login Google (Credential Manager no Android, Google Identity Services na web) conferido pela API, que emite a sessão |
| Hospedagem | Railway, só produção; desenvolvimento é local |
| Push | Firebase Cloud Messaging no Android e Web Push no navegador, disparados por um cron no Railway |

### Onde cada coisa roda

Fora do repositório o app depende de quatro lugares, e só um deles é infraestrutura:

| Lugar | Para quê | Por que fica fora do Railway |
|---|---|---|
| **Railway** | Postgres, API, PowerSync, site e cron de lembretes de produção | — |
| **Google Cloud** | OAuth client do login Google e tela de consentimento | Só o Google emite o token de login do Google |
| **Firebase** | Entrega do push no Android | O Android só acorda app fechado pelo FCM |
| **Play Console** | Publicação do app | É a loja |

Todo projeto Firebase é um projeto do Google Cloud. Se o Firebase estiver no mesmo
projeto do OAuth, Google Cloud e Firebase são duas telas do mesmo projeto, não duas
contas para cuidar.

### Fluxo de dados

```
            Google (Credential Manager no Android, Google Identity Services na web)
                                     │ ID token
                                     ▼
   Compose / React ──▶ Repository ──▶ API (Railway) ── sessão: access token + refresh token
        ▲                  │            │
        │ watch()          │ escrita    │ aplica sob RLS, como o usuário
        │                  ▼            ▼
        └───────── SQLite local     PostgreSQL (Railway)
                      ▲    │ upload ─▶ POST /sync/upload
                      │                 │ replicação lógica
                      └──── stream ◀── PowerSync (Railway)
```

Pontos importantes:

- **O app nunca conecta direto no Postgres.** Leituras saem do SQLite local; escritas
  são gravadas no SQLite local e enviadas pelo conector de upload do PowerSync para
  `POST /sync/upload` da API. A API aplica cada operação **como o usuário logado**: a
  transação roda com o papel `authenticated` e o `sub` do token em
  `request.jwt.claims`, então toda policy RLS e todo trigger continuam valendo
  exatamente como valiam no Supabase.
- **Não há Room nem outbox próprio.** O SQLite gerenciado pelo PowerSync é a fonte
  de verdade local, e a fila de upload é a dele.
- **Resolução de conflito é last-write-wins por linha.** É aceitável porque cada
  partida é registrada por uma pessoa. Para reduzir ainda mais a chance de conflito,
  toda escrita de placar toca apenas as colunas daquela partida
  (`UPDATE matches SET score_a = ?, score_b = ? WHERE id = ?`), então duas pessoas
  marcando pontos em quadras diferentes nunca disputam a mesma linha.
- **Um segredo assina tudo.** A API assina o access token com `JWT_SECRET` (HS256,
  `kid` `upv`, audiências `powersync` e `upv-api`, validade de 55 minutos) e o
  PowerSync valida com o mesmo segredo, configurado em
  [`powersync/service.yaml`](powersync/service.yaml). Nenhum serviço precisa chamar
  o outro.
- **Uma operação recusada não trava a fila.** Se o banco recusa uma escrita por
  regra de negócio ou permissão (códigos `22xxx`, `23xxx`, `42501`, `42703` e
  `P0001`), a API descarta só aquela operação, aplica as outras e devolve a lista do
  que recusou. Erro de rede ou do servidor faz a transação inteira voltar e o
  PowerSync tenta de novo depois.

### A API

| Rota | O que faz |
|---|---|
| `POST /auth/google` | Recebe o ID token do Google e o nonce, confere com o Google, acha ou cria a conta e devolve a sessão |
| `POST /auth/renovar` | Troca o refresh token por um access token novo |
| `POST /auth/sair` | Revoga o refresh token |
| `POST /sync/upload` | Aplica a fila de upload do PowerSync |
| `POST /rpc/entrar_no_grupo`, `/rpc/criar_grupo`, `/rpc/sair_do_grupo` | Chamam as funções do banco com o mesmo nome |
| `GET`/`POST /grupos/:grupo/chaves`, `PATCH`/`DELETE /chaves/:chave` | Chaves de acesso |
| `GET /grupos/:grupo/membros`, `PATCH`/`DELETE /grupos/:grupo/membros/:perfil` | Membros e papéis |
| `GET /grupos/:grupo/pagamentos` | Painel financeiro da diretoria |
| `PUT /arquivos/:bucket/:grupo/:nome` | Envia imagem do mural ou logo do grupo |
| `GET /arquivos/:bucket/:grupo/:nome` | Serve a imagem, sem login |
| `GET /saude` | Healthcheck do Railway |

A API não decide permissão: quem decide é a policy no banco, como antes. Ela só abre
a transação com a identidade certa.

A sessão tem duas partes. O **access token** é um JWT curto, guardado em memória e
nas preferências do aparelho, que autoriza a API e o PowerSync. O **refresh token** é
um valor aleatório guardado em `auth.sessoes` só como hash; ele não gira a cada uso,
vale enquanto a pessoa abrir o app pelo menos uma vez a cada 180 dias e morre no
**Sair**. Não girar é de propósito: o app fica dias offline na quadra, e dois pedidos
de renovação simultâneos com rotação derrubariam a sessão de quem não fez nada de
errado.

### Camadas

```
com.unidospelovolei
├── AppContainer.kt          injeção de dependência manual
├── domain/
│   ├── model/               modelos puros (Player, Team, Match, Standing, ...)
│   └── scheduling/          TeamDraft e RoundRobinScheduler
├── data/
│   ├── AppSchema.kt         espelho local das tabelas sincronizadas
│   ├── ApiClient.kt         HTTP para a API, com renovação do token no 401
│   ├── AuthRepository.kt    login Google e a sessão emitida pela API
│   ├── BackendConnector.kt  credenciais e fila de upload do PowerSync
│   ├── GrupoAtivo.kt        qual grupo está em uso, guardado por conta no aparelho
│   ├── Escopo.kt            observarNoGrupo(): refaz a query quando o grupo muda
│   ├── MeusGruposRepository.kt  entrar com chave, criar grupo, chaves e membros
│   ├── SyncService.kt       liga/desliga o sync conforme a sessão
│   └── *Repository.kt       leitura via watch() e escrita no SQLite local
└── ui/
    ├── theme/ components/   identidade visual
    ├── main/                sessão, papel no grupo, estado do sync
    ├── grupos/              meus grupos, chaves de acesso e membros
    └── games/ standings/ teams/ players/ login/
```

---

## Estrutura do repositório

```
.
├── .railway/
│   └── railway.ts                    serviços, build, cron e variáveis do Railway
├── app/                              módulo Android
├── gradle/libs.versions.toml         catálogo de versões
├── server/                           API (serviços "api" e "avisos" no Railway)
│   ├── src/                          rotas, sessão, upload do sync, arquivos e lembretes
│   ├── migrations/                   schema do banco, aplicado no start da API
│   ├── scripts/migrar-do-supabase.ts cópia única dos dados do Supabase
│   ├── seed.sql                      zera os dados do Unidos e recria 9 times e 38 jogadores
│   └── Dockerfile
├── powersync/
│   ├── service.yaml                  configuração do PowerSync self-hosted
│   ├── sync-rules.yaml               o que cada dispositivo baixa
│   └── Dockerfile                    a imagem oficial com os dois arquivos dentro
├── web/                              aplicação web (React + Vite)
│   ├── src/domain/                   porte em TypeScript dos algoritmos e dos testes
│   ├── src/data/                     consultas e escritas sobre o SQLite do PowerSync
│   ├── src/lib/                      API, sessão, login Google, PowerSync e variáveis
│   ├── src/ui/                       telas e componentes
│   ├── Caddyfile                     servidor do site no Railway
│   └── Dockerfile
├── docker-compose.yaml               Postgres e PowerSync para rodar tudo local
├── local.properties.example          modelo das chaves do app Android
└── keystore.properties.example       modelo das senhas do keystore de release
```

---

## Pré-requisitos

- **Android Studio** (versão recente, com AGP 9 suportado) — é o caminho principal.
- **JDK 17 ou mais novo.** O JDK embutido no Android Studio (JBR) serve.
- **Um emulador com Google Play** ou um dispositivo físico com Play Services.
  O login Google usa o Credential Manager e **não funciona em imagem de emulador
  sem Google Play** — ao criar o AVD, escolha uma imagem com o ícone da Play Store.
- **Node.js 24**, para a API em [`server/`](server/) e a web em [`web/`](web/).
- **Docker Desktop**, para o Postgres e o PowerSync locais.
- Conta no [Railway](https://railway.com) e a CLI dele: `npm i -g @railway/cli`.

---

## Backend no Railway

### O que roda no Railway

Um projeto `unidos-pelo-volei`, só com o ambiente `production`, e cinco serviços:

| Serviço | Pasta | O que é | Domínio público |
|---|---|---|---|
| `Postgres` | — | Banco, template oficial do Railway | não |
| `api` | [`server/`](server/) | A API Node | sim, porta 8080 |
| `powersync` | [`powersync/`](powersync/) | PowerSync self-hosted | sim, porta 8080 |
| `web` | [`web/`](web/) | Site e PWA, servidos por Caddy | sim, porta 8080 |
| `avisos` | [`server/`](server/) | Cron dos lembretes: a imagem da API com outro comando | não |

**Todo push na `main` é deploy em produção**, automático. Cada serviço só redeploya
quando a pasta dele muda, então mexer na web não reinicia a API. Não existe ambiente
de dev na nuvem: tudo se testa na máquina, com a
[stack local](#backend-local), antes de chegar à `main`.

### A infraestrutura está no repositório

[`.railway/railway.ts`](.railway/railway.ts) declara serviços, origem no GitHub,
Dockerfile, comando de start, healthcheck, política de restart, cron e variáveis. Ele
é aplicado pela CLI, não pelo push:

```bash
cd .railway
npm install
railway login
railway link            # escolha o projeto e o ambiente production
railway config plan     # mostra o que vai mudar, sem mudar
railway config apply
```

Mudou o arquivo, repita o `plan` e o `apply`. O código continua indo pelo
`git push`.

Duas coisas ficam fora do arquivo e são feitas no painel: os **domínios**
`*.up.railway.app`, que o Railway não guarda no código, e os **segredos**, que não
podem ir para o Git. Os segredos são variáveis compartilhadas, em
**Project Settings → Shared Variables**:

| Variável compartilhada | Valor | Quem usa |
|---|---|---|
| `JWT_SECRET` | 32 bytes aleatórios em base64url, **diferente do da sua máquina** | `api`, `avisos`, `powersync` |
| `GOOGLE_WEB_CLIENT_ID` | O Web client ID do OAuth | `api`, `web` |
| `VAPID_PUBLIC_KEY` e `VAPID_PRIVATE_KEY` | Par de chaves do Web Push (opcional) | `api`, `avisos`, `web` |
| `FIREBASE_SERVICE_ACCOUNT` | O JSON inteiro da conta de serviço do Firebase (opcional) | `avisos` |

Gere o `JWT_SECRET` com:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('base64url'))"
```

Os serviços leem tudo por referência (`${{shared.JWT_SECRET}}`,
`${{Postgres.DATABASE_URL}}`, `https://${{api.RAILWAY_PUBLIC_DOMAIN}}`), então trocar um
valor é mudar num lugar só e redeployar quem usa.

### Subindo do zero

1. No Railway, **New Project → Empty Project**, com o nome `unidos-pelo-volei`, e
   conecte a conta do GitHub em **Account Settings → Integrations** se ainda não
   estiver.
2. Cadastre as variáveis compartilhadas.
3. `railway link` no ambiente `production` e `railway config apply`. Os cinco
   serviços aparecem e começam o primeiro deploy.
4. Em cada um dos serviços `api`, `powersync` e `web`: **Settings → Networking →
   Generate Domain**, porta **8080**. Depois disso, **redeploy o `web`**: ele embute
   os endereços da API e do PowerSync no build, e no primeiro deploy eles ainda não
   existiam.
5. O primeiro start da API aplica as migrations, cria o banco `powersync_storage` e
   liga `wal_level = logical` no Postgres. Essa última só vale depois de reiniciar o
   banco: o log da API avisa. Faça **Restart** no serviço `Postgres` uma vez e depois
   **Redeploy** no `powersync`.
6. Confira: `https://<api>/saude` responde `{"ok":true}`, o log do `powersync` mostra
   `Replicating`, e o site abre na tela de login.

### Migrations

As migrations ficam em [`server/migrations/`](server/migrations/) e rodam sozinhas no
start da API, em ordem de nome, cada uma numa transação, com o registro do que já
rodou em `public.migracoes`. Não há passo manual: mudar o schema é criar um arquivo
novo com o timestamp na frente, reiniciar a API local (ela aplica na hora), conferir,
e dar push na `main` — o deploy da API aplica em produção.

A primeira, `20260831110000_compatibilidade.sql`, existe porque o banco nasceu no
Supabase. Ela recria o pouco que as migrations antigas esperavam encontrar pronto: o
schema `auth` com `auth.users`, `auth.uid()` e `auth.jwt()`; os papéis `anon`,
`authenticated` e `service_role`; e o schema `storage` com `buckets` e `objects`.
Com isso as migrations antigas rodam **sem nenhuma alteração** num Postgres comum, e
toda a RLS escrita com `auth.uid()` e `to authenticated` continua valendo. O
`auth.uid()` lê o `sub` de `request.jwt.claims`, que é o que a API preenche em cada
transação. A última, `20261125120000_sessoes_proprias.sql`, acrescenta
`auth.users.google_sub` e a tabela `auth.sessoes` dos refresh tokens.

> A migration `..._grupos.sql` é a que torna o app multi-grupo. Ela cria
> `grupos`, `grupo_membros` e `grupo_chaves`, adiciona `grupo_id` em todas as tabelas
> de conteúdo, move o papel de `profiles` para `grupo_membros` e reescreve a RLS
> inteira. Num banco que já existia, tudo o que estava lá vira o grupo
> **Unidos Pelo Vôlei** (id fixo `00000000-0000-4000-8000-000000000001`) e todo mundo
> que já tinha conta entra nele com o papel que tinha. A primeira chave de acesso do
> grupo é criada junto. Ela também conserta um trigger antigo: `posts` chamava o
> `touch_updated_at()`, que escreve em `updated_at`, coluna que `posts` não tem — o
> app nunca editou post, então o erro nunca apareceu. Agora o trigger é o
> `touch_atualizado_em()`, com o nome de coluna certo. Pegue o código da chave com:
>
> ```sql
> select codigo from public.grupo_chaves
> where grupo_id = '00000000-0000-4000-8000-000000000001';
> ```
>
> Virar membro é só a porta: a pessoa ainda aparece sem ficha até a diretoria ligar
> o perfil a um jogador, no pedido de vínculo.

O que cada migration faz:

- **init** — tabelas `profiles`, `players`, `teams`, `team_players`, `rounds` e
  `matches`, com `updated_at` automático e criação do profile no primeiro login.
- **standings_view** — a VIEW `public.standings` com V, S e PP calculados a partir
  das partidas finalizadas, ordenada por vitórias, saldo e pontos pró.
- **rls** — SELECT liberado para qualquer autenticado; INSERT, UPDATE e DELETE só
  para quem tem `profiles.is_admin = true`.
- **powersync** — a `publication` de replicação lógica que o PowerSync exige.
- **genero_dias_desempenho** — a coluna `players.genero`, as tabelas `game_days` e
  `player_day_stats` (o que o "Encerrar dia" arquiva), a VIEW
  `public.player_performance` e a `publication` recriada com as tabelas novas.
- **presenca_do_dia** — torna `player_day_stats.team_id` opcional, para o "Encerrar
  dia" também guardar quem estava presente sem ter entrado em nenhum time.
- **identidade** — `profiles.papel`, `players.profile_id`, `vinculo_pedidos`,
  `player_contatos` e os triggers que aplicam o vínculo e protegem a ficha.
- **presenca_e_avisos** — `presencas`, `config_grupo`, `dispositivos` e `avisos`.
- **mural_agenda_regras** — `posts`, `post_reacoes`, `eventos` e `paginas`.
- **financeiro** — `players.regime`, `config_financeiro`, `cobrancas` e `pagamentos`.
- **avaliacao** — `avaliacoes` (só insert), `avaliacao_registros`,
  `player_evolucao` e `dicas`.
- **tipo_da_chave_pix** — `config_financeiro.pix_tipo`, que diz se a chave é CPF,
  CNPJ, celular, e-mail ou aleatória, para o BR Code sair no formato que o banco
  reconhece.
- **notas_por_fundamento** — troca a estrela única por `players.nota_saque` até
  `players.nota_atitude` mais a `nota_media` derivada, cria
  `player_nota_historico` e a função `recalcular_nota`, que move a nota dia a dia
  com as avaliações dos colegas.
- **areia_quarteto_vinculo_e_mural** — derruba `players.posicao`; deixa o atleta
  escolher o próprio regime, menos a isenção; reduz os tipos de evento a
  `jogo`/`confraternizacao`/`campeonato` (migrando `amistoso` e `outro`); acrescenta
  `posts.imagem_url` e `posts.emoji`; cria o bucket `mural` no Storage com as
  policies de leitura pública e escrita da diretoria; e reescreve a página de regras
  para o vôlei de areia em quarteto.
- **regras_do_volei_unidos** — troca a página de regras pelas regras como o grupo
  realmente joga: set único até 15, bloqueio **não** contando como toque e a regra da
  bola no teto e na corda de aço do ginásio.
- **compatibilidade** — schemas `auth` e `storage`, papéis e `auth.uid()`, para as
  migrations do tempo do Supabase rodarem num Postgres comum.
- **sessoes_proprias** — `auth.users.google_sub` e `auth.sessoes`, a sessão emitida
  pela API.

**Dados de exemplo.** O [`server/seed.sql`](server/seed.sql) apaga os dados do grupo
Unidos e recria 9 times e 38 jogadores — rode só em banco de teste. Na stack local:

```bash
docker compose exec -T postgres psql -U postgres < server/seed.sql
```

Nunca rode o seed em produção.

### Sync rules

As sync rules ficam em [`powersync/sync-rules.yaml`](powersync/sync-rules.yaml) e vão
dentro da imagem do serviço `powersync`, junto com o
[`service.yaml`](powersync/service.yaml). **Publicar sync rules é dar push**: não
existe mais painel do PowerSync. Quando uma migration muda colunas de tabela
sincronizada, a migration e as sync rules sobem no mesmo commit; o PowerSync
reprocessa sozinho quando a configuração muda.

As sync rules mandam para o dispositivo o campeonato inteiro (jogadores, times,
rodadas e partidas) **dos grupos em que a pessoa é membro**, mais a lista desses
grupos, e do `profiles` apenas a linha do próprio usuário. Todo stream de conteúdo
tem a mesma cláusula:

```sql
WHERE grupo_id IN (SELECT grupo_id FROM grupo_membros WHERE profile_id = auth.user_id())
```

Isso usa subconsulta em stream, disponível na **edition 3** das sync rules — o
`config.edition: 3` no topo do arquivo não é opcional. Quem participa de dois grupos
baixa os dois e troca entre eles offline; `grupo_chaves` fica de fora de propósito,
para os códigos de convite não descerem para o aparelho de ninguém.


O PowerSync guarda o estado dele no banco `powersync_storage`, no mesmo Postgres, em
vez de exigir um MongoDB. A replicação usa a `publication powersync` que as migrations
criam.

### Login Google

No [Google Cloud Console](https://console.cloud.google.com), em **APIs & Services →
Credentials**:

**a) Web client ID** — é o `GOOGLE_WEB_CLIENT_ID`. Tanto o Android quanto a web pedem o
token com ele, e a API só aceita token cuja audiência seja ele.

- Em **Authorized JavaScript origins**, cadastre o endereço do site
  (`https://<web>.up.railway.app`) e `http://localhost:5173` para desenvolvimento. É o que libera o botão do Google na
  web.
- **Redirect URIs e client secret não são mais usados.** A web recebe o token direto
  do Google Identity Services, sem redirecionamento, e ninguém troca código por token.

**b) Android client IDs** — um por `applicationId` e SHA-1, só para o Google
reconhecer o APK que está pedindo a credencial. Nada deles vai para o código.

1. **Create Credentials → OAuth client ID → Android**.
2. **Package name**: `com.unidospelovolei.dev` para a variant de dev e
   `com.unidospelovolei` para a de prod.
3. **SHA-1**: o do keystore que assina aquele build. Pegue com

   ```bash
   ./gradlew signingReport
   ```

   ou, para o keystore de debug,

   ```bash
   keytool -list -v -alias androiddebugkey \
     -keystore ~/.android/debug.keystore -storepass android -keypass android
   ```

   No Windows, o keystore fica em `%USERPROFILE%\.android\debug.keystore`.

> O app instalado pela loja é assinado pela chave do **Play App Signing**, não pela
> sua. Cadastre também o SHA-1 que aparece em **Test and release → Setup → App
> signing** no Play Console, senão o login falha só para quem instalou pela loja.

O app manda ao Google o hash SHA-256 de um nonce aleatório e manda à API o valor
original; a API confere que o token traz aquele hash. Isso impede reaproveitar um
token de login capturado.

### Trazendo os dados do Supabase

A cópia é feita uma vez, no dia da troca, pelo
[`server/scripts/migrar-do-supabase.ts`](server/scripts/migrar-do-supabase.ts). Ele
copia `auth.users` (com o `sub` do Google tirado de `auth.identities`, para a mesma
conta continuar sendo a mesma pessoa), todas as tabelas de `public`, e as imagens dos
buckets `mural` e `grupos`; no fim, reescreve as URLs das imagens para a API nova.

1. Faça o primeiro deploy da API no Railway, para as migrations existirem.
2. Ligue **Public Access** no `Postgres` do Railway e copie a `DATABASE_PUBLIC_URL`.
3. No Supabase, botão **Connect**, copie a connection string da **Session pooler**
   (IPv4) com a senha do banco.
4. Rode sem `--confirmar` para ver a contagem do que vai ser copiado:

   ```bash
   cd server
   npm install
   npm run migrar-do-supabase -- \
     --origem "postgresql://postgres.XXXX:SENHA@aws-0-sa-east-1.pooler.supabase.com:5432/postgres" \
     --destino "COLE_AQUI_A_DATABASE_PUBLIC_URL" \
     --supabase-url https://XXXX.supabase.co \
     --api-url https://<api>.up.railway.app
   ```

5. Repita com `--confirmar` no fim. Isso **apaga** o que houver nas tabelas do
   destino e copia tudo de novo, numa transação só: se der erro no meio, nada fica
   pela metade.
6. Desligue o **Public Access** do `Postgres`.

O script desliga os triggers durante a cópia (`session_replication_role = replica`),
para nada ser recalculado nem carimbado com a data de hoje. Ninguém precisa entrar de
novo no grupo nem refazer vínculo: os ids de conta, perfil e jogador são os mesmos.
Cada pessoa só faz login uma vez na versão nova, porque a sessão antiga era do
Supabase.

### Backups

O Railway faz backup do volume do Postgres: aba **Backups** do serviço `Postgres`,
com agenda diária, semanal ou mensal. Ligue a diária. As
imagens do mural e as logos estão dentro do banco (ver
[Mural, agenda e regras](#mural-agenda-e-regras)), então o mesmo backup cobre tudo.

---

## Chaves em local.properties

Copie `local.properties.example` para `local.properties` e preencha. **Esse arquivo
nunca vai para o Git** (já está no `.gitignore`) e nenhuma chave fica hardcoded no
código: elas entram via `BuildConfig`, geradas em [`app/build.gradle.kts`](app/build.gradle.kts).

```properties
sdk.dir=C:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk

dev.API_URL=http://10.0.2.2:3000
dev.POWERSYNC_URL=http://10.0.2.2:8080
dev.GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com

prod.API_URL=https://<api-prod>.up.railway.app
prod.POWERSYNC_URL=https://<powersync-prod>.up.railway.app
prod.GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com
```

As chaves `dev.*` apontam para a [stack local](#backend-local) — `10.0.2.2` é como o
emulador enxerga a sua máquina — e as `prod.*` para os domínios gerados no Railway
para os serviços `api` e `powersync`, com `https://` e sem barra no fim. O `GOOGLE_WEB_CLIENT_ID` é o mesmo
valor da variável compartilhada de mesmo nome.

O prefixo casa com a build variant: `dev.*` alimenta `devDebug`/`devRelease` e
`prod.*` alimenta `prodDebug`/`prodRelease`. Em CI as mesmas chaves podem vir de
variáveis de ambiente (`DEV_API_URL`, `PROD_API_URL`, ...).

Se faltar alguma chave, o app abre numa tela dizendo exatamente qual, em vez de
quebrar.

---

## Assinatura do release

A Play Store só aceita um app assinado com um keystore seu. As senhas desse keystore
seguem o mesmo padrão das chaves de ambiente: ficam em um arquivo fora do Git.

Copie [`keystore.properties.example`](keystore.properties.example) para
`keystore.properties` e preencha:

```properties
storeFile=C\:/Users/SEU_USUARIO/chaves/upload-keystore.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

O keystore em si é gerado uma única vez, **fora da pasta do repositório**. O
`keytool` não cria o diretório, então a pasta precisa existir antes:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\chaves"

keytool -genkeypair -v -keystore "$env:USERPROFILE\chaves\upload-keystore.jks" -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Quando ele pedir a **senha da chave para `upload`**, aperte Enter para reusar a do
keystore: o formato padrão (PKCS12) não suporta senhas diferentes, e digitar outra
faz o keytool ignorá-la com um aviso — o `keystore.properties` ficaria com uma senha
que não existe. Os dois campos de senha recebem o mesmo valor.

> **Faça backup do arquivo e das senhas.** Perder qualquer um dos dois impede
> publicar atualizações do app — a saída é abrir um pedido de reset da chave de
> upload no suporte do Google.

Enquanto `keystore.properties` não existir, o build type `release` continua saindo
**sem assinatura**: [`app/build.gradle.kts`](app/build.gradle.kts) só cria a
`signingConfig` quando encontra o keystore apontado. Isso mantém
`./gradlew assembleDevRelease` funcionando em qualquer máquina, inclusive em um
clone recém-feito. Confira qual configuração cada variant pegou com:

```bash
./gradlew signingReport
```

O `SHA1` que aparece na variant `prodRelease` é o que vai no OAuth client Android do
Google Cloud, senão o login com Google falha no build assinado. O passo a passo de
envio está em [Publicando uma versão na Play Store](#publicando-uma-versão-na-play-store).

Para gerar o arquivo que sobe para a loja:

```bash
./gradlew bundleProdRelease
```

O `.aab` sai em `app/build/outputs/bundle/prodRelease/`.

> **O SHA-1 que vale na loja não é o seu.** A Play Store reassina o bundle com uma
> chave própria (Play App Signing), então o APK instalado pelas pessoas tem outro
> SHA-1. Depois do primeiro upload, pegue os dois em **Test and release → Setup →
> App signing** no Play Console e cadastre ambos no mesmo OAuth client Android.

O `versionCode` em [`app/build.gradle.kts`](app/build.gradle.kts) precisa subir a
cada arquivo enviado à loja, mesmo em reenvio de correção.

---

## Publicando uma versão na Play Store

O roteiro de toda atualização, do código ao celular das pessoas:

1. **Suba o número da versão** em [`app/build.gradle.kts`](app/build.gradle.kts):
   `versionCode` aumenta de 1 em 1 a cada arquivo enviado — mesmo num reenvio de
   correção, a loja recusa um número repetido — e `versionName` é o texto que as
   pessoas veem (`2.0.0`, `2.0.1`...).
2. **Confira o `local.properties`**: as chaves `prod.*` apontam para os serviços do
   Railway, e o `keystore.properties` existe (ver
   [Assinatura do release](#assinatura-do-release)).
3. **Gere o bundle**:

   ```bash
   ./gradlew bundleProdRelease
   ```

   No Windows, `gradlew.bat bundleProdRelease`. O arquivo sai em
   `app/build/outputs/bundle/prodRelease/app-prod-release.aab`. No Android Studio o
   mesmo é **Build → Generate Signed App Bundle / APK**, escolhendo a variant
   `prodRelease`.
4. **No [Play Console](https://play.google.com/console)**, abra o app e vá em
   **Test and release → Production** (ou **Testing → Internal testing**, para testar
   antes com um grupo pequeno) → **Create new release**.
5. **Arraste o `.aab`**, escreva as notas da versão em português (o que a pessoa vai
   notar de diferente) e clique em **Next**.
6. **Revise os avisos** da página de resumo. Avisos amarelos não impedem a
   publicação; erros vermelhos impedem. Clique em **Save** e depois em
   **Send for review** (ou **Start rollout to Production**, conforme a tela).
7. **Espere a revisão do Google** — costuma levar de algumas horas a poucos dias. O
   status aparece em **Publishing overview**. Se o app usa **Managed publishing**
   (ligado em **Publishing overview**), a versão aprovada fica parada esperando você
   clicar em **Publish**; é o jeito de escolher o dia exato em que ela chega às
   pessoas.

Depois de publicada, a atualização chega sozinha para quem tem atualização
automática; os outros veem o botão **Atualizar** na página do app na loja.

---

## Rodando localmente para teste

A separação de ambientes é por **build variant**: trocar de ambiente é só trocar a
variant selecionada, sem mexer no código. O Android Studio seleciona **devDebug**
por padrão, então o build de debug aponta para a stack local — **as chaves de
produção nunca entram num build de debug**, porque `debug` só existe combinado com um flavor e o
flavor é quem carrega as chaves.

| Variant | applicationId | Chaves |
|---|---|---|
| `devDebug` (padrão) | `com.unidospelovolei.dev` | `dev.*` |
| `devRelease` | `com.unidospelovolei.dev` | `dev.*` |
| `prodDebug` | `com.unidospelovolei` | `prod.*` |
| `prodRelease` | `com.unidospelovolei` | `prod.*` |

Como o `applicationId` de dev tem sufixo `.dev`, dá para ter os dois apps instalados
lado a lado no mesmo aparelho.

### Rodar no Android Studio

Um editor sozinho não executa um app Android: é preciso um emulador ou um aparelho.

1. **File → Open** e escolha a pasta do repositório. Espere o *Gradle sync*.
2. Suba o [backend local](#backend-local), preencha o `local.properties` com as
   chaves `dev.*` (ver [Chaves em local.properties](#chaves-em-localproperties)) e rode
   **File → Sync Project with Gradle Files**.
3. Crie ou escolha um dispositivo:
   - **Emulador**: **Tools → Device Manager → Add a new device**. Escolha uma imagem
     de sistema **com Google Play** (ícone da Play Store) — sem isso o login Google
     não funciona.
   - **Aparelho físico**: ative *Opções do desenvolvedor* e *Depuração USB*, conecte
     por USB e autorize o computador.
4. Em **Build → Select Build Variant**, confirme que o módulo `app` está em
   **devDebug**.
5. Clique em **Run ▶**.

Na primeira execução o banco local está vazio: entre com o Google e use **Criar um
grupo**, que já te coloca como diretoria, ou rode o `seed.sql` e entre no grupo
Unidos com a chave dele (ver [Migrations](#migrations)).

Para testar o **offline**, ative o modo avião no dispositivo. O app continua abrindo,
navegando e registrando placar; ao voltar a internet, o indicador volta para
**Online** e as alterações sobem sozinhas.

Para testar o **multiusuário em tempo real**, rode o app em dois dispositivos com
contas diferentes (ambas admin) e marque pontos em partidas diferentes: cada um vê
o placar do outro aparecer.

### Backend local

É assim que se testa qualquer mudança antes de ir para a `main`. O
[`docker-compose.yaml`](docker-compose.yaml) da raiz sobe o Postgres (já com
`wal_level=logical`) e o PowerSync com a mesma imagem do Railway. A API roda direto no
Node, com recarga automática a cada alteração:

```bash
docker compose up -d --build

cd server
cp .env.example .env      # preencha GOOGLE_CLIENT_IDS com o seu Web client ID
npm install
npm run dev
```

O primeiro start da API aplica as migrations e cria o banco do PowerSync. O PowerSync
fica reiniciando até esse banco existir; depois disso ele se acerta sozinho. O
`JWT_SECRET` do `.env.example` é o mesmo do `docker-compose.yaml` e serve só para a
máquina local.

> A API fica fora do compose de propósito: em máquinas com antivírus que inspeciona
> HTTPS, o `npm ci` dentro do Docker falha com erro de certificado. No Railway isso
> não acontece.

`localhost`, dentro de um emulador ou de um celular, é o próprio dispositivo — não a
sua máquina. Use:

| Onde o app roda | API (3000) | PowerSync (8080) |
|---|---|---|
| Emulador Android | `http://10.0.2.2:3000` | `http://10.0.2.2:8080` |
| Aparelho físico na mesma rede | `http://192.168.x.y:3000` | `http://192.168.x.y:8080` |

Descubra o IP da máquina com `ipconfig` (Windows) ou `ip addr` (Linux/macOS).

No `local.properties`:

```properties
dev.API_URL=http://10.0.2.2:3000
dev.POWERSYNC_URL=http://10.0.2.2:8080
dev.GOOGLE_WEB_CLIENT_ID=<o mesmo Web client ID>
```

Esses endereços são HTTP puro, e o Android bloqueia tráfego em texto claro por
padrão. Por isso o projeto traz um
[`app/src/debug/AndroidManifest.xml`](app/src/debug/AndroidManifest.xml) que libera
cleartext **apenas nos builds de debug** — nenhum build de release recebe essa flag.

Com aparelho físico, a URL pública das imagens precisa ser alcançável por ele: ajuste
`URL_PUBLICA` no `server/.env` para o IP da máquina.

### Alternativa por linha de comando

Para quem prefere terminal ou VS Code — o Android Studio continua sendo o caminho
principal, porque compilar não é o mesmo que executar: ainda é preciso um emulador
ou aparelho conectado.

```bash
# Compilar e instalar no dispositivo conectado
./gradlew installDevDebug

# Só o APK, sem instalar (sai em app/build/outputs/apk/dev/debug/)
./gradlew assembleDevDebug

# Testes
./gradlew testDevDebugUnitTest

# Dispositivos visíveis ao adb
adb devices

# Abrir o app sem passar pelo Studio
adb shell am start -n com.unidospelovolei.dev/com.unidospelovolei.ui.MainActivity

# Acompanhar o log do app
adb logcat --pid=$(adb shell pidof -s com.unidospelovolei.dev)
```

Emuladores podem ser iniciados sem o Studio com
`$ANDROID_HOME/emulator/emulator -list-avds` e `emulator -avd NOME_DO_AVD`.

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

---

## Aplicação web

A pasta [`web/`](web/) traz o mesmo app rodando no navegador, para quem usa iPhone e
não tem a versão da Play Store. É um **PWA**: dá para instalar na tela de início e
continuar funcionando com a rede oscilando, porque o PowerSync mantém um SQLite local
no navegador (WebAssembly), igual ao do Android.

O que muda em relação ao app Android:

| | Android | Web |
|---|---|---|
| Login | Credential Manager (token nativo) | Botão do Google Identity Services, também token direto |
| Chaves | `local.properties`, por flavor | `.env.development` / `.env.production`; no Railway, variáveis do serviço `web` |
| Banco local | SQLite nativo | SQLite em WebAssembly, no armazenamento do navegador |
| Distribuição | `.aab` na Play Store | serviço `web` no Railway |

O que **não** muda: a API, o schema, as policies de RLS, as sync rules e as regras dos
dois algoritmos. Os algoritmos foram portados para TypeScript junto com os testes, que
rodam com as mesmas sementes do `SchedulingTest.kt` — inclusive as asserções de valor
exato, porque o gerador aleatório do Kotlin foi reproduzido em
[`web/src/domain/random.ts`](web/src/domain/random.ts).

### Rodando a web localmente

```bash
cd web
npm install
cp .env.example .env.development   # preencha as chaves de dev
npm run dev
```

```properties
VITE_API_URL=http://localhost:3000
VITE_POWERSYNC_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=...apps.googleusercontent.com
```

Essas URLs são a [stack local](#backend-local). O botão do Google só aparece se
`http://localhost:5173` estiver nas **Authorized JavaScript origins** do Web client.

Comandos disponíveis:

```bash
npm run dev         # servidor de desenvolvimento
npm test            # testes dos algoritmos (Vitest)
npm run typecheck   # checagem de tipos
npm run build       # gera dist/ para publicar
npm run preview     # serve o dist/ localmente
```

### Publicando a web

É o serviço `web` do Railway: um push na `main` publica em produção. O [`Dockerfile`](web/Dockerfile) faz o `npm run build` e
entrega o `dist/` para um Caddy, configurado no [`Caddyfile`](web/Caddyfile) com o
fallback de SPA, cache longo para `assets/` e nenhum cache para `index.html` e
`sw.js` — senão a atualização do PWA demoraria a chegar.

O Vite lê as variáveis **no momento do build** e as embute no bundle, do mesmo jeito
que o Gradle embute as chaves no `.aab`. No Railway elas são variáveis do serviço,
declaradas como `ARG` no Dockerfile; trocar uma exige um redeploy do `web`, não só
salvar a variável.

### Páginas públicas

A landing, a política de privacidade e a página de exclusão de conta são HTML
estático em `web/public/`. Saem no mesmo deploy da web, sem build próprio, e ficam
na mesma origem do app:

| Página | Caminho publicado | Onde é exigida |
| --- | --- | --- |
| Landing | `/sobre/` | *App Homepage* na tela de consentimento do Google |
| Privacidade | `/privacidade/` | ficha da Play Store e tela de consentimento |
| Exclusão de conta | `/exclusao/` | formulário de Segurança dos Dados da Play Store |

A raiz `/` é o app, que abre na tela de login — por isso a landing mora em `/sobre/`
e não na raiz: o Google exige que a homepage seja legível sem autenticação.

As três ficam fora do precache do service worker, pela `globIgnores` em
[`web/vite.config.ts`](web/vite.config.ts). Sem isso elas entrariam no pacote que
todo mundo baixa na primeira visita, e um ajuste no texto da política só chegaria
às pessoas no próximo update do worker.

Nenhuma dessas URLs aparece no código do app Android: elas vivem só em campos de
formulário do Play Console e do Google Cloud. Mudar o endereço delas é edição de
configuração, não release nova.

---

## As cinco abas

O app abre no **SOCIAL**, no mural: é a primeira coisa que todo mundo vê, e é onde a
diretoria fala com o grupo. As outras quatro são **JOGOS** (chaveamento e placar),
**CLASSIFICAÇÃO**, **TIMES** e **EU**. As cinco mostram sempre o **grupo do
cabeçalho** — tocar no nome dele abre a troca de grupo.

A aba **EU** ganha uma **bolinha verde** no ícone quando tem companheiro de time
esperando a sua nota. É o único aviso desse tipo no app, e ele some sozinho quando a
fila de avaliações zera.

### A aba EU é um índice, não uma página

A EU não é mais uma pilha de cartões. Ela abre com o **cartão de presença do próximo
sábado** no topo e, embaixo, uma **grade de dois blocos por linha**, cada um com
ícone, nome e uma linha dizendo o que tem lá dentro:

| Bloco | O que abre |
|---|---|
| **Grupos** | A lista dos seus grupos, para trocar, entrar com chave, criar ou sair |
| **Detalhes da conta** | A ficha: nome, como paga, aniversário, telefone e desde quando está no grupo |
| **Financeiro** | O seu extrato, o que está em aberto e o Pix Copia e Cola |
| **Histórico** | Sábados, jogos, vitórias e saldo, mais as seis notas e o gráfico da evolução |
| **Avaliar colegas** | A fila de companheiros esperando a sua nota, com selo vermelho quando tem alguém |
| **Configurações** | Só aparece para a diretoria |

Quem ainda não foi ligado a um jogador vê o fluxo de identificação acima da grade e os
quatro blocos que dependem de ficha ficam apagados, com uma linha explicando o porquê.
**Grupos** e **Configurações** continuam funcionando, porque nenhum dos dois depende de
ter jogador.

### O botão Voltar do aparelho

O app não usa biblioteca de navegação: o destino é estado de composable no Android e
estado de componente na web. Sem nada interceptando, o Voltar do aparelho fechava o
app inteiro de dentro de qualquer subtela.

Cada plataforma resolve do seu jeito, mas as duas obedecem ao mesmo mapa de pais:
chaves, membros, pedidos de vínculo, contas e jogadores e o painel financeiro voltam
para **Configurações**; todo o resto volta para as abas; e estando nas abas fora do
SOCIAL, o Voltar leva para o SOCIAL antes de deixar sair.

- No Android é um `BackHandler`, ligado só enquanto existe para onde voltar. Quando
  não existe, ele fica desligado e o sistema fecha o app como sempre. Diálogos não
  precisam de nada: o `AlertDialog` do Compose já fecha sozinho no Voltar.
- Na web o mesmo mapa roda sobre o History API. Ao entrar na primeira subtela o app
  empilha uma entrada de colchão, e o `popstate` consome essa entrada para navegar
  para trás dentro do app. Na raiz não há colchão, então o Voltar sai de verdade. Os
  botões de voltar da própria interface chamam `history.back()` em vez de trocar o
  estado direto, para a pilha do navegador e a do app nunca saírem de sincronia.

### Configurações, o índice da diretoria

Tudo que só a diretoria faz e não é do dia de jogo passou a morar num lugar só:

- **Pedidos de vínculo** e **Contas e jogadores**, que antes eram um cartão solto na EU.
- **Chaves de acesso**, **Membros do grupo** e **Nome, cidade e logo**, que antes
  ficavam dentro da tela de grupos.
- **Painel financeiro**, que antes era o link "Ver do grupo" dentro do extrato pessoal.
- **Horário e local do sábado**, que antes não tinha tela nenhuma: a coluna existia em
  `config_grupo` e só dava para mudar no banco.

Cada uma dessas ações tem **um caminho só**. O cabeçalho continua trocando de grupo, e
a lista de jogadores e o sorteio continuam na aba TIMES, porque são do dia de jogo, não
da administração.

### O primeiro login

Quem entra pela primeira vez não cai direto nas abas: passa por um **mini tour** de
sete telas — a primeira explica que tudo acontece dentro de um grupo de jogo, as
cinco do meio são uma por aba, e a última é o convite para entrar no grupo.

Terminado o tour, quem ainda não escolheu um grupo cai na tela **Meus grupos**, que
não tem botão de voltar: sem grupo o app não tem o que mostrar. Ali só existem dois
caminhos, **Entrar com uma chave** e **Criar um grupo**. Depois de entrar, o app
segue para a aba **EU**, já na tela de pedir vínculo, e ali tem o recado: *"Não
encontrou seu nome na lista? Entre em contato com a diretoria para adicioná-lo
aqui!"*.

O tour é marcado como visto no armazenamento local do aparelho — `SharedPreferences`
no Android, `localStorage` na web — e não em `profiles`. É preferência de
dispositivo, não dado do grupo: não vale uma coluna no banco nem uma volta no sync, e
reinstalar o app mostrando o tour de novo é comportamento aceitável.

A lista de **Jogadores**, dentro de TIMES, é a lista única do grupo: todo mundo abre
e vê nome, gênero e aniversário de cada um. As **notas por fundamento e a estrela
geral são só da diretoria** nessa lista — o atleta não vê a dos outros. A própria ele
vê, mas na aba **EU**, junto do gráfico de evolução. Também são só da diretoria a
chave **Presente?**, os filtros
**Todos / Presentes / Ausentes**, os botões de presença em massa, o botão de criar e a
edição. Para o atleta a lista é leitura, com um selo indicando quem já tem acesso ao app.

A mesma régua vale no cartão do time e na janela de detalhes dele: a **força do time**
e o **nível de cada jogador do elenco** aparecem só para quem é diretoria.

> As notas continuam descendo para o aparelho de todo mundo pelo sync — a lista de
> jogadores é uma stream só. O que muda é que nenhuma tela do app mostra a nota dos
> outros para o atleta. Se um dia isso precisar virar segredo de verdade, o caminho é
> separar as colunas `nota_*` numa stream de sync exclusiva da diretoria.

## Grupos de jogo e chave de acesso

O app está na Play Store e na web aberta: qualquer pessoa baixa e entra com o Google.
Por isso **a conta sozinha não dá acesso a nada**. O que dá acesso é ser membro de um
**grupo de jogo**, e todo dado do app pertence a um grupo.

`grupos` é a raiz de tudo. `grupo_membros` diz quem participa e com que papel
(`diretoria` ou `atleta`) — uma linha por pessoa **por grupo**, então dá para ser
diretoria no seu grupo e atleta no grupo do amigo. Todas as tabelas de conteúdo
ganharam `grupo_id`: `players`, `teams`, `team_players`, `rounds`, `matches`,
`game_days`, `player_day_stats`, `vinculo_pedidos`, `player_contatos`,
`config_grupo`, `presencas`, `avisos`, `posts`, `post_reacoes`, `eventos`,
`paginas`, `config_financeiro`, `cobrancas`, `pagamentos`, `avaliacoes`,
`avaliacao_registros`, `player_evolucao` e `player_nota_historico`. Ficam de fora só o
que é da pessoa e não do grupo (`profiles`, `dispositivos`) e o conteúdo comum do app
(`dicas`).

### Entrar em um grupo

A entrada é por **chave de acesso**: um código de 8 caracteres, sem letras
ambíguas (nada de O, 0, I ou 1), que a diretoria cria e manda por WhatsApp. Não
existe lista pública de grupos e ninguém descobre um grupo sem a chave.

O app chama a função `entrar_no_grupo(codigo)`, `security definer`, que:

1. normaliza o que a pessoa digitou (`AB12-CD34`, `ab12cd34` e `AB12 CD34` são a
   mesma chave);
2. confere se a chave existe, está ativa, não venceu e não estourou o limite de usos;
3. cria a linha em `grupo_membros` com o papel que a chave concede;
4. incrementa o contador de usos e devolve o id e o nome do grupo.

Quem já é membro pode reusar a chave à vontade: a função devolve o grupo sem gastar
um uso. A tabela `grupo_chaves` **não sincroniza para o dispositivo** — os códigos
vivem só no servidor e a tela de chaves os lê pela API, sob uma policy que só
deixa a diretoria daquele grupo enxergar. É a mesma escolha do painel financeiro:
tela de diretoria, online, e nada de código de convite dormindo no SQLite de todo
mundo.

Cada chave tem **rótulo** (para você saber onde mandou), **limite de usos**,
**validade em dias** e o **papel que concede**. Uma chave de diretoria é o jeito de
trazer um co-organizador sem mexer no banco. Desligar a chave não tira ninguém que
já entrou — o que morre é o convite.

Na web dá para mandar o link pronto: `https://…/?chave=AB12CD34` abre o app já com o
diálogo de entrada preenchido, e a chave some da URL assim que a pessoa entra. No
Android o botão **Convidar** monta a mensagem e abre o compartilhamento do sistema.

### Criar um grupo

`criar_grupo(nome, cidade)` cria o grupo, coloca quem chamou como diretoria e roda o
`preparar_grupo`, que deixa o grupo **usável no primeiro minuto**: os 9 times
coloridos, a configuração de horário, a configuração financeira zerada, as três
páginas de regras e a primeira chave de acesso. Sem isso o grupo novo abriria numa
tela vazia e a pessoa teria que cadastrar time por time antes de entender o app.

Os modelos ficam em `modelos_de_time` e `modelos_de_pagina`, duas tabelas pequenas e
globais — mudar o padrão de todo grupo novo é um `update` nelas, não uma edição de
função. Um limite de 10 grupos criados por conta segura o abuso do botão.

### Nome e logo do grupo

A diretoria muda o nome, a cidade e a **logo** em **EU → Configurações → Nome, cidade e logo do
grupo**. Depois que existe um grupo escolhido, **é a logo dele que fica no cabeçalho**,
no lugar da bola do app, e ela também aparece na lista de grupos e na tela de edição.
Quando o grupo ainda não tem logo, o lugar dela não volta para a bola do app: entra um
círculo com as **iniciais do nome**, o mesmo em todo canto. A bola do app só sobra
onde ainda não há grupo — o login e o tour.

O arquivo vai para o bucket `grupos`, no caminho `<grupo_id>/logo-<uuid>.<ext>`, com
as mesmas policies do mural: leitura pública, escrita só para a diretoria daquele
grupo, checada por `e_diretoria(grupo_do_caminho(name))`. O limite é 2 MB e os
formatos são JPG, PNG e WEBP.

A `grupos` sincroniza, então nome, cidade e `logo_url` são gravados na tabela local e
sobem pelo PowerSync como qualquer outra edição — dá para renomear o grupo sem
internet e a mudança aparece para todo mundo quando a conexão volta. O upload da
imagem, esse sim, precisa de rede, porque vai direto à API.

### Sair, promover e tirar do grupo

Sair é `sair_do_grupo(grupo)`. A única diretoria de um grupo com mais gente **não sai
sem promover alguém antes** — a função recusa com essa mensagem. Se a última pessoa
sair, um trigger passa a diretoria para o membro mais antigo; se não sobrar ninguém,
o grupo é marcado como inativo em vez de virar um grupo órfão com chave funcionando.

Quando alguém sai ou é tirado do grupo, o mesmo trigger solta a ficha do jogador
(`players.profile_id` volta a `NULL`) e limpa o pedido de vínculo pendente. A ficha,
o histórico e as estatísticas continuam no grupo, agora sem dono.

### Como o isolamento é garantido

Em três camadas, e as três precisam concordar:

| Camada | O que faz |
|---|---|
| **Sync rules** | Cada stream filtra por `grupo_id IN (SELECT grupo_id FROM grupo_membros WHERE profile_id = auth.user_id())`. O aparelho só baixa os grupos de quem está logado. |
| **RLS** | `sou_membro(grupo_id)` para ler e `e_diretoria(grupo_id)` para escrever, em toda tabela de grupo. Substituíram o antigo `is_admin()`, que era global. |
| **Consulta local** | Todo `SELECT` do app carrega `WHERE grupo_id = ?` com o **grupo ativo**. Como o SQLite local tem os N grupos da pessoa, sem esse filtro o placar de um grupo apareceria no outro. |

Nas tabelas filhas um trigger `herdar_grupo` preenche e confere o `grupo_id` a partir
do pai (`matches` herda de `rounds`, `presencas` herda de `players`, e assim por
diante). Se um cliente esquecer a coluna, o servidor completa; se mandar o grupo
errado, o servidor recusa. É a rede de segurança da terceira camada.

### O grupo ativo

O grupo escolhido é **preferência de aparelho**, guardada por conta em
`SharedPreferences` no Android e `localStorage` na web — mesma decisão do tour. Não
vale uma coluna no banco: trocar de grupo é instantâneo e offline, e o servidor não
precisa saber em qual grupo você está olhando agora.

No Android um `GrupoAtivo` expõe um `StateFlow` e os repositórios consultam por ele
(`observarNoGrupo` refaz a query quando o grupo muda). Na web um módulo pequeno faz
o mesmo com `useSyncExternalStore`, e `exigirGrupo()` é o que as escritas chamam.
Nenhum ViewModel e nenhuma tela precisou aprender o que é grupo — só o cabeçalho, a
aba **EU** e as telas novas.

O nome do grupo fica no cabeçalho, com uma seta: é ele que evita o pior erro
possível num app multi-grupo, que é digitar o placar do sábado no grupo errado.

## Quem é quem no app

O app tem **dois papéis por grupo**, guardados em `grupo_membros.papel`: `diretoria`
e `atleta`. A diretoria faz tudo que o admin já fazia — jogadores, times, sorteio,
placar — e ganha o financeiro do grupo, o mural, a agenda, as regras, a fila de
aprovações, as chaves de acesso e a lista de membros. O atleta confirma a própria
presença, edita a própria ficha, vê o próprio extrato e o próprio painel de evolução.

O papel é **do grupo, não da conta**. `profiles.papel` e `profiles.is_admin` deixaram
de existir, e com eles a função `is_admin()`: no lugar entraram `sou_membro(grupo)` e
`e_diretoria(grupo)`, que toda policy consulta. O que o app mostra como
`perfil.isAdmin` é o papel no **grupo ativo**, resolvido por um `LEFT JOIN` com
`grupo_membros` na hora de ler o perfil.

Para promover alguém o caminho normal é a tela **EU → Configurações → Membros do grupo**. Se
precisar pelo SQL:

```sql
update public.grupo_membros m
set papel = 'diretoria'
from public.profiles p, public.grupos g
where m.profile_id = p.id
  and m.grupo_id = g.id
  and p.email = 'voce@gmail.com'
  and g.slug = 'unidos-pelo-volei';
```

### Conta e jogador

`profiles` é quem entrou com o Google; `players` é quem joga. A coluna
`players.profile_id` liga os dois, e é ela que faz o app saber que você é você.

O caminho normal é o atleta abrir a aba **EU**, achar o próprio nome na lista de
jogadores ainda sem dono e tocar em **Sou eu**. Isso grava uma linha em
`vinculo_pedidos` com status `pendente`. Alguém da diretoria abre **EU → Configurações → Pedidos de
vínculo** e confirma; um trigger no Postgres preenche `players.profile_id`.

Esse é o padrão que se repete em todo o app: **a escrita vai para uma tabela
sincronizada e o trigger faz o efeito privilegiado**. As escritas dos dois clientes
sobem pelo PowerSync para a API, que aplica tudo sob a RLS, então não há como
chamar função privilegiada de dentro do app — e fazer pela tabela mantém a aprovação
funcionando offline, como o resto.

O caminho da fila não é o único. Em **EU → Configurações → Contas e jogadores** a
diretoria vê a lista inteira de jogadores e liga qualquer um a qualquer conta que já
tenha entrado no app pelo menos uma vez, sem esperar o atleta achar o próprio nome.
O mesmo botão **desvincula**: a conta perde o acesso à ficha, ao extrato e às
avaliações daquele jogador, e o jogador continua no grupo, agora sem dono.

A lista de contas dessa tela vem da API na hora — são os membros do grupo
ativo, lidos de `grupo_membros` com `profiles` embutido —, então ela precisa de
internet; é a mesma escolha já feita para o painel financeiro do grupo. Ela mostra
só quem é do grupo: `profiles` sincroniza apenas o seu próprio perfil, e a policy de
leitura só libera o perfil de quem divide um grupo com você. Vincular manualmente limpa o
pedido pendente daquela conta, para ninguém ficar preso na fila depois de já estar
resolvido.

Se preferir o SQL direto, ele continua valendo:

```sql
update public.players p
set profile_id = f.id
from public.profiles f
where f.email = 'fulano@gmail.com' and p.nome = 'Fulano';
```

O atleta pode editar a própria ficha, mas não tudo: um trigger recusa mudança nas
seis colunas `nota_*`, em `genero`, `ativo`, `entrou_em` e `profile_id` para quem não
é diretoria, e recusa entrar ou sair do regime `isento` — isenção é concessão da
diretoria. `nota_media` e `skill_level` não são checados por serem derivados: outro
trigger os recalcula a partir das seis notas em toda escrita, então mandar um valor
inventado não adianta. Nome, foto, aniversário e a escolha entre **Mensalista** e **Diarista** são
dele.

Não existe mais campo de **posição** na ficha. O grupo joga quarteto na areia, onde
não há rodízio de posição em quadra: levantador, ponteiro e líbero são vocabulário
de sexteto e saíram do app e do banco.

Telefone, contato de emergência e o **ano** de nascimento ficam em
`player_contatos`, tabela à parte que só sincroniza para o dono. O dia e o mês do
aniversário ficam em `players`, porque o grupo inteiro precisa deles para a agenda —
o ano, não.

## Confirmação de presença e lembretes

`players.ativo` continua significando **"está na quadra agora"**, e o Encerrar dia
continua zerando. Quem responde "vou no sábado que vem" grava em `presencas`, tabela
nova, com a data do sábado e a origem da resposta — se foi a própria pessoa ou um
diretor respondendo por ela, porque muita gente avisa pelo WhatsApp.

No sábado de manhã, o botão **Trazer confirmados** liga `ativo` de quem respondeu
`vou` e desliga o resto. Daí para frente o dia segue igual: sorteio, chaveamento,
placar e Encerrar dia não sabem que a chamada existe.

A **hora e o local** que aparecem nesse cartão vivem em `config_grupo` e agora têm
tela: **EU → Configurações → Horário e local do sábado**. A hora é validada no formato
de 24 horas antes de salvar, e o local aceita texto livre. Os dois descem para todo
mundo pelo sync e aparecem no cartão de presença de cada um.

### Ligando o push

O push é **opcional**: sem as chaves, o app funciona igual e só não registra o
aparelho. Para ligar:

1. No [Firebase](https://console.firebase.google.com), use (ou crie) o projeto e
   adicione um app Android para cada package name (`com.unidospelovolei.dev` e
   `com.unidospelovolei`). Dá para escolher o mesmo projeto do Google Cloud do OAuth
   na hora de criar, e aí fica tudo num lugar.
2. Copie **Project ID**, **App ID**, **API key** e **Sender ID** para o
   `local.properties`, como mostra o `local.properties.example`. Não é preciso
   `google-services.json`: o app monta o `FirebaseOptions` na mão a partir dessas
   quatro chaves, seguindo o mesmo padrão das outras credenciais do projeto.
3. Em **Configurações do projeto → Contas de serviço → Gerar nova chave privada**,
   baixe o JSON e cole o conteúdo inteiro na variável compartilhada
   `FIREBASE_SERVICE_ACCOUNT` do Railway.
4. Gere um par de chaves VAPID para o Web Push (`npx web-push generate-vapid-keys`) e
   cadastre as duas como variáveis compartilhadas `VAPID_PUBLIC_KEY` e
   `VAPID_PRIVATE_KEY`. A pública também vai no build da web, por referência; a
   privada só os serviços do servidor leem.

Quem dispara é o serviço `avisos`: a mesma imagem da API rodando
[`server/src/avisos.ts`](server/src/avisos.ts) como cron, com a agenda
`0 11,22 * * 5,6`. Os horários do Railway são em **UTC**: 22h de sexta é 19h em
Brasília e 11h de sábado é 8h. O script olha o dia e a hora em que acordou e decide —
sexta 22h manda o convite para todo mundo, sábado 11h cobra só quem ainda não
respondeu, e nas outras duas execuções da agenda ele sai sem fazer nada.

Para disparar fora da agenda no Railway, use `railway cron run` com o serviço
`avisos` selecionado — lembrando que ele só envia algo se for sexta 22h ou sábado 11h
UTC. Para testar o envio em qualquer dia, rode localmente contra a stack local, com o
tipo forçado no argumento:

```bash
cd server
npm run build
node --env-file=.env dist/avisos.js convite
```

> **No iPhone o push só chega se a pessoa instalar o site na tela de início.** É
> limitação da Apple: o Safari só entrega Web Push a partir do iOS 16.4 e apenas
> para PWA instalado. Como o público da web é justamente quem tem iPhone, conte com
> isso e mantenha o resumo do sábado indo para o grupo do WhatsApp também.

## Mural, agenda e regras

O mural é da diretoria: só ela publica, o grupo lê e reage. Cada publicação pode
levar uma **imagem** e escolhe o **emoji de reação** do grupo — 👏 é o padrão, mas dá
para trocar por 🔥, 🏐, 🎉 e mais alguns. O emoji fica gravado no post, então o botão
de reagir muda de cara de recado para recado.

Recado publicado com pressa se conserta no lugar: o lápis no cartão abre a mesma
tela da publicação, já preenchida, e salva por cima. Dá para corrigir o texto,
trocar o emoji, fixar ou desafixar e mexer na imagem — trocar por outra, tirar de
vez ou deixar como está. O autor e a data de publicação não mudam, só o
`atualizado_em`. Editar é da diretoria, como publicar e apagar: a policy de
`update` em `posts` já era dela desde a migração de grupos.

Trocar ou tirar a imagem mexe só na coluna `imagem_url`: o arquivo antigo continua no
bucket, como já acontece ao apagar um recado. Faxina de arquivo solto é serviço de
rotina no Storage, não do app.

As imagens vão para o bucket `mural`: leitura pública, escrita só para a diretoria,
5 MB e JPG/PNG/WEBP/GIF. O "bucket" é uma linha em `storage.buckets` e cada arquivo
é uma linha em `storage.objects`, com o conteúdo numa coluna `bytea` no próprio
Postgres. A API grava a linha **como o usuário**, então as mesmas policies de antes
(`e_diretoria(grupo_do_caminho(name))`) decidem quem pode enviar, e serve a imagem
em `/arquivos/mural/<grupo>/<arquivo>` com cache de um ano — o nome tem um uuid,
então nunca muda de conteúdo. Guardar no banco em vez de num volume deixa o backup do
Postgres cobrindo as fotos também; o volume é pequeno, uma foto por recado. Elas
**não** passam pelo PowerSync — o upload precisa de internet e a foto é baixada por
URL na hora de exibir. É o mesmo
raciocínio do painel financeiro: o sync carrega linha de banco, não arquivo. Offline,
o recado aparece com o texto e a imagem fica com um aviso no lugar.

A agenda guarda o que foge da rotina, em três tipos: **Jogo**, **Confraternização** e
**Campeonato**. O sábado é toda semana e não faz sentido cadastrar 52 vezes. A data é
digitada em **dd/mm/aaaa**, com máscara e validação de calendário antes de virar o
`timestamptz`, e o campo **Local** busca endereços enquanto se digita.

> O autocomplete de endereço usa o [Photon](https://photon.komoot.io), geocoder
> aberto sobre dados do OpenStreetMap: sem chave, sem billing e sem cadastro, com
> 350 ms de espera entre a tecla e a busca. O campo continua sendo texto livre, então
> trocar por Google Places depois é mexer só em
> [`EnderecoRepository.kt`](app/src/main/java/com/unidospelovolei/data/EnderecoRepository.kt)
> e [`enderecos.ts`](web/src/lib/enderecos.ts) — nada no banco muda.

Aniversário e tempo de casa **não são eventos cadastrados**. Saem por consulta de
`players.entrou_em` e do dia e mês de nascimento, então mudam de ano sozinhos e
ninguém precisa manter.

As páginas de regras são texto com uma formatação mínima, escrita uma vez em cada
cliente: `#` no começo da linha vira título e `-` vira item de lista. Puxar uma
biblioteca de Markdown para Compose e outra para React custaria mais manutenção do
que as duas regras que a diretoria vai realmente usar.

## Financeiro

O grupo cobra dos dois jeitos, e cada atleta tem um `regime` na ficha:
`mensalista`, `diarista` ou `isento`. A diretoria aperta **Gerar mensalidade deste
mês** e o app cria uma cobrança com uma linha de pagamento para cada mensalista;
**Gerar diária de hoje** faz o mesmo para os diaristas que estão presentes. Gerar
duas vezes a mesma competência não duplica nada.

**Quem escolhe o regime é o próprio atleta**, na ficha em EU: dois botões,
Mensalista ou Diarista, e o texto embaixo explica o que cada um significa. O RLS
deixa: a policy de dono já permitia o `update` na própria linha, e o trigger que
protege a ficha ganhou uma regra só para `isento` — entrar ou sair da isenção
continua sendo decisão da diretoria, porque isso é perdão de dívida, não preferência.

**Todo valor é inteiro em centavos.** R$ 25,50 é gravado como `2550`, e nenhum
número quebrado encosta em dinheiro — erro de arredondamento em cobrança de grupo de
amigos vira discussão, e discussão de centavo não se resolve por commit.

O Pix Copia e Cola é montado dentro do app, em
[`PixBrCode.kt`](app/src/main/java/com/unidospelovolei/domain/financeiro/PixBrCode.kt)
e [`pix.ts`](web/src/domain/pix.ts): o BR Code estático é uma cadeia de campos com um
CRC16 no fim, cerca de sessenta linhas em cada cliente, sem dependência e
funcionando offline. Os dois portes têm os mesmos testes, incluindo um que confere
que produzem exatamente a mesma string.

### O tipo da chave importa, e o app cuida do formato

O campo 01 do BR Code tem que levar a chave **no formato exato em que ela está
registrada no banco**. Celular é `+55` seguido de DDD e número, tudo junto; CPF e CNPJ
são só dígitos; e-mail e chave aleatória vão em caixa baixa. Se a chave for embutida
como a pessoa digitou, o código é gerado, o CRC bate, o banco lê e responde *"essa
chave Pix não está registrada"* — que é o sintoma mais confuso possível, porque nada
no código parece errado.

Por isso `config_financeiro.pix_tipo` diz de que tipo é a chave, e
`normalizarChave` aplica a regra daquele tipo na hora de salvar e de novo na hora de
gerar o código. A diretoria digita como quiser — `(47) 99999-8888` vira
`+5547999998888` sozinho — e o diálogo mostra embaixo do campo como a chave vai entrar
no código, ou o que está faltando quando ela ainda não é válida.

O tipo é escolhido à mão porque **não dá para adivinhar**: onze dígitos podem ser um
CPF ou um celular com DDD sem código de país, e chutar errado quebraria a chave de
quem usa CPF. A migration que criou a coluna deduziu o tipo onde era possível —
arroba vira e-mail, quatorze dígitos viram CNPJ, começar com `+` vira celular — e
deixou o caso ambíguo como `aleatoria`, que é o modo "usa como está digitado" e
preserva o comportamento antigo.

O extrato do atleta sincroniza e funciona offline. **O painel com o dinheiro de
todo mundo, não**: ele lê direto da API quando a diretoria abre. É uma escolha
deliberada — o PowerSync baixa linhas cruas para o aparelho, e sincronizar o extrato
do grupo inteiro deixaria tudo legível no SQLite de qualquer celular da diretoria.
Ler online mantém a RLS sendo a única guardiã e evita ter que colocar o papel dentro
do JWT.

## Nota por fundamento e avaliação entre colegas

A habilidade de um jogador não é mais uma estrela só. São **seis notas**, uma por
fundamento — saque, passe, ataque, bloqueio, defesa e atitude — cada uma entre 1,00 e
5,00, guardadas em `players.nota_saque` até `players.nota_atitude`. A **nota geral** é
a média das seis, mantida por trigger em `players.nota_media`, e é ela que as estrelas
mostram e que o sorteio lê. `players.skill_level` continua existindo como o
arredondamento dessa média, também por trigger, para não quebrar nada que ainda o leia.

A diretoria **não digita mais uma estrela só** ao cadastrar alguém: dá as seis notas,
na mesma escala em que os colegas avaliam. Esse é o ponto de partida, e fica gravado
no histórico como um ponto de origem `diretoria`.

### Como a nota se mexe sozinha

No fim do sábado, cada um dá nota de 1 a 5 aos **três companheiros de time** nos
mesmos seis fundamentos. Quando um dia de jogo junta **pelo menos duas** avaliações
para a mesma pessoa, a nota dela anda em direção ao que o time achou:

```
nota = nota + 0,15 × (média_do_dia − nota)
```

com o passo limitado a **0,25 por dia** e a nota presa entre 1,00 e 5,00. É devagar de
propósito: um sábado ruim não derruba ninguém, e uma sequência de cinco sábados ruins
tira cerca de meia estrela. O limite de dois avaliadores por dia existe pelo anonimato
— com uma nota só, quem recebe conseguiria deduzir de quem veio olhando o próprio
gráfico.

O cálculo é **refeito do zero** a cada avaliação nova, em `recalcular_nota`: parte do
último ponto `diretoria`, percorre em ordem os dias encerrados depois dele e aplica o
passo dia a dia. Por isso uma avaliação que chega atrasada não desalinha a linha, e o
resultado nunca depende da ordem em que as notas foram enviadas.

A diretoria pode reajustar as seis notas quando quiser. O ajuste entra no histórico
como novo ponto `diretoria` e **vira o novo ponto de partida**: os sábados seguintes
passam a puxar a partir dali, e os pontos anteriores continuam no gráfico.

A migration **não aplica retroativamente** as avaliações que já estavam no banco. Ela
copia a estrela antiga para os seis fundamentos e grava esse ponto de partida com a
data de hoje, então só os sábados encerrados depois dela contam. Notas dadas quando a
regra era "a avaliação não mexe no sorteio" não deveriam mudar a força de ninguém sem
aviso. Quem quiser o contrário troca o `now()` do `insert` final da migration por
`coalesce(p.created_at, now())` antes de rodar: a primeira avaliação nova reconstrói a
linha inteira desde a entrada do jogador.

### O gráfico

`player_nota_historico` guarda uma linha por evento — o ajuste da diretoria e cada
sábado que mexeu na nota — com as seis notas, a média e quantos avaliaram naquele dia.
Na aba **EU** o jogador vê as seis barras com a variação no período, filtros de
**3 meses / 6 meses / este ano / tudo** e um gráfico de linha. A linha mostra a média
geral por padrão; tocar numa barra troca para aquele fundamento. Ponto dourado é
ajuste da diretoria, ponto verde é sábado avaliado. No fim do ano o filtro **Este ano**
já é a retrospectiva.

### O que continua anônimo

- `avaliacoes` **não entra na publication do PowerSync**, e no schema dos clientes é
  declarada como somente-inserção. A nota sobe para o servidor e **nunca desce para
  aparelho nenhum** — nem para o de quem é diretoria.
- A RLS de `avaliacoes` tem policy de `insert` e **nenhuma de `select`**. Ninguém lê
  a nota individual pelo app, em nenhum papel.
- `player_nota_historico` só desce para o dono (`profile_id = auth.user_id()` na sync
  rule, e a policy aceita também a diretoria para consulta direta pela API).
- `avaliacao_registros` guarda só o *fato* de você ter avaliado alguém, sem a nota,
  para o app saber o que já foi preenchido sem vazar nada.
- `player_evolucao` continua existindo com as médias cruas recebidas e a contagem
  total. O app usa dela só o total de avaliações recebidas; quem manda na nota agora é
  `players`.

### O que mudou no sorteio

**Agora a nota dos colegas chega ao sorteio**, porque o sorteio lê `nota_media` e ela
se move. A escolha foi deliberada: a nota de partida da diretoria envelhece, e o time
equilibrado do sábado é o que reflete como as pessoas estão jogando agora. O ritmo
lento é o que impede a força dos times de balançar de semana em semana. O `TeamDraft`
passou a somar a média em ponto flutuante em vez de inteiros, o que também deixa o
equilíbrio mais fino do que era com estrelas cheias.

O app reage sozinho: em segundos o sync traz o profile atualizado e os botões de
edição aparecem, sem precisar reiniciar.

---

## Testes

```bash
./gradlew testDevDebugUnitTest   # app Android
cd web && npm test               # aplicação web
```

Os dois conjuntos cobrem os mesmos casos, com as mesmas sementes: o porte em
TypeScript reproduz o `kotlin.random.Random`, então as asserções de valor exato
(quantidade de rodadas, distribuição das fases) valem nos dois lados.

Cobrem os dois algoritmos do domínio
([`SchedulingTest.kt`](app/src/test/java/com/unidospelovolei/domain/SchedulingTest.kt)):
cotas de gênero, equilíbrio de força, variação entre dois sorteios e afastamento de
duplas já repetidas; e, no chaveamento, **toda rodada com todas as quadras cheias** em
nove combinações de times e quadras, as 12 rodadas exatas de 9 times em 3 quadras,
round-robin completo sem confronto repetido quando a conta divide, a revanche que
fecha a última rodada quando não divide, o teto de jogos emendados no formato que
permite, e os casos de número par de times e de quadras sobrando.

---

## Decisões de projeto

Onde o enunciado deixava espaço, foi escolhida a opção mais simples que atende ao MVP:

- **A classificação lê a definição da VIEW `standings`, reproduzida no SQLite local.**
  O PowerSync replica tabelas, não views, então a view existe no Postgres (é o que a
  migration cria e o que qualquer consumidor server-side usa) e a mesma consulta é
  reproduzida sobre as tabelas já sincronizadas, em
  [`StandingsRepository.kt`](app/src/main/java/com/unidospelovolei/data/StandingsRepository.kt).
  Sem isso a aba de classificação não funcionaria offline. **Ao mexer em uma das
  duas definições, mexa na outra** — as duas foram conferidas lado a lado sobre os
  mesmos dados (incluindo time sem partida, time inativo, partida agendada e
  empate) e devolvem exatamente as mesmas linhas na mesma ordem.
- **`team_players` tem uma coluna `id` própria.** A chave lógica continua sendo
  `(team_id, player_id)`, garantida por `UNIQUE`, mas o PowerSync exige chave
  primária simples em toda tabela sincronizada.
- **Injeção de dependência manual** ([`AppContainer.kt`](app/src/main/java/com/unidospelovolei/AppContainer.kt))
  em vez de Hilt. O grafo é pequeno e de vida única; um container escrito à mão dá o
  mesmo resultado sem processamento de anotações no build.
- **Navegação com estado em Compose**, sem `navigation-compose`. São três abas e
  algumas telas empilhadas — uma `sealed interface` de destinos resolve.
- **Strings em português direto no código**, sem `strings.xml`. O app é de um grupo
  específico e monolíngue; extrair para recursos só faria sentido com um segundo
  idioma.
- **Empate não finaliza partida.** Não existe empate em vôlei, então finalizar com
  placar igual é bloqueado com uma mensagem em vez de gravar `winner_id` nulo.
- **O número de quadras é escolhido na hora de gerar o chaveamento**, não guardado em
  tabela de configuração. O cabeçalho deduz o formato a partir dos dados
  (times ativos e maior quadra usada).
- **Gerar chaveamento apaga o anterior**, com diálogo de confirmação. Chaveamento
  incremental é complexidade que o MVP não pede.
- **`minSdk` 26**, o que cobre Android 8 em diante e evita `desugaring` para
  `java.time`.
- **O grupo ativo é filtro de consulta, não de sincronização.** O aparelho baixa
  todos os grupos da pessoa e o app filtra por `grupo_id` na hora de ler. Assinar e
  cancelar streams a cada troca de grupo daria o mesmo resultado com um download a
  cada troca e sem offline no grupo que não está aberto — quem participa de dois
  grupos costuma ter dois grupos pequenos, não dois campeonatos.
- **A chave de acesso não sincroniza.** `grupo_chaves` fica fora das sync rules e é
  lida pela API só pela diretoria do grupo. Um convite que desce para o SQLite
  de todo membro é um convite vazado.
- **Sem lista pública de grupos.** Entrar depende de alguém te passar a chave. Um
  diretório de grupos convidaria gente estranha a pedir entrada em grupo de sábado
  de bairro, que é exatamente o que a chave existe para evitar.
- **Papel vive em `grupo_membros`, não em `profiles`.** Ser diretoria é uma relação
  com um grupo, não um atributo da pessoa; `profiles.is_admin` e `profiles.papel`
  foram removidos junto com a função `is_admin()`.
- **No Android, o que fala com a API usa `JsonObject`, não classe
  `@Serializable`.** O build não aplica o plugin de compilação do
  `kotlinx.serialization`, então uma classe anotada compila normalmente mas **não
  ganha serializer**: a falha só aparece em produção, como
  *"Serializer for class 'X' is not found"*, no momento da chamada. `JsonObject` tem
  serializer embutido e funciona sem plugin. As poucas leituras e escritas online
  (chaves, membros, painel financeiro) montam e leem o JSON à mão, com os auxiliares
  de [`Json.kt`](app/src/main/java/com/unidospelovolei/data/Json.kt). **Não volte a
  anotar DTO com `@Serializable`** sem antes aplicar o plugin
  `org.jetbrains.kotlin.plugin.serialization` no `app/build.gradle.kts` — se aplicar,
  aí sim as classes voltam a valer a pena.
- **A API é fina e a regra mora no banco.** Ela não reimplementa permissão: abre a
  transação com o papel `authenticated` e o `sub` do usuário e deixa a RLS, os
  triggers e as funções `security definer` decidirem, exatamente como o Postgrest
  fazia. Por isso a troca do Supabase pelo Railway não mexeu em nenhuma policy.
- **Os nomes `auth` e `storage` ficaram.** Renomear daria um diff enorme nas
  migrations antigas sem ganho nenhum; a camada de compatibilidade recria só o que
  elas usam.
- **Só a API conhece o Google.** O PowerSync confia no JWT da API por um segredo
  compartilhado (HS256) em vez de uma URL de JWKS, então nenhum serviço depende de
  outro estar no ar para validar um token.

## Fora do escopo do MVP

Coisas visíveis no protótipo web ou que costumam ser pedidas, mas que **não** estão
aqui porque não constam do escopo descrito:

- **Exportar resultados** (o botão verde de exportar planilha do protótipo).
- Foto na ficha do jogador: a coluna `players.foto_url` existe, mas só o mural e a
  logo do grupo enviam imagem por enquanto.
- Múltiplos campeonatos ou temporadas em paralelo **dentro do mesmo grupo**:
  `rounds`, `matches` e `standings` continuam assumindo um único campeonato em
  andamento por grupo. A aba de campeonatos é conteúdo, não gestão.
- Comentário no mural (só reação).
- Convite por link no Android: a web aceita `?chave=...`, o app trata o código
  colado. App Link com domínio próprio fica para quando o domínio existir.
- Transferir jogadores, histórico ou financeiro de um grupo para outro.
- Baixa automática de Pix: não há integração bancária. A diretoria dá baixa na mão.

---

## Solução de problemas

**O login abre e fecha sem entrar.**
Quase sempre é OAuth. Confira, nesta ordem: (1) o *Web client ID* em
`local.properties` é o do tipo **Web**, não o Android; (2) existe um *Android client
ID* com o package name da variant que você está rodando (`com.unidospelovolei.dev`
em dev) e com o SHA-1 do keystore que assinou o build; (3) a variável compartilhada
`GOOGLE_WEB_CLIENT_ID` do ambiente tem esse mesmo valor — é com ela que a API
confere o token.

**Na web, o botão do Google não aparece ou dá erro de origem.**
O endereço do site precisa estar em **Authorized JavaScript origins** do Web client,
exatamente como aparece na barra do navegador (com `https://`, sem barra no fim). A
mudança no Google Cloud pode levar alguns minutos para valer.

**"Credencial inesperada devolvida pelo Credential Manager" ou nenhuma conta aparece.**
O emulador provavelmente não tem Google Play. Crie um AVD com imagem de sistema com
Play Store e faça login numa conta Google no dispositivo.

**O app abre mas fica sempre Offline.**
Abra o log do serviço `powersync`. `Replicating` é sinal de vida; erro de
`wal_level` quer dizer que falta reiniciar o `Postgres` uma vez depois do primeiro
start da API; `401` em toda conexão quer dizer que o `JWT_SECRET` que o PowerSync lê
não é o mesmo da API. Confira também se `POWERSYNC_URL` aponta para o ambiente certo.
No caminho local, lembre que o emulador enxerga a máquina como `10.0.2.2`, nunca
`localhost`.

**Entrei no app e ele pede uma chave de acesso.**
É o esperado: a conta sozinha não dá acesso a grupo nenhum. Peça a chave a quem
organiza o seu grupo, ou crie o seu em **Criar um grupo**. Veja
[Grupos de jogo e chave de acesso](#grupos-de-jogo-e-chave-de-acesso).

**Crio a chave e ela não aparece na lista.**
A tela de chaves fala com a API na hora, não com o banco local, e mostra a
mensagem que o servidor devolveu em vez de um "precisa de internet" genérico. Leia o
recado: `permission denied` é grant faltando, `new row violates row-level security
policy` é você não ser diretoria **naquele** grupo, e `Serializer for class ... is
not found` é DTO anotado com `@Serializable` num build sem o plugin de serialização —
veja [Decisões de projeto](#decisões-de-projeto). Para ver o que existe de fato:

```sql
select c.codigo, c.ativa, c.usos, g.nome
from public.grupo_chaves c
join public.grupos g on g.id = c.grupo_id;
```

**"Chave nao encontrada" com o código certo.**
Confira se a chave não foi desativada, se não venceu e se não estourou o limite de
usos — os três casos têm mensagem própria, e "não encontrada" é só quando o código
não existe mesmo. Maiúscula, minúscula e o hífen do meio não importam.

**Entrei no grupo mas o app está vazio.**
O grupo entra na hora, os dados vêm pelo sync logo depois. Se ficar vazio, confira o
indicador do cabeçalho: sem internet a entrada é registrada, mas nada desce.

**Troquei de grupo e vejo os dados do outro.**
Alguma consulta ficou sem o `WHERE grupo_id = ?`. Todo `SELECT` de tabela de grupo
passa pelo `observarNoGrupo` no Android e pelos hooks com `useGrupoAtivo()` na web.

**Sincroniza a leitura mas as edições não sobem.**
É a RLS fazendo o trabalho dela: o usuário não é da diretoria **naquele grupo**. Use
**EU → Configurações → Membros do grupo**, ou o `update` de
[Quem é quem no app](#quem-é-quem-no-app).

Vale saber como isso aparece: um `INSERT` sem permissão devolve erro, mas
`UPDATE` e `DELETE` **não** — a cláusula `USING` da policy simplesmente filtra
todas as linhas e a operação afeta zero registros, sem mensagem. Então uma edição
de não-admin fica no SQLite local até o sync trazer o valor do servidor por cima.
Na prática não acontece, porque o app esconde os controles de edição de quem não
é admin.

**Nada aparece no app mesmo com dados no banco.**
Confira se a `publication powersync` existe (migration `..._powersync.sql`) e se as
tabelas listadas nela batem com as das sync rules. A API também usa essa publication
como lista das tabelas em que o upload pode escrever: tabela fora dela é recusada
com `Tabela não sincronizada`.

**A API não sobe no Railway.**
O log do deploy mostra a migration que falhou e a mensagem do Postgres. Uma
migration com erro não é marcada como aplicada, então corrigir o arquivo e dar push
roda ela de novo. Para não chegar a isso, rode a mudança na stack local antes do
push: a API local aplica as mesmas migrations, na mesma ordem, e falha do mesmo jeito.

**Erro de certificado no Gradle: `PKIX path building failed`.**
Um antivírus com inspeção de HTTPS (Norton, Kaspersky, ESET) ou um proxy corporativo
está interceptando TLS com um certificado que a JVM não conhece. O `curl` funciona e
o Gradle não. Duas saídas: desligar a varredura de HTTPS do antivírus, ou importar o
certificado raiz dele no truststore do JDK usado pelo Studio:

```bash
keytool -importcert -trustcacerts -alias antivirus-ssl \
  -file caminho/para/o/certificado.cer \
  -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
```

**Build falha com "requires compileSdk 37 or later".**
Instale a plataforma correspondente em **Tools → SDK Manager → SDK Platforms**.

**Mudei o `local.properties` e o app não viu.**
As chaves entram no `BuildConfig` em tempo de build. Rode
**File → Sync Project with Gradle Files** e reinstale o app.
