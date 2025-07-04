package com.meuapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.*
import android.util.Log
import com.facebook.react.bridge.*
import com.google.android.gms.location.LocationServices

class NetworkInfoModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext)

    override fun getName(): String = "NetworkInfoModule"

    @SuppressLint("MissingPermission")
    @ReactMethod
    fun getNetworkInfo(promise: Promise) {
        val telephonyManager = reactApplicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val info = Arguments.createMap()

        try {
            val cellInfoList = telephonyManager.allCellInfo
            var rsrp = 0
            var rsrq = 0
            var cellId = 0
            var radioTech = ""

            for (cellInfo in cellInfoList.orEmpty()) {
                when {
                    cellInfo is CellInfoLte && cellInfo.isRegistered -> {
                        val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte
                        rsrp = signal.rsrp
                        rsrq = signal.rsrq
                        cellId = cellInfo.cellIdentity.ci
                        radioTech = "LTE"
                        break
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr && cellInfo.isRegistered -> {
                        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        val identity = cellInfo.cellIdentity as CellIdentityNr
                        rsrp = signal.dbm // Usando dbm em vez de ssbDbm
                        rsrq = signal.csiRsrq ?: 0 // Usando csiRsrq em vez de ssbRsrq
                        cellId = identity.nci.toInt()
                        radioTech = "NR"
                        break
                    }
                }
            }

            info.putInt("rsrp", rsrp)
            info.putInt("rsrq", rsrq)
            info.putInt("cellId", cellId)
            info.putString("technology", radioTech)

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        info.putDouble("latitude", location.latitude)
                        info.putDouble("longitude", location.longitude)
                    } else {
                        info.putDouble("latitude", 0.0)
                        info.putDouble("longitude", 0.0)
                    }
                    promise.resolve(info)
                }
                .addOnFailureListener { e ->
                    promise.reject("LOCATION_ERROR", "Erro ao obter localização", e)
                }

        } catch (e: SecurityException) {
            promise.reject("PERMISSION_DENIED", "Permissões não concedidas", e)
        } catch (e: Exception) {
            promise.reject("NETWORK_ERROR", "Erro ao obter dados da rede", e)
        }
    }
}