package com.pransetu.app.core.network.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncManager {

    /**
     * Enqueues a one-time sync task to run immediately when network is connected.
     * Useful for triggering a sync as soon as connectivity is restored.
     */
    fun enqueueOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<SosSyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            SosSyncWorker.WORK_NAME + "_OneTime",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Enqueues a periodic sync task to run every 15 minutes (minimum allowed by WorkManager).
     * Useful as a fallback to ensure offline records are eventually synced.
     */
    fun enqueuePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<SosSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SosSyncWorker.WORK_NAME + "_Periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
