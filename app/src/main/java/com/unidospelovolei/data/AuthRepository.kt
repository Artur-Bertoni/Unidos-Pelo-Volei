package com.unidospelovolei.data

import android.content.Context
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

sealed interface EstadoDaSessao {
    data object Carregando : EstadoDaSessao

    data class Autenticado(
        val usuarioId: String,
        val email: String?,
    ) : EstadoDaSessao

    data class Desconectado(
        val saiu: Boolean,
    ) : EstadoDaSessao
}

class AuthRepository(
    private val apiProvider: () -> ApiClient,
    private val appContext: Context,
    private val googleWebClientId: String,
) {
    private val preferencias = appContext.getSharedPreferences("sessao", Context.MODE_PRIVATE)
    private val trava = Mutex()

    private val _estado = MutableStateFlow<EstadoDaSessao>(EstadoDaSessao.Carregando)
    val estado: StateFlow<EstadoDaSessao> = _estado.asStateFlow()

    val usuarioId: Flow<String?> =
        estado.map { (it as? EstadoDaSessao.Autenticado)?.usuarioId }.distinctUntilChanged()

    init {
        val usuario = preferencias.getString(USUARIO, null)
        val refresh = preferencias.getString(REFRESH, null)
        _estado.value =
            if (usuario != null && refresh != null) {
                EstadoDaSessao.Autenticado(usuario, preferencias.getString(EMAIL, null))
            } else {
                EstadoDaSessao.Desconectado(saiu = false)
            }
    }

    suspend fun signInWithGoogle(activityContext: Context) {
        check(googleWebClientId.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID nao configurado em local.properties"
        }

        val nonceOriginal = UUID.randomUUID().toString()
        val opcao =
            GetSignInWithGoogleOption
                .Builder(googleWebClientId)
                .setNonce(sha256(nonceOriginal))
                .build()

        val resposta =
            try {
                CredentialManager
                    .create(appContext)
                    .getCredential(
                        context = activityContext,
                        request = GetCredentialRequest.Builder().addCredentialOption(opcao).build(),
                    )
            } catch (e: NoCredentialException) {
                throw IllegalStateException(
                    "Nenhuma conta Google disponivel neste aparelho. Adicione uma conta " +
                        "nas configuracoes do Android e tente de novo.",
                    e,
                )
            } catch (e: GetCredentialCancellationException) {
                throw IllegalStateException("Login cancelado.", e)
            }

        val credencial = resposta.credential
        check(credencial is CustomCredential && credencial.type in TIPOS_GOOGLE) {
            "Credencial inesperada devolvida pelo Credential Manager: ${credencial.type}"
        }

        val tokenGoogle = GoogleIdTokenCredential.createFrom(credencial.data).idToken
        val sessao =
            apiProvider().post(
                "/auth/google",
                buildJsonObject {
                    put("idToken", tokenGoogle)
                    put("nonce", nonceOriginal)
                },
                autenticado = false,
            )
        trava.withLock { guardar(sessao.jsonObject) }
    }

    suspend fun signOut() {
        val refresh = preferencias.getString(REFRESH, null)
        trava.withLock { limpar() }
        _estado.value = EstadoDaSessao.Desconectado(saiu = true)
        if (refresh != null) {
            runCatching {
                apiProvider().post("/auth/sair", buildJsonObject { put("refresh_token", refresh) }, autenticado = false)
            }
        }
    }

    suspend fun tokenDeAcesso(forcarRenovacao: Boolean = false): String? =
        trava.withLock {
            val refresh = preferencias.getString(REFRESH, null) ?: return@withLock null
            val atual = preferencias.getString(ACESSO, null)
            val expiraEm = preferencias.getLong(EXPIRA_EM, 0L)
            val agora = System.currentTimeMillis() / 1000
            if (!forcarRenovacao && atual != null && expiraEm - agora > MARGEM_EM_SEGUNDOS) return@withLock atual

            val sessao =
                try {
                    apiProvider().post(
                        "/auth/renovar",
                        buildJsonObject { put("refresh_token", refresh) },
                        autenticado = false,
                    )
                } catch (e: ErroDaApi) {
                    if (e.status == 401) {
                        limpar()
                        _estado.value = EstadoDaSessao.Desconectado(saiu = true)
                        return@withLock null
                    }
                    throw e
                }
            guardar(sessao.jsonObject)
            preferencias.getString(ACESSO, null)
        }

    private fun guardar(sessao: JsonObject) {
        val usuario = sessao["user"]?.jsonObject
        val id = usuario?.textoObrigatorio("id") ?: throw IllegalStateException("O servidor não devolveu o usuário.")
        val email = usuario.texto("email")
        preferencias.edit {
            putString(ACESSO, sessao.textoObrigatorio("access_token"))
            putLong(EXPIRA_EM, sessao["expires_at"]?.jsonPrimitive?.longOrNull ?: 0L)
            putString(REFRESH, sessao.textoObrigatorio("refresh_token"))
            putString(USUARIO, id)
            putString(EMAIL, email)
        }
        val anterior = _estado.value
        if (anterior !is EstadoDaSessao.Autenticado || anterior.usuarioId != id) {
            _estado.value = EstadoDaSessao.Autenticado(id, email)
        }
    }

    private fun limpar() {
        preferencias.edit { clear() }
    }

    private fun sha256(valor: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(valor.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val ACESSO = "access_token"
        const val EXPIRA_EM = "expires_at"
        const val REFRESH = "refresh_token"
        const val USUARIO = "user_id"
        const val EMAIL = "email"
        const val MARGEM_EM_SEGUNDOS = 120L

        val TIPOS_GOOGLE =
            setOf(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
            )
    }
}
