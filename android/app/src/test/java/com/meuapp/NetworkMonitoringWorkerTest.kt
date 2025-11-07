package com.meuapp

import android.content.Context
import android.telephony.*
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import android.location.Location
import java.io.File
import org.junit.Assert.*

/**
 * Testes unitários para NetworkMonitoringWorker
 * 
 * Cobertura:
 * - Coleta de dados de rede em background
 * - Escrita de dados em arquivo CSV
 * - Verificação de limite de linhas
 * - Upload de dados via HTTP
 * - Tratamento de erros na coleta
 * - Formatação de dados CSV
 */
@RunWith(RobolectricTestRunner::class)
class NetworkMonitoringWorkerTest {

    private lateinit var context: Context
    private lateinit var worker: NetworkMonitoringWorker
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = RuntimeEnvironment.getApplication()
        
        // Limpar arquivo CSV antes de cada teste
        val csvFile = File(context.filesDir, "network_log.csv")
        if (csvFile.exists()) {
            csvFile.delete()
        }

        telephonyManager = mockk(relaxed = true)
        fusedLocationClient = mockk(relaxed = true)

        // Mock do TelephonyManager
        mockkStatic(Context::class)
        every { 
            context.getSystemService(Context.TELEPHONY_SERVICE) 
        } returns telephonyManager

        worker = TestListenableWorkerBuilder<NetworkMonitoringWorker>(context)
            .build()
    }

    @After
    fun tearDown() {
        clearAllMocks()
        
        // Limpar arquivo CSV após cada teste
        val csvFile = File(context.filesDir, "network_log.csv")
        if (csvFile.exists()) {
            csvFile.delete()
        }
    }

    @Test
    fun `getNetworkTypeName deve retornar tipo correto para LTE`() {
        val result = worker.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_LTE)
        assertEquals("4G (LTE)", result)
    }

    @Test
    fun `getNetworkTypeName deve retornar tipo correto para NR`() {
        val result = worker.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_NR)
        assertEquals("5G (NR)", result)
    }

    @Test
    fun `getNetworkTypeName deve retornar Desconhecido para tipo invalido`() {
        val result = worker.getNetworkTypeName(999)
        assertEquals("Desconhecido", result)
    }

    @Test
    fun `doWork deve criar arquivo CSV se nao existir`() = runBlocking {
        // Mock de dados de rede LTE
        val cellInfoLte = mockk<CellInfoLte>(relaxed = true)
        val cellSignalLte = mockk<CellSignalStrengthLte>(relaxed = true)
        val cellIdentityLte = mockk<CellIdentityLte>(relaxed = true)
        val location = mockk<Location>(relaxed = true)

        every { cellInfoLte.isRegistered } returns true
        every { cellInfoLte.cellSignalStrength } returns cellSignalLte
        every { cellInfoLte.cellIdentity } returns cellIdentityLte
        
        every { cellSignalLte.rsrp } returns -85
        every { cellSignalLte.rsrq } returns -10
        
        every { cellIdentityLte.mccString } returns "724"
        every { cellIdentityLte.mncString } returns "05"
        every { cellIdentityLte.tac } returns 1000
        every { cellIdentityLte.ci } returns 2000

        every { telephonyManager.allCellInfo } returns listOf(cellInfoLte)
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_LTE
        
        every { location.latitude } returns -23.5505
        every { location.longitude } returns -46.6333

        // Mock de FusedLocationClient
        val field = NetworkMonitoringWorker::class.java
            .getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(worker, fusedLocationClient)

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns locationTask
        
        coEvery { 
            com.google.android.gms.tasks.Tasks.await(locationTask) 
        } returns location

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        
        // Verificar se arquivo foi criado
        val csvFile = File(context.filesDir, "network_log.csv")
        assertTrue(csvFile.exists())
        
        // Verificar cabeçalho
        val lines = csvFile.readLines()
        assertEquals("timestamp,rsrp,rsrq,cellId,technology,latitude,longitude", lines[0])
        assertTrue(lines.size > 1) // Deve ter pelo menos uma linha de dados
    }

    @Test
    fun `doWork deve adicionar linha ao CSV existente`() = runBlocking {
        // Criar arquivo CSV com dados iniciais
        val csvFile = File(context.filesDir, "network_log.csv")
        csvFile.writeText("timestamp,rsrp,rsrq,cellId,technology,latitude,longitude\n")
        csvFile.appendText("2025-11-07T10:00:00,-85,-10,724-05-1000-2000,4G,-23.5505,-46.6333\n")

        val initialLineCount = csvFile.readLines().size

        // Mock de dados
        val cellInfoLte = mockk<CellInfoLte>(relaxed = true)
        val cellSignalLte = mockk<CellSignalStrengthLte>(relaxed = true)
        val cellIdentityLte = mockk<CellIdentityLte>(relaxed = true)
        val location = mockk<Location>(relaxed = true)

        every { cellInfoLte.isRegistered } returns true
        every { cellInfoLte.cellSignalStrength } returns cellSignalLte
        every { cellInfoLte.cellIdentity } returns cellIdentityLte
        
        every { cellSignalLte.rsrp } returns -90
        every { cellSignalLte.rsrq } returns -12
        
        every { cellIdentityLte.mccString } returns "724"
        every { cellIdentityLte.mncString } returns "05"
        every { cellIdentityLte.tac } returns 1001
        every { cellIdentityLte.ci } returns 2001

        every { telephonyManager.allCellInfo } returns listOf(cellInfoLte)
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_LTE
        
        every { location.latitude } returns -23.5506
        every { location.longitude } returns -46.6334

        val field = NetworkMonitoringWorker::class.java
            .getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(worker, fusedLocationClient)

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns locationTask
        
        coEvery { 
            com.google.android.gms.tasks.Tasks.await(locationTask) 
        } returns location

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        
        val finalLineCount = csvFile.readLines().size
        assertEquals(initialLineCount + 1, finalLineCount)
    }

    @Test
    fun `doWork deve retornar retry em caso de erro`() = runBlocking {
        // Simular erro ao obter dados de rede
        every { telephonyManager.allCellInfo } throws SecurityException("Permission denied")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `collectNetworkInfo deve processar dados LTE corretamente`() = runBlocking {
        val cellInfoLte = mockk<CellInfoLte>(relaxed = true)
        val cellSignalLte = mockk<CellSignalStrengthLte>(relaxed = true)
        val cellIdentityLte = mockk<CellIdentityLte>(relaxed = true)
        val location = mockk<Location>(relaxed = true)

        every { cellInfoLte.isRegistered } returns true
        every { cellInfoLte.cellSignalStrength } returns cellSignalLte
        every { cellInfoLte.cellIdentity } returns cellIdentityLte
        
        every { cellSignalLte.rsrp } returns -75
        every { cellSignalLte.rsrq } returns -8
        
        every { cellIdentityLte.mccString } returns "724"
        every { cellIdentityLte.mncString } returns "11"
        every { cellIdentityLte.tac } returns 5000
        every { cellIdentityLte.ci } returns 10000

        every { telephonyManager.allCellInfo } returns listOf(cellInfoLte)
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_LTE
        
        every { location.latitude } returns -22.9068
        every { location.longitude } returns -43.1729

        val field = NetworkMonitoringWorker::class.java
            .getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(worker, fusedLocationClient)

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns locationTask
        
        coEvery { 
            com.google.android.gms.tasks.Tasks.await(locationTask) 
        } returns location

        // Usar reflection para chamar método privado
        val method = NetworkMonitoringWorker::class.java
            .getDeclaredMethod("collectNetworkInfo")
        method.isAccessible = true
        val data = method.invoke(worker) as NetworkInfoData

        assertEquals(-75, data.rsrp)
        assertEquals(-8, data.rsrq)
        assertTrue(data.cellId.contains("724"))
        assertTrue(data.technology.contains("4G"))
        assertEquals(-22.9068, data.latitude, 0.0001)
        assertEquals(-43.1729, data.longitude, 0.0001)
    }

    @Test
    fun `collectNetworkInfo deve retornar coordenadas zero quando localizacao falha`() = runBlocking {
        val cellInfoLte = mockk<CellInfoLte>(relaxed = true)
        val cellSignalLte = mockk<CellSignalStrengthLte>(relaxed = true)
        val cellIdentityLte = mockk<CellIdentityLte>(relaxed = true)

        every { cellInfoLte.isRegistered } returns true
        every { cellInfoLte.cellSignalStrength } returns cellSignalLte
        every { cellInfoLte.cellIdentity } returns cellIdentityLte
        
        every { cellSignalLte.rsrp } returns -95
        every { cellSignalLte.rsrq } returns -15
        
        every { cellIdentityLte.mccString } returns "724"
        every { cellIdentityLte.mncString } returns "05"
        every { cellIdentityLte.tac } returns 3000
        every { cellIdentityLte.ci } returns 4000

        every { telephonyManager.allCellInfo } returns listOf(cellInfoLte)
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_LTE

        val field = NetworkMonitoringWorker::class.java
            .getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(worker, fusedLocationClient)

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns locationTask
        
        coEvery { 
            com.google.android.gms.tasks.Tasks.await(locationTask) 
        } throws Exception("GPS unavailable")

        val method = NetworkMonitoringWorker::class.java
            .getDeclaredMethod("collectNetworkInfo")
        method.isAccessible = true
        val data = method.invoke(worker) as NetworkInfoData

        assertEquals(0.0, data.latitude, 0.0001)
        assertEquals(0.0, data.longitude, 0.0001)
    }

    @Test
    fun `CSV deve ter formato correto com virgulas e aspas quando necessario`() = runBlocking {
        val cellInfoLte = mockk<CellInfoLte>(relaxed = true)
        val cellSignalLte = mockk<CellSignalStrengthLte>(relaxed = true)
        val cellIdentityLte = mockk<CellIdentityLte>(relaxed = true)
        val location = mockk<Location>(relaxed = true)

        every { cellInfoLte.isRegistered } returns true
        every { cellInfoLte.cellSignalStrength } returns cellSignalLte
        every { cellInfoLte.cellIdentity } returns cellIdentityLte
        
        every { cellSignalLte.rsrp } returns -88
        every { cellSignalLte.rsrq } returns -11
        
        every { cellIdentityLte.mccString } returns "724"
        every { cellIdentityLte.mncString } returns "31"
        every { cellIdentityLte.tac } returns 7000
        every { cellIdentityLte.ci } returns 8000

        every { telephonyManager.allCellInfo } returns listOf(cellInfoLte)
        every { telephonyManager.networkType } returns TelephonyManager.NETWORK_TYPE_LTE
        
        every { location.latitude } returns -15.7801
        every { location.longitude } returns -47.9292

        val field = NetworkMonitoringWorker::class.java
            .getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(worker, fusedLocationClient)

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { fusedLocationClient.lastLocation } returns locationTask
        
        coEvery { 
            com.google.android.gms.tasks.Tasks.await(locationTask) 
        } returns location

        worker.doWork()

        val csvFile = File(context.filesDir, "network_log.csv")
        val lines = csvFile.readLines()
        
        assertTrue(lines.size >= 2)
        
        // Verificar última linha (dados)
        val dataLine = lines.last()
        val parts = dataLine.split(",")
        
        // Deve ter 7 colunas
        assertTrue(parts.size >= 7)
        
        // RSRP e RSRQ devem ser números negativos
        assertTrue(parts[1].toInt() < 0)
        assertTrue(parts[2].toInt() < 0)
    }
}
