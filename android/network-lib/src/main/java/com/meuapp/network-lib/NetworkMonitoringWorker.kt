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
// import okhttp3.OkHttpClient
// import okhttp3.MediaType.Companion.toMediaType
import com.google.android.gms.tasks.Tasks

// import okhttp3.Request
// import okhttp3.RequestBody.Companion.toRequestBody


import java.net.HttpURLConnection
import java.net.URL
// location
// const url = ""
data class NetworkInfoData(
    val rsrp: Int,
    val rsrq: Int,
    val cellId: String,
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
        private const val MAX_LINES = 1
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
    //     fun runNow(context: Context) {
    //     val immediateWork = OneTimeWorkRequestBuilder<NetworkMonitoringWorker>().build()
    //     WorkManager.getInstance(context).enqueue(immediateWork)
    // }
    }
    // Função para enviar o CSV via POST sem usar OkHttp
    private fun uploadCsvWithHttpUrlConnection(csvData: String) {
        val urlString = "http://56.125.158.250:8080/upload-csv/"
        val connection: HttpURLConnection?

        try {
            // Criar URL e abrir conexão
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000 // 10 segundos
                readTimeout = 10000    // 10 segundos
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "text/csv")
            }

            // Escrever o corpo da requisição
            connection.outputStream.use { os ->
                val input = csvData.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
                os.flush()
            }

            // Ler a resposta
            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            Log.d("HTTP_DEBUG", "Response Code: $responseCode")
            Log.d("HTTP_DEBUG", "Response Body: $responseMessage")

            connection.disconnect()

        } catch (e: Exception) {
            Log.e("HTTP_ERROR", "Erro ao enviar CSV: ${e.message}", e)
        }
    }


    private suspend fun collectNetworkInfo(): NetworkInfoData {
        val telephonyManager = applicationContext.getSystemService(
            Context.TELEPHONY_SERVICE
        ) as TelephonyManager

        var rsrp = 0
        var rsrq = 0
        var cellId = ""
        var radioTech = ""

        val cellInfoList = telephonyManager.allCellInfo

        for (cellInfo in cellInfoList.orEmpty()) {
                  when {
                // LTE
                cellInfo is CellInfoLte && cellInfo.isRegistered -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte
                    rsrp = signal.rsrp
                    rsrq = signal.rsrq

                    val identity = cellInfo.cellIdentity
                    val mcc = identity.mccString ?: "000"
                    val mnc = identity.mncString ?: "00"
                    val tac = identity.tac
                    val ci = identity.ci
                    val eci = (tac * 256) + (ci % 256)
                    
                    val cgi = "$mcc-$mnc-$tac:-$ci"
                    cellId = cgi

                    val networkType = telephonyManager.networkType
                    radioTech = getNetworkTypeName(networkType)
                    break
                }

                // 5G NR
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr && cellInfo.isRegistered -> {
                    val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val identity = cellInfo.cellIdentity as CellIdentityNr
                    rsrp = signal.dbm
                    rsrq = signal.csiRsrq ?: 0

                    val mcc = identity.mccString ?: "000"
                    val mnc = identity.mncString ?: "00"
                    val tac = identity.tac
                    val nci = identity.nci

                    val cgi = "$mcc$mnc:$tac:$nci"
                    cellId = cgi
                    radioTech = "NR"
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
    Log.d("NetworkWorker", "Exportando dados...")

    val file = File(applicationContext.filesDir, LOG_FILE)
    if (!file.exists()) {
        Log.w("NetworkWorker", "Arquivo CSV não encontrado, exportação cancelada.")
        return
    }

    try {
        // Ler conteúdo do CSV
        val csvData = file.readText()
        Log.d("NetworkWorker", "Tamanho do CSV: ${csvData.length} bytes")

        // Enviar dados
        val urlString = "http://56.125.158.250:8080/upload-csv/"
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2000 // 2 segundos
                readTimeout = 10000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "text/csv")
            }

            // Escrevendo os dados
            connection.outputStream.use { os ->
                os.write(csvData.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            // Ler a resposta
            val responseCode = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erro desconhecido"
            }

            if (responseCode in 200..299) {
                Log.d("NetworkWorker", "Dados exportados com sucesso: $responseBody e resp {$responseCode}")
            } else {
                Log.e("NetworkWorker", "Erro na exportação ($responseCode): $responseBody")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e("NetworkWorker", "Erro na conexão: ${e.message}", e)
        }

    } catch (e: Exception) {
        Log.e("NetworkWorker", "Erro ao exportar dados: ${e.message}", e)
    } finally {
        // Reinicia o arquivo CSV
        Log.d("NetworkWorker", "Reiniciando arquivo CSV")
        file.writeText("timestamp,rsrp,rsrq,cellId,technology,latitude,longitude\n")
        Log.d("NetworkWorker", "Dados exportados e arquivo reiniciado")
    }
}

}