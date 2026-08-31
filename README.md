# Unidos Pelo Vôlei

App Android nativo para controlar jogadores, times, chaveamento, placar e histórico
do grupo de vôlei de sábado. Substitui o protótipo web mantendo a mesma identidade
visual, agora com backend real, login Google, sincronização multiusuário em tempo
real e distribuição de times por nível de habilidade.

---

## Sumário

- [O que o app faz](#o-que-o-app-faz)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do backend](#configuração-do-backend)
  - [1. Criar o projeto no Supabase](#1-criar-o-projeto-no-supabase)
  - [2. Rodar as migrations](#2-rodar-as-migrations)
  - [3. Credenciais OAuth no Google Cloud](#3-credenciais-oauth-no-google-cloud)
  - [4. Provedor Google no Supabase Auth](#4-provedor-google-no-supabase-auth)
  - [5. Instância do PowerSync](#5-instância-do-powersync)
- [Chaves em local.properties](#chaves-em-localproperties)
- [Rodando localmente para teste](#rodando-localmente-para-teste)
  - [Rodar no Android Studio](#rodar-no-android-studio)
  - [Caminho 1 — projeto Supabase e instância PowerSync só de dev (recomendado)](#caminho-1--projeto-supabase-e-instância-powersync-só-de-dev-recomendado)
  - [Caminho 2 — tudo local com Docker](#caminho-2--tudo-local-com-docker)
  - [Alternativa por linha de comando](#alternativa-por-linha-de-comando)
- [Virar administrador](#virar-administrador)
- [Testes](#testes)
- [Decisões de projeto](#decisões-de-projeto)
- [Fora do escopo do MVP](#fora-do-escopo-do-mvp)
- [Solução de problemas](#solução-de-problemas)

---

## O que o app faz

| # | Funcionalidade | Onde fica |
|---|---|---|
| 1 | CRUD de jogadores com nível de habilidade de 1 a 5 e flag de ativo | aba **TIMES** → botão **Jogadores** (só admin) |
| 2 | Gestão de times coloridos (nome, cor, sigla de 2 letras) | aba **TIMES** → **Novo time** / ícone de lápis no cartão |
| 3 | Distribuição equilibrada por habilidade (snake draft) com a força de cada time | aba **TIMES** → **Distribuir** |
| 4 | Geração do chaveamento round-robin com folgas, agrupado em fases | aba **JOGOS** → **Gerar chaveamento** |
| 5 | Placar ao vivo por partida, com vencedor marcado automaticamente | aba **JOGOS** → toque em uma partida |
| 6 | Classificação geral com V, S e PP e desempate na ordem correta | aba **CLASSIFICAÇÃO** |
| 7 | Histórico de jogos por time (fase, rodada, quadra, placar, resultado) | aba **TIMES** → toque em um time |
| 8 | Login com Google; usuário comum só lê, admin edita | tela inicial |
| 9 | Funciona offline e sincroniza ao reconectar | em todo o app |

O indicador **Online / Conectando / Offline** no cabeçalho mostra o estado do sync.

### Como os algoritmos funcionam

**Snake draft** ([`SnakeDraft.kt`](app/src/main/java/com/unidospelovolei/domain/scheduling/SnakeDraft.kt)):
os jogadores ativos são ordenados por habilidade decrescente e distribuídos em
serpentina — a primeira leva vai do time 1 ao time N, a segunda volta do N ao 1.
Quem escolhe por último em uma leva escolhe primeiro na seguinte, o que mantém a
soma de habilidade dos times próxima.

**Chaveamento** ([`RoundRobinScheduler.kt`](app/src/main/java/com/unidospelovolei/domain/scheduling/RoundRobinScheduler.kt)):
o método do círculo fixa o primeiro time e gira os demais, gerando todos os
confrontos possíveis sem repetição (com número ímpar de times entra um
"fantasma", e quem cai contra ele folga). Como o método do círculo produz mais
partidas simultâneas do que existem quadras, os confrontos entram numa fila e são
empacotados em rodadas de no máximo `quadras` partidas, sem repetir time na mesma
rodada. Uma **fase** é um ciclo completo de folgas: o número de rodadas até todos
terem folgado uma vez.

Com 9 times e 3 quadras isso dá exatamente 12 rodadas de 3 partidas (36 confrontos
= round-robin completo), agrupadas em 4 fases de 3 rodadas — o mesmo formato do
protótipo. Há testes cobrindo isso.

A habilidade **não** entra no chaveamento, apenas na formação dos times.

---

## Stack e arquitetura

| Camada | Tecnologia |
|---|---|
| App | Kotlin + Jetpack Compose, MVVM |
| Build | Gradle 9.7.1 + Android Gradle Plugin 9.3.2, `compileSdk` 37, `minSdk` 26 |
| Backend | Supabase (PostgreSQL gerenciado), acessado pelo SDK Kotlin da comunidade `io.github.jan-tennert.supabase` (Auth + Postgrest) |
| Sync offline | PowerSync (`com.powersync`), SQLite local sincronizado nos dois sentidos |
| Autenticação | Supabase Auth com login Google via Credential Manager (Google Identity Services) |

### Fluxo de dados

```
                          Google Identity Services
                                    │  ID token
                                    ▼
   Compose UI ──▶ ViewModel ──▶ Repository ──▶ SQLite local (PowerSync)
        ▲                                            │        ▲
        └──────── Flow (watch) ──────────────────────┘        │
                                                     upload   │ replicação
                                                     via      │ lógica
                                                     Postgrest│
                                                              ▼
                                            Supabase / PostgreSQL  ◀── PowerSync Service
```

Pontos importantes:

- **O app nunca conecta direto no Postgres.** Leituras saem do SQLite local; escritas
  são gravadas no SQLite local e enviadas ao Supabase pelo conector de upload do
  PowerSync, que aplica a mudança via Postgrest respeitando as policies RLS.
- **Não há Room nem outbox próprio.** O SQLite gerenciado pelo PowerSync é a fonte
  de verdade local, e a fila de upload é a dele.
- **Resolução de conflito é last-write-wins por linha.** É aceitável porque cada
  partida é registrada por uma pessoa. Para reduzir ainda mais a chance de conflito,
  toda escrita de placar toca apenas as colunas daquela partida
  (`UPDATE matches SET score_a = ?, score_b = ? WHERE id = ?`), então duas pessoas
  marcando pontos em quadras diferentes nunca disputam a mesma linha.
- **O JWT do Supabase autoriza tudo.** O mesmo token autentica o PowerSync e as
  escritas no Postgrest.

### Camadas

```
com.unidospelovolei
├── AppContainer.kt          injeção de dependência manual
├── domain/
│   ├── model/               modelos puros (Player, Team, Match, Standing, ...)
│   └── scheduling/          SnakeDraft e RoundRobinScheduler
├── data/
│   ├── AppSchema.kt         espelho local das tabelas sincronizadas
│   ├── AuthRepository.kt    login Google + Supabase Auth
│   ├── SyncService.kt       liga/desliga o sync conforme a sessão
│   └── *Repository.kt       leitura via watch() e escrita no SQLite local
└── ui/
    ├── theme/ components/   identidade visual
    ├── main/                sessão, papel do usuário, estado do sync
    └── games/ standings/ teams/ players/ login/
```

---

## Estrutura do repositório

```
.
├── app/                              módulo Android
├── gradle/libs.versions.toml         catálogo de versões
├── supabase/
│   ├── config.toml                   config da Supabase CLI (start/db push/reset)
│   ├── migrations/
│   │   ├── 20260831120000_init.sql            tabelas e triggers
│   │   ├── 20260831120100_standings_view.sql  VIEW standings
│   │   ├── 20260831120200_rls.sql             policies RLS
│   │   └── 20260831120300_powersync.sql       publication de replicação
│   └── seed.sql                      seed opcional (9 times, 38 jogadores)
├── powersync/
│   ├── sync-rules.yaml               o que cada dispositivo baixa
│   └── self-host/                    Docker para o caminho totalmente local
└── local.properties.example          modelo das chaves
```

---

## Pré-requisitos

- **Android Studio** (versão recente, com AGP 9 suportado) — é o caminho principal.
- **JDK 17 ou mais novo.** O JDK embutido no Android Studio (JBR) serve.
- **Um emulador com Google Play** ou um dispositivo físico com Play Services.
  O login Google usa o Credential Manager e **não funciona em imagem de emulador
  sem Google Play** — ao criar o AVD, escolha uma imagem com o ícone da Play Store.
- Conta no [Supabase](https://supabase.com) e no [PowerSync](https://powersync.com)
  (os planos gratuitos bastam para dev).
- Para o caminho totalmente local: Docker Desktop e a
  [Supabase CLI](https://supabase.com/docs/guides/cli).

---

## Configuração do backend

### 1. Criar o projeto no Supabase

1. Em [supabase.com/dashboard](https://supabase.com/dashboard), **New project**.
2. Guarde a senha do banco: ela é usada para conectar o PowerSync.
3. Em **Project Settings → API**, anote:
   - **Project URL** → vira `SUPABASE_URL`
   - **anon public key** → vira `SUPABASE_ANON_KEY`
4. Ainda em **Project Settings → API**, no bloco **Security**, deixe assim:

   | Opção | Estado | Motivo |
   |---|---|---|
   | Enable Data API | **ligado** | O app grava pelo Postgrest, tanto no conector de upload do PowerSync quanto no supabase-kt. Sem ele nada escreve. |
   | Automatically expose new tables | **desligado** | A migration de RLS já faz os `grant` explícitos nas 6 tabelas, na view `standings` e na função `is_admin()`. Desligar evita expor ao `anon` alguma tabela criada depois. |
   | Enable automatic RLS | **ligado** | Redundante para as tabelas do projeto, que já ligam RLS na migration, mas garante que uma tabela criada no futuro nasça fechada. |

   Ajuste isso **antes** de rodar as migrations: a RLS automática funciona por
   event trigger no `CREATE TABLE`. Use as mesmas opções no projeto de dev.

> A anon key é pública por definição (vai dentro do app). Quem protege os dados é a
> RLS, não a chave. Ainda assim ela fica em `local.properties`, fora do Git.

### 2. Rodar as migrations

**Pelo painel:** abra **SQL Editor** e execute, nesta ordem, o conteúdo de:

1. `supabase/migrations/20260831120000_init.sql`
2. `supabase/migrations/20260831120100_standings_view.sql`
3. `supabase/migrations/20260831120200_rls.sql`
4. `supabase/migrations/20260831120300_powersync.sql`

E, se quiser dados de exemplo, `supabase/seed.sql`.

**Pela CLI** — faz o mesmo, com a vantagem de registrar o que já rodou em cada
projeto, o que ajuda quando você mantém dev e prod em paralelo. O repositório já
traz o `supabase/config.toml`, então **não** é preciso rodar `supabase init`.

A [Supabase CLI](https://supabase.com/docs/guides/cli) não precisa ser instalada:
com Node no PATH, o `npx` baixa e roda na hora.

```bash
npx supabase@latest login
npx supabase@latest link --project-ref SEU_PROJECT_REF
npx supabase@latest db push
```

O `login` abre o navegador para gerar o token da sua conta e vem antes do `link`,
senão ele falha por falta de autenticação. O `link` pede a **senha do banco**
(a definida ao criar o projeto, não a senha da sua conta Supabase).

Nenhum desses três comandos usa Docker: eles falam direto com o Postgres remoto.
Só `supabase start` e `supabase db reset`, do caminho local, precisam dele.

Se preferir o comando `supabase` permanente em vez de `npx` toda vez, no Windows
use o [Scoop](https://scoop.sh) (`npm i -g supabase` não é suportado):

```powershell
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase
```

O **project ref** é o identificador do projeto, uma string de ~20 letras. Ele
aparece em três lugares, sempre igual:

- no subdomínio da URL do projeto — `https://`**`abcdefghijklmnopqrst`**`.supabase.co`,
  a mesma URL que vira `SUPABASE_URL`;
- na URL do painel — `https://supabase.com/dashboard/project/`**`abcdefghijklmnopqrst`**;
- em **Project Settings → General**, no campo *Project ID*.

`link` associa a pasta ao projeto remoto (ele pede a senha do banco) e roda uma
vez só; `db push` envia as migrations pendentes. Para trocar de projeto depois,
basta rodar `link` de novo com o outro ref.

O que cada migration faz:

- **init** — tabelas `profiles`, `players`, `teams`, `team_players`, `rounds` e
  `matches`, com `updated_at` automático e criação do profile no primeiro login.
- **standings_view** — a VIEW `public.standings` com V, S e PP calculados a partir
  das partidas finalizadas, ordenada por vitórias, saldo e pontos pró.
- **rls** — SELECT liberado para qualquer autenticado; INSERT, UPDATE e DELETE só
  para quem tem `profiles.is_admin = true`.
- **powersync** — a `publication` de replicação lógica que o PowerSync exige.

### 3. Credenciais OAuth no Google Cloud

No [Google Cloud Console](https://console.cloud.google.com), crie (ou escolha) um
projeto e vá em **APIs & Services → Credentials**. Configure a *OAuth consent
screen* antes, se ainda não existir.

**a) Web client ID** — é o que o app envia ao Credential Manager e o que o Supabase
valida.

1. **Create Credentials → OAuth client ID → Web application**.
2. Em **Authorized redirect URIs**, adicione o callback do Supabase:
   `https://SEU-PROJETO.supabase.co/auth/v1/callback`
3. Anote o **Client ID** (vira `GOOGLE_WEB_CLIENT_ID`) e o **Client secret**.

**b) Android client ID** — não vai para o `local.properties`; ele existe para o
Google reconhecer o APK que está pedindo a credencial.

1. **Create Credentials → OAuth client ID → Android**.
2. **Package name**: `com.unidospelovolei.dev` para a variant de dev
   (a variant `prod` usa `com.unidospelovolei` — crie um segundo client Android
   para ela quando for publicar).
3. **SHA-1**: o do keystore de debug. Pegue com

   ```bash
   ./gradlew signingReport
   ```

   e copie o `SHA1` da variant `devDebug`. Alternativa direta:

   ```bash
   keytool -list -v -alias androiddebugkey \
     -keystore ~/.android/debug.keystore -storepass android -keypass android
   ```

   No Windows, o keystore fica em `%USERPROFILE%\.android\debug.keystore`.

> Se um dia gerar um APK assinado com outro keystore, cadastre também o SHA-1 dele,
> senão o login falha só naquele build.

### 4. Provedor Google no Supabase Auth

1. **Authentication → Sign In / Providers** (na seção *Configuration* do menu
   lateral, logo abaixo de *Policies*), role até **Auth Providers** e clique em
   **Google**. Habilite o provedor.
2. **Client ID** — cole o **Web client ID** do passo anterior. É esse campo que faz
   o Supabase aceitar o `signInWithIdToken` que o app envia. Se o dashboard mostrar
   *Client IDs* no plural, ele aceita vários separados por vírgula; se mostrar um
   campo **Authorized Client IDs** à parte, coloque o mesmo valor nos dois.
3. **Client Secret** — o secret do mesmo client Web.
4. **Skip nonce checks** — deixe **desligado**. O app manda o hash SHA-256 do nonce
   para o Google e o valor original para o Supabase, então a checagem passa e
   continua protegendo contra replay de token. (O `skip_nonce_check = true` do
   `supabase/config.toml` vale só para a stack local do Docker.)
5. Salve.

### 5. Instância do PowerSync

1. Em [powersync.journeyapps.com](https://powersync.journeyapps.com), crie uma
   instância.
2. **Connect to database**: escolha Supabase/Postgres e informe a connection string
   do projeto (Supabase → **Project Settings → Database → Connection string**,
   modo *Session* ou *Direct*). Use a senha do banco definida no passo 1.
3. Na aba de autenticação da instância, ative a integração com **Supabase Auth**
   (o PowerSync valida o mesmo JWT que o app já usa).
4. Em **Sync Rules**, cole o conteúdo de [`powersync/sync-rules.yaml`](powersync/sync-rules.yaml)
   e faça o **Deploy**.
5. Copie a **instance URL** — vira `POWERSYNC_URL`.

As sync rules mandam para o dispositivo o campeonato inteiro (jogadores, times,
rodadas e partidas) e, do `profiles`, apenas a linha do próprio usuário.

---

## Chaves em local.properties

Copie `local.properties.example` para `local.properties` e preencha. **Esse arquivo
nunca vai para o Git** (já está no `.gitignore`) e nenhuma chave fica hardcoded no
código: elas entram via `BuildConfig`, geradas em [`app/build.gradle.kts`](app/build.gradle.kts).

```properties
sdk.dir=C:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk

dev.SUPABASE_URL=https://SEU-PROJETO-DEV.supabase.co
dev.SUPABASE_ANON_KEY=...
dev.POWERSYNC_URL=https://SUA-INSTANCIA-DEV.powersync.journeyapps.com
dev.GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com

prod.SUPABASE_URL=
prod.SUPABASE_ANON_KEY=
prod.POWERSYNC_URL=
prod.GOOGLE_WEB_CLIENT_ID=
```

O prefixo casa com a build variant: `dev.*` alimenta `devDebug`/`devRelease` e
`prod.*` alimenta `prodDebug`/`prodRelease`. Em CI as mesmas chaves podem vir de
variáveis de ambiente (`DEV_SUPABASE_URL`, `PROD_SUPABASE_URL`, ...).

Se faltar alguma chave, o app abre numa tela dizendo exatamente qual, em vez de
quebrar.

---

## Rodando localmente para teste

A separação de ambientes é por **build variant**: trocar de ambiente é só trocar a
variant selecionada, sem mexer no código. O Android Studio seleciona **devDebug**
por padrão, então o build de debug aponta para dev — **as chaves de produção nunca
entram num build de debug**, porque `debug` só existe combinado com um flavor e o
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
2. Preencha o `local.properties` com as chaves do ambiente de dev
   (ver [Chaves em local.properties](#chaves-em-localproperties)) e rode
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

Na primeira execução: entre com o Google, depois
[promova seu usuário a admin](#virar-administrador) para liberar as telas de edição.

Para testar o **offline**, ative o modo avião no dispositivo. O app continua abrindo,
navegando e registrando placar; ao voltar a internet, o indicador volta para
**Online** e as alterações sobem sozinhas.

Para testar o **multiusuário em tempo real**, rode o app em dois dispositivos com
contas diferentes (ambas admin) e marque pontos em partidas diferentes: cada um vê
o placar do outro aparecer.

### Caminho 1 — projeto Supabase e instância PowerSync só de dev (recomendado)

Sem Docker. A ideia é ter uma cópia inteira do backend só para desenvolvimento.

1. Crie um **segundo projeto Supabase** (plano gratuito), separado do de produção.
2. Rode nele as mesmas migrations de
   [Rodar as migrations](#2-rodar-as-migrations), mais o `seed.sql` se quiser dados
   de teste.
3. Configure nele o provedor Google. Dá para reaproveitar o mesmo *Web client ID*,
   desde que você adicione o callback do projeto de dev
   (`https://SEU-PROJETO-DEV.supabase.co/auth/v1/callback`) nas *Authorized redirect
   URIs* do client.
4. Crie uma **instância PowerSync de dev** apontando para esse projeto e publique as
   mesmas sync rules.
5. Preencha as chaves `dev.*` no `local.properties`.
6. Rode a variant `devDebug`.

Vantagem: o ambiente é idêntico ao de produção, incluindo TLS, RLS e o comportamento
real do PowerSync em nuvem.

### Caminho 2 — tudo local com Docker

> **Ressalva importante:** o PowerSync em nuvem **não alcança um Postgres em
> localhost**. Se o Supabase está rodando na sua máquina, o PowerSync também precisa
> rodar na sua máquina, self-hosted. Por isso este caminho exige os dois em Docker.

**a) Supabase local**

```bash
supabase start
```

Anote a saída: `API URL` (normalmente `http://127.0.0.1:54321`), `anon key`,
`DB URL` (porta `54322`) e `JWT secret`.

Aplique o schema:

```bash
supabase db reset      # roda supabase/migrations + supabase/seed.sql
```

**b) PowerSync self-hosted**

```bash
cd powersync/self-host
cp .env.example .env
# edite .env: PS_SUPABASE_JWT_SECRET com o "JWT secret" do supabase status
docker compose up
```

O compose sobe o serviço do PowerSync, o MongoDB que ele usa como armazenamento
interno de buckets e monta [`powersync/sync-rules.yaml`](powersync/sync-rules.yaml)
como configuração de sync — o mesmo arquivo publicado no PowerSync Cloud. O Postgres
do Supabase local é alcançado do container por `host.docker.internal:54322`.

> O serviço PowerSync é baseado no
> [self-host-demo oficial](https://github.com/powersync-ja/self-host-demo). Se a
> imagem mudar de formato de configuração, confira lá.

**c) Endereços vistos pelo dispositivo**

`localhost`, dentro de um emulador ou de um celular, é o próprio dispositivo — não a
sua máquina. Use:

| Onde o app roda | Supabase (54321) | PowerSync (8080) |
|---|---|---|
| Emulador Android | `http://10.0.2.2:54321` | `http://10.0.2.2:8080` |
| Aparelho físico na mesma rede | `http://192.168.x.y:54321` | `http://192.168.x.y:8080` |

Descubra o IP da máquina com `ipconfig` (Windows) ou `ip addr` (Linux/macOS).

No `local.properties`:

```properties
dev.SUPABASE_URL=http://10.0.2.2:54321
dev.SUPABASE_ANON_KEY=<anon key do supabase status>
dev.POWERSYNC_URL=http://10.0.2.2:8080
dev.GOOGLE_WEB_CLIENT_ID=<mesmo Web client ID>
```

Esses endereços são HTTP puro, e o Android bloqueia tráfego em texto claro por
padrão. Por isso o projeto traz um
[`app/src/debug/AndroidManifest.xml`](app/src/debug/AndroidManifest.xml) que libera
cleartext **apenas nos builds de debug** — nenhum build de release recebe essa flag.
Não é preciso mexer em nada.

**d) Login Google na stack local**

O `supabase/config.toml` já traz o bloco `[auth.external.google]` ligado, com o
client id e o segredo vindos de variáveis de ambiente — nada de segredo no
repositório. Defina as duas antes do `supabase start`:

```bash
export SUPABASE_AUTH_EXTERNAL_GOOGLE_CLIENT_ID=<o mesmo Web client ID>
export SUPABASE_AUTH_EXTERNAL_GOOGLE_SECRET=<o client secret do mesmo client>
supabase start
```

No PowerShell, `$env:SUPABASE_AUTH_EXTERNAL_GOOGLE_CLIENT_ID = "..."`.

Esse bloco tem `skip_nonce_check = true`, recomendação da própria CLI para o
login Google local. Vale **só** para a stack local: os projetos na nuvem não leem
este arquivo e continuam checando o nonce normalmente.

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

## Virar administrador

Todo usuário que entra é criado como leitor. Depois do primeiro login, rode no SQL
Editor do Supabase:

```sql
update public.profiles set is_admin = true where email = 'voce@gmail.com';
```

O app reage sozinho: em segundos o sync traz o profile atualizado e os botões de
edição aparecem, sem precisar reiniciar.

---

## Testes

```bash
./gradlew testDevDebugUnitTest
```

Cobrem os dois algoritmos do domínio
([`SchedulingTest.kt`](app/src/test/java/com/unidospelovolei/domain/SchedulingTest.kt)):
equilíbrio do snake draft, round-robin completo com 9 times em 3 quadras, ausência
de time repetido na mesma rodada, agrupamento das fases e os casos de número par de
times e de quadras sobrando.

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

## Fora do escopo do MVP

Coisas visíveis no protótipo web ou que costumam ser pedidas, mas que **não** estão
aqui porque não constam do escopo descrito:

- **Exportar resultados** (o botão verde de exportar planilha do protótipo).
- Notificações push, estatísticas por jogador, múltiplos campeonatos ou temporadas,
  e edição do papel de admin dentro do app (é feita por SQL).

---

## Solução de problemas

**O login abre e fecha sem entrar.**
Quase sempre é OAuth. Confira, nesta ordem: (1) o *Web client ID* em
`local.properties` é o do tipo **Web**, não o Android; (2) existe um *Android client
ID* com o package name da variant que você está rodando (`com.unidospelovolei.dev`
em dev) e com o SHA-1 do keystore de debug; (3) esse mesmo Web client ID está em
**Authorized Client IDs** no provedor Google do Supabase.

**"Credencial inesperada devolvida pelo Credential Manager" ou nenhuma conta aparece.**
O emulador provavelmente não tem Google Play. Crie um AVD com imagem de sistema com
Play Store e faça login numa conta Google no dispositivo.

**O app abre mas fica sempre Offline.**
Verifique se a instância PowerSync está com a integração de Supabase Auth ativa e com
as sync rules publicadas, e se `POWERSYNC_URL` aponta para a instância certa. No
caminho local, lembre que o emulador enxerga a máquina como `10.0.2.2`, nunca
`localhost`.

**Sincroniza a leitura mas as edições não sobem.**
É a RLS fazendo o trabalho dela: o usuário não é admin. Rode o `update` de
[Virar administrador](#virar-administrador).

Vale saber como isso aparece: um `INSERT` sem permissão devolve erro, mas
`UPDATE` e `DELETE` **não** — a cláusula `USING` da policy simplesmente filtra
todas as linhas e a operação afeta zero registros, sem mensagem. Então uma edição
de não-admin fica no SQLite local até o sync trazer o valor do servidor por cima.
Na prática não acontece, porque o app esconde os controles de edição de quem não
é admin.

**Nada aparece no app mesmo com dados no Supabase.**
Confira se a `publication powersync` existe (migration `..._powersync.sql`) e se as
tabelas listadas nela batem com as das sync rules.

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
