create table if not exists public.posts (
    id                uuid primary key default gen_random_uuid(),
    autor_profile_id  uuid references public.profiles (id) on delete set null,
    autor_nome        text,
    titulo            text not null,
    corpo             text not null default '',
    fixado            boolean     not null default false,
    publicado_em      timestamptz not null default now(),
    atualizado_em     timestamptz not null default now()
);

create index if not exists posts_publicado_em_idx on public.posts (fixado desc, publicado_em desc);

create table if not exists public.post_reacoes (
    id         uuid primary key default gen_random_uuid(),
    post_id    uuid not null references public.posts (id) on delete cascade,
    profile_id uuid not null references public.profiles (id) on delete cascade,
    emoji      text not null default '👏',
    criado_em  timestamptz not null default now(),
    unique (post_id, profile_id)
);

create index if not exists post_reacoes_post_idx on public.post_reacoes (post_id);

create table if not exists public.eventos (
    id         uuid primary key default gen_random_uuid(),
    titulo     text        not null,
    descricao  text,
    tipo       text        not null default 'confraternizacao'
                           check (tipo in ('jogo', 'confraternizacao', 'amistoso', 'campeonato', 'outro')),
    inicio     timestamptz not null,
    fim        timestamptz,
    local      text,
    criado_por uuid references public.profiles (id) on delete set null,
    criado_em  timestamptz not null default now()
);

create index if not exists eventos_inicio_idx on public.eventos (inicio);

create table if not exists public.paginas (
    id             uuid primary key default gen_random_uuid(),
    slug           text not null unique,
    categoria      text not null default 'grupo'
                        check (categoria in ('volei', 'grupo', 'campeonato')),
    titulo         text not null,
    corpo          text not null default '',
    ordem          integer     not null default 0,
    atualizado_por uuid references public.profiles (id) on delete set null,
    atualizado_em  timestamptz not null default now()
);

create index if not exists paginas_categoria_idx on public.paginas (categoria, ordem);

drop trigger if exists posts_touch_updated_at on public.posts;
create trigger posts_touch_updated_at
    before update on public.posts
    for each row execute function public.touch_updated_at();

alter table public.posts        enable row level security;
alter table public.post_reacoes enable row level security;
alter table public.eventos      enable row level security;
alter table public.paginas      enable row level security;

do $$
declare
    t text;
begin
    foreach t in array array['posts', 'eventos', 'paginas']
    loop
        execute format('drop policy if exists %I on public.%I', t || '_select_authenticated', t);
        execute format(
            'create policy %I on public.%I for select to authenticated using (true)',
            t || '_select_authenticated', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_insert_admin', t);
        execute format(
            'create policy %I on public.%I for insert to authenticated with check (public.is_admin())',
            t || '_insert_admin', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_update_admin', t);
        execute format(
            'create policy %I on public.%I for update to authenticated using (public.is_admin()) with check (public.is_admin())',
            t || '_update_admin', t
        );

        execute format('drop policy if exists %I on public.%I', t || '_delete_admin', t);
        execute format(
            'create policy %I on public.%I for delete to authenticated using (public.is_admin())',
            t || '_delete_admin', t
        );
    end loop;
end;
$$;

drop policy if exists post_reacoes_select_authenticated on public.post_reacoes;
create policy post_reacoes_select_authenticated on public.post_reacoes
    for select to authenticated using (true);

drop policy if exists post_reacoes_insert_dono on public.post_reacoes;
create policy post_reacoes_insert_dono on public.post_reacoes
    for insert to authenticated with check (profile_id = auth.uid());

drop policy if exists post_reacoes_delete_dono on public.post_reacoes;
create policy post_reacoes_delete_dono on public.post_reacoes
    for delete to authenticated
    using (public.is_admin() or profile_id = auth.uid());

insert into public.paginas (slug, categoria, titulo, corpo, ordem)
values
    (
        'regras-do-volei',
        'volei',
        'Regras do vôlei',
        E'# Como se joga\n\nSeis jogadores de cada lado, três na frente e três atrás. Ganha o ponto quem faz a bola tocar a quadra adversária, ou quando o adversário erra.\n\n# Toques\n\n- Cada time tem no máximo **três toques** antes de devolver a bola.\n- O bloqueio não conta como toque.\n- O mesmo jogador não pode tocar duas vezes seguidas, salvo depois do bloqueio.\n\n# Contagem\n\n- O set vai até **25 pontos**, com dois de vantagem.\n- O rodízio acontece toda vez que o time recupera o saque.\n\n# Faltas mais comuns\n\n- Encostar na rede durante a jogada.\n- Invadir a quadra adversária por baixo da rede.\n- Segurar ou conduzir a bola em vez de bater.\n- Atacar a bola de saque perto da rede.',
        1
    ),
    (
        'regras-do-grupo',
        'grupo',
        'Regras do grupo',
        E'# Combinados do sábado\n\nA diretoria edita esta página direto no app. O que estiver escrito aqui vale para todo mundo.\n\n- **Confirme a presença** até a sexta à noite, para a gente montar os times certos.\n- **Chegue no horário.** Quem chega depois do sorteio entra na próxima rodada.\n- **Respeito acima de tudo.** O nível de todo mundo é diferente e o sábado é de todo mundo.\n\n# Mensalidade e diária\n\nA regra de cobrança fica na aba do financeiro, e cada um vê o próprio extrato.',
        1
    ),
    (
        'campeonatos',
        'campeonato',
        'Campeonatos',
        E'# Regulamento\n\nEsta página está esperando o regulamento do próximo campeonato. Quem é da diretoria pode editar aqui mesmo, pelo lápis no canto.',
        1
    )
on conflict (slug) do nothing;

grant select, insert, update, delete on
    public.posts, public.post_reacoes, public.eventos, public.paginas
    to authenticated;

drop publication if exists powersync;

create publication powersync for table
    public.profiles,
    public.players,
    public.teams,
    public.team_players,
    public.rounds,
    public.matches,
    public.game_days,
    public.player_day_stats,
    public.vinculo_pedidos,
    public.player_contatos,
    public.config_grupo,
    public.presencas,
    public.dispositivos,
    public.avisos,
    public.posts,
    public.post_reacoes,
    public.eventos,
    public.paginas;
