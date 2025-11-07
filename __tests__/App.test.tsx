/**
 * @format
 * Testes completos para o componente App
 * 
 * Cobertura:
 * - Renderização inicial e loading state
 * - Solicitação de permissões Android
 * - Obtenção e exibição de dados de rede
 * - Tratamento de erros
 * - Início e parada de gravação
 * - Exportação de CSV
 * - Atualização manual de dados
 * - Monitoramento em background
 */

import React from 'react';
import {render, fireEvent, waitFor, act} from '@testing-library/react-native';
import App from '../App';
import {NativeModules, PermissionsAndroid, Platform} from 'react-native';

// Mock dos módulos nativos
jest.mock('react-native/Libraries/Utilities/Platform', () => ({
  OS: 'android',
  select: jest.fn((obj) => obj.android),
}));

jest.mock('react-native/Libraries/PermissionsAndroid/PermissionsAndroid', () => ({
  PERMISSIONS: {
    ACCESS_FINE_LOCATION: 'android.permission.ACCESS_FINE_LOCATION',
    READ_PHONE_STATE: 'android.permission.READ_PHONE_STATE',
  },
  RESULTS: {
    GRANTED: 'granted',
    DENIED: 'denied',
  },
  requestMultiple: jest.fn(),
}));

// Mock do módulo nativo NetworkInfoModule
const mockGetNetworkInfo = jest.fn();
NativeModules.NetworkInfoModule = {
  getNetworkInfo: mockGetNetworkInfo,
};

// Mock do fetch global
global.fetch = jest.fn();

describe('App Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    
    // Reset do mock de fetch
    (global.fetch as jest.Mock).mockReset();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  describe('Renderização Inicial', () => {
    it('deve renderizar o título corretamente', () => {
      const {getByText} = render(<App />);
      expect(getByText('Monitor de Rede em Tempo Real')).toBeTruthy();
    });

    it('deve mostrar loading inicialmente', () => {
      const {getByText} = render(<App />);
      expect(getByText('Obtendo dados de rede...')).toBeTruthy();
    });
  });

  describe('Permissões', () => {
    it('deve solicitar permissões no Android', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-12345-67890',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      render(<App />);

      await waitFor(() => {
        expect(mockRequestMultiple).toHaveBeenCalledWith([
          'android.permission.ACCESS_FINE_LOCATION',
          'android.permission.READ_PHONE_STATE',
        ]);
      });
    });

    it('deve exibir erro quando permissões são negadas', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.DENIED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      const {findByText} = render(<App />);

      const errorText = await findByText(/Permissões necessárias não concedidas/i);
      expect(errorText).toBeTruthy();
    });
  });

  describe('Obtenção de Dados de Rede', () => {
    it('deve exibir dados de rede corretamente', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-12345-67890',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText} = render(<App />);

      await waitFor(() => {
        expect(mockGetNetworkInfo).toHaveBeenCalled();
      });

      expect(await findByText(/Tecnologia: 4G/i)).toBeTruthy();
      expect(await findByText(/ID da Célula: 724-05-12345-67890/i)).toBeTruthy();
      expect(await findByText(/RSRP: -85 dBm/i)).toBeTruthy();
      expect(await findByText(/RSRQ: -10 dB/i)).toBeTruthy();
    });

    it('deve classificar qualidade do sinal corretamente - Excelente', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -80,
        rsrq: -8,
        cellId: '724-05-1-1',
        technology: '5G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText} = render(<App />);

      expect(await findByText(/Excelente/i)).toBeTruthy();
    });

    it('deve classificar qualidade do sinal corretamente - Ruim', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -110,
        rsrq: -18,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText} = render(<App />);

      expect(await findByText(/Ruim/i)).toBeTruthy();
    });

    it('deve tratar erro na obtenção de dados', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockRejectedValue(new Error('Network error'));

      const {findByText} = render(<App />);

      expect(await findByText(/Erro: Network error/i)).toBeTruthy();
    });
  });

  describe('Funcionalidade de Gravação', () => {
    it('deve iniciar gravação ao clicar em Iniciar Captura', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Iniciar Captura')).toBeTruthy();
      });

      fireEvent.press(getByText('Iniciar Captura'));

      await waitFor(() => {
        expect(getByText(/Gravando\.\.\./i)).toBeTruthy();
      });
    });

    it('deve parar gravação ao clicar em Parar Captura', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Iniciar Captura')).toBeTruthy();
      });

      // Iniciar gravação
      fireEvent.press(getByText('Iniciar Captura'));

      await waitFor(() => {
        expect(getByText('Parar Captura')).toBeTruthy();
      });

      // Parar gravação
      fireEvent.press(getByText('Parar Captura'));

      await waitFor(() => {
        expect(getByText(/Inativo/i)).toBeTruthy();
      });
    });

    it('deve acumular dados durante gravação', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Iniciar Captura')).toBeTruthy();
      });

      // Verificar contador inicial
      expect(await findByText(/Dados capturados: 0 registros/i)).toBeTruthy();

      // Iniciar gravação
      fireEvent.press(getByText('Iniciar Captura'));

      // Aguardar atualização automática (30 segundos)
      act(() => {
        jest.advanceTimersByTime(30000);
      });

      await waitFor(() => {
        expect(getByText(/Dados capturados: 1 registros/i)).toBeTruthy();
      });
    });
  });

  describe('Exportação de CSV', () => {
    it('deve exportar CSV com sucesso', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
        timestamp: '2025-11-07T10:00:00',
      });

      (global.fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: async () => ({message: 'CSV recebido com sucesso'}),
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Iniciar Captura')).toBeTruthy();
      });

      // Iniciar e coletar dados
      fireEvent.press(getByText('Iniciar Captura'));

      act(() => {
        jest.advanceTimersByTime(30000);
      });

      // Exportar CSV
      await waitFor(() => {
        expect(getByText('Exportar CSV')).toBeTruthy();
      });

      fireEvent.press(getByText('Exportar CSV'));

      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          'http://56.125.158.250:8080/upload-csv/',
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'text/csv',
            }),
          }),
        );
      });
    });

    it('deve exibir erro quando não há dados para exportar', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Exportar CSV')).toBeTruthy();
      });

      fireEvent.press(getByText('Exportar CSV'));

      await waitFor(() => {
        expect(
          getByText(/Nenhum dado foi capturado para exportar/i),
        ).toBeTruthy();
      });
    });

    it('deve tratar erro no servidor ao exportar', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
        timestamp: '2025-11-07T10:00:00',
      });

      (global.fetch as jest.Mock).mockResolvedValue({
        ok: false,
        status: 500,
        json: async () => ({detail: 'Internal server error'}),
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Iniciar Captura')).toBeTruthy();
      });

      fireEvent.press(getByText('Iniciar Captura'));

      act(() => {
        jest.advanceTimersByTime(30000);
      });

      fireEvent.press(getByText('Exportar CSV'));

      await waitFor(() => {
        expect(getByText(/Erro do servidor/i)).toBeTruthy();
      });
    });
  });

  describe('Atualização Manual', () => {
    it('deve atualizar dados ao clicar em Atualizar Agora', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {findByText, getByText} = render(<App />);

      await waitFor(() => {
        expect(getByText('Atualizar Agora')).toBeTruthy();
      });

      const initialCallCount = mockGetNetworkInfo.mock.calls.length;

      fireEvent.press(getByText('Atualizar Agora'));

      await waitFor(() => {
        expect(mockGetNetworkInfo.mock.calls.length).toBeGreaterThan(
          initialCallCount,
        );
      });
    });
  });

  describe('Monitoramento em Background', () => {
    it('deve atualizar dados automaticamente a cada 30 segundos', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      render(<App />);

      await waitFor(() => {
        expect(mockGetNetworkInfo).toHaveBeenCalled();
      });

      const initialCallCount = mockGetNetworkInfo.mock.calls.length;

      // Avançar 30 segundos
      act(() => {
        jest.advanceTimersByTime(30000);
      });

      await waitFor(() => {
        expect(mockGetNetworkInfo.mock.calls.length).toBeGreaterThan(
          initialCallCount,
        );
      });
    });

    it('deve limpar intervalo ao desmontar componente', async () => {
      const mockRequestMultiple = PermissionsAndroid.requestMultiple as jest.Mock;
      mockRequestMultiple.mockResolvedValue({
        'android.permission.ACCESS_FINE_LOCATION': PermissionsAndroid.RESULTS.GRANTED,
        'android.permission.READ_PHONE_STATE': PermissionsAndroid.RESULTS.GRANTED,
      });

      mockGetNetworkInfo.mockResolvedValue({
        rsrp: -85,
        rsrq: -10,
        cellId: '724-05-1-1',
        technology: '4G',
        latitude: -23.5505,
        longitude: -46.6333,
      });

      const {unmount} = render(<App />);

      await waitFor(() => {
        expect(mockGetNetworkInfo).toHaveBeenCalled();
      });

      unmount();

      const callCountAfterUnmount = mockGetNetworkInfo.mock.calls.length;

      // Avançar tempo após desmonte
      act(() => {
        jest.advanceTimersByTime(60000);
      });

      // Não deve haver mais chamadas
      expect(mockGetNetworkInfo.mock.calls.length).toBe(callCountAfterUnmount);
    });
  });
});
