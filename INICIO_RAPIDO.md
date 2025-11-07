# ⚡ INÍCIO RÁPIDO - 3 Comandos para Rodar os Testes

## 🎯 Passo 1: Instalar Dependências

Abra o PowerShell no diretório do projeto e rode:

```powershell
npm install
```

**Tempo estimado:** 2-5 minutos  
**O que isso faz:** Baixa todas as bibliotecas necessárias para os testes

---

## 🧪 Passo 2: Rodar os Testes

```powershell
npm test
```

**O que você vai ver:** Lista de testes passando ✅

```
PASS  __tests__/App.test.tsx
  App Component
    Renderização Inicial
      ✓ deve renderizar o título corretamente
      ✓ deve mostrar loading inicialmente
    Permissões
      ✓ deve solicitar permissões no Android
      ...

Test Suites: 1 passed, 1 total
Tests:       17 passed, 17 total
```

---

## 📊 Passo 3: Ver Cobertura de Código

```powershell
npm test -- --coverage
```

**O que você vai ver:** Relatório de cobertura

```
----------------------|---------|----------|---------|---------|
File                  | % Stmts | % Branch | % Funcs | % Lines |
----------------------|---------|----------|---------|---------|
All files             |   80.25 |    65.41 |   78.12 |   80.51 |
 App.tsx              |   82.14 |    67.85 |   80.00 |   82.35 |
----------------------|---------|----------|---------|---------|
```


