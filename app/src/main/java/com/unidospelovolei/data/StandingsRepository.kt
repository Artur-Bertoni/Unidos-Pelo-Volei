package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.Standing
import kotlinx.coroutines.flow.Flow

/**
 * Classificacao geral.
 *
 * No Postgres a classificacao e a VIEW `public.standings`
 * (supabase/migrations/20260831120100_standings_view.sql). O PowerSync replica
 * tabelas, nao views, entao a mesma definicao e reproduzida aqui sobre as
 * tabelas ja sincronizadas -- assim a tela continua funcionando offline.
 * Ao mexer em uma das duas, mexa na outra.
 */
class StandingsRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeStandings(): Flow<List<Standing>> = db.watch(STANDINGS_SQL) { it.toStanding() }

    companion object {
        val STANDINGS_SQL =
            """
            WITH lados AS (
                SELECT
                    m.team_a_id AS team_id,
                    m.score_a   AS pontos_pro,
                    m.score_b   AS pontos_contra,
                    CASE WHEN m.winner_id = m.team_a_id THEN 1 ELSE 0 END AS venceu
                FROM matches m
                WHERE m.status = 'finalizado'
                UNION ALL
                SELECT
                    m.team_b_id,
                    m.score_b,
                    m.score_a,
                    CASE WHEN m.winner_id = m.team_b_id THEN 1 ELSE 0 END
                FROM matches m
                WHERE m.status = 'finalizado'
            )
            SELECT
                t.id      AS team_id,
                t.nome    AS nome,
                t.sigla   AS sigla,
                t.cor_hex AS cor_hex,
                COUNT(l.team_id)                                    AS jogos,
                COALESCE(SUM(l.venceu), 0)                          AS vitorias,
                COALESCE(SUM(1 - l.venceu), 0)                      AS derrotas,
                COALESCE(SUM(l.pontos_pro - l.pontos_contra), 0)    AS saldo_pontos,
                COALESCE(SUM(l.pontos_pro), 0)                      AS pontos_pro,
                COALESCE(SUM(l.pontos_contra), 0)                   AS pontos_contra
            FROM teams t
            LEFT JOIN lados l ON l.team_id = t.id
            WHERE t.ativo = 1
            GROUP BY t.id, t.nome, t.sigla, t.cor_hex, t.ordem
            ORDER BY vitorias DESC, saldo_pontos DESC, pontos_pro DESC, t.ordem
            """.trimIndent()
    }
}
