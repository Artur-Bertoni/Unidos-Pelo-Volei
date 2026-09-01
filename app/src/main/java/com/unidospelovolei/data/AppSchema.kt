package com.unidospelovolei.data

import com.powersync.db.schema.Column
import com.powersync.db.schema.Index
import com.powersync.db.schema.IndexedColumn
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table

val AppSchema: Schema =
    Schema(
        Table(
            name = "players",
            columns =
                listOf(
                    Column.text("nome"),
                    Column.integer("skill_level"),
                    Column.text("genero"),
                    Column.integer("ativo"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
        ),
        Table(
            name = "teams",
            columns =
                listOf(
                    Column.text("nome"),
                    Column.text("cor_hex"),
                    Column.text("sigla"),
                    Column.integer("ativo"),
                    Column.integer("ordem"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
        ),
        Table(
            name = "team_players",
            columns =
                listOf(
                    Column.text("team_id"),
                    Column.text("player_id"),
                ),
            indexes =
                listOf(
                    Index("por_time", IndexedColumn.ascending("team_id")),
                    Index("por_jogador", IndexedColumn.ascending("player_id")),
                ),
        ),
        Table(
            name = "rounds",
            columns =
                listOf(
                    Column.integer("numero"),
                    Column.integer("fase"),
                    Column.text("created_at"),
                ),
        ),
        Table(
            name = "matches",
            columns =
                listOf(
                    Column.text("round_id"),
                    Column.integer("quadra"),
                    Column.text("team_a_id"),
                    Column.text("team_b_id"),
                    Column.integer("score_a"),
                    Column.integer("score_b"),
                    Column.text("status"),
                    Column.text("winner_id"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
            indexes =
                listOf(
                    Index("por_rodada", IndexedColumn.ascending("round_id")),
                ),
        ),
        Table(
            name = "profiles",
            columns =
                listOf(
                    Column.text("email"),
                    Column.text("nome"),
                    Column.integer("is_admin"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
        ),
        Table(
            name = "game_days",
            columns =
                listOf(
                    Column.text("encerrado_em"),
                    Column.integer("partidas"),
                    Column.text("created_at"),
                ),
        ),
        Table(
            name = "player_day_stats",
            columns =
                listOf(
                    Column.text("day_id"),
                    Column.text("player_id"),
                    Column.text("team_id"),
                    Column.text("team_nome"),
                    Column.text("team_cor_hex"),
                    Column.integer("jogos"),
                    Column.integer("vitorias"),
                    Column.integer("derrotas"),
                    Column.integer("pontos_pro"),
                    Column.integer("pontos_contra"),
                    Column.text("created_at"),
                ),
            indexes =
                listOf(
                    Index("por_dia", IndexedColumn.ascending("day_id")),
                    Index("por_atleta", IndexedColumn.ascending("player_id")),
                ),
        ),
    )
