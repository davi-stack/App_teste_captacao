// File: NetworkMonitoringWorker.kt
package com.meuapp

import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.telephony.CellIdentityNr
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import com.google.android.gms.tasks.Tasks

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
// location
data class NetworkInfoData(
    val rsrp: Int,
    val rsrq: Int,
    val cellId: Int,
    val technology: String,
    val latitude: Double,
    val longitude: Double
)

class NetworkMonitoringWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    companion object {
        private const val MAX_LINES = 10
        private const val LOG_FILE = "network_log.csv"
        
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NetworkMonitoringWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "NetworkLogger",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }

    private suspend fun collectNetworkInfo(): NetworkInfoData {
        val telephonyManager = applicationContext.getSystemService(
            Context.TELEPHONY_SERVICE
        ) as TelephonyManager

        var rsrp = 0
        var rsrq = 0
        var cellId = 0
        var radioTech = ""

        val cellInfoList = telephonyManager.allCellInfo

        for (cellInfo in cellInfoList.orEmpty()) {
            when {
                cellInfo is CellInfoLte && cellInfo.isRegistered -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte
                    rsrp = signal.rsrp
                    rsrq = signal.rsrq
                    cellId = cellInfo.cellIdentity.ci
                    radioTech = getNetworkTypeName(telephonyManager.networkType)
                    break
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        cellInfo is CellInfoNr && cellInfo.isRegistered -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val identity = cellInfo.cellIdentity as CellIdentityNr
                    rsrp = signal.dbm
                    rsrq = signal.csiRsrq ?: 0
                    cellId = identity.nci.toInt()
                    radioTech = "5G (NR)"
                    break
                }
            }
        }

        val location = try {
            Tasks.await(fusedLocationClient.lastLocation)
        } catch (e: Exception) {
            Log.e("NetworkWorker", "Erro ao obter localização", e)
            null
        }

        return NetworkInfoData(
            rsrp = rsrp,
            rsrq = rsrq,
            cellId = cellId,
            technology = radioTech,
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0
        )
    }   

    private fun getNetworkTypeName(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS)"
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
            TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
            TelephonyManager.NETWORK_TYPE_NR -> "5G (NR)"
            else -> "Desconhecido"
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d("NetworkWorker", "Coletando dados de rede...")
            val data = collectNetworkInfo()
            
            val dataMap = mapOf(
                "timestamp" to SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                ).format(Date()),
                "rsrp" to data.rsrp,
                "rsrq" to data.rsrq,
                "cellId" to data.cellId,
                "technology" to data.technology,
                "latitude" to data.latitude,
                "longitude" to data.longitude
            )
            appendToCsv(dataMap)
            checkFileAndExport()
            Log.d("NetworkWorker", "Dados salvos com sucesso")
            Result.success()
        } catch (e: Exception) {
            Log.e("NetworkWorker", "Erro ao coletar dados: ${e.message}", e)
            Result.retry()
        }
    }

    private fun appendToCsv(data: Map<String, Any>) {
        val file = File(applicationContext.filesDir, LOG_FILE)
        if (!file.exists()) {
            file.writeText("timestamp,rsrp,rsrq,cellId,technology,latitude,longitude\n")
        }
        val line = "${data["timestamp"]},${data["rsrp"]},${data["rsrq"]}," +
                "${data["cellId"]},${data["technology"]}," +
                "${data["latitude"]},${data["longitude"]}\n"
        file.appendText(line)
    }

    private fun checkFileAndExport() {
        val file = File(applicationContext.filesDir, LOG_FILE)
        if (file.exists()) {
            val lineCount = file.readLines().size
            Log.d("NetworkWorker", "Arquivo possui $lineCount linhas")
            
            if (lineCount >= MAX_LINES) {
                Log.d("NetworkWorker", "Limite de $MAX_LINES linhas atingido - chamando export")
                exportData()
            }
        }
    }

    private fun exportData() {
        // Implementação básica de exportação
        Log.d("NetworkWorker", "Exportando dados...")
        
        val file = File(applicationContext.filesDir, LOG_FILE)
        if (file.exists()) {
            try{

        val csvData = file.readText()
        
        // 2. Configurar a requisição HTTP
        val client = OkHttpClient()
        val mediaType = "text/csv".toMediaType()
        val requestBody = csvData.toRequestBody(mediaType)
        
        // 3. Criar a requisição POST para sua API
        val request = Request.Builder()
            .url("http://54.233.209.5:8080/upload-csv/")
            .post(requestBody)
            .addHeader("Content-Type", "text/csv")
            .build()
        
        // 4. Enviar a requisição
        val response = client.newCall(request).execute()

        val file = File(applicationContext.filesDir, LOG_FILE)
        } catch (e: Exception) {
            Log.e("NetworkWorker", "Erro ao exportar dados: ${e.message}", e)
        } finally {
            // 5. Reiniciar o arquivo CSV
            Log.d("NetworkWorker", "Reiniciando arquivo CSV")
        }
            file.writeText("timestamp,rsrp,rsrq,cellId,technology,latitude,longitude\n")
            Log.d("NetworkWorker", "Dados exportados e arquivo reiniciado")
        }
    }
}