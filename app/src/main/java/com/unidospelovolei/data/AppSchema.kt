package com.unidospelovolei.data

import com.powersync.db.schema.Column
import com.powersync.db.schema.Index
import com.powersync.db.schema.IndexedColumn
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table

/**
 * Espelho local (SQLite) das tabelas sincronizadas pelo PowerSync.
 *
 * Regras da biblioteca que valem lembrar:
 *  - a coluna `id` e implicita e nao pode ser declarada aqui;
 *  - o SQLite nao tem boolean, entao `ativo` e `is_admin` chegam como 0/1;
 *  - timestamps chegam como texto ISO-8601.
 *
 * Precisa bater com as sync rules em `powersync/sync-rules.yaml`.
 */
val AppSchema: Schema =
    Schema(
        Table(
            name = "players",
            columns =
                listOf(
                    Column.text("nome"),
                    Column.integer("skill_level"),
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
    )
