package org.wordpress.android.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.helpers.logfile.LogFileProvider
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EncryptedLogging @Inject constructor(
    private val dispatcher: Dispatcher,
    private val encryptedLogStore: EncryptedLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)

    fun start() {
        coroutineScope.launch {
            dispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
            encryptedLogStore.uploadQueuedEncryptedLogs()
        }
    }

    fun queueCurrentLog(logFileProvider: LogFileProvider): String? {
        // TODO: Check how long log files are kept for and increase the duration if necessary
        logFileProvider.getLogFiles().lastOrNull()?.let { file ->
            if (file.exists()) {
                val uuid = UUID.randomUUID().toString()
                val payload = UploadEncryptedLogPayload(uuid = uuid, file = file)
                dispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
                return uuid
            }
        }
        return null
    }

    @Suppress("unused")
    @Subscribe(threadMode = ASYNC)
    fun onEncryptedLogUploaded(event: OnEncryptedLogUploaded) {
        if (event.isError) {
            AppLog.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} failed to upload with error: ${event.error}")
        } else {
            AppLog.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} uploaded successfully!")
        }
    }
}
