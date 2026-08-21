package com.pransetu.app.core.network.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pransetu.app.PransetuApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A WorkManager Worker that attempts to sync pending (offline) SOS records
 * to the backend (Firestore) when network connectivity is restored.
 */
class SosSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Retrieve dependencies from the Application class
                // In a production app, we would use Hilt/Dagger for DI.
                val app = applicationContext as? PransetuApplication
                val sosRepository = app?.sosRepository
                
                if (sosRepository == null) {
                    return@withContext Result.failure()
                }
                
                // Trigger the retry logic in the Room repository
                val successCount = sosRepository.retryPendingSos()
                
                // If we successfully synced any records, log it or handle it.
                // It's considered a success if we executed without throwing.
                Result.success()
            } catch (e: Exception) {
                // If we threw an exception, we want to retry later.
                Result.retry()
            }
        }
    }
    
    companion object {
        const val WORK_NAME = "SosOfflineSyncWorker"
    }
}
