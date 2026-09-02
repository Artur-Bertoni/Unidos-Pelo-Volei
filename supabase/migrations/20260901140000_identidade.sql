alter table public.profiles
    add column if not exists papel text not null default 'atleta';

update public.profiles set papel = 'diretoria' where is_admin and papel <> 'diretoria';

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_papel_valido') then
        alter table public.profiles
            add constraint profiles_papel_valido check (papel in ('diretoria', 'atleta'));
    end if;
end;
$$;

create or replace function public.sincronizar_papel()
    returns trigger
    language plpgsql
as $$
begin
    if tg_op = 'UPDATE'
        and new.is_admin is distinct from old.is_admin
        and new.papel is not distinct from old.papel
    then
        new.papel := case when new.is_admin then 'diretoria' else 'atleta' end;
    end if;

    new.is_admin := (new.papel = 'diretoria');
    return new;
end;
$$;

drop trigger if exists profiles_sincronizar_papel on public.profiles;
create trigger profiles_sincronizar_papel
    before insert or update on public.profiles
    for each row execute function public.sincronizar_papel();

alter table public.players
    add column if not exists profile_id     uuid references public.profiles (id) on delete set null,
    add column if not exists posicao        text,
    add column if not exists foto_url       text,
    add column if not exists nascimento_dia integer,
    add column if not exists nascimento_mes integer,
    add column if not exists entrou_em      date;

create unique index if not exists players_profile_id_unico
    on public.players (profile_id)
    where profile_id is not null;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'players_nascimento_valido') then
        alter table public.players
            add constraint players_nascimento_valido check (
                (nascimento_dia is null and nascimento_mes is null)
                or (nascimento_dia between 1 and 31 and nascimento_mes between 1 and 12)
            );
    end if;

    if not exists (select 1 from pg_constraint where conname = 'players_posicao_valida') then
        alter table public.players
            add constraint players_posicao_valida check (
                posicao is null
                or posicao in ('levantador', 'ponteiro', 'oposto', 'central', 'libero')
            );
    end if;
end;
$$;

create table if not exists public.vinculo_pedidos (
    id           uuid primary key default gen_random_uuid(),
    profile_id   uuid not null references public.profiles (id) on delete cascade,
    player_id    uuid not null references public.players (id) on delete cascade,
    profile_nome text,
    status       text        not null default 'pendente'
                             check (status in ('pendente', 'aprovado', 'recusado')),
    criado_em    timestamptz not null default now(),
    decidido_por uuid references public.profiles (id) on delete set null,
    decidido_em  timestamptz
);

create unique index if not exists vinculo_pedidos_um_pendente
    on public.vinculo_pedidos (profile_id)
    where status = 'pendente';

create index if not exists vinculo_pedidos_status_idx
    on public.vinculo_pedidos (status, criado_em);

create table if not exists public.player_contatos (
    id                 uuid primary key default gen_random_uuid(),
    player_id          uuid not null unique references public.players (id) on delete cascade,
    profile_id         uuid references public.profiles (id) on delete set null,
    telefone           text,
    contato_emergencia text,
    nascimento_ano     integer,
    atualizado_em      timestamptz not null default now()
);

create index if not exists player_contatos_profile_idx on public.player_contatos (profile_id);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'player_contatos_ano_valido') then
        alter table public.player_contatos
            add constraint player_contatos_ano_valido check (
                nascimento_ano is null or nascimento_ano between 1900 and 2200
            );
    end if;
end;
$$;

create or replace function public.completar_pedido_de_vinculo()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if new.profile_nome is null then
        select coalesce(p.nome, p.email)
        into new.profile_nome
        from public.profiles p
        where p.id = new.profile_id;
    end if;

    if tg_op = 'UPDATE'
        and new.status is distinct from old.status
        and new.status <> 'pendente'
    then
        new.decidido_por := coalesce(new.decidido_por, auth.uid());
        new.decidido_em := coalesce(new.decidido_em, now());
    end if;

    return new;
end;
$$;

drop trigger if exists vinculo_pedidos_completar on public.vinculo_pedidos;
create trigger vinculo_pedidos_completar
    before insert or update on public.vinculo_pedidos
    for each row execute function public.completar_pedido_de_vinculo();

create or replace function public.aplicar_vinculo()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if new.status = 'aprovado'
        and (tg_op = 'INSERT' or old.status is distinct from 'aprovado')
    then
        update public.players
        set profile_id = null, updated_at = now()
        where profile_id = new.profile_id and id <> new.player_id;

        update public.players
        set profile_id = new.profile_id, updated_at = now()
        where id = new.player_id;
    end if;

    return new;
end;
$$;

drop trigger if exists vinculo_pedidos_aplicar on public.vinculo_pedidos;
create trigger vinculo_pedidos_aplicar
    after insert or update on public.vinculo_pedidos
    for each row execute function public.aplicar_vinculo();

create or replace function public.proteger_ficha_do_jogador()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if auth.uid() is null or public.is_admin() then
        return new;
    end if;

    if new.skill_level is distinct from old.skill_level
        or new.genero is distinct from old.genero
        or new.ativo is distinct from old.ativo
        or new.entrou_em is distinct from old.entrou_em
        or new.profile_id is distinct from old.profile_id
    then
        raise exception 'Nivel, genero, presenca, entrada e vinculo so a diretoria altera.';
    end if;

    return new;
end;
$$;

drop trigger if exists players_proteger_ficha on public.players;
create trigger players_proteger_ficha
    before update on public.players
    for each row execute function public.proteger_ficha_do_jogador();

create or replace function public.espelhar_dono_do_contato()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    update public.player_contatos
    set profile_id = new.profile_id
    where player_id = new.id and profile_id is distinct from new.profile_id;

    return new;
end;
$$;

drop trigger if exists players_espelhar_contato on public.players;
create trigger players_espelhar_contato
    after update of profile_id on public.players
    for each row execute function public.espelhar_dono_do_contato();

create or replace function public.completar_contato()
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

drop trigger if exists player_contatos_completar on public.player_contatos;
create trigger player_contatos_completar
    before insert or update on public.player_contatos
    for each row execute function public.completar_contato();

alter table public.vinculo_pedidos enable row level security;
alter table public.player_contatos enable row level security;

drop policy if exists players_update_dono on public.players;
create policy players_update_dono on public.players
    for update to authenticated
    using (profile_id = auth.uid())
    with check (profile_id = auth.uid());

drop policy if exists vinculo_pedidos_select_authenticated on public.vinculo_pedidos;
create policy vinculo_pedidos_select_authenticated on public.vinculo_pedidos
    for select to authenticated
    using (true);

drop policy if exists vinculo_pedidos_insert_proprio on public.vinculo_pedidos;
create policy vinculo_pedidos_insert_proprio on public.vinculo_pedidos
    for insert to authenticated
    with check (profile_id = auth.uid() and status = 'pendente');

drop policy if exists vinculo_pedidos_insert_admin on public.vinculo_pedidos;
create policy vinculo_pedidos_insert_admin on public.vinculo_pedidos
    for insert to authenticated
    with check (public.is_admin());

drop policy if exists vinculo_pedidos_update_admin on public.vinculo_pedidos;
create policy vinculo_pedidos_update_admin on public.vinculo_pedidos
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists vinculo_pedidos_delete_proprio on public.vinculo_pedidos;
create policy vinculo_pedidos_delete_proprio on public.vinculo_pedidos
    for delete to authenticated
    using (public.is_admin() or (profile_id = auth.uid() and status = 'pendente'));

drop policy if exists player_contatos_select_dono on public.player_contatos;
create policy player_contatos_select_dono on public.player_contatos
    for select to authenticated
    using (public.is_admin() or profile_id = auth.uid());

drop policy if exists player_contatos_insert_dono on public.player_contatos;
create policy player_contatos_insert_dono on public.player_contatos
    for insert to authenticated
    with check (public.is_admin() or profile_id = auth.uid());

drop policy if exists player_contatos_update_dono on public.player_contatos;
create policy player_contatos_update_dono on public.player_contatos
    for update to authenticated
    using (public.is_admin() or profile_id = auth.uid())
    with check (public.is_admin() or profile_id = auth.uid());

drop policy if exists player_contatos_delete_admin on public.player_contatos;
create policy player_contatos_delete_admin on public.player_contatos
    for delete to authenticated
    using (public.is_admin());

grant select, insert, update, delete on
    public.vinculo_pedidos, public.player_contatos
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
    public.player_contatos;
