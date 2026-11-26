create table if not exists public.grupos (
    id         uuid primary key default gen_random_uuid(),
    nome       text        not null check (btrim(nome) <> ''),
    slug       text        not null unique,
    cidade     text,
    sobre      text,
    logo_url   text,
    criado_por uuid references public.profiles (id) on delete set null,
    criado_em  timestamptz not null default now(),
    ativo      boolean     not null default true
);

create table if not exists public.grupo_membros (
    id         uuid primary key default gen_random_uuid(),
    grupo_id   uuid not null references public.grupos (id) on delete cascade,
    profile_id uuid not null references public.profiles (id) on delete cascade,
    papel      text not null default 'atleta' check (papel in ('diretoria', 'atleta')),
    entrou_em  timestamptz not null default now(),
    chave_id   uuid,
    unique (grupo_id, profile_id)
);

create index if not exists grupo_membros_profile_idx on public.grupo_membros (profile_id);

create or replace function public.normalizar_chave(entrada text)
    returns text
    language sql
    immutable
as $$
select upper(regexp_replace(coalesce(entrada, ''), '[^a-zA-Z0-9]', '', 'g'));
$$;

create table if not exists public.grupo_chaves (
    id         uuid primary key default gen_random_uuid(),
    grupo_id   uuid    not null references public.grupos (id) on delete cascade,
    codigo     text    not null unique,
    rotulo     text,
    papel      text    not null default 'atleta' check (papel in ('diretoria', 'atleta')),
    usos       integer not null default 0 check (usos >= 0),
    usos_max   integer check (usos_max is null or usos_max > 0),
    expira_em  timestamptz,
    ativa      boolean not null default true,
    criada_por uuid references public.profiles (id) on delete set null,
    criada_em  timestamptz not null default now()
);

create index if not exists grupo_chaves_grupo_idx on public.grupo_chaves (grupo_id, ativa);

create or replace function public.gerar_codigo_de_chave()
    returns text
    language plpgsql
    volatile
    security definer
    set search_path = public
as $$
declare
    alfabeto text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    sorteado text;
begin
    loop
        sorteado := '';
        for i in 1..8 loop
            sorteado := sorteado || substr(alfabeto, 1 + floor(random() * length(alfabeto))::int, 1);
        end loop;
        exit when not exists (select 1 from public.grupo_chaves c where c.codigo = sorteado);
    end loop;
    return sorteado;
end;
$$;

alter table public.grupo_chaves
    alter column codigo set default public.gerar_codigo_de_chave();

create or replace function public.gerar_slug(nome text)
    returns text
    language sql
    immutable
as $$
select coalesce(
    nullif(
        btrim(
            regexp_replace(
                lower(
                    translate(
                        coalesce(nome, ''),
                        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
                        'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'
                    )
                ),
                '[^a-z0-9]+', '-', 'g'
            ),
            '-'
        ),
        ''
    ),
    'grupo'
);
$$;

create or replace function public.sou_membro(grupo uuid)
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
select exists (
    select 1 from public.grupo_membros m
    where m.grupo_id = grupo and m.profile_id = auth.uid()
);
$$;

create or replace function public.e_diretoria(grupo uuid)
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
select exists (
    select 1 from public.grupo_membros m
    where m.grupo_id = grupo and m.profile_id = auth.uid() and m.papel = 'diretoria'
);
$$;

create or replace function public.grupo_do_caminho(caminho text)
    returns uuid
    language plpgsql
    immutable
as $$
begin
    return split_part(coalesce(caminho, ''), '/', 1)::uuid;
exception
    when others then return null;
end;
$$;

create table if not exists public.modelos_de_time (
    id      uuid primary key default gen_random_uuid(),
    nome    text    not null unique,
    cor_hex text    not null,
    sigla   text    not null,
    ordem   integer not null default 0
);

create table if not exists public.modelos_de_pagina (
    id        uuid primary key default gen_random_uuid(),
    slug      text    not null unique,
    categoria text    not null,
    titulo    text    not null,
    corpo     text    not null default '',
    ordem     integer not null default 0
);

insert into public.modelos_de_time (nome, cor_hex, sigla, ordem) values
    ('Azul',     '#2F80ED', 'AZ', 1),
    ('Laranja',  '#E8590C', 'LR', 2),
    ('Preto',    '#3F444D', 'PR', 3),
    ('Rosa',     '#E8437F', 'RS', 4),
    ('Roxo',     '#8B5CF6', 'RX', 5),
    ('Verde',    '#16A34A', 'VD', 6),
    ('Cinza',    '#9CA3AF', 'CZ', 7),
    ('Vermelho', '#E23B3B', 'VM', 8),
    ('Amarelo',  '#EAB308', 'AM', 9)
on conflict (nome) do nothing;

insert into public.modelos_de_pagina (slug, categoria, titulo, corpo, ordem) values
    (
        'regras-do-volei',
        'volei',
        'Regras do vôlei de areia - quarteto',
        E'# O formato\n\nQuatro de cada lado, na areia. Não existe rodízio de posição dentro da quadra: o que roda é a **ordem de saque**, e ela é fixa do começo ao fim do set.\n\n# Contagem\n\n- Set até **15 pontos**, com dois de vantagem.\n- Ponto em toda jogada, saque de quem ganhou o ponto.\n- A diretoria edita esta página no app e ajusta a contagem ao que o grupo combinou.\n\n# Toques\n\n- Três toques por equipe antes de devolver a bola.\n- O mesmo jogador não toca duas vezes seguidas, salvo depois do bloqueio.\n\n# O que a areia proíbe\n\n- **Largada de dedo aberto.** Para colocar a bola, use os nós dos dedos, a mão fechada ou a mão espalmada.\n- Levantada de toque só atravessa a rede se sair perpendicular aos ombros de quem levantou.\n- Segurar, conduzir ou acompanhar a bola no toque.\n\n# Faltas de sempre\n\n- Encostar na rede durante a jogada.\n- Invadir a quadra adversária por baixo da rede atrapalhando o adversário.\n- Atacar a bola de saque perto da rede.\n\n# Vento, sol e bom senso\n\nNa areia o vento é parte do jogo: a bola vai voltar, vai cair na sua mão e vai fugir dela. Combine o lado antes do set e reclame menos, jogue mais.',
        1
    ),
    (
        'regras-do-grupo',
        'grupo',
        'Regras do grupo',
        E'# Combinados do dia de jogo\n\nA diretoria edita esta página direto no app. O que estiver escrito aqui vale para todo mundo.\n\n- **Confirme a presença** na véspera, para a gente montar os times certos.\n- **Chegue no horário.** Quem chega depois do sorteio entra na próxima rodada.\n- **Respeito acima de tudo.** O nível de todo mundo é diferente e o dia é de todo mundo.\n\n# Mensalidade e diária\n\nA regra de cobrança fica na aba do financeiro, e cada um vê o próprio extrato.',
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

insert into public.grupos (id, nome, slug, criado_por)
select
    '00000000-0000-4000-8000-000000000001'::uuid,
    'Unidos Pelo Vôlei',
    'unidos-pelo-volei',
    (select p.id from public.profiles p where p.papel = 'diretoria' order by p.created_at limit 1)
where not exists (select 1 from public.grupos g where g.id = '00000000-0000-4000-8000-000000000001'::uuid);

insert into public.grupo_membros (grupo_id, profile_id, papel)
select '00000000-0000-4000-8000-000000000001'::uuid, p.id, p.papel
from public.profiles p
on conflict (grupo_id, profile_id) do nothing;

insert into public.grupo_chaves (grupo_id, rotulo, papel)
select '00000000-0000-4000-8000-000000000001'::uuid, 'Chave do grupo', 'atleta'
where not exists (
    select 1 from public.grupo_chaves c
    where c.grupo_id = '00000000-0000-4000-8000-000000000001'::uuid
);

drop trigger if exists posts_touch_updated_at on public.posts;

do $$
declare
    alvo text;
    tabelas text[] := array[
        'players', 'teams', 'team_players', 'rounds', 'matches',
        'game_days', 'player_day_stats', 'vinculo_pedidos', 'player_contatos',
        'config_grupo', 'presencas', 'avisos', 'posts', 'post_reacoes',
        'eventos', 'paginas', 'config_financeiro', 'cobrancas', 'pagamentos',
        'avaliacoes', 'avaliacao_registros', 'player_evolucao'
    ];
begin
    foreach alvo in array tabelas
    loop
        execute format('alter table public.%I add column if not exists grupo_id uuid', alvo);
        execute format(
            'update public.%I set grupo_id = %L where grupo_id is null',
            alvo, '00000000-0000-4000-8000-000000000001'
        );
        execute format('delete from public.%I where grupo_id is null', alvo);
        execute format('alter table public.%I alter column grupo_id set not null', alvo);

        if not exists (
            select 1 from pg_constraint where conname = alvo || '_grupo_fk'
        ) then
            execute format(
                'alter table public.%I add constraint %I foreign key (grupo_id)
                 references public.grupos (id) on delete cascade',
                alvo, alvo || '_grupo_fk'
            );
        end if;

        execute format(
            'create index if not exists %I on public.%I (grupo_id)',
            alvo || '_grupo_idx', alvo
        );
    end loop;
end;
$$;

create or replace function public.touch_atualizado_em()
    returns trigger
    language plpgsql
as $$
begin
    new.atualizado_em := now();
    return new;
end;
$$;

drop trigger if exists posts_touch_atualizado_em on public.posts;
create trigger posts_touch_atualizado_em
    before update on public.posts
    for each row execute function public.touch_atualizado_em();

drop index if exists public.players_profile_id_unico;
create unique index if not exists players_grupo_profile_unico
    on public.players (grupo_id, profile_id)
    where profile_id is not null;

drop index if exists public.vinculo_pedidos_um_pendente;
create unique index if not exists vinculo_pedidos_um_pendente_no_grupo
    on public.vinculo_pedidos (grupo_id, profile_id)
    where status = 'pendente';

alter table public.rounds drop constraint if exists rounds_numero_key;
create unique index if not exists rounds_numero_no_grupo on public.rounds (grupo_id, numero);

alter table public.paginas drop constraint if exists paginas_slug_key;
create unique index if not exists paginas_slug_no_grupo on public.paginas (grupo_id, slug);

create unique index if not exists config_grupo_um_por_grupo on public.config_grupo (grupo_id);
create unique index if not exists config_financeiro_um_por_grupo on public.config_financeiro (grupo_id);

create or replace function public.herdar_grupo()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    tabela_pai text := tg_argv[0];
    coluna     text := tg_argv[1];
    referencia uuid;
    grupo_pai  uuid;
begin
    referencia := (to_jsonb(new) ->> coluna)::uuid;
    if referencia is null then
        return new;
    end if;

    execute format('select grupo_id from public.%I where id = $1', tabela_pai)
        into grupo_pai using referencia;

    if grupo_pai is null then
        return new;
    end if;

    if new.grupo_id is null then
        new.grupo_id := grupo_pai;
    elsif new.grupo_id <> grupo_pai then
        raise exception 'Este registro pertence a outro grupo.';
    end if;

    return new;
end;
$$;

do $$
declare
    ligacao text[];
    ligacoes text[][] := array[
        array['team_players', 'teams', 'team_id'],
        array['matches', 'rounds', 'round_id'],
        array['player_day_stats', 'game_days', 'day_id'],
        array['post_reacoes', 'posts', 'post_id'],
        array['pagamentos', 'cobrancas', 'cobranca_id'],
        array['avaliacoes', 'game_days', 'day_id'],
        array['avaliacao_registros', 'game_days', 'day_id'],
        array['presencas', 'players', 'player_id'],
        array['player_contatos', 'players', 'player_id'],
        array['player_evolucao', 'players', 'player_id'],
        array['vinculo_pedidos', 'players', 'player_id']
    ];
begin
    foreach ligacao slice 1 in array ligacoes
    loop
        execute format('drop trigger if exists %I on public.%I', ligacao[1] || '_herdar_grupo', ligacao[1]);
        execute format(
            'create trigger %I before insert or update on public.%I
             for each row execute function public.herdar_grupo(%L, %L)',
            ligacao[1] || '_herdar_grupo', ligacao[1], ligacao[2], ligacao[3]
        );
    end loop;
end;
$$;

drop trigger if exists profiles_sincronizar_papel on public.profiles;
drop function if exists public.sincronizar_papel();

alter table public.profiles drop column if exists papel;
alter table public.profiles drop column if exists is_admin;

create or replace function public.proteger_ficha_do_jogador()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    if new.grupo_id is distinct from old.grupo_id then
        raise exception 'Um jogador nao muda de grupo.';
    end if;

    if auth.uid() is null or public.e_diretoria(new.grupo_id) then
        return new;
    end if;

    if new.profile_id is null
        and old.profile_id = auth.uid()
        and new.skill_level is not distinct from old.skill_level
        and new.genero is not distinct from old.genero
        and new.ativo is not distinct from old.ativo
        and new.entrou_em is not distinct from old.entrou_em
    then
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

    if new.regime is distinct from old.regime
        and (new.regime = 'isento' or old.regime = 'isento')
    then
        raise exception 'Isencao so a diretoria concede.';
    end if;

    return new;
end;
$$;

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
        where profile_id = new.profile_id
            and grupo_id = new.grupo_id
            and id <> new.player_id;

        update public.players
        set profile_id = new.profile_id, updated_at = now()
        where id = new.player_id;
    end if;

    return new;
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

    return new;
end;
$$;

create or replace function public.compartilha_grupo(alvo uuid)
    returns boolean
    language sql
    stable
    security definer
    set search_path = public
as $$
select exists (
    select 1
    from public.grupo_membros meu
    join public.grupo_membros dele on dele.grupo_id = meu.grupo_id
    where meu.profile_id = auth.uid() and dele.profile_id = alvo
);
$$;

create or replace function public.proteger_ultima_diretoria()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    restantes integer;
begin
    if old.papel <> 'diretoria' or new.papel = 'diretoria' then
        return new;
    end if;

    select count(*) into restantes
    from public.grupo_membros m
    where m.grupo_id = old.grupo_id and m.papel = 'diretoria' and m.id <> old.id;

    if restantes = 0 then
        raise exception 'O grupo ficaria sem diretoria. Promova outra pessoa antes.';
    end if;

    return new;
end;
$$;

create or replace function public.arrumar_saida_do_grupo()
    returns trigger
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    herdeiro uuid;
begin
    if not exists (select 1 from public.grupos g where g.id = old.grupo_id) then
        return old;
    end if;

    update public.players
    set profile_id = null, updated_at = now()
    where grupo_id = old.grupo_id and profile_id = old.profile_id;

    delete from public.vinculo_pedidos
    where grupo_id = old.grupo_id and profile_id = old.profile_id and status = 'pendente';

    if old.papel <> 'diretoria' then
        return old;
    end if;

    if exists (
        select 1 from public.grupo_membros m
        where m.grupo_id = old.grupo_id and m.papel = 'diretoria'
    ) then
        return old;
    end if;

    select m.id into herdeiro
    from public.grupo_membros m
    where m.grupo_id = old.grupo_id
    order by m.entrou_em
    limit 1;

    if herdeiro is null then
        update public.grupos set ativo = false where id = old.grupo_id;
    else
        update public.grupo_membros set papel = 'diretoria' where id = herdeiro;
    end if;

    return old;
end;
$$;

drop trigger if exists grupo_membros_proteger_diretoria on public.grupo_membros;
create trigger grupo_membros_proteger_diretoria
    before update on public.grupo_membros
    for each row execute function public.proteger_ultima_diretoria();

drop trigger if exists grupo_membros_repassar_diretoria on public.grupo_membros;
create trigger grupo_membros_repassar_diretoria
    after delete on public.grupo_membros
    for each row execute function public.arrumar_saida_do_grupo();

do $$
declare
    r record;
    tabelas text[] := array[
        'profiles', 'players', 'teams', 'team_players', 'rounds', 'matches',
        'game_days', 'player_day_stats', 'vinculo_pedidos', 'player_contatos',
        'config_grupo', 'presencas', 'avisos', 'posts', 'post_reacoes',
        'eventos', 'paginas', 'config_financeiro', 'cobrancas', 'pagamentos',
        'avaliacoes', 'avaliacao_registros', 'player_evolucao', 'dicas'
    ];
begin
    for r in
        select tablename, policyname
        from pg_policies
        where schemaname = 'public' and tablename = any (tabelas)
    loop
        execute format('drop policy %I on public.%I', r.policyname, r.tablename);
    end loop;

    for r in
        select policyname
        from pg_policies
        where schemaname = 'storage' and tablename = 'objects' and policyname like 'mural%'
    loop
        execute format('drop policy %I on storage.objects', r.policyname);
    end loop;
end;
$$;

drop function if exists public.is_admin();

alter table public.grupos            enable row level security;
alter table public.grupo_membros     enable row level security;
alter table public.grupo_chaves      enable row level security;
alter table public.modelos_de_time   enable row level security;
alter table public.modelos_de_pagina enable row level security;

create policy grupos_select_membro on public.grupos
    for select to authenticated
    using (public.sou_membro(id));

create policy grupos_update_diretoria on public.grupos
    for update to authenticated
    using (public.e_diretoria(id))
    with check (public.e_diretoria(id));

create policy grupo_membros_select_membro on public.grupo_membros
    for select to authenticated
    using (public.sou_membro(grupo_id));

create policy grupo_membros_update_diretoria on public.grupo_membros
    for update to authenticated
    using (public.e_diretoria(grupo_id))
    with check (public.e_diretoria(grupo_id));

create policy grupo_membros_delete_diretoria_ou_proprio on public.grupo_membros
    for delete to authenticated
    using (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy grupo_chaves_diretoria_le on public.grupo_chaves
    for select to authenticated
    using (public.e_diretoria(grupo_id));

create policy grupo_chaves_diretoria_cria on public.grupo_chaves
    for insert to authenticated
    with check (public.e_diretoria(grupo_id));

create policy grupo_chaves_diretoria_edita on public.grupo_chaves
    for update to authenticated
    using (public.e_diretoria(grupo_id))
    with check (public.e_diretoria(grupo_id));

create policy grupo_chaves_diretoria_apaga on public.grupo_chaves
    for delete to authenticated
    using (public.e_diretoria(grupo_id));

create policy modelos_de_time_leitura on public.modelos_de_time
    for select to authenticated using (true);

create policy modelos_de_pagina_leitura on public.modelos_de_pagina
    for select to authenticated using (true);

create policy profiles_select_do_meu_grupo on public.profiles
    for select to authenticated
    using (id = auth.uid() or public.compartilha_grupo(id));

create policy profiles_update_proprio on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

do $$
declare
    alvo text;
    tabelas text[] := array[
        'players', 'teams', 'team_players', 'rounds', 'matches',
        'game_days', 'player_day_stats', 'config_grupo', 'avisos',
        'posts', 'eventos', 'paginas', 'config_financeiro', 'cobrancas'
    ];
begin
    foreach alvo in array tabelas
    loop
        execute format(
            'create policy %I on public.%I for select to authenticated
             using (public.sou_membro(grupo_id))',
            alvo || '_select_membro', alvo
        );
        execute format(
            'create policy %I on public.%I for insert to authenticated
             with check (public.e_diretoria(grupo_id))',
            alvo || '_insert_diretoria', alvo
        );
        execute format(
            'create policy %I on public.%I for update to authenticated
             using (public.e_diretoria(grupo_id)) with check (public.e_diretoria(grupo_id))',
            alvo || '_update_diretoria', alvo
        );
        execute format(
            'create policy %I on public.%I for delete to authenticated
             using (public.e_diretoria(grupo_id))',
            alvo || '_delete_diretoria', alvo
        );
    end loop;
end;
$$;

create policy players_update_dono on public.players
    for update to authenticated
    using (profile_id = auth.uid())
    with check (profile_id = auth.uid());

create policy vinculo_pedidos_select_membro on public.vinculo_pedidos
    for select to authenticated
    using (public.sou_membro(grupo_id));

create policy vinculo_pedidos_insert_proprio on public.vinculo_pedidos
    for insert to authenticated
    with check (public.sou_membro(grupo_id) and profile_id = auth.uid() and status = 'pendente');

create policy vinculo_pedidos_insert_diretoria on public.vinculo_pedidos
    for insert to authenticated
    with check (public.e_diretoria(grupo_id));

create policy vinculo_pedidos_update_diretoria on public.vinculo_pedidos
    for update to authenticated
    using (public.e_diretoria(grupo_id))
    with check (public.e_diretoria(grupo_id));

create policy vinculo_pedidos_delete_dono on public.vinculo_pedidos
    for delete to authenticated
    using (public.e_diretoria(grupo_id) or (profile_id = auth.uid() and status = 'pendente'));

create policy player_contatos_select_dono on public.player_contatos
    for select to authenticated
    using (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy player_contatos_insert_dono on public.player_contatos
    for insert to authenticated
    with check (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy player_contatos_update_dono on public.player_contatos
    for update to authenticated
    using (public.e_diretoria(grupo_id) or profile_id = auth.uid())
    with check (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy player_contatos_delete_diretoria on public.player_contatos
    for delete to authenticated
    using (public.e_diretoria(grupo_id));

create policy presencas_select_membro on public.presencas
    for select to authenticated
    using (public.sou_membro(grupo_id));

create policy presencas_insert_dono on public.presencas
    for insert to authenticated
    with check (public.e_diretoria(grupo_id) or public.sou_o_jogador(player_id));

create policy presencas_update_dono on public.presencas
    for update to authenticated
    using (public.e_diretoria(grupo_id) or public.sou_o_jogador(player_id))
    with check (public.e_diretoria(grupo_id) or public.sou_o_jogador(player_id));

create policy presencas_delete_dono on public.presencas
    for delete to authenticated
    using (public.e_diretoria(grupo_id) or public.sou_o_jogador(player_id));

create policy post_reacoes_select_membro on public.post_reacoes
    for select to authenticated
    using (public.sou_membro(grupo_id));

create policy post_reacoes_insert_dono on public.post_reacoes
    for insert to authenticated
    with check (public.sou_membro(grupo_id) and profile_id = auth.uid());

create policy post_reacoes_delete_dono on public.post_reacoes
    for delete to authenticated
    using (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy pagamentos_select_dono on public.pagamentos
    for select to authenticated
    using (public.e_diretoria(grupo_id) or profile_id = auth.uid());

create policy pagamentos_insert_diretoria on public.pagamentos
    for insert to authenticated
    with check (public.e_diretoria(grupo_id));

create policy pagamentos_update_diretoria on public.pagamentos
    for update to authenticated
    using (public.e_diretoria(grupo_id))
    with check (public.e_diretoria(grupo_id));

create policy pagamentos_delete_diretoria on public.pagamentos
    for delete to authenticated
    using (public.e_diretoria(grupo_id));

create policy avaliacoes_insert_avaliador on public.avaliacoes
    for insert to authenticated
    with check (public.sou_membro(grupo_id) and public.sou_o_jogador(avaliador_player_id));

create policy avaliacao_registros_select_dono on public.avaliacao_registros
    for select to authenticated
    using (profile_id = auth.uid());

create policy player_evolucao_select_dono on public.player_evolucao
    for select to authenticated
    using (profile_id = auth.uid());

create policy dicas_select_authenticated on public.dicas
    for select to authenticated using (true);

create policy mural_leitura_publica on storage.objects
    for select to public
    using (bucket_id = 'mural');

create policy mural_envio_diretoria on storage.objects
    for insert to authenticated
    with check (bucket_id = 'mural' and public.e_diretoria(public.grupo_do_caminho(name)));

create policy mural_troca_diretoria on storage.objects
    for update to authenticated
    using (bucket_id = 'mural' and public.e_diretoria(public.grupo_do_caminho(name)))
    with check (bucket_id = 'mural' and public.e_diretoria(public.grupo_do_caminho(name)));

create policy mural_exclusao_diretoria on storage.objects
    for delete to authenticated
    using (bucket_id = 'mural' and public.e_diretoria(public.grupo_do_caminho(name)));

create or replace function public.preparar_grupo(grupo uuid, dono uuid)
    returns void
    language plpgsql
    security definer
    set search_path = public
as $$
begin
    insert into public.config_grupo (grupo_id, jogo_hora)
    values (grupo, '09:00')
    on conflict (grupo_id) do nothing;

    insert into public.config_financeiro (grupo_id, mensalidade_centavos, diaria_centavos)
    values (grupo, 0, 0)
    on conflict (grupo_id) do nothing;

    insert into public.teams (grupo_id, nome, cor_hex, sigla, ativo, ordem)
    select grupo, m.nome, m.cor_hex, m.sigla, true, m.ordem
    from public.modelos_de_time m
    where not exists (select 1 from public.teams t where t.grupo_id = grupo);

    insert into public.paginas (grupo_id, slug, categoria, titulo, corpo, ordem, atualizado_por)
    select grupo, m.slug, m.categoria, m.titulo, m.corpo, m.ordem, dono
    from public.modelos_de_pagina m
    on conflict do nothing;

    insert into public.grupo_chaves (grupo_id, rotulo, papel, criada_por)
    select grupo, 'Chave do grupo', 'atleta', dono
    where not exists (select 1 from public.grupo_chaves c where c.grupo_id = grupo);
end;
$$;

revoke execute on function public.preparar_grupo(uuid, uuid) from public;

create or replace function public.criar_grupo(p_nome text, p_cidade text default null)
    returns table (grupo_id uuid, nome text, papel text)
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    dono       uuid := auth.uid();
    novo       uuid;
    base       text;
    escolhido  text;
    tentativa  integer := 0;
begin
    if dono is null then
        raise exception 'Entre na sua conta para criar um grupo.';
    end if;

    if btrim(coalesce(p_nome, '')) = '' then
        raise exception 'O grupo precisa de um nome.';
    end if;

    if (select count(*) from public.grupos g where g.criado_por = dono) >= 10 then
        raise exception 'Voce ja criou grupos demais. Fale com a gente se precisar de mais.';
    end if;

    base := left(public.gerar_slug(p_nome), 40);
    escolhido := base;
    while exists (select 1 from public.grupos g where g.slug = escolhido) loop
        tentativa := tentativa + 1;
        escolhido := base || '-' || tentativa;
    end loop;

    insert into public.grupos (nome, slug, cidade, criado_por)
    values (btrim(p_nome), escolhido, nullif(btrim(coalesce(p_cidade, '')), ''), dono)
    returning id into novo;

    insert into public.grupo_membros (grupo_id, profile_id, papel)
    values (novo, dono, 'diretoria');

    perform public.preparar_grupo(novo, dono);

    return query
    select g.id, g.nome, 'diretoria'::text
    from public.grupos g
    where g.id = novo;
end;
$$;

create or replace function public.entrar_no_grupo(p_codigo text)
    returns table (grupo_id uuid, nome text, papel text)
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    quem        uuid := auth.uid();
    procurado   text := public.normalizar_chave(p_codigo);
    chave       public.grupo_chaves%rowtype;
    grupo       public.grupos%rowtype;
    meu_papel   text;
begin
    if quem is null then
        raise exception 'Entre na sua conta para usar a chave.';
    end if;

    if length(procurado) < 4 then
        raise exception 'Chave de acesso invalida.';
    end if;

    select * into chave from public.grupo_chaves c where c.codigo = procurado;
    if not found then
        raise exception 'Chave nao encontrada. Confira o codigo com a diretoria do grupo.';
    end if;

    select * into grupo from public.grupos g where g.id = chave.grupo_id;
    if not grupo.ativo then
        raise exception 'Este grupo esta fechado no momento.';
    end if;

    select m.papel into meu_papel
    from public.grupo_membros m
    where m.grupo_id = chave.grupo_id and m.profile_id = quem;

    if meu_papel is not null then
        return query select grupo.id, grupo.nome, meu_papel;
        return;
    end if;

    if not chave.ativa then
        raise exception 'Esta chave foi desativada. Peca uma nova a diretoria.';
    end if;

    if chave.expira_em is not null and chave.expira_em < now() then
        raise exception 'Esta chave venceu. Peca uma nova a diretoria.';
    end if;

    if chave.usos_max is not null and chave.usos >= chave.usos_max then
        raise exception 'Esta chave ja foi usada o numero de vezes combinado.';
    end if;

    insert into public.grupo_membros (grupo_id, profile_id, papel, chave_id)
    values (chave.grupo_id, quem, chave.papel, chave.id);

    update public.grupo_chaves c set usos = c.usos + 1 where c.id = chave.id;

    return query select grupo.id, grupo.nome, chave.papel;
end;
$$;

create or replace function public.sair_do_grupo(p_grupo uuid)
    returns void
    language plpgsql
    security definer
    set search_path = public
as $$
declare
    quem uuid := auth.uid();
    meu_papel text;
begin
    if quem is null then
        raise exception 'Entre na sua conta primeiro.';
    end if;

    select m.papel into meu_papel
    from public.grupo_membros m
    where m.grupo_id = p_grupo and m.profile_id = quem;

    if meu_papel is null then
        return;
    end if;

    if meu_papel = 'diretoria'
        and not exists (
            select 1 from public.grupo_membros m
            where m.grupo_id = p_grupo and m.papel = 'diretoria' and m.profile_id <> quem
        )
        and exists (
            select 1 from public.grupo_membros m
            where m.grupo_id = p_grupo and m.profile_id <> quem
        )
    then
        raise exception 'Voce e a unica diretoria do grupo. Promova outra pessoa antes de sair.';
    end if;

    delete from public.grupo_membros
    where grupo_id = p_grupo and profile_id = quem;
end;
$$;

drop view if exists public.standings;
drop view if exists public.player_performance;

create view public.standings as
with lados as (
    select
        m.grupo_id                 as grupo_id,
        m.team_a_id                as team_id,
        m.score_a                  as pontos_pro,
        m.score_b                  as pontos_contra,
        (m.winner_id = m.team_a_id) as venceu
    from public.matches m
    where m.status = 'finalizado'
    union all
    select
        m.grupo_id,
        m.team_b_id,
        m.score_b,
        m.score_a,
        (m.winner_id = m.team_b_id)
    from public.matches m
    where m.status = 'finalizado'
)
select
    t.grupo_id                                                      as grupo_id,
    t.id                                                            as team_id,
    t.nome,
    t.sigla,
    t.cor_hex,
    coalesce(count(l.team_id), 0)::int                              as jogos,
    coalesce(
        sum(case when l.venceu then 1 else 0 end)
            filter (where l.team_id is not null), 0
    )::int                                                          as vitorias,
    coalesce(
        sum(case when l.venceu then 0 else 1 end)
            filter (where l.team_id is not null), 0
    )::int                                                          as derrotas,
    coalesce(sum(l.pontos_pro - l.pontos_contra), 0)::int           as saldo_pontos,
    coalesce(sum(l.pontos_pro), 0)::int                             as pontos_pro,
    coalesce(sum(l.pontos_contra), 0)::int                          as pontos_contra
from public.teams t
left join lados l on l.team_id = t.id
where t.ativo
group by t.grupo_id, t.id, t.nome, t.sigla, t.cor_hex, t.ordem
order by vitorias desc, saldo_pontos desc, pontos_pro desc, t.ordem;

create view public.player_performance as
select
    p.grupo_id                                            as grupo_id,
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
group by p.grupo_id, p.id, p.nome, p.genero, p.skill_level
order by vitorias desc, saldo_pontos desc, p.nome;

alter view public.standings set (security_invoker = true);
alter view public.player_performance set (security_invoker = true);

grant usage on schema public to authenticated;

grant select, insert, update, delete on
    public.grupos, public.grupo_membros, public.grupo_chaves
    to authenticated;

grant select on public.modelos_de_time, public.modelos_de_pagina to authenticated;
grant select on public.standings, public.player_performance to authenticated;

grant execute on function public.sou_membro(uuid) to authenticated;
grant execute on function public.e_diretoria(uuid) to authenticated;
grant execute on function public.compartilha_grupo(uuid) to authenticated;
grant execute on function public.gerar_codigo_de_chave() to authenticated;
grant execute on function public.gerar_slug(text) to authenticated;
grant execute on function public.normalizar_chave(text) to authenticated;
grant execute on function public.grupo_do_caminho(text) to authenticated;
grant execute on function public.criar_grupo(text, text) to authenticated;
grant execute on function public.entrar_no_grupo(text) to authenticated;
grant execute on function public.sair_do_grupo(uuid) to authenticated;

revoke insert, update, delete on public.dicas from authenticated;

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
    public.dicas;
