alter table public.players
    drop constraint if exists players_posicao_valida;

alter table public.players
    drop column if exists posicao;

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

    if new.regime is distinct from old.regime
        and (new.regime = 'isento' or old.regime = 'isento')
    then
        raise exception 'Isencao so a diretoria concede.';
    end if;

    return new;
end;
$$;

update public.eventos set tipo = 'jogo' where tipo = 'amistoso';
update public.eventos set tipo = 'confraternizacao' where tipo not in ('jogo', 'campeonato');

alter table public.eventos
    alter column tipo set default 'jogo';

do $$
declare
    antiga text;
begin
    for antiga in
        select c.conname
        from pg_constraint c
        where c.conrelid = 'public.eventos'::regclass
          and c.contype = 'c'
          and pg_get_constraintdef(c.oid) like '%amistoso%'
    loop
        execute format('alter table public.eventos drop constraint %I', antiga);
    end loop;

    if not exists (select 1 from pg_constraint where conname = 'eventos_tipo_valido') then
        alter table public.eventos
            add constraint eventos_tipo_valido check (tipo in ('jogo', 'confraternizacao', 'campeonato'));
    end if;
end;
$$;

alter table public.posts
    add column if not exists imagem_url text,
    add column if not exists emoji text not null default '👏';

update public.posts set emoji = '👏' where emoji is null or btrim(emoji) = '';

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'mural',
    'mural',
    true,
    5242880,
    array['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
on conflict (id) do update
set public = true,
    file_size_limit = 5242880,
    allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

drop policy if exists mural_leitura_publica on storage.objects;
create policy mural_leitura_publica on storage.objects
    for select to public
    using (bucket_id = 'mural');

drop policy if exists mural_envio_diretoria on storage.objects;
create policy mural_envio_diretoria on storage.objects
    for insert to authenticated
    with check (bucket_id = 'mural' and public.is_admin());

drop policy if exists mural_troca_diretoria on storage.objects;
create policy mural_troca_diretoria on storage.objects
    for update to authenticated
    using (bucket_id = 'mural' and public.is_admin())
    with check (bucket_id = 'mural' and public.is_admin());

drop policy if exists mural_exclusao_diretoria on storage.objects;
create policy mural_exclusao_diretoria on storage.objects
    for delete to authenticated
    using (bucket_id = 'mural' and public.is_admin());

update public.paginas
set titulo = 'Regras do vôlei de areia — quarteto',
    corpo = E'# O formato do grupo\n\nQuatro de cada lado, na areia, quadra de 16 m por 8 m. Não existe rodízio de posição dentro da quadra: quem está na rede fica na rede, quem está no fundo fica no fundo. O que roda é a **ordem de saque**, e ela é fixa do começo ao fim do set.\n\n# Contagem\n\n- Set até **21 pontos**, com dois de vantagem.\n- Melhor de três. O terceiro set, se precisar, vai até **15**, também com dois de vantagem.\n- Ponto em toda jogada, saque de quem ganhou o ponto.\n- Troca de lado a cada **7 pontos** somados; no set decisivo, a cada **5**.\n\n# Toques\n\n- Três toques por equipe antes de devolver a bola.\n- **O bloqueio conta como toque.** Bloqueou, sobraram dois.\n- Quem bloqueia pode tocar de novo em seguida: o bloqueio é o primeiro dos três.\n- O mesmo jogador não toca duas vezes seguidas fora essa situação.\n\n# O que a areia proíbe\n\n- **Largada de dedo aberto.** Para colocar a bola, use os nós dos dedos, a mão fechada ou a mão espalmada.\n- **Levantar de toque na recepção do saque.** Recebeu, recebe de manchete.\n- Levantada de toque só atravessa a rede se sair perpendicular aos ombros de quem levantou.\n- Segurar, conduzir ou acompanhar a bola no toque.\n\n# Faltas de sempre\n\n- Encostar na rede durante a jogada.\n- Invadir a quadra adversária por baixo da rede atrapalhando o adversário.\n- Sacar fora da ordem combinada.\n- Atacar a bola de saque perto da rede.\n\n# Vento, sol e bom senso\n\nNa areia o vento é parte do jogo: a bola vai voltar, vai cair na sua mão e vai fugir dela. Combine o lado antes do set e reclame menos, jogue mais.'
where slug = 'regras-do-volei';
