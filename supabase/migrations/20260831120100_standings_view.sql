create or replace view public.standings as
with lados as (
    select
        m.team_a_id                as team_id,
        m.score_a                  as pontos_pro,
        m.score_b                  as pontos_contra,
        (m.winner_id = m.team_a_id) as venceu
    from public.matches m
    where m.status = 'finalizado'
    union all
    select
        m.team_b_id,
        m.score_b,
        m.score_a,
        (m.winner_id = m.team_b_id)
    from public.matches m
    where m.status = 'finalizado'
)
select
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
group by t.id, t.nome, t.sigla, t.cor_hex, t.ordem
order by vitorias desc, saldo_pontos desc, pontos_pro desc, t.ordem;
