package com.meuapp.networkinfo

import android.content.Context
import androidx.work.WorkManager
import com.meuapp.NetworkInfoPackage
import com.meuapp.NetworkMonitoringWorker

object NetworkInfoSDK {

    fun start(context: Context) {
        // agenda o worker
        NetworkMonitoringWorker.schedule(context)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("NetworkLogger")
    }
}
