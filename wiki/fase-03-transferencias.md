# Fase 03 — Sistema de Transferências e Consistência Financeira

> **Projeto:** trajetoria-dev | **Nível:** Especialista Júnior | **Tempo estimado:** 6–8h

---

## Visão Geral

O projeto deixa de ser um cadastro e passa a ser uma **aplicação financeira**. O objetivo é
implementar a lógica de **Transferência PIX entre Clientes**, garantindo que a transação só
ocorra se ambos os clientes estiverem aptos e se houver saldo suficiente.

---

## Novas Definições de Domínio

### Entidade: `Transacao`

Representa o registro histórico de cada movimentação.

| Atributo | Tipo | Detalhe |
|----------|------|---------|
| `id` | `Long` | Chave primária |
| `valor` | `BigDecimal` | Valor movimentado |
| `dataTransacao` | `LocalDateTime` | Momento da operação |
| `descricao` | `String` | Texto livre |
| `clienteOrigem` | `Cliente` | Quem envia |
| `clienteDestino` | `Cliente` | Quem recebe |
| `status` | `StatusTransacao` | Ver enum abaixo |
| `tipo` | `TipoTransacao` | Ver enum abaixo |

#### Enum `StatusTransacao`

`PENDENTE` | `CONCLUIDA` | `FALHA` | `ESTORNADA`

#### Enum `TipoTransacao`

`TRANSFERENCIA` | `DEPOSITO`

---

### Evolução da Entidade `Cliente`

| Atributo | Tipo | Observação |
|----------|------|------------|
| `saldo` | `BigDecimal` | Inicia em `0.00` — use **obrigatoriamente** `BigDecimal` |

> **Por quê `BigDecimal`?** `double` e `float` têm erros de precisão em ponto flutuante.
> Em operações financeiras isso é inaceitável.

---

## Regras de Negócio & Casos de Uso

### Transferência entre Contas (PIX)

| # | Regra | Descrição |
|---|-------|-----------|
| 1 | Existência | O cliente de destino deve existir no banco |
| 2 | Disponibilidade | Origem **e** destino não podem estar `INATIVO` ou `BLOQUEADO` |
| 3 | Saldo | Origem deve ter `saldo >= valor` da transferência |
| 4 | Atomicidade | Débito na origem e crédito no destino devem ocorrer simultaneamente |

### Depósito em Conta

Endpoint para fins de teste — permite ao cliente "gerar" saldo.

- **Regra:** Apenas clientes com status `ATIVO` podem receber depósitos.

---

## Requisitos da Entrega

### DTOs Financeiros

| DTO | Campos |
|-----|--------|
| `TransferenciaRequest` | `idDestino`, `valor`, `descricao` |
| `TransacaoResponse` | Quem enviou, quem recebeu, valor, status |

### Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/financeiro/depositar` | Aumenta o saldo do cliente |
| `POST` | `/financeiro/transferir` | Fluxo completo de validação e transferência |
| `GET` | `/clientes/{id}/extrato` | Lista transações onde o cliente foi origem ou destino |

### Lógica de Negócio (Service)

O método `transferir` deve concentrar todas as validações de saldo e de status (`ATIVO`).

---

## Spoiler — Problema para a Fase 04

> Nesta Fase 03, o dev vai criar uma lógica cheia de `if (status == PENDENTE)` para decidir
> se pode concluir ou falhar a transação. Na Fase 05 isso será resolvido com o **State Pattern**.

---

## Dica do Mentor

> "Trabalhar com transferência entre dois usuários é o primeiro passo para entender
> Transacionalidade. Se o dinheiro sair de uma conta mas não chegar na outra por um erro do
> sistema, temos um problema gravíssimo. Garanta que validar o status do Cliente Destino é tão
> importante quanto validar o saldo do Origem!"

---

[← Fase 02](fase-02-refinamento-dominio.md) | [← Wiki](README.md) | [Próxima fase →](fase-04-segregacao-dominio.md)
