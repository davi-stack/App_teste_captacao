import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, PermissionsAndroid, Platform, ActivityIndicator, TouchableOpacity } from 'react-native';
import { NativeModules } from 'react-native';

// Interface para os dados de rede
interface NetworkInfo {
  rsrp: number;
  rsrq: number;
  cellId: number;
  technology: string;
  latitude: number;
  longitude: number;
}

const App = () => {
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  // Referência para o intervalo de atualização
  const intervalRef = React.useRef<NodeJS.Timeout | null>(null);

  // Função para buscar informações da rede
  const fetchNetworkInfo = useCallback(async () => {
    try {
      setLoading(true);
      const info = await NativeModules.NetworkInfoModule.getNetworkInfo();
      setNetworkInfo(info);
      setLastUpdate(new Date());
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Erro ao obter informações da rede');
    } finally {
      setLoading(false);
    }
  }, []);

  // Função para solicitar permissões
  const requestPermissions = useCallback(async () => {
    try {
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
      return true; // Para iOS/outras plataformas
    } catch (err) {
      console.error('Erro ao solicitar permissões:', err);
      return false;
    }
  }, []);

  // Iniciar monitoramento contínuo
  const startMonitoring = useCallback(async () => {
    const hasPermissions = await requestPermissions();
    
    if (!hasPermissions) {
      setError('Permissões necessárias não foram concedidas');
      setLoading(false);
      return;
    }

    // Busca inicial
    fetchNetworkInfo();
    
    // Configura intervalo para atualizações rápidas (1 segundo)
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    intervalRef.current = setInterval(fetchNetworkInfo, 1000);
  }, [fetchNetworkInfo, requestPermissions]);

  // Parar monitoramento
  const stopMonitoring = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  // Iniciar ao montar o componente
  useEffect(() => {
    startMonitoring();
    
    // Limpar ao desmontar
    return () => {
      stopMonitoring();
    };
  }, [startMonitoring, stopMonitoring]);

  // Classificar qualidade do sinal
  const getSignalQuality = (rsrp: number) => {
    if (rsrp >= -85) return 'Excelente';
    if (rsrp >= -95) return 'Boa';
    if (rsrp >= -105) return 'Regular';
    return 'Ruim';
  };

  // Formatar data da última atualização
  const formatLastUpdate = () => {
    if (!lastUpdate) return '';
    return `Última atualização: ${lastUpdate.toLocaleTimeString()}`;
  };

  // Renderizar conteúdo principal
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
          <TouchableOpacity onPress={fetchNetworkInfo} style={styles.actionButton}>
            <Text style={styles.actionButtonText}>Atualizar Agora</Text>
          </TouchableOpacity>
          
          {intervalRef.current ? (
            <TouchableOpacity onPress={stopMonitoring} style={[styles.actionButton, styles.stopButton]}>
              <Text style={styles.actionButtonText}>Parar Monitoramento</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity onPress={startMonitoring} style={[styles.actionButton, styles.startButton]}>
              <Text style={styles.actionButtonText}>Iniciar Monitoramento</Text>
            </TouchableOpacity>
          )}
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
  },
  actionButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    backgroundColor: '#007bff',
    alignItems: 'center',
  },
  startButton: {
    backgroundColor: '#28a745',
  },
  stopButton: {
    backgroundColor: '#dc3545',
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