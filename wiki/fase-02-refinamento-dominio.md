# Fase 02 — Refinamento de Domínio e Integridade de Dados

> **Projeto:** trajetoria-dev | **Nível:** Intermediário | **Tempo estimado:** 4–6h

---

## Visão Geral

Evoluímos o CRUD básico para um **Sistema de Gestão de Identidade**. O foco principal é:

- **Integridade de Dados** — regras que impedem dados inválidos de entrar no sistema.
- **Segurança da Informação** — separação entre o que o banco armazena e o que o usuário vê.

---

## Arquitetura e Novos Conceitos

### 1. Evolução do Modelo de Domínio

| Ação | Detalhe |
|------|---------|
| Limpeza técnica | Remover `corFavorita` e `tamanhoSapato` |
| Segurança | Adicionar campo `String senha` |
| Ciclo de vida | Adicionar `StatusCliente status` (Enum) |

#### Enum `StatusCliente`

| Status | Descrição |
|--------|-----------|
| `ATIVO` | Cadastro liberado para todas as operações |
| `INATIVO` | Cliente desativado (histórico preservado, sem acesso) |
| `BLOQUEADO` | Suspensão temporária por regra de segurança |

---

### 2. Camada de Transferência — DTOs com Java Records

A partir desta fase, a **Entidade JPA fica escondida no banco**. A comunicação externa ocorre
via Records:

| Record | Finalidade |
|--------|-----------|
| `ClienteRequest` | Entrada de dados no cadastro (inclui `senha` e `dataNascimento`) |
| `ClienteResponse` | Saída de dados — **nunca deve conter o campo `senha`** |
| `SenhaUpdate` | Objeto minimalista para troca de credenciais |

---

## Regras de Negócio & Casos de Uso

### Validação de Maioridade (Cadastro)

O `ClienteService` deve calcular a idade a partir de `dataNascimento`.

- **Regra:** Bloquear cadastros de usuários com menos de 18 anos.
- **Dica:** Use `java.time.LocalDate` e `java.time.Period`.

### Gestão de Estado — Soft Delete

O comando de exclusão física foi descontinuado. Agora trabalhamos com **inativação lógica**.

| Endpoint | Comportamento |
|----------|--------------|
| `PATCH /clientes/{id}/status` | Altera o status para outro status — registro permanece no banco |

### Segregação de Credenciais — Troca de Senha

Dados sensíveis não são alterados no fluxo comum de atualização de perfil.

| Endpoint | Comportamento |
|----------|--------------|
| `PATCH /clientes/{id}/alterar-senha` | Atualiza exclusivamente o campo `senha` |

---

## Requisitos da Entrega

- **Refatoração do Controller**
  - Remover o endpoint `DELETE`.
  - Substituir retornos de `Entity` por `ClienteResponse`.
  - Implementar os novos endpoints `PATCH`.
- **Lógica de Filtro:** `GET /clientes` deve retornar apenas clientes com status `ATIVO`.
- **Encapsulamento:** A senha deve ser salva no banco, mas **nunca** aparecer no JSON de resposta.

---

## Dica do Mentor

> "No mercado sênior, não se 'apaga' nada. Dados são histórico. Ao implementar o Soft Delete
> e Enums, você está criando um sistema auditável e resiliente. O uso de Records do Java 21
> mostra que você está atualizado com as melhores práticas de performance e código limpo.
> O segredo desta fase é entender que o seu banco de dados e a sua API são mundos diferentes!"

---

[← Fase 01](fase-01-crud-clientes.md) | [← Wiki](README.md) | [Próxima fase →](fase-03-transferencias.md)
