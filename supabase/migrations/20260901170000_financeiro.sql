alter table public.players
    add column if not exists regime text not null default 'mensalista';

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'players_regime_valido') then
        alter table public.players
            add constraint players_regime_valido check (regime in ('mensalista', 'diarista', 'isento'));
    end if;
end;
$$;

create table if not exists public.config_financeiro (
    id                    uuid primary key default gen_random_uuid(),
    pix_chave             text,
    pix_nome              text,
    pix_cidade            text,
    mensalidade_centavos  integer not null default 0 check (mensalidade_centavos >= 0),
    diaria_centavos       integer not null default 0 check (diaria_centavos >= 0),
    atualizado_em         timestamptz not null default now()
);

insert into public.config_financeiro (mensalidade_centavos, diaria_centavos)
select 0, 0
where not exists (select 1 from public.config_financeiro);

create table if not exists public.cobrancas (
    id             uuid primary key default gen_random_uuid(),
    titulo         text    not null,
    tipo           text    not null default 'mensalidade'
                           check (tipo in ('mensalidade', 'diaria', 'avulsa')),
    valor_centavos integer not null check (valor_centavos >= 0),
    competencia    date,
    vence_em       date,
    criado_por     uuid references public.profiles (id) on delete set null,
    criado_em      timestamptz not null default now()
);

create index if not exists cobrancas_competencia_idx on public.cobrancas (competencia desc, criado_em desc);

create table if not exists public.pagamentos (
    id              uuid primary key default gen_random_uuid(),
    cobranca_id     uuid    not null references public.cobrancas (id) on delete cascade,
    player_id       uuid    not null references public.players (id) on delete cascade,
    profile_id      uuid references public.profiles (id) on delete set null,
    valor_centavos  integer not null check (valor_centavos >= 0),
    status          text    not null default 'pendente'
                            check (status in ('pendente', 'pago', 'isento')),
    pago_em         timestamptz,
    registrado_por  uuid references public.profiles (id) on delete set null,
    observacao      text,
    criado_em       timestamptz not null default now(),
    unique (cobranca_id, player_id)
);

create index if not exists pagamentos_player_idx on public.pagamentos (player_id, status);
create index if not exists pagamentos_profile_idx on public.pagamentos (profile_id);

create or replace function public.completar_pagamento()
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

    if new.status = 'pago' and new.pago_em is null then
        new.pago_em := now();
    end if;

    if new.status <> 'pago' then
        new.pago_em := null;
    end if;

    return new;
end;
$$;

drop trigger if exists pagamentos_completar on public.pagamentos;
create trigger pagamentos_completar
    before insert or update on public.pagamentos
    for each row execute function public.completar_pagamento();

create or replace function public.espelhar_dono_dos_pagamentos()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    update public.pagamentos
    set profile_id = new.profile_id
    where player_id = new.id and profile_id is distinct from new.profile_id;

    return new;
end;
$$;

drop trigger if exists players_espelhar_pagamentos on public.players;
create trigger players_espelhar_pagamentos
    after update of profile_id on public.players
    for each row execute function public.espelhar_dono_dos_pagamentos();

alter table public.config_financeiro enable row level security;
alter table public.cobrancas         enable row level security;
alter table public.pagamentos        enable row level security;

drop policy if exists config_financeiro_select_authenticated on public.config_financeiro;
create policy config_financeiro_select_authenticated on public.config_financeiro
    for select to authenticated using (true);

drop policy if exists config_financeiro_update_admin on public.config_financeiro;
create policy config_financeiro_update_admin on public.config_financeiro
    for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

drop policy if exists cobrancas_select_authenticated on public.cobrancas;
create policy cobrancas_select_authenticated on public.cobrancas
    for select to authenticated using (true);

drop policy if exists cobrancas_insert_admin on public.cobrancas;
create policy cobrancas_insert_admin on public.cobrancas
    for insert to authenticated with check (public.is_admin());

drop policy if exists cobrancas_update_admin on public.cobrancas;
create policy cobrancas_update_admin on public.cobrancas
    for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

drop policy if exists cobrancas_delete_admin on public.cobrancas;
create policy cobrancas_delete_admin on public.cobrancas
    for delete to authenticated using (public.is_admin());

drop policy if exists pagamentos_select_dono on public.pagamentos;
create policy pagamentos_select_dono on public.pagamentos
    for select to authenticated
    using (public.is_admin() or profile_id = auth.uid());

drop policy if exists pagamentos_insert_admin on public.pagamentos;
create policy pagamentos_insert_admin on public.pagamentos
    for insert to authenticated with check (public.is_admin());

drop policy if exists pagamentos_update_admin on public.pagamentos;
create policy pagamentos_update_admin on public.pagamentos
    for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

drop policy if exists pagamentos_delete_admin on public.pagamentos;
create policy pagamentos_delete_admin on public.pagamentos
    for delete to authenticated using (public.is_admin());

grant select, insert, update, delete on
    public.config_financeiro, public.cobrancas, public.pagamentos
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
    public.paginas,
    public.config_financeiro,
    public.cobrancas,
    public.pagamentos;
