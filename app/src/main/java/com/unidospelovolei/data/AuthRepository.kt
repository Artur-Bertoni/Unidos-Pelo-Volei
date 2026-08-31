package com.unidospelovolei.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.UUID

/**
 * Login com Google.
 *
 * O fluxo tem duas etapas: o Credential Manager (Google Identity Services)
 * devolve um ID token do Google e o Supabase Auth troca esse token por uma
 * sessao. O JWT do Supabase e o que autoriza tanto o PowerSync quanto as
 * escritas no Postgrest.
 *
 * O nonce vai em duas versoes: o Google recebe o hash SHA-256 e o Supabase
 * recebe o valor original, que ele mesmo compara com o hash dentro do token.
 */
class AuthRepository(
    private val supabase: SupabaseClient,
    private val appContext: Context,
    private val googleWebClientId: String,
) {
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    val currentUserId: String? get() = supabase.auth.currentUserOrNull()?.id

    suspend fun signInWithGoogle(activityContext: Context) {
        check(googleWebClientId.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID nao configurado em local.properties"
        }

        val nonceOriginal = UUID.randomUUID().toString()
        val nonceHash = sha256(nonceOriginal)

        val opcao =
            GetSignInWithGoogleOption
                .Builder(googleWebClientId)
                .setNonce(nonceHash)
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

        supabase.auth.signInWith(IDToken) {
            idToken = tokenGoogle
            provider = Google
            nonce = nonceOriginal
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    private fun sha256(valor: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(valor.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        val TIPOS_GOOGLE =
            setOf(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
            )
    }
}
