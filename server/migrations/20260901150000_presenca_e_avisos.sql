create table if not exists public.config_grupo (
    id            uuid primary key default gen_random_uuid(),
    jogo_hora     text        not null default '09:00',
    jogo_local    text,
    atualizado_em timestamptz not null default now()
);

insert into public.config_grupo (jogo_hora)
select '09:00'
where not exists (select 1 from public.config_grupo);

create table if not exists public.presencas (
    id            uuid primary key default gen_random_uuid(),
    player_id     uuid not null references public.players (id) on delete cascade,
    profile_id    uuid references public.profiles (id) on delete set null,
    data          date not null,
    status        text not null default 'vou' check (status in ('vou', 'nao_vou', 'talvez')),
    origem        text not null default 'atleta' check (origem in ('atleta', 'diretoria')),
    registrado_por uuid references public.profiles (id) on delete set null,
    atualizado_em timestamptz not null default now(),
    unique (player_id, data)
);

create index if not exists presencas_data_idx on public.presencas (data desc);
create index if not exists presencas_player_idx on public.presencas (player_id);

create table if not exists public.dispositivos (
    id         uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    token      text not null unique,
    plataforma text not null check (plataforma in ('android', 'web')),
    ativo      boolean     not null default true,
    visto_em   timestamptz not null default now(),
    criado_em  timestamptz not null default now()
);

create index if not exists dispositivos_profile_idx on public.dispositivos (profile_id, ativo);

create table if not exists public.avisos (
    id        uuid primary key default gen_random_uuid(),
    tipo      text not null default 'lembrete'
                   check (tipo in ('lembrete', 'cobranca', 'mural', 'avaliacao')),
    titulo    text not null,
    corpo     text,
    referencia text,
    criado_em timestamptz not null default now()
);

create index if not exists avisos_criado_em_idx on public.avisos (criado_em desc);

create or replace function public.completar_presenca()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if new.profile_id is null then
        select p.profile_id into new.profile_id
        from public.players p
        where p.id = new.player_id;
    end if;

    new.atualizado_em := now();
    return new;
end;
$$;

drop trigger if exists presencas_completar on public.presencas;
create trigger presencas_completar
    before insert or update on public.presencas
    for each row execute function public.completar_presenca();

create or replace function public.sou_o_jogador(alvo uuid)
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
select exists (
    select 1 from public.players p
    where p.id = alvo and p.profile_id = auth.uid()
);
$$;

revoke execute on function public.sou_o_jogador(uuid) from public;
grant execute on function public.sou_o_jogador(uuid) to authenticated;

alter table public.config_grupo enable row level security;
alter table public.presencas    enable row level security;
alter table public.dispositivos enable row level security;
alter table public.avisos       enable row level security;

drop policy if exists config_grupo_select_authenticated on public.config_grupo;
create policy config_grupo_select_authenticated on public.config_grupo
    for select to authenticated using (true);

drop policy if exists config_grupo_update_admin on public.config_grupo;
create policy config_grupo_update_admin on public.config_grupo
    for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

drop policy if exists presencas_select_authenticated on public.presencas;
create policy presencas_select_authenticated on public.presencas
    for select to authenticated using (true);

drop policy if exists presencas_insert_dono on public.presencas;
create policy presencas_insert_dono on public.presencas
    for insert to authenticated
    with check (public.is_admin() or public.sou_o_jogador(player_id));

drop policy if exists presencas_update_dono on public.presencas;
create policy presencas_update_dono on public.presencas
    for update to authenticated
    using (public.is_admin() or public.sou_o_jogador(player_id))
    with check (public.is_admin() or public.sou_o_jogador(player_id));

drop policy if exists presencas_delete_dono on public.presencas;
create policy presencas_delete_dono on public.presencas
    for delete to authenticated
    using (public.is_admin() or public.sou_o_jogador(player_id));

drop policy if exists dispositivos_select_dono on public.dispositivos;
create policy dispositivos_select_dono on public.dispositivos
    for select to authenticated using (profile_id = auth.uid());

drop policy if exists dispositivos_insert_dono on public.dispositivos;
create policy dispositivos_insert_dono on public.dispositivos
    for insert to authenticated with check (profile_id = auth.uid());

drop policy if exists dispositivos_update_dono on public.dispositivos;
create policy dispositivos_update_dono on public.dispositivos
    for update to authenticated
    using (profile_id = auth.uid()) with check (profile_id = auth.uid());

drop policy if exists dispositivos_delete_dono on public.dispositivos;
create policy dispositivos_delete_dono on public.dispositivos
    for delete to authenticated using (profile_id = auth.uid());

drop policy if exists avisos_select_authenticated on public.avisos;
create policy avisos_select_authenticated on public.avisos
    for select to authenticated using (true);

drop policy if exists avisos_insert_admin on public.avisos;
create policy avisos_insert_admin on public.avisos
    for insert to authenticated with check (public.is_admin());

drop policy if exists avisos_delete_admin on public.avisos;
create policy avisos_delete_admin on public.avisos
    for delete to authenticated using (public.is_admin());

grant select, insert, update, delete on
    public.config_grupo, public.presencas, public.dispositivos, public.avisos
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
    public.avisos;
