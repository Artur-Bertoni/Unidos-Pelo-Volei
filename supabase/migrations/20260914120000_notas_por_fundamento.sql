alter table public.players add column if not exists nota_saque    numeric(3, 2);
alter table public.players add column if not exists nota_passe    numeric(3, 2);
alter table public.players add column if not exists nota_ataque   numeric(3, 2);
alter table public.players add column if not exists nota_bloqueio numeric(3, 2);
alter table public.players add column if not exists nota_defesa   numeric(3, 2);
alter table public.players add column if not exists nota_atitude  numeric(3, 2);
alter table public.players add column if not exists nota_media    numeric(3, 2);

update public.players
set nota_saque    = coalesce(nota_saque, skill_level),
    nota_passe    = coalesce(nota_passe, skill_level),
    nota_ataque   = coalesce(nota_ataque, skill_level),
    nota_bloqueio = coalesce(nota_bloqueio, skill_level),
    nota_defesa   = coalesce(nota_defesa, skill_level),
    nota_atitude  = coalesce(nota_atitude, skill_level),
    nota_media    = coalesce(nota_media, skill_level)
where nota_saque is null
   or nota_passe is null
   or nota_ataque is null
   or nota_bloqueio is null
   or nota_defesa is null
   or nota_atitude is null
   or nota_media is null;

alter table public.players
    alter column nota_saque    set default 3.00,
    alter column nota_passe    set default 3.00,
    alter column nota_ataque   set default 3.00,
    alter column nota_bloqueio set default 3.00,
    alter column nota_defesa   set default 3.00,
    alter column nota_atitude  set default 3.00,
    alter column nota_media    set default 3.00,
    alter column nota_saque    set not null,
    alter column nota_passe    set not null,
    alter column nota_ataque   set not null,
    alter column nota_bloqueio set not null,
    alter column nota_defesa   set not null,
    alter column nota_atitude  set not null,
    alter column nota_media    set not null;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'players_notas_entre_1_e_5') then
        alter table public.players add constraint players_notas_entre_1_e_5 check (
            nota_saque between 1 and 5
            and nota_passe between 1 and 5
            and nota_ataque between 1 and 5
            and nota_bloqueio between 1 and 5
            and nota_defesa between 1 and 5
            and nota_atitude between 1 and 5
        );
    end if;
end;
$$;

create index if not exists players_nota_media_idx on public.players (grupo_id, nota_media desc);

create table if not exists public.player_nota_historico (
    id            uuid primary key default gen_random_uuid(),
    grupo_id      uuid not null references public.grupos (id) on delete cascade,
    player_id     uuid not null references public.players (id) on delete cascade,
    profile_id    uuid references public.profiles (id) on delete set null,
    day_id        uuid references public.game_days (id) on delete cascade,
    origem        text not null check (origem in ('diretoria', 'avaliacao')),
    avaliadores   smallint not null default 0,
    nota_saque    numeric(3, 2) not null,
    nota_passe    numeric(3, 2) not null,
    nota_ataque   numeric(3, 2) not null,
    nota_bloqueio numeric(3, 2) not null,
    nota_defesa   numeric(3, 2) not null,
    nota_atitude  numeric(3, 2) not null,
    media         numeric(3, 2) not null,
    registrado_em timestamptz not null default now()
);

create index if not exists player_nota_historico_linha_idx
    on public.player_nota_historico (player_id, registrado_em);

create index if not exists player_nota_historico_profile_idx
    on public.player_nota_historico (profile_id);

create index if not exists player_nota_historico_grupo_idx
    on public.player_nota_historico (grupo_id);

create unique index if not exists player_nota_historico_dia_idx
    on public.player_nota_historico (player_id, day_id)
    where day_id is not null;

create or replace function public.peso_da_avaliacao()
    returns numeric
    language sql
    immutable
as $$
select 0.15::numeric;
$$;

create or replace function public.limite_por_dia()
    returns numeric
    language sql
    immutable
as $$
select 0.25::numeric;
$$;

create or replace function public.minimo_de_avaliadores()
    returns integer
    language sql
    immutable
as $$
select 2;
$$;

create or replace function public.passo_da_nota(
    atual        numeric,
    media_do_dia numeric
)
    returns numeric
    language sql
    immutable
as $$
select round(
    greatest(1.00::numeric, least(5.00::numeric,
        atual + greatest(
            -public.limite_por_dia(),
            least(public.limite_por_dia(), public.peso_da_avaliacao() * (media_do_dia - atual))
        )
    )), 2);
$$;

create or replace function public.calcular_media_do_jogador()
    returns trigger
    language plpgsql
as $$
begin
    new.nota_media := round(
        (new.nota_saque + new.nota_passe + new.nota_ataque
            + new.nota_bloqueio + new.nota_defesa + new.nota_atitude) / 6.0, 2
    );
    new.skill_level := greatest(1, least(5, round(new.nota_media)::integer));
    return new;
end;
$$;

drop trigger if exists players_calcular_media on public.players;
create trigger players_calcular_media
    before insert or update on public.players
    for each row execute function public.calcular_media_do_jogador();

create or replace function public.proteger_ficha_do_jogador()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    notas_mudaram boolean;
begin
    if new.grupo_id is distinct from old.grupo_id then
        raise exception 'Um jogador nao muda de grupo.';
    end if;

    if auth.uid() is null
        or coalesce(current_setting('app.recalculo', true), 'off') = 'on'
        or public.e_diretoria(new.grupo_id)
    then
        return new;
    end if;

    notas_mudaram :=
        new.nota_saque is distinct from old.nota_saque
        or new.nota_passe is distinct from old.nota_passe
        or new.nota_ataque is distinct from old.nota_ataque
        or new.nota_bloqueio is distinct from old.nota_bloqueio
        or new.nota_defesa is distinct from old.nota_defesa
        or new.nota_atitude is distinct from old.nota_atitude;

    if new.profile_id is null
        and old.profile_id = auth.uid()
        and not notas_mudaram
        and new.genero is not distinct from old.genero
        and new.ativo is not distinct from old.ativo
        and new.entrou_em is not distinct from old.entrou_em
    then
        return new;
    end if;

    if notas_mudaram
        or new.genero is distinct from old.genero
        or new.ativo is distinct from old.ativo
        or new.entrou_em is distinct from old.entrou_em
        or new.profile_id is distinct from old.profile_id
    then
        raise exception 'Notas, genero, presenca, entrada e vinculo so a diretoria altera.';
    end if;

    if new.regime is distinct from old.regime
        and (new.regime = 'isento' or old.regime = 'isento')
    then
        raise exception 'Isencao so a diretoria concede.';
    end if;

    return new;
end;
$$;

create or replace function public.registrar_nota_da_diretoria()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if coalesce(current_setting('app.recalculo', true), 'off') = 'on' then
        return new;
    end if;

    if tg_op = 'UPDATE'
        and new.nota_saque is not distinct from old.nota_saque
        and new.nota_passe is not distinct from old.nota_passe
        and new.nota_ataque is not distinct from old.nota_ataque
        and new.nota_bloqueio is not distinct from old.nota_bloqueio
        and new.nota_defesa is not distinct from old.nota_defesa
        and new.nota_atitude is not distinct from old.nota_atitude
    then
        return new;
    end if;

    insert into public.player_nota_historico (
        grupo_id, player_id, profile_id, day_id, origem, avaliadores,
        nota_saque, nota_passe, nota_ataque, nota_bloqueio, nota_defesa, nota_atitude,
        media, registrado_em
    ) values (
        new.grupo_id, new.id, new.profile_id, null, 'diretoria', 0,
        new.nota_saque, new.nota_passe, new.nota_ataque,
        new.nota_bloqueio, new.nota_defesa, new.nota_atitude,
        new.nota_media, now()
    );

    return new;
end;
$$;

drop trigger if exists players_registrar_nota on public.players;
create trigger players_registrar_nota
    after insert or update of
        nota_saque, nota_passe, nota_ataque, nota_bloqueio, nota_defesa, nota_atitude
    on public.players
    for each row execute function public.registrar_nota_da_diretoria();

create or replace function public.recalcular_nota(alvo uuid)
    returns void
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    base     record;
    dia      record;
    dono     uuid;
    grupo    uuid;
    saque    numeric;
    passe    numeric;
    ataque   numeric;
    bloqueio numeric;
    defesa   numeric;
    atitude  numeric;
begin
    select h.* into base
    from public.player_nota_historico h
    where h.player_id = alvo and h.origem = 'diretoria'
    order by h.registrado_em desc, h.id
    limit 1;

    if not found then
        return;
    end if;

    select p.profile_id, p.grupo_id into dono, grupo
    from public.players p
    where p.id = alvo;

    delete from public.player_nota_historico
    where player_id = alvo
        and origem = 'avaliacao'
        and registrado_em > base.registrado_em;

    saque    := base.nota_saque;
    passe    := base.nota_passe;
    ataque   := base.nota_ataque;
    bloqueio := base.nota_bloqueio;
    defesa   := base.nota_defesa;
    atitude  := base.nota_atitude;

    for dia in
        select
            a.day_id        as day_id,
            d.encerrado_em  as encerrado_em,
            count(*)        as avaliadores,
            avg(a.saque)    as saque,
            avg(a.passe)    as passe,
            avg(a.ataque)   as ataque,
            avg(a.bloqueio) as bloqueio,
            avg(a.defesa)   as defesa,
            avg(a.atitude)  as atitude
        from public.avaliacoes a
        join public.game_days d on d.id = a.day_id
        where a.avaliado_player_id = alvo
            and d.encerrado_em > base.registrado_em
        group by a.day_id, d.encerrado_em
        having count(*) >= public.minimo_de_avaliadores()
        order by d.encerrado_em, a.day_id
    loop
        saque    := public.passo_da_nota(saque, dia.saque);
        passe    := public.passo_da_nota(passe, dia.passe);
        ataque   := public.passo_da_nota(ataque, dia.ataque);
        bloqueio := public.passo_da_nota(bloqueio, dia.bloqueio);
        defesa   := public.passo_da_nota(defesa, dia.defesa);
        atitude  := public.passo_da_nota(atitude, dia.atitude);

        insert into public.player_nota_historico (
            grupo_id, player_id, profile_id, day_id, origem, avaliadores,
            nota_saque, nota_passe, nota_ataque, nota_bloqueio, nota_defesa, nota_atitude,
            media, registrado_em
        ) values (
            grupo, alvo, dono, dia.day_id, 'avaliacao', dia.avaliadores,
            saque, passe, ataque, bloqueio, defesa, atitude,
            round((saque + passe + ataque + bloqueio + defesa + atitude) / 6.0, 2),
            dia.encerrado_em
        );
    end loop;

    perform set_config('app.recalculo', 'on', true);

    update public.players p
    set nota_saque    = saque,
        nota_passe    = passe,
        nota_ataque   = ataque,
        nota_bloqueio = bloqueio,
        nota_defesa   = defesa,
        nota_atitude  = atitude
    where p.id = alvo
        and (p.nota_saque, p.nota_passe, p.nota_ataque,
             p.nota_bloqueio, p.nota_defesa, p.nota_atitude)
            is distinct from (saque, passe, ataque, bloqueio, defesa, atitude);

    perform set_config('app.recalculo', 'off', true);
end;
$$;

create or replace function public.recalcular_evolucao()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    alvo   uuid := new.avaliado_player_id;
    total  integer;
    dono   uuid;
    grupo  uuid;
begin
    select count(*) into total from public.avaliacoes where avaliado_player_id = alvo;
    select p.profile_id, p.grupo_id into dono, grupo from public.players p where p.id = alvo;

    insert into public.player_evolucao (player_id, profile_id, grupo_id, total_avaliacoes, atualizado_em)
    values (alvo, dono, grupo, total, now())
    on conflict (player_id) do update
        set profile_id = excluded.profile_id,
            total_avaliacoes = excluded.total_avaliacoes,
            atualizado_em = now();

    if total >= public.minimo_de_avaliacoes() then
        update public.player_evolucao e
        set saque_media    = m.saque,
            passe_media    = m.passe,
            ataque_media   = m.ataque,
            bloqueio_media = m.bloqueio,
            defesa_media   = m.defesa,
            atitude_media  = m.atitude
        from (
            select
                round(avg(saque)::numeric, 2)    as saque,
                round(avg(passe)::numeric, 2)    as passe,
                round(avg(ataque)::numeric, 2)   as ataque,
                round(avg(bloqueio)::numeric, 2) as bloqueio,
                round(avg(defesa)::numeric, 2)   as defesa,
                round(avg(atitude)::numeric, 2)  as atitude
            from public.avaliacoes
            where avaliado_player_id = alvo
        ) m
        where e.player_id = alvo;
    end if;

    insert into public.avaliacao_registros (
        day_id, grupo_id, avaliador_player_id, avaliado_player_id, profile_id
    )
    select
        new.day_id,
        new.grupo_id,
        new.avaliador_player_id,
        new.avaliado_player_id,
        p.profile_id
    from public.players p
    where p.id = new.avaliador_player_id
    on conflict (day_id, avaliador_player_id, avaliado_player_id) do nothing;

    perform public.recalcular_nota(alvo);

    return new;
end;
$$;

create or replace function public.espelhar_dono_da_evolucao()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    update public.player_evolucao
    set profile_id = new.profile_id
    where player_id = new.id and profile_id is distinct from new.profile_id;

    update public.player_nota_historico
    set profile_id = new.profile_id
    where player_id = new.id and profile_id is distinct from new.profile_id;

    update public.avaliacao_registros
    set profile_id = new.profile_id
    where avaliador_player_id = new.id and profile_id is distinct from new.profile_id;

    return new;
end;
$$;

insert into public.player_nota_historico (
    grupo_id, player_id, profile_id, day_id, origem, avaliadores,
    nota_saque, nota_passe, nota_ataque, nota_bloqueio, nota_defesa, nota_atitude,
    media, registrado_em
)
select
    p.grupo_id, p.id, p.profile_id, null, 'diretoria', 0,
    p.nota_saque, p.nota_passe, p.nota_ataque,
    p.nota_bloqueio, p.nota_defesa, p.nota_atitude,
    p.nota_media, now()
from public.players p
where not exists (
    select 1 from public.player_nota_historico h where h.player_id = p.id
);

alter table public.player_nota_historico enable row level security;

drop policy if exists player_nota_historico_select_dono on public.player_nota_historico;
create policy player_nota_historico_select_dono on public.player_nota_historico
    for select to authenticated
    using (profile_id = auth.uid() or public.e_diretoria(grupo_id));

grant select on public.player_nota_historico to authenticated;

grant execute on function public.peso_da_avaliacao() to authenticated;
grant execute on function public.limite_por_dia() to authenticated;
grant execute on function public.minimo_de_avaliadores() to authenticated;

drop view if exists public.player_performance;

create view public.player_performance as
select
    p.grupo_id                                            as grupo_id,
    p.id                                                  as player_id,
    p.nome,
    p.genero,
    p.skill_level,
    p.nota_media,
    coalesce(count(s.id), 0)::int                         as dias,
    coalesce(sum(s.jogos), 0)::int                        as jogos,
    coalesce(sum(s.vitorias), 0)::int                     as vitorias,
    coalesce(sum(s.derrotas), 0)::int                     as derrotas,
    coalesce(sum(s.pontos_pro), 0)::int                   as pontos_pro,
    coalesce(sum(s.pontos_contra), 0)::int                as pontos_contra,
    coalesce(sum(s.pontos_pro - s.pontos_contra), 0)::int as saldo_pontos
from public.players p
left join public.player_day_stats s on s.player_id = p.id
group by p.grupo_id, p.id, p.nome, p.genero, p.skill_level, p.nota_media
order by vitorias desc, saldo_pontos desc, p.nome;

alter view public.player_performance set (security_invoker = true);

grant select on public.player_performance to authenticated;

drop publication if exists powersync;

create publication powersync for table
    public.profiles,
    public.grupos,
    public.grupo_membros,
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
    public.pagamentos,
    public.avaliacao_registros,
    public.player_evolucao,
    public.player_nota_historico,
    public.dicas;
