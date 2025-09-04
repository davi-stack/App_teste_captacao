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
    
    fun getNetworkTypeName(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE)"
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G (CDMA)"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2G (1xRTT)"
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G (iDEN)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G (EVDO)"
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA -> "3G (HSPA)"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "3G (eHRPD)"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA+)"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Desconhecido"
        }
    }
@SuppressLint("MissingPermission", "NewApi")
@ReactMethod
fun getNetworkInfo(promise: Promise) {
    val telephonyManager = reactApplicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val info = Arguments.createMap()

    try {
        val cellInfoList = telephonyManager.allCellInfo
        var rsrp = 0
        var rsrq = 0
        var rssi = 0
        var cellId = ""
        var radioTech = ""
        var band = ""
        var mcc = "000"
        var mnc = "00"
        var lac = 0
        var tac = 0
        var ci = 0
        var pci = 0

        for (cellInfo in cellInfoList.orEmpty()) {
            if (!cellInfo.isRegistered) continue

            when {
                // 2G GSM
                cellInfo is CellInfoGsm -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthGsm
                    val identity = cellInfo.cellIdentity as CellIdentityGsm
                    
                    mcc = identity.mccString ?: "000"
                    mnc = identity.mncString ?: "00"
                    lac = identity.lac
                    ci = identity.cid
                    rssi = signal.dbm
                    
                    // Formato: MCC-MNC-LAC-CI (todos com padding zeros)
                    cellId = "${mcc.padStart(3, '0')}-${mnc.padStart(2, '0')}-" +
                             "${lac.toString().padStart(5, '0')}-" +
                             "${ci.toString().padStart(5, '0')}"
                    
                    radioTech = "2G"
                    band = "GSM_${identity.arfcn}"
                    break
                }

                // 3G WCDMA
                cellInfo is CellInfoWcdma -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthWcdma
                    val identity = cellInfo.cellIdentity as CellIdentityWcdma
                    
                    mcc = identity.mccString ?: "000"
                    mnc = identity.mncString ?: "00"
                    lac = identity.lac
                    ci = identity.cid
                    rssi = signal.dbm
                    
                    // Formato: MCC-MNC-LAC-CI (5 dígitos cada)
                    cellId = "${mcc.padStart(3, '0')}-${mnc.padStart(2, '0')}-" +
                             "${lac.toString().padStart(5, '0')}-" +
                             "${ci.toString().padStart(5, '0')}"
                    
                    radioTech = "3G"
                    band = "WCDMA_${identity.uarfcn}"
                    pci = identity.psc
                    break
                }

                // 4G LTE
                cellInfo is CellInfoLte -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte
                    val identity = cellInfo.cellIdentity
                    
                    mcc = identity.mccString ?: "000"
                    mnc = identity.mncString ?: "00"
                    tac = identity.tac
                    ci = identity.ci
                    rsrp = signal.rsrp
                    rsrq = signal.rsrq
                    rssi = signal.dbm
                    
                    // Para LTE: MCC-MNC-TAC-CI (TAC e CI com 5 dígitos)
                    cellId = "${mcc.padStart(3, '0')}-${mnc.padStart(2, '0')}-" +
                             "${tac.toString().padStart(5, '0')}-" +
                             "${ci.toString().padStart(5, '0')}"
                    
                    radioTech = "4G"
                    band = "LTE_${identity.bandwidth}"
                    pci = identity.pci
                    break
                }

                // 5G NR (Android 10+)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val identity = cellInfo.cellIdentity as CellIdentityNr
                    
                    mcc = identity.mccString ?: "000"
                    mnc = identity.mncString ?: "00"
                    tac = identity.tac
                    val nci = identity.nci
                    rsrp = signal.dbm
                    rsrq = signal.csiRsrq ?: 0
                    rssi = signal.dbm
                    
                    // Para 5G NR: MCC-MNC-TAC-NCI (NCI com 10 dígitos)
                    cellId = "${mcc.padStart(3, '0')}-${mnc.padStart(2, '0')}-" +
                             "${tac.toString().padStart(5, '0')}-" +
                             "${nci.toString().padStart(10, '0')}"
                    
                    radioTech = "5G"
                    band = "NR_${identity.bands?.firstOrNull() ?: 0}"
                    pci = identity.pci
                    break
                }
            }
        }

        // Preencher informações
        info.putString("cellId", cellId)
        info.putString("technology", radioTech)
        info.putString("band", band)
        info.putInt("rsrp", rsrp)
        info.putInt("rsrq", rsrq)
        info.putInt("rssi", rssi)
        info.putInt("pci", pci)
        info.putString("mcc", mcc)
        info.putString("mnc", mnc)
        info.putInt("lac", lac)
        info.putInt("tac", tac)
        info.putInt("ci", ci)

        // Obter localização
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
                info.putDouble("latitude", 0.0)
                info.putDouble("longitude", 0.0)
                promise.resolve(info)
            }

    } catch (e: SecurityException) {
        promise.reject("PERMISSION_DENIED", "Permissões não concedidas", e)
    } catch (e: Exception) {
        promise.reject("NETWORK_ERROR", "Erro ao obter dados da rede", e)
    }
}


}


