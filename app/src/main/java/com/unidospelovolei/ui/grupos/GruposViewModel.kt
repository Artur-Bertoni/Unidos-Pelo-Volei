package com.unidospelovolei.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.AuthRepository
import com.unidospelovolei.data.GrupoAtivo
import com.unidospelovolei.data.GrupoStorage
import com.unidospelovolei.data.MeusGruposRepository
import com.unidospelovolei.domain.model.ChaveDeAcesso
import com.unidospelovolei.domain.model.MembroDoGrupo
import com.unidospelovolei.domain.model.MeuGrupo
import com.unidospelovolei.domain.model.Papel
import com.unidospelovolei.ui.grupo.ImagemEscolhida
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GruposUiState(
    val usuarioId: String? = null,
    val grupos: List<MeuGrupo> = emptyList(),
    val grupoAtual: MeuGrupo? = null,
    val chaves: List<ChaveDeAcesso> = emptyList(),
    val membros: List<MembroDoGrupo> = emptyList(),
    val carregandoLista: Boolean = false,
    val falhaNaLista: String? = null,
    val salvando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
) {
    val souDiretoria: Boolean get() = grupoAtual?.souDiretoria == true
}

private data class Listas(
    val chaves: List<ChaveDeAcesso> = emptyList(),
    val membros: List<MembroDoGrupo> = emptyList(),
    val carregando: Boolean = false,
    val falha: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class GruposViewModel(
    authRepository: AuthRepository,
    private val repositorio: MeusGruposRepository,
    private val grupoStorage: GrupoStorage,
    private val grupoAtivo: GrupoAtivo,
) : ViewModel() {
    private val chaves = MutableStateFlow<List<ChaveDeAcesso>>(emptyList())
    private val membros = MutableStateFlow<List<MembroDoGrupo>>(emptyList())
    private val carregandoLista = MutableStateFlow(false)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)
    private val aviso = MutableStateFlow<String?>(null)

    private val usuarioId =
        authRepository.sessionStatus.map { status ->
            (status as? SessionStatus.Authenticated)?.session?.user?.id
        }

    private val meusGrupos =
        usuarioId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repositorio.observeMeusGrupos(id).catch { emit(emptyList()) }
        }

    private val falhaNaLista = MutableStateFlow<String?>(null)

    private val listas =
        combine(chaves, membros, carregandoLista, falhaNaLista) { c, m, carregando, falha ->
            Listas(chaves = c, membros = m, carregando = carregando, falha = falha)
        }

    private val controles =
        combine(salvando, erro, aviso) { gravando, falha, mensagem ->
            Triple(gravando, falha, mensagem)
        }

    val estado: StateFlow<GruposUiState> =
        combine(
            usuarioId,
            meusGrupos,
            grupoAtivo.id,
            listas,
            controles,
        ) { id, grupos, escolhido, lista, (gravando, falha, mensagem) ->
            GruposUiState(
                usuarioId = id,
                grupos = grupos,
                grupoAtual = grupos.firstOrNull { it.id == escolhido },
                chaves = lista.chaves,
                membros = lista.membros,
                carregandoLista = lista.carregando,
                falhaNaLista = lista.falha,
                salvando = gravando,
                erro = falha,
                aviso = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GruposUiState())

    fun selecionar(grupoId: String) {
        grupoAtivo.selecionar(grupoId)
        chaves.value = emptyList()
        membros.value = emptyList()
    }

    fun limparMensagens() {
        erro.value = null
        aviso.value = null
    }

    fun entrarComChave(codigo: String) =
        executar {
            val grupo = repositorio.entrarComChave(codigo)
            grupoAtivo.selecionar(grupo.id)
            aviso.value = "Você entrou no ${grupo.nome}. Baixando os dados do grupo."
        }

    fun criarGrupo(
        nome: String,
        cidade: String?,
    ) = executar {
        val grupo = repositorio.criarGrupo(nome, cidade)
        grupoAtivo.selecionar(grupo.id)
        aviso.value = "Grupo ${grupo.nome} criado. Você é a diretoria e já tem uma chave de acesso."
    }

    fun salvarIdentidade(
        nome: String,
        cidade: String?,
        logoNova: ImagemEscolhida?,
        removerLogo: Boolean,
    ) {
        val grupo = estado.value.grupoAtual ?: return
        executar {
            val logo =
                when {
                    logoNova != null ->
                        grupoStorage.enviarLogo(grupo.id, logoNova.bytes, logoNova.extensao)

                    removerLogo -> null
                    else -> grupo.logoUrl
                }
            repositorio.salvarIdentidade(grupo.id, nome, cidade, logo)
            aviso.value = "Grupo atualizado."
        }
    }

    fun sair(grupoId: String) =
        executar {
            repositorio.sair(grupoId)
            if (grupoAtivo.atual == grupoId) grupoAtivo.selecionar(null)
            aviso.value = "Você saiu do grupo."
        }

    fun carregarChaves() {
        val grupo = estado.value.grupoAtual ?: return
        if (carregandoLista.value) return
        viewModelScope.launch {
            carregandoLista.value = true
            falhaNaLista.value = null
            runCatching { repositorio.listarChaves(grupo.id) }
                .onSuccess { chaves.value = it }
                .onFailure {
                    val recado = it.message ?: "Não foi possível carregar as chaves."
                    erro.value = recado
                    falhaNaLista.value = recado
                }
            carregandoLista.value = false
        }
    }

    fun criarChave(
        rotulo: String?,
        papel: Papel,
        usosMax: Int?,
        expiraEm: String?,
    ) {
        val grupo = estado.value.grupoAtual ?: return
        executar {
            val nova = repositorio.criarChave(grupo.id, rotulo, papel, usosMax, expiraEm)
            chaves.value = listOf(nova) + chaves.value
            aviso.value = "Chave ${nova.codigoFormatado} criada. Mande para quem vai entrar."
        }
    }

    fun alternarChave(chave: ChaveDeAcesso) =
        executar {
            repositorio.definirChaveAtiva(chave.id, !chave.ativa)
            chaves.value = chaves.value.map { if (it.id == chave.id) it.copy(ativa = !chave.ativa) else it }
        }

    fun excluirChave(chave: ChaveDeAcesso) =
        executar {
            repositorio.excluirChave(chave.id)
            chaves.value = chaves.value.filterNot { it.id == chave.id }
        }

    fun carregarMembros() {
        val grupo = estado.value.grupoAtual ?: return
        if (carregandoLista.value) return
        viewModelScope.launch {
            carregandoLista.value = true
            falhaNaLista.value = null
            runCatching { repositorio.listarMembros(grupo.id) }
                .onSuccess { membros.value = it }
                .onFailure {
                    val recado = it.message ?: "Não foi possível carregar os membros."
                    erro.value = recado
                    falhaNaLista.value = recado
                }
            carregandoLista.value = false
        }
    }

    fun definirPapel(
        membro: MembroDoGrupo,
        papel: Papel,
    ) {
        val grupo = estado.value.grupoAtual ?: return
        executar {
            repositorio.definirPapel(grupo.id, membro.profileId, papel)
            membros.value =
                membros.value.map { if (it.profileId == membro.profileId) it.copy(papel = papel) else it }
        }
    }

    fun removerMembro(membro: MembroDoGrupo) {
        val grupo = estado.value.grupoAtual ?: return
        executar {
            repositorio.removerMembro(grupo.id, membro.profileId)
            membros.value = membros.value.filterNot { it.profileId == membro.profileId }
        }
    }

    private fun executar(acao: suspend () -> Unit) {
        if (salvando.value) return
        viewModelScope.launch {
            salvando.value = true
            erro.value = null
            runCatching { acao() }
                .onFailure { erro.value = it.message ?: "Não foi possível concluir." }
            salvando.value = false
        }
    }
}
