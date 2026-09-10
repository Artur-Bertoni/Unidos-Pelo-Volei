package com.unidospelovolei.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GrupoAtivo(
    context: Context,
) {
    private val preferencias =
        context.applicationContext.getSharedPreferences("grupos", Context.MODE_PRIVATE)

    private val estado = MutableStateFlow<String?>(null)
    private var usuario: String? = null

    val id: StateFlow<String?> = estado.asStateFlow()

    val atual: String? get() = estado.value

    fun exigir(): String =
        estado.value ?: throw IllegalStateException("Escolha um grupo antes de continuar.")

    fun paraUsuario(usuarioId: String?) {
        if (usuario == usuarioId) return
        usuario = usuarioId
        estado.value = usuarioId?.let { preferencias.getString(chaveDe(it), null) }
    }

    fun selecionar(grupoId: String?) {
        estado.value = grupoId
        val dono = usuario ?: return
        val editor = preferencias.edit()
        if (grupoId == null) editor.remove(chaveDe(dono)) else editor.putString(chaveDe(dono), grupoId)
        editor.apply()
    }

    private fun chaveDe(usuarioId: String): String = "grupo_ativo_$usuarioId"
}
