-- =============================================================================
-- Unidos Pelo Volei - schema base
-- =============================================================================
-- Convencoes adotadas para funcionar com o PowerSync:
--   * toda tabela sincronizada tem uma unica coluna `id` do tipo uuid como
--     chave primaria (o PowerSync nao suporta chave composta);
--   * booleanos chegam no SQLite do dispositivo como 0/1 e timestamps como
--     texto ISO-8601.
-- =============================================================================

create extension if not exists pgcrypto;

-- -----------------------------------------------------------------------------
-- profiles: papel do usuario autenticado (quem pode escrever)
-- -----------------------------------------------------------------------------
create table if not exists public.profiles (
    id         uuid primary key references auth.users (id) on delete cascade,
    email      text,
    nome       text,
    is_admin   boolean     not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table public.profiles is
    'Perfil do usuario logado. is_admin = true libera escrita nas demais tabelas via RLS.';

-- Cria o profile automaticamente no primeiro login (inclusive login Google).
create or replace function public.handle_new_user()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    insert into public.profiles (id, email, nome)
    values (
        new.id,
        new.email,
        coalesce(
            new.raw_user_meta_data ->> 'full_name',
            new.raw_user_meta_data ->> 'name',
            new.email
        )
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row
execute function public.handle_new_user();

-- -----------------------------------------------------------------------------
-- players
-- -----------------------------------------------------------------------------
create table if not exists public.players (
    id          uuid primary key default gen_random_uuid(),
    nome        text        not null,
    skill_level integer     not null default 3 check (skill_level between 1 and 5),
    ativo       boolean     not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index if not exists players_ativo_idx on public.players (ativo);

-- -----------------------------------------------------------------------------
-- teams
-- -----------------------------------------------------------------------------
create table if not exists public.teams (
    id         uuid primary key default gen_random_uuid(),
    nome       text        not null,
    cor_hex    text        not null check (cor_hex ~ '^#[0-9A-Fa-f]{6}$'),
    sigla      text        not null check (char_length(sigla) = 2),
    ativo      boolean     not null default true,
    ordem      integer     not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on column public.teams.ordem is
    'Ordem fixa usada pelo metodo do circulo ao gerar o chaveamento.';

-- -----------------------------------------------------------------------------
-- team_players: composicao atual de cada time
-- -----------------------------------------------------------------------------
-- A chave logica e (team_id, player_id), garantida por UNIQUE. A coluna `id`
-- existe porque o PowerSync exige chave primaria simples.
create table if not exists public.team_players (
    id        uuid primary key default gen_random_uuid(),
    team_id   uuid not null references public.teams (id) on delete cascade,
    player_id uuid not null references public.players (id) on delete cascade,
    unique (team_id, player_id)
);

create index if not exists team_players_team_idx on public.team_players (team_id);
create index if not exists team_players_player_idx on public.team_players (player_id);

-- -----------------------------------------------------------------------------
-- rounds
-- -----------------------------------------------------------------------------
create table if not exists public.rounds (
    id         uuid primary key default gen_random_uuid(),
    numero     integer     not null unique,
    fase       integer     not null default 1,
    created_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- matches
-- -----------------------------------------------------------------------------
create table if not exists public.matches (
    id         uuid primary key default gen_random_uuid(),
    round_id   uuid    not null references public.rounds (id) on delete cascade,
    quadra     integer not null check (quadra >= 1),
    team_a_id  uuid    not null references public.teams (id) on delete cascade,
    team_b_id  uuid    not null references public.teams (id) on delete cascade,
    score_a    integer not null default 0 check (score_a >= 0),
    score_b    integer not null default 0 check (score_b >= 0),
    status     text    not null default 'agendado' check (status in ('agendado', 'finalizado')),
    winner_id  uuid references public.teams (id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint matches_times_diferentes check (team_a_id <> team_b_id),
    unique (round_id, quadra)
);

create index if not exists matches_round_idx on public.matches (round_id);
create index if not exists matches_team_a_idx on public.matches (team_a_id);
create index if not exists matches_team_b_idx on public.matches (team_b_id);

-- -----------------------------------------------------------------------------
-- updated_at automatico
-- -----------------------------------------------------------------------------
create or replace function public.touch_updated_at()
    returns trigger
    language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
    before update on public.profiles
    for each row execute function public.touch_updated_at();

drop trigger if exists players_touch_updated_at on public.players;
create trigger players_touch_updated_at
    before update on public.players
    for each row execute function public.touch_updated_at();

drop trigger if exists teams_touch_updated_at on public.teams;
create trigger teams_touch_updated_at
    before update on public.teams
    for each row execute function public.touch_updated_at();

drop trigger if exists matches_touch_updated_at on public.matches;
create trigger matches_touch_updated_at
    before update on public.matches
    for each row execute function public.touch_updated_at();
