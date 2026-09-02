package com.unidospelovolei.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unidospelovolei.BuildConfig
import com.unidospelovolei.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object Push {
    const val CANAL: String = "lembretes"

    val configurado: Boolean
        get() =
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
                BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
                BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
                BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    fun iniciar(context: Context) {
        if (!configurado) return
        if (FirebaseApp.getApps(context).isNotEmpty()) return

        val opcoes =
            FirebaseOptions
                .Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()

        runCatching { FirebaseApp.initializeApp(context, opcoes) }
        criarCanal(context)
    }

    fun criarCanal(context: Context) {
        val canal =
            NotificationChannel(
                CANAL,
                "Lembretes do sábado",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Aviso de jogo, cobrança e recados do grupo." }

        context
            .getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)
    }

    fun permissaoNecessaria(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun temPermissao(context: Context): Boolean {
        val permissao = permissaoNecessaria() ?: return true
        return ActivityCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun token(): String? {
        if (!configurado) return null
        return runCatching {
            suspendCancellableCoroutine { continuacao ->
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnSuccessListener { continuacao.resume(it) }
                    .addOnFailureListener { continuacao.resume(null) }
            }
        }.getOrNull()
    }
}

class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"] ?: return
        val corpo = message.notification?.body ?: message.data["corpo"].orEmpty()

        if (!Push.temPermissao(this)) return

        val abrir =
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        val notificacao =
            NotificationCompat
                .Builder(this, Push.CANAL)
                .setSmallIcon(R.drawable.logo_upv)
                .setContentTitle(titulo)
                .setContentText(corpo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(titulo.hashCode(), notificacao)
        }
    }

    override fun onNewToken(token: String) {
        TokenPendente.ultimo = token
    }
}

object TokenPendente {
    @Volatile
    var ultimo: String? = null
}
