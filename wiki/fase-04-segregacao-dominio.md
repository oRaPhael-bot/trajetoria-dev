# Fase 04 — Segregação de Domínio e Confiabilidade Transacional

> **Projeto:** trajetoria-dev | **Nível:** Especialista | **Tempo estimado:** 6–8h

---

## Visão Geral

Aplicamos o princípio de **Responsabilidade Única (SRP)** no nível de banco de dados.
Separamos a **Identidade** (`Cliente`) do **Patrimônio** (`ContaCliente`). Além disso,
garantimos a **Atomicidade** das operações com `@Transactional`.

---

## Reestruturação de Arquitetura

### Entidade: `ContaCliente`

O `saldo` deixa de ser um campo solto no `Cliente`. A conta passa a ser o motor financeiro.

| Atributo | Tipo | Detalhe |
|----------|------|---------|
| `id` | `Long` | Chave primária |
| `numeroConta` | `String` | Gerado aleatoriamente ou sequencial |
| `saldo` | `BigDecimal` | Saldo disponível |
| `statusConta` | `StatusConta` | `ATIVA` ou `BLOQUEADA` |
| `cliente` | `Cliente` | Relacionamento `@OneToOne` |

---

### Evolução da Entidade `Transacao`

Rastreabilidade total entre **contas** (não mais entre clientes).

| Atributo | Tipo | Detalhe |
|----------|------|---------|
| `contaOrigem` | `ContaCliente` | `@ManyToOne` |
| `contaDestino` | `ContaCliente` | `@ManyToOne` |
| `valor` | `BigDecimal` | Valor movimentado |
| `dataHora` | `LocalDateTime` | Momento da operação |
| `tipo` | `TipoTransacao` | `TRANSFERENCIA` ou `DEPOSITO` |
| `status` | `StatusTransacao` | Inicia como `PENDENTE` |

---

## Regras de Negócio & Casos de Uso

### Caso de Uso 1: Provisionamento de Conta (Onboarding)

| Endpoint | `POST /financeiro/contas` |
|----------|--------------------------|
| Pré-condição | Cliente com status `ATIVO` |
| Ação opcional | Depósito inicial ao criar a conta |

### Caso de Uso 2: Transferência Atômica

Implementar `transferir(idOrigem, idDestino, valor)` no `FinanceiroService`.

#### Regra de Ouro — `@Transactional`

> Se houver falha ao creditar a conta destino (ex: conta bloqueada), o débito na conta origem
> sofre **rollback automático**. O saldo do usuário não pode "sumir no limbo".

#### Validações obrigatórias

1. Ambas as contas devem estar com `statusConta == ATIVA`.
2. O dono da conta destino não pode estar `INATIVO`.
3. `saldo_origem >= valor` da transferência.

---

## Requisitos da Entrega

### Separação de Serviços (SoC)

| Serviço | Responsabilidade |
|---------|-----------------|
| `FinanceiroController` / `FinanceiroService` | Contas e movimentações financeiras |
| `ClienteService` | Identidade — **não deve conhecer saldo ou transações** |

### Garantia de Tipagem

- Uso estrito de `BigDecimal` para valores monetários.

### Gestão de Exceções

| Exceção | HTTP |
|---------|------|
| `SaldoInsuficienteException` | `422 Unprocessable Entity` |
| `ContaInativaException` | `400 Bad Request` |

---

## Por que essa melhoria é importante? (Visão de Arquiteto)

| Benefício | Motivo |
|-----------|--------|
| **Escalabilidade** | Um cliente poderá ter várias contas (Corrente, Poupança, Investimento) sem refatoração |
| **Integridade** | `@Transactional` evita condições de corrida e perda de saldo em falhas de rede |
| **Segurança** | Bloquear o cliente (identidade) agora é diferente de bloquear a conta (financeiro) |

---

## Dica do Mentor

> "Foque em explicar para o dev que o banco de dados é o solo sagrado. Se o código Java
> falhar, o `@Transactional` garante que o solo permaneça firme. A segregação de contas é o
> que permite que o sistema cresça sem virar um código 'espaguete' onde tudo está misturado
> na classe Cliente."

---

[← Fase 03](fase-03-transferencias.md) | [← Wiki](README.md) | [Próxima fase →](fase-05-state-pattern.md)
