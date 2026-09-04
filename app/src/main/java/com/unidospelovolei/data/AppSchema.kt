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
                    Column.text("profile_id"),
                    Column.text("foto_url"),
                    Column.integer("nascimento_dia"),
                    Column.integer("nascimento_mes"),
                    Column.text("entrou_em"),
                    Column.text("regime"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
            indexes =
                listOf(
                    Index("por_perfil", IndexedColumn.ascending("profile_id")),
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
                    Column.text("papel"),
                    Column.integer("is_admin"),
                    Column.text("created_at"),
                    Column.text("updated_at"),
                ),
        ),
        Table(
            name = "vinculo_pedidos",
            columns =
                listOf(
                    Column.text("profile_id"),
                    Column.text("player_id"),
                    Column.text("profile_nome"),
                    Column.text("status"),
                    Column.text("criado_em"),
                    Column.text("decidido_por"),
                    Column.text("decidido_em"),
                ),
            indexes =
                listOf(
                    Index("por_perfil", IndexedColumn.ascending("profile_id")),
                    Index("por_situacao", IndexedColumn.ascending("status")),
                ),
        ),
        Table(
            name = "player_contatos",
            columns =
                listOf(
                    Column.text("player_id"),
                    Column.text("profile_id"),
                    Column.text("telefone"),
                    Column.text("contato_emergencia"),
                    Column.integer("nascimento_ano"),
                    Column.text("atualizado_em"),
                ),
            indexes =
                listOf(
                    Index("por_atleta", IndexedColumn.ascending("player_id")),
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
        Table(
            name = "config_grupo",
            columns =
                listOf(
                    Column.text("jogo_hora"),
                    Column.text("jogo_local"),
                    Column.text("atualizado_em"),
                ),
        ),
        Table(
            name = "presencas",
            columns =
                listOf(
                    Column.text("player_id"),
                    Column.text("profile_id"),
                    Column.text("data"),
                    Column.text("status"),
                    Column.text("origem"),
                    Column.text("registrado_por"),
                    Column.text("atualizado_em"),
                ),
            indexes =
                listOf(
                    Index("por_data", IndexedColumn.ascending("data")),
                    Index("por_presente", IndexedColumn.ascending("player_id")),
                ),
        ),
        Table(
            name = "dispositivos",
            columns =
                listOf(
                    Column.text("profile_id"),
                    Column.text("token"),
                    Column.text("plataforma"),
                    Column.integer("ativo"),
                    Column.text("visto_em"),
                    Column.text("criado_em"),
                ),
        ),
        Table(
            name = "avisos",
            columns =
                listOf(
                    Column.text("tipo"),
                    Column.text("titulo"),
                    Column.text("corpo"),
                    Column.text("referencia"),
                    Column.text("criado_em"),
                ),
        ),
        Table(
            name = "posts",
            columns =
                listOf(
                    Column.text("autor_profile_id"),
                    Column.text("autor_nome"),
                    Column.text("titulo"),
                    Column.text("corpo"),
                    Column.text("imagem_url"),
                    Column.text("emoji"),
                    Column.integer("fixado"),
                    Column.text("publicado_em"),
                    Column.text("atualizado_em"),
                ),
        ),
        Table(
            name = "post_reacoes",
            columns =
                listOf(
                    Column.text("post_id"),
                    Column.text("profile_id"),
                    Column.text("emoji"),
                    Column.text("criado_em"),
                ),
            indexes =
                listOf(
                    Index("por_post", IndexedColumn.ascending("post_id")),
                ),
        ),
        Table(
            name = "eventos",
            columns =
                listOf(
                    Column.text("titulo"),
                    Column.text("descricao"),
                    Column.text("tipo"),
                    Column.text("inicio"),
                    Column.text("fim"),
                    Column.text("local"),
                    Column.text("criado_por"),
                    Column.text("criado_em"),
                ),
            indexes =
                listOf(
                    Index("por_inicio", IndexedColumn.ascending("inicio")),
                ),
        ),
        Table(
            name = "paginas",
            columns =
                listOf(
                    Column.text("slug"),
                    Column.text("categoria"),
                    Column.text("titulo"),
                    Column.text("corpo"),
                    Column.integer("ordem"),
                    Column.text("atualizado_por"),
                    Column.text("atualizado_em"),
                ),
        ),
        Table(
            name = "config_financeiro",
            columns =
                listOf(
                    Column.text("pix_chave"),
                    Column.text("pix_nome"),
                    Column.text("pix_cidade"),
                    Column.integer("mensalidade_centavos"),
                    Column.integer("diaria_centavos"),
                    Column.text("atualizado_em"),
                ),
        ),
        Table(
            name = "cobrancas",
            columns =
                listOf(
                    Column.text("titulo"),
                    Column.text("tipo"),
                    Column.integer("valor_centavos"),
                    Column.text("competencia"),
                    Column.text("vence_em"),
                    Column.text("criado_por"),
                    Column.text("criado_em"),
                ),
        ),
        Table(
            name = "pagamentos",
            columns =
                listOf(
                    Column.text("cobranca_id"),
                    Column.text("player_id"),
                    Column.text("profile_id"),
                    Column.integer("valor_centavos"),
                    Column.text("status"),
                    Column.text("pago_em"),
                    Column.text("registrado_por"),
                    Column.text("observacao"),
                    Column.text("criado_em"),
                ),
            indexes =
                listOf(
                    Index("por_cobranca", IndexedColumn.ascending("cobranca_id")),
                ),
        ),
        Table(
            name = "avaliacoes",
            columns =
                listOf(
                    Column.text("day_id"),
                    Column.text("avaliador_player_id"),
                    Column.text("avaliado_player_id"),
                    Column.integer("saque"),
                    Column.integer("passe"),
                    Column.integer("ataque"),
                    Column.integer("bloqueio"),
                    Column.integer("defesa"),
                    Column.integer("atitude"),
                    Column.text("criado_em"),
                ),
            insertOnly = true,
        ),
        Table(
            name = "avaliacao_registros",
            columns =
                listOf(
                    Column.text("day_id"),
                    Column.text("avaliador_player_id"),
                    Column.text("avaliado_player_id"),
                    Column.text("profile_id"),
                    Column.text("criado_em"),
                ),
            indexes =
                listOf(
                    Index("por_dia_avaliado", IndexedColumn.ascending("day_id")),
                ),
        ),
        Table(
            name = "player_evolucao",
            columns =
                listOf(
                    Column.text("player_id"),
                    Column.text("profile_id"),
                    Column.integer("total_avaliacoes"),
                    Column.real("saque_media"),
                    Column.real("passe_media"),
                    Column.real("ataque_media"),
                    Column.real("bloqueio_media"),
                    Column.real("defesa_media"),
                    Column.real("atitude_media"),
                    Column.text("atualizado_em"),
                ),
        ),
        Table(
            name = "dicas",
            columns =
                listOf(
                    Column.text("atributo"),
                    Column.real("faixa_max"),
                    Column.text("titulo"),
                    Column.text("texto"),
                    Column.integer("ordem"),
                ),
        ),
    )
