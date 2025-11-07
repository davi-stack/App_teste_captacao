import React, { useState, useEffect, useCallback, useRef } from 'react';
import { 
  View,
  StyleSheet, 
  PermissionsAndroid, 
  Platform, 
  ActivityIndicator, 
  TouchableOpacity,
  AppState,
  ToastAndroid
} from 'react-native';
import { NativeModules } from 'react-native';
// import { uploadPdfFile } from './service/send';
// import { format } from 'date-fns';

interface NetworkInfo {
  rsrp: number;
  rsrq: number;
  cellId: number;
  technology: string;
  latitude: number;
  longitude: number;
  timestamp?: string;
}
import { Text} from "react-native";

const App = () => {
  
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [recordedData, setRecordedData] = useState<NetworkInfo[]>([]);
  const appState = useRef(AppState.currentState);
  
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const requestPermissions = useCallback(async () => {
    try {
      // Verifica se o app está em foreground
      if (appState.current !== 'active') {
        return false;
      }

      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
        ]);

        return (
          granted['android.permission.ACCESS_FINE_LOCATION'] === PermissionsAndroid.RESULTS.GRANTED &&
          granted['android.permission.READ_PHONE_STATE'] === PermissionsAndroid.RESULTS.GRANTED
        );
      }
      return true;
    } catch (err) {
      console.error('Erro ao solicitar permissões:', err);
      setError('Erro ao solicitar permissões');
      return false;
    }
  }, []);

  const fetchNetworkInfo = useCallback(async () => {
    try {
      setLoading(true);
      const hasPermissions = await requestPermissions();
      
      if (!hasPermissions) {
        setError('Permissões necessárias não concedidas');
        return;
      }

      const info = await NativeModules.NetworkInfoModule.getNetworkInfo();
      const timestamp = new Date().toISOString();
      setNetworkInfo({ ...info, timestamp });
      setLastUpdate(new Date());
      setError(null);
      
      if (isRecording) {
        setRecordedData(prev => [...prev, { ...info, timestamp }]);
      }
    } catch (err: any) {
      setError(err.message || 'Erro ao obter informações da rede');
    } finally {
      setLoading(false);
    }
  }, [isRecording, requestPermissions]);

  const startMonitoring = useCallback(async () => {
    fetchNetworkInfo();
    
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    intervalRef.current = setInterval(fetchNetworkInfo, 30000);
  }, [fetchNetworkInfo]);

  const stopMonitoring = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const startRecording = useCallback(() => {
    setIsRecording(true);
    setRecordedData([]);
  }, []);

  const stopRecording = useCallback(() => {
    setIsRecording(false);
  }, []);

  const exportToCSV = useCallback(async () => {
    if (recordedData.length === 0) {
      setError('Nenhum dado foi capturado para exportar');
      return;
    }
  
    try {
      let csvContent = 'timestamp,rsrp,rsrq,cellId,technology,latitude,longitude\n';
  
      recordedData.forEach(data => {
        csvContent += `"${data.timestamp}",${data.rsrp},${data.rsrq},${data.cellId},"${data.technology}",${data.latitude},${data.longitude}\n`;
      });
  
      const response = await fetch('http://56.125.158.250:8080/upload-csv/', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/csv',
        },
        body: csvContent,
      });
      ToastAndroid.show('Enviando CSV para o servidor...', ToastAndroid.SHORT);
  
      const result = await response.json();
  
      if (!response.ok) {
        console.error('Erro do servidor:', result);
        setError(`Erro do servidor: ${result.detail || response.status}`);
      } else {
        console.log('CSV enviado com sucesso:', result);
        // alert(`Sucesso: ${result.message}`);
        setRecordedData([]);
      }
    } catch (err) {
      console.error('Erro ao enviar CSV:', err);
      setError('Erro ao enviar CSV para o servidor' );
    }
  }, [recordedData]);
  

  useEffect(() => {
    // Listener para mudanças no estado do app
    const subscription = AppState.addEventListener('change', nextAppState => {
      appState.current = nextAppState;
    });

    startMonitoring();
    
    return () => {
      stopMonitoring();
      subscription.remove();
    };
  }, [startMonitoring, stopMonitoring]);

  const getSignalQuality = (rsrp: number) => {
    if (rsrp >= -85) return 'Excelente';
    if (rsrp >= -95) return 'Boa';
    if (rsrp >= -105) return 'Regular';
    return 'Ruim';
  };

  const formatLastUpdate = () => {
    if (!lastUpdate) return '';
    return `Última atualização: ${lastUpdate.toLocaleTimeString()}`;
  };

  const renderContent = () => {
    if (loading && !networkInfo) {
      return (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007bff" />
          <Text style={styles.loadingText}>Obtendo dados de rede...</Text>
        </View>
      );
    }

    if (error) {
      return (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>Erro: {error}</Text>
          <TouchableOpacity onPress={startMonitoring} style={styles.retryButton}>
            <Text style={styles.retryButtonText}>Tentar novamente</Text>
          </TouchableOpacity>
        </View>
      );
    }

    return (
      <>
        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>Dados da Célula</Text>
          <Text>Tecnologia: {networkInfo?.technology || 'N/A'}</Text>
          <Text>ID da Célula: {networkInfo?.cellId || 'N/A'}</Text>
          <Text>RSRP: {networkInfo?.rsrp ? `${networkInfo.rsrp} dBm (${getSignalQuality(networkInfo.rsrp)})` : 'N/A'}</Text>
          <Text>RSRQ: {networkInfo?.rsrq ? `${networkInfo.rsrq} dB` : 'N/A'}</Text>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>Localização</Text>
          <Text>Latitude: {networkInfo?.latitude || 'N/A'}</Text>
          <Text>Longitude: {networkInfo?.longitude || 'N/A'}</Text>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>Gravação</Text>
          <Text>Status: {isRecording ? 'Gravando...' : 'Inativo'}</Text>
          <Text>Dados capturados: {recordedData.length} registros</Text>
        </View>
      </>
    );
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Monitor de Rede em Tempo Real</Text>
      
      {renderContent()}
      
      <View style={styles.footer}>
        <Text style={styles.updateInfo}>{formatLastUpdate()}</Text>
        
        <View style={styles.buttonGroup}>
          <TouchableOpacity 
            onPress={fetchNetworkInfo} 
            style={styles.actionButton}
          >
            <Text style={styles.actionButtonText}>Atualizar Agora</Text>
          </TouchableOpacity>
          
          {!isRecording ? (
            <TouchableOpacity 
              onPress={startRecording} 
              style={[styles.actionButton, styles.startButton]}
            >
              <Text style={styles.actionButtonText}>Iniciar Captura</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity 
              onPress={stopRecording} 
              style={[styles.actionButton, styles.stopButton]}
            >
              <Text style={styles.actionButtonText}>Parar Captura</Text>
            </TouchableOpacity>
          )}
          
          <TouchableOpacity 
            onPress={exportToCSV} 
            style={[styles.actionButton, styles.exportButton]}
            disabled={recordedData.length === 0}
          >
            <Text style={styles.actionButtonText}>Exportar CSV</Text>
          </TouchableOpacity>
        </View>
      </View>
      
      
    </View>
  );
};


const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
    color: '#333',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 15,
    fontSize: 16,
    color: '#666',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  errorText: {
    color: '#d9534f',
    fontSize: 18,
    marginBottom: 20,
    textAlign: 'center',
  },
  infoSection: {
    marginBottom: 20,
    padding: 15,
    backgroundColor: '#f8f9fa',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#e9ecef',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10,
    color: '#495057',
  },
  footer: {
    marginTop: 'auto',
    paddingTop: 15,
    borderTopWidth: 1,
    borderTopColor: '#dee2e6',
  },
  updateInfo: {
    textAlign: 'center',
    color: '#6c757d',
    marginBottom: 15,
  },
  buttonGroup: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    flexWrap: 'wrap',
  },
  actionButton: {
    flex: 1,
    minWidth: '30%',
    padding: 12,
    borderRadius: 8,
    backgroundColor: '#007bff',
    alignItems: 'center',
    marginBottom: 10,
  },
  startButton: {
    backgroundColor: '#28a745',
  },
  stopButton: {
    backgroundColor: '#dc3545',
  },
  exportButton: {
    backgroundColor: '#6c757d',
  },
  actionButtonText: {
    color: 'white',
    fontWeight: '500',
  },
  retryButton: {
    padding: 12,
    borderRadius: 8,
    backgroundColor: '#007bff',
    alignItems: 'center',
    marginTop: 15,
  },
  retryButtonText: {
    color: 'white',
    fontWeight: '500',
  },
});

export default App;