package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.ChaveDeAcesso
import com.unidospelovolei.domain.model.MembroDoGrupo
import com.unidospelovolei.domain.model.MeuGrupo
import com.unidospelovolei.domain.model.Papel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MeusGruposRepository(
    private val db: PowerSyncDatabase,
    private val supabase: SupabaseClient,
    private val grupoAtivo: GrupoAtivo,
) {
    fun observeMeusGrupos(profileId: String): Flow<List<MeuGrupo>> =
        db.watch(
            """
            SELECT g.id, g.nome, g.cidade, g.logo_url, g.ativo, m.papel
            FROM grupo_membros m
            JOIN grupos g ON g.id = m.grupo_id
            WHERE m.profile_id = ?
            ORDER BY g.nome COLLATE NOCASE
            """.trimIndent(),
            listOf(profileId),
        ) { it.toMeuGrupo() }

    fun observeGrupoAtual(profileId: String): Flow<MeuGrupo?> =
        combine(observeMeusGrupos(profileId), grupoAtivo.id) { grupos, escolhido ->
            grupos.firstOrNull { it.id == escolhido }
        }

    suspend fun salvarIdentidade(
        grupoId: String,
        nome: String,
        cidade: String?,
        logoUrl: String?,
    ) {
        require(nome.isNotBlank()) { "O grupo precisa de um nome." }
        db.execute(
            "UPDATE grupos SET nome = ?, cidade = ?, logo_url = ? WHERE id = ?",
            listOf(nome.trim(), cidade?.trim()?.ifBlank { null }, logoUrl, grupoId),
        )
    }

    suspend fun entrarComChave(codigo: String): MeuGrupo {
        val limpo = codigo.filter { it.isLetterOrDigit() }.uppercase()
        require(limpo.length >= 4) { "Digite a chave que a diretoria do grupo passou." }

        val resposta =
            supabase.postgrest
                .rpc("entrar_no_grupo", buildJsonObject { put("p_codigo", limpo) })
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: throw IllegalStateException("Chave não encontrada.")

        return MeuGrupo(
            id = resposta.textoObrigatorio("grupo_id"),
            nome = resposta.texto("nome").orEmpty(),
            cidade = null,
            papel = Papel.from(resposta.texto("papel")),
        )
    }

    suspend fun criarGrupo(
        nome: String,
        cidade: String?,
    ): MeuGrupo {
        require(nome.isNotBlank()) { "O grupo precisa de um nome." }

        val resposta =
            supabase.postgrest
                .rpc(
                    "criar_grupo",
                    buildJsonObject {
                        put("p_nome", nome.trim())
                        put("p_cidade", cidade?.trim()?.ifBlank { null })
                    },
                ).decodeList<JsonObject>()
                .firstOrNull()
                ?: throw IllegalStateException("Não foi possível criar o grupo.")

        return MeuGrupo(
            id = resposta.textoObrigatorio("grupo_id"),
            nome = resposta.texto("nome").orEmpty(),
            cidade = cidade?.trim()?.ifBlank { null },
            papel = Papel.DIRETORIA,
        )
    }

    suspend fun sair(grupoId: String) {
        supabase.postgrest.rpc("sair_do_grupo", buildJsonObject { put("p_grupo", grupoId) })
    }

    suspend fun listarChaves(grupoId: String): List<ChaveDeAcesso> =
        supabase
            .from("grupo_chaves")
            .select { filter { eq("grupo_id", grupoId) } }
            .decodeList<JsonObject>()
            .map { it.paraChave() }
            .sortedWith(compareByDescending<ChaveDeAcesso> { it.ativa }.thenBy { it.rotulo ?: "" })

    suspend fun criarChave(
        grupoId: String,
        rotulo: String?,
        papel: Papel,
        usosMax: Int?,
        expiraEm: String?,
    ): ChaveDeAcesso =
        supabase
            .from("grupo_chaves")
            .insert(
                buildJsonObject {
                    put("grupo_id", grupoId)
                    put("rotulo", rotulo?.trim()?.ifBlank { null })
                    put("papel", papel.value)
                    put("usos_max", usosMax)
                    put("expira_em", expiraEm)
                },
            ) { select() }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?.paraChave()
            ?: throw IllegalStateException("O servidor não devolveu a chave criada.")

    suspend fun definirChaveAtiva(
        chaveId: String,
        ativa: Boolean,
    ) {
        supabase
            .from("grupo_chaves")
            .update({ set("ativa", ativa) }) { filter { eq("id", chaveId) } }
    }

    suspend fun excluirChave(chaveId: String) {
        supabase.from("grupo_chaves").delete { filter { eq("id", chaveId) } }
    }

    suspend fun listarMembrosDoGrupoAtivo(): List<MembroDoGrupo> = listarMembros(grupoAtivo.exigir())

    suspend fun listarMembros(grupoId: String): List<MembroDoGrupo> =
        supabase
            .from("grupo_membros")
            .select(Columns.raw("profile_id, papel, profiles(id, nome, email)")) {
                filter { eq("grupo_id", grupoId) }
            }.decodeList<JsonObject>()
            .map { linha ->
                val perfil = linha.objetoEmbutido("profiles")
                MembroDoGrupo(
                    profileId = linha.textoObrigatorio("profile_id"),
                    nome = perfil?.texto("nome"),
                    email = perfil?.texto("email"),
                    papel = Papel.from(linha.texto("papel")),
                )
            }.sortedBy { it.rotulo.lowercase() }

    suspend fun definirPapel(
        grupoId: String,
        profileId: String,
        papel: Papel,
    ) {
        supabase.from("grupo_membros").update({ set("papel", papel.value) }) {
            filter {
                eq("grupo_id", grupoId)
                eq("profile_id", profileId)
            }
        }
    }

    suspend fun removerMembro(
        grupoId: String,
        profileId: String,
    ) {
        supabase.from("grupo_membros").delete {
            filter {
                eq("grupo_id", grupoId)
                eq("profile_id", profileId)
            }
        }
    }

    private fun JsonObject.paraChave(): ChaveDeAcesso =
        ChaveDeAcesso(
            id = textoObrigatorio("id"),
            codigo = texto("codigo").orEmpty(),
            rotulo = texto("rotulo"),
            papel = Papel.from(texto("papel")),
            usos = inteiro("usos"),
            usosMax = inteiroOuNulo("usos_max"),
            expiraEm = texto("expira_em"),
            ativa = booleano("ativa", true),
        )
}
