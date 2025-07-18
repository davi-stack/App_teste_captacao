import { startNetworkLogger } from './backgroundTask';

// Esta função será chamada quando o app iniciar
export const initBackgroundMonitoring = async () => {
  try {
    await startNetworkLogger();
    console.log('Monitoramento contínuo iniciado');
  } catch (error) {
    console.error('Falha ao iniciar monitoramento contínuo:', error);
  }
};