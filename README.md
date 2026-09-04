# Unidos Pelo Vôlei

Controle de jogadores, times, chaveamento, placar e histórico do grupo de vôlei de
sábado, com backend real, login Google, sincronização multiusuário em tempo real e
sorteio de times equilibrado por gênero e habilidade.

São **dois clientes sobre o mesmo backend**:

| Cliente | Pasta | Para quem |
|---|---|---|
| App Android nativo | [`app/`](app/) | Quem usa Android instala pela Play Store |
| Aplicação web (PWA) | [`web/`](web/) | Quem usa iPhone — ou qualquer navegador — acessa pelo link |

Os dois falam com o mesmo projeto Supabase e a mesma instância PowerSync, então
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
- [Configuração do backend](#configuração-do-backend)
  - [1. Criar o projeto no Supabase](#1-criar-o-projeto-no-supabase)
  - [2. Rodar as migrations](#2-rodar-as-migrations)
  - [3. Credenciais OAuth no Google Cloud](#3-credenciais-oauth-no-google-cloud)
  - [4. Provedor Google no Supabase Auth](#4-provedor-google-no-supabase-auth)
  - [5. Instância do PowerSync](#5-instância-do-powersync)
- [Chaves em local.properties](#chaves-em-localproperties)
- [Assinatura do release](#assinatura-do-release)
- [Rodando localmente para teste](#rodando-localmente-para-teste)
  - [Rodar no Android Studio](#rodar-no-android-studio)
  - [Caminho 1 — projeto Supabase e instância PowerSync só de dev (recomendado)](#caminho-1--projeto-supabase-e-instância-powersync-só-de-dev-recomendado)
  - [Caminho 2 — tudo local com Docker](#caminho-2--tudo-local-com-docker)
  - [Alternativa por linha de comando](#alternativa-por-linha-de-comando)
- [Aplicação web](#aplicação-web)
  - [Rodando a web localmente](#rodando-a-web-localmente)
  - [Publicando a web](#publicando-a-web)
  - [Páginas públicas](#páginas-públicas)
- [As cinco abas](#as-cinco-abas)
- [Quem é quem no app](#quem-é-quem-no-app)
- [Confirmação de presença e lembretes](#confirmação-de-presença-e-lembretes)
- [Mural, agenda e regras](#mural-agenda-e-regras)
- [Financeiro](#financeiro)
- [Avaliação entre colegas](#avaliação-entre-colegas)
- [Testes](#testes)
- [Decisões de projeto](#decisões-de-projeto)
- [Fora do escopo do MVP](#fora-do-escopo-do-mvp)
- [Solução de problemas](#solução-de-problemas)

---

## O que o app faz

| # | Funcionalidade | Onde fica |
|---|---|---|
| 1 | CRUD de jogadores com nível de habilidade em estrelas de 1 a 5 e gênero — o nível **só a diretoria vê e edita** | aba **TIMES** → botão **Jogadores** |
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
| 13 | Ficha do membro com aniversário, telefone e contato de emergência | aba **EU** → lápis |
| 13b | Cada um escolhe como paga: **Mensalista** ou **Diarista** | aba **EU** → lápis |
| 14 | Confirmação antecipada do sábado (**Vou / Talvez / Não vou**), pela pessoa ou pela diretoria | aba **EU** e aba **SOCIAL** → **Chamada** |
| 15 | **Trazer confirmados**: quem respondeu que vem vira presença na lista de hoje | aba **SOCIAL** → **Chamada** (só diretoria) |
| 16 | Lembrete por notificação na sexta à noite e no sábado de manhã | push no Android e no PWA |
| 17 | Mural de recados da diretoria, com imagem anexada e emoji de reação escolhido por publicação — é a tela que abre | aba **SOCIAL** → **Mural** |
| 18 | Agenda de eventos, mais aniversários e tempo de casa calculados sozinhos | aba **SOCIAL** → **Agenda** |
| 19 | Páginas de regras do vôlei de areia em quarteto, regras do grupo e campeonatos, editáveis pela diretoria | aba **SOCIAL** → **Regras** |
| 20 | Mensalidade e diária, extrato pessoal e Pix Copia e Cola | aba **EU**; painel do grupo só para a diretoria |
| 21 | Avaliação anônima entre companheiros de time e painel de evolução com dicas; bolinha verde na aba **EU** quando alguém espera a sua nota | aba **EU** → **Avaliar agora** |
| 22 | Mini tour no primeiro login, terminando na tela de pedir vínculo a um jogador | primeira entrada |

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
   ali, o que aproxima a força total dos elencos.

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
| Aplicação web | React + TypeScript sobre Vite, PWA instalável |
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
│   └── scheduling/          TeamDraft e RoundRobinScheduler
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
│   │   ├── 20260831120300_powersync.sql       publication de replicação
│   │   ├── 20260901120000_genero_dias_desempenho.sql  gênero e histórico do dia
│   │   ├── 20260901130000_presenca_do_dia.sql presença sem time no histórico
│   │   ├── 20260901140000_identidade.sql       vínculo conta-jogador, papéis e ficha
│   │   ├── 20260901150000_presenca_e_avisos.sql confirmação do sábado e push
│   │   ├── 20260901160000_mural_agenda_regras.sql mural, agenda e páginas
│   │   ├── 20260901170000_financeiro.sql       cobranças, pagamentos e Pix
│   │   ├── 20260901180000_avaliacao.sql        avaliação anônima e evolução
│   │   ├── 20260904120000_areia_quarteto_vinculo_e_mural.sql  quarteto, vínculo manual e mural com imagem
│   │   └── 20260904130000_regras_do_volei_unidos.sql  as regras como o grupo joga
│   └── seed.sql                      zera os dados e recria 9 times e 38 jogadores
│   └── functions/
│       └── enviar-avisos/            Edge Function que dispara os lembretes
├── powersync/
│   ├── sync-rules.yaml               o que cada dispositivo baixa
│   └── self-host/                    Docker para o caminho totalmente local
├── web/                              aplicação web (React + Vite)
│   ├── src/domain/                   porte em TypeScript dos algoritmos e dos testes
│   ├── src/data/                     consultas e escritas sobre o SQLite do PowerSync
│   ├── src/lib/                      Supabase, PowerSync e leitura das variáveis
│   ├── src/ui/                       telas e componentes
│   └── .env.example                  modelo das chaves da web
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
- Conta no [Supabase](https://supabase.com) e no [PowerSync](https://powersync.com)
  (os planos gratuitos bastam para dev).
- **Node.js 20 ou mais novo**, só para a aplicação web em [`web/`](web/). Quem for
  mexer apenas no app Android não precisa.
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
5. `supabase/migrations/20260901120000_genero_dias_desempenho.sql`
6. `supabase/migrations/20260901130000_presenca_do_dia.sql`
7. `supabase/migrations/20260901140000_identidade.sql`
8. `supabase/migrations/20260901150000_presenca_e_avisos.sql`
9. `supabase/migrations/20260901160000_mural_agenda_regras.sql`
10. `supabase/migrations/20260901170000_financeiro.sql`
11. `supabase/migrations/20260901180000_avaliacao.sql`
12. `supabase/migrations/20260904120000_areia_quarteto_vinculo_e_mural.sql`
13. `supabase/migrations/20260904130000_regras_do_volei_unidos.sql`

E, se quiser dados de exemplo, `supabase/seed.sql`.

> **Atenção:** o `seed.sql` começa apagando `player_day_stats`, `game_days`,
> `matches`, `rounds`, `team_players`, `players` e `teams`, para você sempre
> reiniciar de um estado limpo. Ele **não** toca em `profiles`, então quem já é
> admin continua admin. Rode só em banco de teste.

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

Depois do primeiro `link`, o ciclo de teste é só `npx supabase@latest db push` a
cada migration nova. Todas as migrations aqui são idempotentes (`if not exists`,
`create or replace`, `drop ... if exists`), então rodar de novo uma que já passou
não quebra nada — útil se você aplicou as primeiras pelo SQL Editor e o histórico
da CLI está vazio.

O `db push` **não** roda o `supabase/seed.sql` em projeto remoto: o seed só entra
no `supabase db reset` do caminho local com Docker. Para semear o projeto remoto,
cole o arquivo no SQL Editor — lembrando que ele apaga os dados do campeonato
antes de recriar (veja o aviso acima).

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
- **areia_quarteto_vinculo_e_mural** — derruba `players.posicao`; deixa o atleta
  escolher o próprio regime, menos a isenção; reduz os tipos de evento a
  `jogo`/`confraternizacao`/`campeonato` (migrando `amistoso` e `outro`); acrescenta
  `posts.imagem_url` e `posts.emoji`; cria o bucket `mural` no Storage com as
  policies de leitura pública e escrita da diretoria; e reescreve a página de regras
  para o vôlei de areia em quarteto.
- **regras_do_volei_unidos** — troca a página de regras pelas regras como o grupo
  realmente joga: set único até 15, bloqueio **não** contando como toque e a regra da
  bola no teto e na corda de aço do ginásio.

> A migration nova **derruba uma coluna** (`players.posicao`). Depois de aplicá-la,
> recarregue o schema na instância do PowerSync (**Deploy sync rules** ou o restart da
> instância) para a replicação parar de enviar a coluna que não existe mais. Os
> clientes já não a declaram no schema local, então o SQLite de cada aparelho se
> ajusta sozinho na primeira abertura.

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
> senão o login falha só naquele build. É o caso do keystore de release — ver
> [Assinatura do release](#assinatura-do-release).

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

1. Em [dashboard.powersync.com](https://dashboard.powersync.com), crie uma
   instância.
2. **Connect to database**: escolha Supabase/Postgres e informe a connection string
   do projeto. Ela fica no botão verde **Connect**, no topo do dashboard do
   Supabase (não em Project Settings). Troque o `[YOUR-PASSWORD]` pela senha do
   banco definida no passo 1.

   O modal mostra três variantes, e a escolha importa:

   | Variante | Porta | Serve? |
   |---|---|---|
   | Direct connection | 5432 | sim |
   | Session pooler | 5432 | sim |
   | Transaction pooler | 6543 | **não** |

   A *transaction pooler* devolve a conexão ao pool a cada statement, e replicação
   lógica precisa de conexão persistente — o PowerSync conecta e não replica nada,
   sem erro visível. Comece pela **Direct connection**; em projetos do plano Free
   ela é só IPv6, então se o PowerSync não alcançar, use a **Session pooler**,
   que é IPv4.
3. Na aba de autenticação da instância, ative a integração com **Supabase Auth**
   (o PowerSync valida o mesmo JWT que o app já usa).
4. Em **Sync Rules**, cole o conteúdo de [`powersync/sync-rules.yaml`](powersync/sync-rules.yaml)
   e faça o **Deploy**.
5. Copie a **instance URL** — vira `POWERSYNC_URL`. Ela fica no botão **Connect**,
   no topo do dashboard do PowerSync: o diálogo traz a URL com botão de copiar.
   Copie exatamente o que aparecer ali; o domínio varia conforme a época em que a
   instância foi criada (`<id>.powersync.journeyapps.com` ou `<id>.powersync.com`).
   Use a URL inteira, com `https://` e sem barra no fim.

> **Uma instância por banco.** A instância PowerSync replica um único banco de
> origem, então dev e prod **não** compartilham instância: se você tem dois
> projetos Supabase, precisa de duas instâncias. Reaproveitar a mesma faz o
> ambiente errado falhar na autenticação, porque a instância valida o JWT contra
> um projeto só.

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
Google Cloud, senão o login com Google falha no build assinado.

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

## Aplicação web

A pasta [`web/`](web/) traz o mesmo app rodando no navegador, para quem usa iPhone e
não tem a versão da Play Store. É um **PWA**: dá para instalar na tela de início e
continuar funcionando com a rede oscilando, porque o PowerSync mantém um SQLite local
no navegador (WebAssembly), igual ao do Android.

O que muda em relação ao app Android:

| | Android | Web |
|---|---|---|
| Login | Credential Manager (token nativo) | Redirecionamento do Supabase (`signInWithOAuth`) |
| Chaves | `local.properties`, por flavor | `.env.development` / `.env.production` |
| Banco local | SQLite nativo | SQLite em WebAssembly, no armazenamento do navegador |
| Distribuição | `.aab` na Play Store | deploy de arquivos estáticos |

O que **não** muda: o schema, as policies de RLS, as sync rules e as regras dos dois
algoritmos. Os algoritmos foram portados para TypeScript junto com os testes, que
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

As três variáveis são as mesmas do `local.properties`, com outro nome:

```properties
VITE_SUPABASE_URL=https://SEU-PROJETO.supabase.co
VITE_SUPABASE_ANON_KEY=...
VITE_POWERSYNC_URL=https://SUA-INSTANCIA.powersync.journeyapps.com
```

Não há `GOOGLE_WEB_CLIENT_ID` aqui: no navegador o login usa o fluxo de
redirecionamento, e quem conhece o client é o painel do Supabase.

> **Cadastre a URL do site no Supabase.** Em **Authentication → URL Configuration**,
> preencha a *Site URL* e acrescente os endereços de desenvolvimento
> (`http://localhost:5173`) à lista de *Redirect URLs*. Sem isso o login volta do
> Google e não encontra para onde ir.

Comandos disponíveis:

```bash
npm run dev         # servidor de desenvolvimento
npm test            # testes dos algoritmos (Vitest)
npm run typecheck   # checagem de tipos
npm run build       # gera dist/ para publicar
npm run preview     # serve o dist/ localmente
```

### Publicando a web

O build gera arquivos estáticos em `web/dist`, então serve qualquer hospedagem de
site estático. **Cloudflare Pages** e **Vercel** têm plano gratuito e fazem deploy a
cada push:

- **Root directory**: `web`
- **Build command**: `npm run build`
- **Output directory**: `dist`
- **Variáveis de ambiente**: as três `VITE_*` de produção

Como o Vite lê as variáveis **no momento do build** e as embute no bundle — do mesmo
jeito que o Gradle embute as chaves no `.aab` —, trocar uma chave exige um novo
deploy, não uma mudança de configuração no servidor.

Depois do primeiro deploy, volte ao Supabase e acrescente o domínio publicado às
*Redirect URLs*, e ao Google Cloud em *Domínios autorizados* se ainda não estiver lá.

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

> A cópia antiga em `docs/`, servida pelo GitHub Pages, continua no ar de propósito:
> a ficha já aprovada na loja aponta para lá enquanto a revisão dos links novos não
> sai. Depois que a revisão aprovar, `docs/` pode ser apagada.

---

## As cinco abas

O app abre no **SOCIAL**, no mural: é a primeira coisa que todo mundo vê, e é onde a
diretoria fala com o grupo. As outras quatro são **JOGOS** (chaveamento e placar),
**CLASSIFICAÇÃO**, **TIMES** e **EU**.

A aba **EU** ganha uma **bolinha verde** no ícone quando tem companheiro de time
esperando a sua nota. É o único aviso desse tipo no app, e ele some sozinho quando a
fila de avaliações zera.

### O primeiro login

Quem entra pela primeira vez não cai direto nas abas: passa por um **mini tour** de
seis telas, uma por aba, terminando no convite para achar o próprio nome na lista de
jogadores. O último botão leva direto para a aba **EU**, já na tela de pedir vínculo,
e ali tem o recado: *"Não encontrou seu nome na lista? Entre em contato com a
diretoria para adicioná-lo aqui!"*.

O tour é marcado como visto no armazenamento local do aparelho — `SharedPreferences`
no Android, `localStorage` na web — e não em `profiles`. É preferência de
dispositivo, não dado do grupo: não vale uma coluna no banco nem uma volta no sync, e
reinstalar o app mostrando o tour de novo é comportamento aceitável.

A lista de **Jogadores**, dentro de TIMES, é a lista única do grupo: todo mundo abre
e vê nome, gênero e aniversário de cada um. O **nível de habilidade em estrelas é só
da diretoria** — o atleta não vê o dos outros nem o próprio, e nem sabe que existe
uma escala. Também são só da diretoria a chave **Presente?**, os filtros
**Todos / Presentes / Ausentes**, os botões de presença em massa, o botão de criar e a
edição. Para o atleta a lista é leitura, com um selo indicando quem já tem acesso ao app.

A mesma régua vale no cartão do time e na janela de detalhes dele: a **força do time**
e o **nível de cada jogador do elenco** aparecem só para quem é diretoria.

> O nível continua descendo para o aparelho de todo mundo pelo sync — a lista de
> jogadores é uma stream só. O que muda é que nenhuma tela do app mostra o nível
> para o atleta. Se um dia isso precisar virar segredo de verdade, o caminho é
> separar `skill_level` numa stream de sync exclusiva da diretoria.

## Quem é quem no app

O app tem **dois papéis**, guardados em `profiles.papel`: `diretoria` e `atleta`.
A diretoria faz tudo que o admin já fazia — jogadores, times, sorteio, placar — e
ganha o financeiro do grupo, o mural, a agenda, as regras e a fila de aprovações. O
atleta confirma a própria presença, edita a própria ficha, vê o próprio extrato e o
próprio painel de evolução.

`is_admin` **continua existindo** e continua sendo o que a RLS lê. Um trigger a
mantém em sincronia com `papel` nos dois sentidos: mudar `papel` atualiza
`is_admin`, e o `update` antigo em `is_admin` atualiza `papel`. Por isso as policies
que já estavam escritas não precisaram de uma linha de alteração, e os dois clientes
seguem lendo a mesma coluna de sempre.

Para promover alguém, depois do primeiro login:

```sql
update public.profiles set papel = 'diretoria' where email = 'voce@gmail.com';
```

### Conta e jogador

`profiles` é quem entrou com o Google; `players` é quem joga. A coluna
`players.profile_id` liga os dois, e é ela que faz o app saber que você é você.

O caminho normal é o atleta abrir a aba **EU**, achar o próprio nome na lista de
jogadores ainda sem dono e tocar em **Sou eu**. Isso grava uma linha em
`vinculo_pedidos` com status `pendente`. Alguém da diretoria abre **Pedidos de
acesso** e confirma; um trigger no Postgres preenche `players.profile_id`.

Esse é o padrão que se repete em todo o app: **a escrita vai para uma tabela
sincronizada e o trigger faz o efeito privilegiado**. As escritas dos dois clientes
sobem pelo PowerSync, que aplica tudo via Postgrest sob a RLS, então não há como
chamar função privilegiada de dentro do app — e fazer pela tabela mantém a aprovação
funcionando offline, como o resto.

O caminho da fila não é o único. Em **EU → Contas e jogadores → Vincular à mão** a
diretoria vê a lista inteira de jogadores e liga qualquer um a qualquer conta que já
tenha entrado no app pelo menos uma vez, sem esperar o atleta achar o próprio nome.
O mesmo botão **desvincula**: a conta perde o acesso à ficha, ao extrato e às
avaliações daquele jogador, e o jogador continua no grupo, agora sem dono.

A lista de contas dessa tela vem do Postgrest na hora (`profiles` só sincroniza o
seu próprio perfil), então ela precisa de internet — é a mesma escolha já feita para
o painel financeiro do grupo. Vincular à mão limpa o pedido pendente daquela conta,
para ninguém ficar preso na fila depois de já estar resolvido.

Se preferir o SQL direto, ele continua valendo:

```sql
update public.players p
set profile_id = f.id
from public.profiles f
where f.email = 'fulano@gmail.com' and p.nome = 'Fulano';
```

O atleta pode editar a própria ficha, mas não tudo: um trigger recusa mudança em
`skill_level`, `genero`, `ativo`, `entrou_em` e `profile_id` para quem não é
diretoria, e recusa entrar ou sair do regime `isento` — isenção é concessão da
diretoria. Nome, foto, aniversário e a escolha entre **Mensalista** e **Diarista** são
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

### Ligando o push

O push é **opcional**: sem as chaves, o app funciona igual e só não registra o
aparelho. Para ligar:

1. Crie um projeto no [Firebase](https://console.firebase.google.com) e adicione um
   app Android com o package name da variant (`com.unidospelovolei.dev` em dev).
2. Copie **Project ID**, **App ID**, **API key** e **Sender ID** para o
   `local.properties`, como mostra o `local.properties.example`. Não é preciso
   `google-services.json`: o app monta o `FirebaseOptions` na mão a partir dessas
   quatro chaves, seguindo o mesmo padrão das outras credenciais do projeto.
3. Gere um par de chaves VAPID para o Web Push (`npx web-push generate-vapid-keys`).
   A **pública** vai em `VITE_VAPID_PUBLIC_KEY` no `.env` da web; a **privada**
   nunca sai do servidor.
4. Publique a Edge Function e cadastre os secrets:

   ```bash
   npx supabase@latest functions deploy enviar-avisos
   npx supabase@latest secrets set VAPID_PUBLIC_KEY=... VAPID_PRIVATE_KEY=...
   npx supabase@latest secrets set FIREBASE_SERVICE_ACCOUNT="$(cat conta-de-servico.json)"
   ```

5. Agende os dois disparos com `pg_cron` e `pg_net`, no SQL Editor. O primeiro
   convida todo mundo na sexta às 19h; o segundo cobra, no sábado às 8h, só quem
   ainda não respondeu:

   ```sql
   select cron.schedule(
       'convite-de-sexta', '0 22 * * 5',
       $$select net.http_post(
           url := 'https://SEU-PROJETO.supabase.co/functions/v1/enviar-avisos',
           headers := '{"Authorization": "Bearer SUA_SERVICE_ROLE_KEY", "Content-Type": "application/json"}'::jsonb,
           body := '{"tipo": "convite"}'::jsonb
       )$$
   );

   select cron.schedule(
       'cobranca-de-sabado', '0 11 * * 6',
       $$select net.http_post(
           url := 'https://SEU-PROJETO.supabase.co/functions/v1/enviar-avisos',
           headers := '{"Authorization": "Bearer SUA_SERVICE_ROLE_KEY", "Content-Type": "application/json"}'::jsonb,
           body := '{"tipo": "cobranca-resposta"}'::jsonb
       )$$
   );
   ```

   Os horários do `cron` estão em **UTC**: 22h e 11h UTC são 19h de sexta e 8h de
   sábado no horário de Brasília.

> **No iPhone o push só chega se a pessoa instalar o site na tela de início.** É
> limitação da Apple: o Safari só entrega Web Push a partir do iOS 16.4 e apenas
> para PWA instalado. Como o público da web é justamente quem tem iPhone, conte com
> isso e mantenha o resumo do sábado indo para o grupo do WhatsApp também.

## Mural, agenda e regras

O mural é da diretoria: só ela publica, o grupo lê e reage. Cada publicação pode
levar uma **imagem** e escolhe o **emoji de reação** do grupo — 👏 é o padrão, mas dá
para trocar por 🔥, 🏐, 🎉 e mais alguns. O emoji fica gravado no post, então o botão
de reagir muda de cara de recado para recado.

As imagens vão para o bucket `mural` do Supabase Storage: leitura pública, escrita só
para a diretoria, 5 MB e JPG/PNG/WEBP/GIF. Elas **não** passam pelo PowerSync — o
upload precisa de internet e a foto é baixada por URL na hora de exibir. É o mesmo
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

O extrato do atleta sincroniza e funciona offline. **O painel com o dinheiro de
todo mundo, não**: ele lê direto do Postgrest quando a diretoria abre. É uma escolha
deliberada — o PowerSync baixa linhas cruas para o aparelho, e sincronizar o extrato
do grupo inteiro deixaria tudo legível no SQLite de qualquer celular da diretoria.
Ler online mantém a RLS sendo a única guardiã e evita ter que colocar o papel dentro
do JWT.

## Avaliação entre colegas

No fim do sábado, cada um dá nota de 1 a 5 aos **três companheiros de time** em seis
fundamentos: saque, passe, ataque, bloqueio, defesa e atitude. Semanas depois, cada
um vê a própria média e uma dica do que treinar no ponto mais fraco.

O anonimato não é só esconder o nome na tela:

- `avaliacoes` **não entra na publication do PowerSync**, e no schema dos clientes é
  declarada como somente-inserção. A nota sobe para o servidor e **nunca desce para
  aparelho nenhum** — nem para o de quem é diretoria.
- A RLS de `avaliacoes` tem policy de `insert` e **nenhuma de `select`**. Ninguém lê
  a nota individual pelo app, em nenhum papel.
- O que sincroniza de volta é `player_evolucao`, mantida por trigger, com médias e
  contagem, filtrada para o próprio dono.
- Há um **mínimo de cinco avaliações** antes de qualquer número aparecer. Com time
  de quatro, quem recebe três notas consegue adivinhar de quem vieram; com cinco,
  vindas de sábados diferentes, não consegue mais.
- `avaliacao_registros` guarda só o *fato* de você ter avaliado alguém, sem a nota,
  para o app saber o que já foi preenchido sem vazar nada.

**A nota dos colegas não mexe no sorteio.** `skill_level` continua sendo a estrela
que a diretoria dá e continua sendo a única coisa que o `TeamDraft` lê. Se a média
dos colegas alimentasse o sorteio, a força dos times mudaria sozinha toda semana e a
nota viraria assunto político dentro do grupo.

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

## Fora do escopo do MVP

Coisas visíveis no protótipo web ou que costumam ser pedidas, mas que **não** estão
aqui porque não constam do escopo descrito:

- **Exportar resultados** (o botão verde de exportar planilha do protótipo).
- Foto na ficha do jogador: a coluna `players.foto_url` existe, mas só o mural usa o
  Supabase Storage por enquanto.
- Múltiplos campeonatos ou temporadas em paralelo: `rounds`, `matches` e
  `standings` continuam assumindo um único campeonato em andamento. A aba de
  campeonatos é conteúdo, não gestão.
- Comentário no mural (só reação), e edição do papel dentro do app (é feita por SQL).
- Baixa automática de Pix: não há integração bancária. A diretoria dá baixa na mão.

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
É a RLS fazendo o trabalho dela: o usuário não é da diretoria. Rode o `update` de
[Quem é quem no app](#quem-é-quem-no-app).

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
