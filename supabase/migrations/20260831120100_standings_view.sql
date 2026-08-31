-- =============================================================================
-- VIEW standings: classificacao geral calculada a partir das partidas
-- finalizadas.
-- =============================================================================
-- Criterios de desempate, nesta ordem:
--   1. V  - vitorias
--   2. S  - saldo de pontos (pontos pro - pontos contra)
--   3. PP - pontos pro
--
-- Observacao: o app le a mesma consulta no SQLite local do PowerSync, porque o
-- PowerSync replica tabelas e nao views. A definicao esta espelhada em
-- app/src/main/java/com/unidospelovolei/data/StandingsQueries.kt e as duas
-- devem ser mantidas em sincronia.
-- =============================================================================

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
    coalesce(sum(case when l.venceu then 1 else 0 end), 0)::int     as vitorias,
    coalesce(sum(case when l.venceu then 0 else 1 end), 0)::int     as derrotas,
    coalesce(sum(l.pontos_pro - l.pontos_contra), 0)::int           as saldo_pontos,
    coalesce(sum(l.pontos_pro), 0)::int                             as pontos_pro,
    coalesce(sum(l.pontos_contra), 0)::int                          as pontos_contra
from public.teams t
left join lados l on l.team_id = t.id
where t.ativo
group by t.id, t.nome, t.sigla, t.cor_hex, t.ordem
order by vitorias desc, saldo_pontos desc, pontos_pro desc, t.ordem;

comment on view public.standings is
    'Classificacao geral (V, S, PP) a partir das partidas finalizadas.';
