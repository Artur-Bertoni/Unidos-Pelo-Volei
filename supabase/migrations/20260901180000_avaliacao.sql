create table if not exists public.avaliacoes (
    id                  uuid primary key default gen_random_uuid(),
    day_id              uuid not null references public.game_days (id) on delete cascade,
    avaliador_player_id uuid not null references public.players (id) on delete cascade,
    avaliado_player_id  uuid not null references public.players (id) on delete cascade,
    saque               smallint not null check (saque between 1 and 5),
    passe               smallint not null check (passe between 1 and 5),
    ataque              smallint not null check (ataque between 1 and 5),
    bloqueio            smallint not null check (bloqueio between 1 and 5),
    defesa              smallint not null check (defesa between 1 and 5),
    atitude             smallint not null check (atitude between 1 and 5),
    criado_em           timestamptz not null default now(),
    unique (day_id, avaliador_player_id, avaliado_player_id),
    constraint avaliacoes_nao_e_autoavaliacao check (avaliador_player_id <> avaliado_player_id)
);

create index if not exists avaliacoes_avaliado_idx on public.avaliacoes (avaliado_player_id);

create table if not exists public.avaliacao_registros (
    id                  uuid primary key default gen_random_uuid(),
    day_id              uuid not null references public.game_days (id) on delete cascade,
    avaliador_player_id uuid not null references public.players (id) on delete cascade,
    avaliado_player_id  uuid not null references public.players (id) on delete cascade,
    profile_id          uuid references public.profiles (id) on delete set null,
    criado_em           timestamptz not null default now(),
    unique (day_id, avaliador_player_id, avaliado_player_id)
);

create index if not exists avaliacao_registros_profile_idx on public.avaliacao_registros (profile_id);

create table if not exists public.player_evolucao (
    id                uuid primary key default gen_random_uuid(),
    player_id         uuid not null unique references public.players (id) on delete cascade,
    profile_id        uuid references public.profiles (id) on delete set null,
    total_avaliacoes  integer not null default 0,
    saque_media       numeric(3, 2),
    passe_media       numeric(3, 2),
    ataque_media      numeric(3, 2),
    bloqueio_media    numeric(3, 2),
    defesa_media      numeric(3, 2),
    atitude_media     numeric(3, 2),
    atualizado_em     timestamptz not null default now()
);

create index if not exists player_evolucao_profile_idx on public.player_evolucao (profile_id);

create table if not exists public.dicas (
    id        uuid primary key default gen_random_uuid(),
    atributo  text not null check (
                  atributo in ('saque', 'passe', 'ataque', 'bloqueio', 'defesa', 'atitude')
              ),
    faixa_max numeric(3, 2) not null default 5.00,
    titulo    text not null,
    texto     text not null,
    ordem     integer not null default 0
);

create index if not exists dicas_atributo_idx on public.dicas (atributo, faixa_max, ordem);

create or replace function public.minimo_de_avaliacoes()
    returns integer
    language sql
    immutable
as $$
select 5;
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
begin
    select count(*) into total from public.avaliacoes where avaliado_player_id = alvo;
    select p.profile_id into dono from public.players p where p.id = alvo;

    insert into public.player_evolucao (player_id, profile_id, total_avaliacoes, atualizado_em)
    values (alvo, dono, total, now())
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
        day_id, avaliador_player_id, avaliado_player_id, profile_id
    )
    select
        new.day_id,
        new.avaliador_player_id,
        new.avaliado_player_id,
        p.profile_id
    from public.players p
    where p.id = new.avaliador_player_id
    on conflict (day_id, avaliador_player_id, avaliado_player_id) do nothing;

    return new;
end;
$$;

drop trigger if exists avaliacoes_recalcular on public.avaliacoes;
create trigger avaliacoes_recalcular
    after insert on public.avaliacoes
    for each row execute function public.recalcular_evolucao();

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

    update public.avaliacao_registros
    set profile_id = new.profile_id
    where avaliador_player_id = new.id and profile_id is distinct from new.profile_id;

    return new;
end;
$$;

drop trigger if exists players_espelhar_evolucao on public.players;
create trigger players_espelhar_evolucao
    after update of profile_id on public.players
    for each row execute function public.espelhar_dono_da_evolucao();

alter table public.avaliacoes          enable row level security;
alter table public.avaliacao_registros enable row level security;
alter table public.player_evolucao     enable row level security;
alter table public.dicas               enable row level security;

drop policy if exists avaliacoes_insert_avaliador on public.avaliacoes;
create policy avaliacoes_insert_avaliador on public.avaliacoes
    for insert to authenticated
    with check (public.sou_o_jogador(avaliador_player_id));

drop policy if exists avaliacao_registros_select_dono on public.avaliacao_registros;
create policy avaliacao_registros_select_dono on public.avaliacao_registros
    for select to authenticated
    using (profile_id = auth.uid());

drop policy if exists player_evolucao_select_dono on public.player_evolucao;
create policy player_evolucao_select_dono on public.player_evolucao
    for select to authenticated
    using (profile_id = auth.uid());

drop policy if exists dicas_select_authenticated on public.dicas;
create policy dicas_select_authenticated on public.dicas
    for select to authenticated using (true);

drop policy if exists dicas_insert_admin on public.dicas;
create policy dicas_insert_admin on public.dicas
    for insert to authenticated with check (public.is_admin());

drop policy if exists dicas_update_admin on public.dicas;
create policy dicas_update_admin on public.dicas
    for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

drop policy if exists dicas_delete_admin on public.dicas;
create policy dicas_delete_admin on public.dicas
    for delete to authenticated using (public.is_admin());

grant insert on public.avaliacoes to authenticated;
grant select on public.avaliacao_registros, public.player_evolucao to authenticated;
grant select, insert, update, delete on public.dicas to authenticated;

insert into public.dicas (atributo, faixa_max, titulo, texto, ordem) values
    ('saque', 3.00, 'Firme o ritual antes de sacar',
     'Escolha um ritual e repita sempre: dois toques na bola, olhar para o alvo, respirar, bater. A maior parte do saque errado no sábado é pressa, não técnica. Mire no meio da quadra até acertar dez seguidos, e só então comece a procurar o canto.', 1),
    ('saque', 5.00, 'Comece a escolher o alvo',
     'Com o saque já constante, passe a mirar: o meio entre dois passadores, a linha do fundo, ou quem acabou de entrar. Saque que cai onde ninguém se entende vale mais que saque forte.', 2),
    ('passe', 3.00, 'Chegue antes da bola',
     'Passe bom começa com o pé, não com o braço. Saia do lugar assim que o saque sair da mão do adversário e pare com os dois pés no chão antes do toque. Braços firmes e juntos, sem balançar: a bola sobe com o corpo, não com a braçada.', 1),
    ('passe', 5.00, 'Trabalhe a bola difícil',
     'Você já domina o passe fácil. Peça para alguém sacar forte e treine o passe fora do eixo, com um pé só e caindo. É o que separa o passador do time que ganha.', 2),
    ('ataque', 3.00, 'Ajuste os três passos',
     'A entrada é esquerda-direita-esquerda para destro, terminando com os dois pés juntos. Treine a entrada sem bola até virar automático. Bater forte com o tempo errado tira mais ponto do que dá.', 1),
    ('ataque', 5.00, 'Aprenda a largada',
     'Com o ataque forte já resolvido, o próximo salto é variar: a largada por cima do bloqueio e a bola na diagonal curta. Ataque previsível vira bloqueio fácil.', 2),
    ('bloqueio', 3.00, 'Mãos antes do salto',
     'Suba com as mãos já acima da cabeça e os dedos abertos e firmes. O erro mais comum é subir com o braço embaixo e passar a bola por baixo do bloqueio. Não pule na finta: espere o atacante sair do chão.', 1),
    ('bloqueio', 5.00, 'Feche a linha com o companheiro',
     'Bloqueio bom é de dois. Combine quem fecha a paralela e quem fecha a diagonal antes do ponto começar, e suba junto — bloqueio aberto é um convite.', 2),
    ('defesa', 3.00, 'Fique baixo e parado no toque',
     'Joelhos dobrados, peso à frente, mãos soltas na frente do corpo. Quem defende em pé chega tarde em tudo. E defenda com o corpo atrás da bola, não com o braço esticado para o lado.', 1),
    ('defesa', 5.00, 'Leia o atacante, não a bola',
     'Olhe o ombro e o cotovelo de quem vai bater: eles dizem a direção antes do contato. Aprenda a sair do lugar meio segundo antes e você chega em bola que hoje passa.', 2),
    ('atitude', 3.00, 'Fale mais dentro de quadra',
     'A maior parte da bola perdida entre dois jogadores é silêncio, não falta de técnica. Chame "minha" alto, avise quem está livre, conte o número de toques. É a melhora mais rápida que existe e não depende de treino.', 1),
    ('atitude', 5.00, 'Puxe quem está mais travado',
     'Você já joga junto. O próximo passo é levantar o time: comemore o ponto do outro, chame quem errou para a próxima jogada. O grupo do sábado se sustenta nisso muito mais do que no placar.', 2)
on conflict do nothing;

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
    public.pagamentos,
    public.avaliacao_registros,
    public.player_evolucao,
    public.dicas;
