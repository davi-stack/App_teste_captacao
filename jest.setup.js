// jest.setup.js
// Remover import que não existe mais
// import '@testing-library/jest-native/extend-expect';

// Mock do ToastAndroid
jest.mock('react-native/Libraries/Components/ToastAndroid/ToastAndroid', () => ({
  show: jest.fn(),
  SHORT: 0,
  LONG: 1,
  TOP: 2,
  BOTTOM: 3,
  CENTER: 4,
}));

// Mock do AppState
jest.mock('react-native/Libraries/AppState/AppState', () => ({
  addEventListener: jest.fn(() => ({
    remove: jest.fn(),
  })),
  currentState: 'active',
}));

// Suprimir warnings específicos
const originalWarn = console.warn;
const originalError = console.error;

beforeAll(() => {
  console.warn = (...args) => {
    const message = args[0];
    if (
      typeof message === 'string' &&
      (message.includes('Animated:') ||
        message.includes('VirtualizedLists') ||
        message.includes('componentWillReceiveProps'))
    ) {
      return;
    }
    originalWarn(...args);
  };

  console.error = (...args) => {
    const message = args[0];
    if (
      typeof message === 'string' &&
      (message.includes('Warning: ReactDOM.render') ||
        message.includes('Not implemented: HTMLFormElement'))
    ) {
      return;
    }
    originalError(...args);
  };
});

afterAll(() => {
  console.warn = originalWarn;
  console.error = originalError;
});
