package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.model.CategoriaPagina
import com.unidospelovolei.domain.model.EMOJI_PADRAO
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.Post
import com.unidospelovolei.domain.model.TipoEvento
import kotlinx.coroutines.flow.Flow

class GrupoRepository(
    private val db: PowerSyncDatabase,
) {
    fun observePosts(profileId: String): Flow<List<Post>> =
        db.watch(
            """
            SELECT
                p.id, p.autor_nome, p.titulo, p.corpo, p.imagem_url, p.emoji,
                p.fixado, p.publicado_em,
                (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id) AS reacoes,
                (SELECT COUNT(*) FROM post_reacoes r WHERE r.post_id = p.id AND r.profile_id = ?) AS reagi
            FROM posts p
            ORDER BY p.fixado DESC, p.publicado_em DESC
            """.trimIndent(),
            listOf(profileId),
        ) { it.toPost() }

    fun observeEventos(): Flow<List<Evento>> =
        db.watch(
            """
            SELECT id, titulo, descricao, tipo, inicio, local
            FROM eventos
            ORDER BY inicio
            """.trimIndent(),
        ) { it.toEvento() }

    fun observePaginas(): Flow<List<Pagina>> =
        db.watch(
            """
            SELECT id, slug, categoria, titulo, corpo, ordem
            FROM paginas
            ORDER BY categoria, ordem, titulo COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPagina() }

    suspend fun publicar(
        autorProfileId: String,
        autorNome: String?,
        titulo: String,
        corpo: String,
        fixado: Boolean,
        imagemUrl: String?,
        emoji: String,
    ) {
        val agora = agoraIso()
        db.execute(
            """
            INSERT INTO posts (
                id, autor_profile_id, autor_nome, titulo, corpo, imagem_url, emoji,
                fixado, publicado_em, atualizado_em
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(
                novoId(),
                autorProfileId,
                autorNome,
                titulo.trim(),
                corpo.trim(),
                imagemUrl?.trim()?.ifBlank { null },
                emoji.ifBlank { EMOJI_PADRAO },
                if (fixado) 1 else 0,
                agora,
                agora,
            ),
        )
    }

    suspend fun excluirPost(postId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM post_reacoes WHERE post_id = ?", listOf(postId))
            tx.execute("DELETE FROM posts WHERE id = ?", listOf(postId))
        }
    }

    suspend fun alternarReacao(
        postId: String,
        profileId: String,
        emoji: String,
    ) {
        db.writeTransactionAsync { tx ->
            val existente =
                tx
                    .getAll(
                        "SELECT id FROM post_reacoes WHERE post_id = ? AND profile_id = ?",
                        listOf(postId, profileId),
                    ) { it.getString("id") }
                    .firstOrNull()

            if (existente == null) {
                tx.execute(
                    "INSERT INTO post_reacoes (id, post_id, profile_id, emoji, criado_em) VALUES (?, ?, ?, ?, ?)",
                    listOf(novoId(), postId, profileId, emoji.ifBlank { EMOJI_PADRAO }, agoraIso()),
                )
            } else {
                tx.execute("DELETE FROM post_reacoes WHERE id = ?", listOf(existente))
            }
        }
    }

    suspend fun salvarEvento(
        eventoId: String?,
        titulo: String,
        descricao: String?,
        tipo: TipoEvento,
        inicio: String,
        local: String?,
        criadoPor: String,
    ) {
        if (eventoId == null) {
            db.execute(
                """
                INSERT INTO eventos (id, titulo, descricao, tipo, inicio, local, criado_por, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(
                    novoId(),
                    titulo.trim(),
                    descricao?.trim()?.ifBlank { null },
                    tipo.value,
                    inicio,
                    local?.trim()?.ifBlank { null },
                    criadoPor,
                    agoraIso(),
                ),
            )
        } else {
            db.execute(
                """
                UPDATE eventos
                SET titulo = ?, descricao = ?, tipo = ?, inicio = ?, local = ?
                WHERE id = ?
                """.trimIndent(),
                listOf(
                    titulo.trim(),
                    descricao?.trim()?.ifBlank { null },
                    tipo.value,
                    inicio,
                    local?.trim()?.ifBlank { null },
                    eventoId,
                ),
            )
        }
    }

    suspend fun excluirEvento(eventoId: String) {
        db.execute("DELETE FROM eventos WHERE id = ?", listOf(eventoId))
    }

    suspend fun salvarPagina(
        paginaId: String,
        titulo: String,
        corpo: String,
        atualizadoPor: String,
    ) {
        db.execute(
            """
            UPDATE paginas
            SET titulo = ?, corpo = ?, atualizado_por = ?, atualizado_em = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(titulo.trim(), corpo, atualizadoPor, agoraIso(), paginaId),
        )
    }

    suspend fun criarPagina(
        categoria: CategoriaPagina,
        titulo: String,
        atualizadoPor: String,
    ) {
        val slug =
            titulo
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "pagina-${novoId().take(8)}" }

        db.execute(
            """
            INSERT INTO paginas (id, slug, categoria, titulo, corpo, ordem, atualizado_por, atualizado_em)
            VALUES (?, ?, ?, ?, '', 99, ?, ?)
            """.trimIndent(),
            listOf(novoId(), slug, categoria.value, titulo.trim(), atualizadoPor, agoraIso()),
        )
    }

    suspend fun excluirPagina(paginaId: String) {
        db.execute("DELETE FROM paginas WHERE id = ?", listOf(paginaId))
    }
}
