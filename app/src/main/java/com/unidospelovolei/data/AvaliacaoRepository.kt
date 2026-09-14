package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.unidospelovolei.domain.model.AvaliacaoPendente
import com.unidospelovolei.domain.model.Dica
import com.unidospelovolei.domain.model.Evolucao
import com.unidospelovolei.domain.model.NotasDaAvaliacao
import com.unidospelovolei.domain.model.PontoDaNota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AvaliacaoRepository(
    private val db: PowerSyncDatabase,
    private val grupoAtivo: GrupoAtivo,
) {
    fun observePendentes(playerId: String): Flow<List<AvaliacaoPendente>> =
        db.observarNoGrupo(
            grupoAtivo,
            PENDENTES_SQL,
            listOf(playerId, playerId),
        ) { cursor ->
            AvaliacaoPendente(
                dayId = cursor.getString("day_id"),
                avaliadoPlayerId = cursor.getString("avaliado_player_id"),
                avaliadoNome = cursor.getStringOptional("avaliado_nome").orEmpty(),
            )
        }

    fun observeEvolucao(): Flow<Evolucao?> =
        db
            .observarNoGrupo(
                grupoAtivo,
                """
                SELECT player_id, total_avaliacoes
                FROM player_evolucao
                WHERE grupo_id = ?
                LIMIT 1
                """.trimIndent(),
            ) { it.toEvolucao() }
            .map { it.firstOrNull() }

    fun observeHistorico(playerId: String): Flow<List<PontoDaNota>> =
        db.observarNoGrupo(
            grupoAtivo,
            """
            SELECT id, origem, avaliadores, nota_saque, nota_passe, nota_ataque,
                   nota_bloqueio, nota_defesa, nota_atitude, media, registrado_em
            FROM player_nota_historico
            WHERE grupo_id = ? AND player_id = ?
            ORDER BY registrado_em, id
            """.trimIndent(),
            listOf(playerId),
        ) { it.toPontoDaNota() }

    fun observeDicas(): Flow<List<Dica>> =
        db.watch(
            """
            SELECT id, atributo, faixa_max, titulo, texto
            FROM dicas
            ORDER BY atributo, faixa_max, ordem
            """.trimIndent(),
        ) { it.toDica() }

    suspend fun avaliar(
        dayId: String,
        avaliadorPlayerId: String,
        avaliadoPlayerId: String,
        notas: NotasDaAvaliacao,
    ) {
        db.execute(
            """
            INSERT INTO avaliacoes (
                id, grupo_id, day_id, avaliador_player_id, avaliado_player_id,
                saque, passe, ataque, bloqueio, defesa, atitude, criado_em
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(
                novoId(),
                grupoAtivo.exigir(),
                dayId,
                avaliadorPlayerId,
                avaliadoPlayerId,
                notas.saque,
                notas.passe,
                notas.ataque,
                notas.bloqueio,
                notas.defesa,
                notas.atitude,
                agoraIso(),
            ),
        )
    }

    private companion object {
        const val PENDENTES_SQL =
            """
            SELECT
                meu.day_id                AS day_id,
                colega.player_id          AS avaliado_player_id,
                p.nome                    AS avaliado_nome
            FROM player_day_stats meu
            JOIN player_day_stats colega
                ON colega.day_id = meu.day_id
                AND colega.team_id = meu.team_id
                AND colega.player_id <> meu.player_id
            JOIN players p ON p.id = colega.player_id
            JOIN (
                SELECT id FROM game_days WHERE grupo_id = ? ORDER BY encerrado_em DESC LIMIT 4
            ) d ON d.id = meu.day_id
            WHERE meu.player_id = ?
                AND meu.team_id IS NOT NULL
                AND NOT EXISTS (
                    SELECT 1 FROM avaliacao_registros r
                    WHERE r.day_id = meu.day_id
                        AND r.avaliador_player_id = ?
                        AND r.avaliado_player_id = colega.player_id
                )
            ORDER BY meu.day_id DESC, p.nome COLLATE NOCASE
            """
    }
}
