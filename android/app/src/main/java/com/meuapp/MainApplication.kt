package com.meuapp

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import com.meuapp.NetworkInfoPackage

class MainApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> {
                val packages = PackageList(this).packages.toMutableList()
                packages.add(NetworkInfoPackage())
                return packages
            }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        }

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, OpenSourceMergedSoMapping)
        
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            load()
        }
        
        // Chama explicitamente o método para ativar a captura
        scheduleNetworkMonitoringWorker()
    }

    private fun scheduleNetworkMonitoringWorker() {
        try {
            WorkManager.getInstance(this).cancelUniqueWork("NetworkLogger")
            
            NetworkMonitoringWorker.schedule(this)
            Log.d("MainApplication", "NetworkMonitoringWorker ativado e agendado com sucesso")
        } catch (e: Exception) {
            Log.e("MainApplication", "Erro ao ativar NetworkMonitoringWorker", e)
        }
    }
}