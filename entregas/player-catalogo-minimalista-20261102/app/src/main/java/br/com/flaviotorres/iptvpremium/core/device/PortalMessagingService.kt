package br.com.flaviotorres.iptvpremium.core.device

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.flaviotorres.iptvpremium.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Push only requests the existing authenticated queue; it never trusts playlist payloads from FCM. */
internal object PortalPush {
    @Volatile private var hints: PushHintCoordinator? = null
    private fun coordinator(context: Context): PushHintCoordinator = hints ?: synchronized(this) {
        hints ?: PushHintCoordinator(
            readPending = { preferences(context.applicationContext).getString("pendingHint", null) },
            savePending = { value -> check(preferences(context.applicationContext).edit().apply {
                if (value == null) remove("pendingHint") else putString("pendingHint", value)
            }.commit()) { "Nao foi possivel persistir o aviso." } }
        ).also { hints = it }
    }

    suspend fun observeWhileStarted(context: Context) {
        withContext(Dispatchers.IO) {
            coordinator(context).observe(sync = { synchronizeLists(context) }, failed = {
                Log.i("PortalPush", "Aviso pendente; nova tentativa agendada.")
                schedule(context, listsChanged = true)
            })
        }
    }

    private suspend fun synchronizeLists(context: Context) {
        // Once storage changes begin, finish the authenticated ACK even if the Activity stops.
        withContext(NonCancellable + Dispatchers.IO) {
            Log.i("PortalPush", "Iniciando sincronizacao de listas por aviso.")
            PortalListPreparation.begin()
            try { DevicePortalSync(context).sync(force = true, automatic = true, includeSubtitleCommands = true) }
            finally { PortalListPreparation.finish() }
            Log.i("PortalPush", "Sincronizacao por aviso concluida.")
        }
    }

    suspend fun consumePending(context: Context) {
        coordinator(context).drain { synchronizeLists(context) }
    }

    fun received(context: Context) {
        // Keep durable fallback even if the foreground consumer wins the race.
        coordinator(context).received()
        Log.i("PortalPush", "Aviso de listas recebido.")
        schedule(context, listsChanged = true)
    }

    fun initialize(context: Context) {
        if (BuildConfig.FIREBASE_APP_ID.isBlank() || BuildConfig.FIREBASE_API_KEY.isBlank() ||
            BuildConfig.FIREBASE_PROJECT_ID.isBlank() || BuildConfig.FIREBASE_SENDER_ID.isBlank() ||
            BuildConfig.DEVICE_PORTAL_URL.isBlank()) return
        if (GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) return
        try {
            if (FirebaseApp.getApps(context).none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build())
            }
            val messaging = FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled = true
            messaging.token.addOnSuccessListener { token -> saveToken(context, token); schedule(context) }
                .addOnFailureListener { Log.i("PortalPush", "Avisos indisponiveis; verificacao na abertura preservada.") }
        } catch (_: Exception) { Log.i("PortalPush", "Firebase indisponivel; sincronizacao manual preservada.") }
    }

    fun saveToken(context: Context, token: String) {
        if (token.length in 20..4096) preferences(context).edit().putString("token", token).apply()
    }
    private fun preferences(context: Context) = context.getSharedPreferences("portal_push_v1", Context.MODE_PRIVATE)

    fun schedule(context: Context, listsChanged: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<PortalPushWorker>()
            .setInputData(androidx.work.workDataOf("listsChanged" to listsChanged))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // A token retry cannot hold a list hint behind its backoff. Foreground consumes directly.
        val queue = if (listsChanged) "portal-list-hints-v2" else "portal-token-registration-v2"
        WorkManager.getInstance(context).enqueueUniqueWork(queue, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    suspend fun registerToken(context: Context) {
        val token = preferences(context).getString("token", null) ?: return
        DevicePortalSync(context).registerPushToken(token, BuildConfig.FIREBASE_PROJECT_ID)
    }
}

class PortalMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PortalPush.saveToken(applicationContext, token)
        PortalPush.schedule(applicationContext)
    }
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.from != BuildConfig.FIREBASE_SENDER_ID || !isPortalContentHint(message.data["type"],message.data["version"])) return
        PortalPush.received(applicationContext)
    }
}

class PortalPushWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (BuildConfig.DEVICE_PORTAL_URL.isBlank()) return@withContext Result.success()
        try {
            if (inputData.getBoolean("listsChanged", false)) {
                PortalPush.consumePending(applicationContext)
            } else if (PortalIdentityStore(applicationContext).registeredIdentity() == null) {
                DevicePortalSync(applicationContext).sync(force = true, automatic = true, includeSubtitleCommands = true)
            }
            if (!inputData.getBoolean("listsChanged", false)) PortalPush.registerToken(applicationContext)
            Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            Log.i("PortalPush", "Sincronizacao adiada; a fila sera verificada novamente na abertura.")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// IPTV_FCM_REPEATED_HINTS_V2
