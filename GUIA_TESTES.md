# 🧪 Guia de Testes - App de Monitoramento de Rede

## 📋 Pré-requisitos

### Para Testes TypeScript/Jest (NÃO requer ambiente Android completo)

Você **SÓ** precisa de:
- ✅ Node.js 18+ instalado
- ✅ npm ou yarn

Você **NÃO** precisa de:
- ❌ Android Studio
- ❌ SDK Android
- ❌ Emulador
- ❌ Java/JDK (para testes JS)

### Para Testes Kotlin/JUnit (requer ambiente Android)

Você precisa de:
- ✅ Node.js 18+
- ✅ Android Studio
- ✅ SDK Android (API 24-35)
- ✅ JDK 17+

---

## 🚀 Instalação das Dependências

### Passo 1: Instalar Dependências Node.js

```powershell
# No diretório raiz do projeto
cd "c:\Users\lucas\OneDrive\Documentos\App_teste_captacao-main\App_teste_captacao-main"

# Instalar todas as dependências
npm install

# OU se preferir yarn
yarn install
```

Isso vai instalar:
- `jest` - Framework de testes
- `@testing-library/react-native` - Utilitários para testar componentes React Native
- `@testing-library/jest-native` - Matchers customizados
- `@types/jest` - Tipos TypeScript para Jest
- E todas as outras dependências do projeto

### Passo 2: Verificar Instalação

```powershell
# Ver versão do Node.js
node --version
# Deve mostrar: v18.x.x ou superior

# Ver versão do npm
npm --version

# Ver se Jest está instalado
npx jest --version
```

---

## 🧪 Rodando os Testes

### Testes TypeScript/Jest (Recomendado para começar)

#### Rodar TODOS os testes
```powershell
npm test
```

#### Rodar com cobertura
```powershell
npm test -- --coverage
```

#### Rodar em modo watch (re-executa ao salvar arquivo)
```powershell
npm test -- --watch
```

#### Rodar apenas o teste do App.tsx
```powershell
npm test -- App.test.tsx
```

#### Rodar testes com output detalhado
```powershell
npm test -- --verbose
```

### Testes Kotlin/JUnit (Requer Android Studio)

#### Opção 1: Via Android Studio (Mais Fácil)
1. Abrir Android Studio
2. Abrir o projeto em `android/`
3. Navegar para `app/src/test/java/com/meuapp/`
4. Clicar direito em `NetworkInfoModuleTest.kt` ou `NetworkMonitoringWorkerTest.kt`
5. Selecionar "Run 'NetworkInfoModuleTest'"

#### Opção 2: Via Linha de Comando (Gradle)
```powershell
# No diretório android/
cd android

# Rodar todos os testes unitários
.\gradlew.bat test

# Rodar com relatório detalhado
.\gradlew.bat test --info

# Rodar apenas testes de um módulo
.\gradlew.bat :app:test

# Ver relatório de testes (após rodar)
# Abrir: android\app\build\reports\tests\testDebugUnitTest\index.html
```

---

## 📊 Interpretando os Resultados

### Jest (TypeScript)

Saída esperada:
```
PASS  __tests__/App.test.tsx
  App Component
    Renderização Inicial
      ✓ deve renderizar o título corretamente (45ms)
      ✓ deve mostrar loading inicialmente (23ms)
    Permissões
      ✓ deve solicitar permissões no Android (67ms)
      ...

Test Suites: 1 passed, 1 total
Tests:       17 passed, 17 total
Snapshots:   0 total
Time:        5.234s
```

Com cobertura:
```
----------------------|---------|----------|---------|---------|
File                  | % Stmts | % Branch | % Funcs | % Lines |
----------------------|---------|----------|---------|---------|
All files             |   80.25 |    65.41 |   78.12 |   80.51 |
 App.tsx              |   82.14 |    67.85 |   80.00 |   82.35 |
----------------------|---------|----------|---------|---------|
```

### Gradle (Kotlin)

Saída esperada:
```
> Task :app:testDebugUnitTest

com.meuapp.NetworkInfoModuleTest > getNetworkTypeName deve retornar 2G para GPRS PASSED

com.meuapp.NetworkInfoModuleTest > getNetworkTypeName deve retornar 4G para LTE PASSED

...

BUILD SUCCESSFUL in 12s
```

---

## ❌ Problemas Comuns e Soluções

### Problema 1: "Cannot find module 'react-native'"
```powershell
# Solução: Reinstalar dependências
rm -r node_modules
rm package-lock.json
npm install
```

### Problema 2: "Jest encountered an unexpected token"
```powershell
# Solução: Limpar cache do Jest
npx jest --clearCache
npm test
```

### Problema 3: Testes Kotlin não rodam - "SDK not found"
```powershell
# Solução: Configurar ANDROID_HOME
# No PowerShell, adicionar ao perfil:
$env:ANDROID_HOME = "C:\Users\SeuUsuario\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\platform-tools"
```

### Problema 4: "MockK initialization failed"
```gradle
// Adicionar ao android/app/build.gradle
android {
    testOptions {
        unitTests {
            includeAndroidResources = true
            returnDefaultValues = true
        }
    }
}
```

### Problema 5: Testes passam mas há warnings
```
// Isso é normal! Warnings de deprecated APIs ou 
// "Not implemented: HTMLFormElement.prototype.submit"
// são esperados em testes React Native e podem ser ignorados
```

---

## 📈 Estrutura de Testes Criada

```
App_teste_captacao-main/
├── __tests__/
│   └── App.test.tsx                    ← 17 testes TypeScript
├── android/
│   └── app/
│       └── src/
│           └── test/
│               └── java/
│                   └── com/
│                       └── meuapp/
│                           ├── NetworkInfoModuleTest.kt      ← 9 testes
│                           └── NetworkMonitoringWorkerTest.kt ← 7 testes
├── jest.config.js                      ← Configuração Jest
├── jest.setup.js                       ← Setup de testes
└── QA_REPORT.md                        ← Relatório técnico
```

**Total: 33 testes implementados**

---

## 🎯 Checklist para Você

### Testes JavaScript (COMECE AQUI - Não precisa de Android Studio)

- [ ] Node.js instalado e funcionando
- [ ] Rodou `npm install` com sucesso
- [ ] Rodou `npm test` e viu testes passando
- [ ] Rodou `npm test -- --coverage` e viu relatório de cobertura
- [ ] Entendeu como adicionar novos testes

### Testes Kotlin (Opcional - Requer Android Studio)

- [ ] Android Studio instalado
- [ ] SDK Android configurado
- [ ] Projeto `android/` abre sem erros
- [ ] Consegue rodar testes unitários via Android Studio
- [ ] OU consegue rodar `gradlew.bat test` com sucesso

---

## 💡 Próximos Passos

### 1. Rodar os Testes Agora
```powershell
# Teste simples para verificar se tudo funciona
npm test -- --testNamePattern="deve renderizar o título"
```

### 2. Ver Cobertura de Código
```powershell
npm test -- --coverage --coverageReporters=html
# Abrir: coverage/lcov-report/index.html no navegador
```

### 3. Adicionar Novos Testes
Edite `__tests__/App.test.tsx` e adicione:
```typescript
it('meu novo teste', () => {
  const {getByText} = render(<App />);
  expect(getByText('Algum Texto')).toBeTruthy();
});
```

### 4. Corrigir Código Baseado nos Testes
Veja o relatório `QA_REPORT.md` para prioridades de correção

---
