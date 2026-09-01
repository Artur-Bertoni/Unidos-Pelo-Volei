alter table public.players
    add column if not exists genero text not null default 'masculino';

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'players_genero_valido'
    ) then
        alter table public.players
            add constraint players_genero_valido check (genero in ('masculino', 'feminino'));
    end if;
end;
$$;

create table if not exists public.game_days (
    id           uuid primary key default gen_random_uuid(),
    encerrado_em timestamptz not null default now(),
    partidas     integer     not null default 0,
    created_at   timestamptz not null default now()
);

create index if not exists game_days_encerrado_em_idx on public.game_days (encerrado_em desc);

create table if not exists public.player_day_stats (
    id            uuid    primary key default gen_random_uuid(),
    day_id        uuid    not null references public.game_days (id) on delete cascade,
    player_id     uuid    not null references public.players (id) on delete cascade,
    team_id       uuid    not null,
    team_nome     text,
    team_cor_hex  text,
    jogos         integer not null default 0,
    vitorias      integer not null default 0,
    derrotas      integer not null default 0,
    pontos_pro    integer not null default 0,
    pontos_contra integer not null default 0,
    created_at    timestamptz not null default now(),
    unique (day_id, player_id)
);

create index if not exists player_day_stats_day_idx    on public.player_day_stats (day_id);
create index if not exists player_day_stats_player_idx on public.player_day_stats (player_id);
create index if not exists player_day_stats_team_idx   on public.player_day_stats (day_id, team_id);

create or replace view public.player_performance as
select
    p.id                                                  as player_id,
    p.nome,
    p.genero,
    p.skill_level,
    coalesce(count(s.id), 0)::int                         as dias,
    coalesce(sum(s.jogos), 0)::int                        as jogos,
    coalesce(sum(s.vitorias), 0)::int                     as vitorias,
    coalesce(sum(s.derrotas), 0)::int                     as derrotas,
    coalesce(sum(s.pontos_pro), 0)::int                   as pontos_pro,
    coalesce(sum(s.pontos_contra), 0)::int                as pontos_contra,
    coalesce(sum(s.pontos_pro - s.pontos_contra), 0)::int as saldo_pontos
from public.players p
left join public.player_day_stats s on s.player_id = p.id
group by p.id, p.nome, p.genero, p.skill_level
order by vitorias desc, saldo_pontos desc, p.nome;

alter table public.game_days        enable row level security;
alter table public.player_day_stats enable row level security;

do $$
declare
    t text;
begin
    foreach t in array array['game_days', 'player_day_stats']
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

grant select, insert, update, delete on
    public.game_days, public.player_day_stats
    to authenticated;
grant select on public.player_performance to authenticated;

alter view public.player_performance set (security_invoker = true);

drop publication if exists powersync;

create publication powersync for table
    public.profiles,
    public.players,
    public.teams,
    public.team_players,
    public.rounds,
    public.matches,
    public.game_days,
    public.player_day_stats;
