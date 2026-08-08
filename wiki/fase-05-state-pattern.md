# Fase 05 — Design Patterns: Gerenciamento de Estados

> **Projeto:** trajetoria-dev | **Nível:** Especialista | **Tempo estimado:** 6–10h

---

## O que é o State Pattern?

O padrão **State** permite que um objeto altere seu comportamento quando seu estado interno
muda. Em vez de um único método com `switch-case` ou `if-else` gigante para verificar o
status de uma transação, **cada status vira uma classe própria**.

---

## Problemas que ele resolve

| Problema | Como o State Pattern ajuda |
|----------|---------------------------|
| **Complexidade Condicional** | Elimina `if (status == PENDENTE && acao == CANCELAR)` espalhados pelo código |
| **Violação do OCP** | Para adicionar um novo status, cria-se uma nova classe — sem alterar código existente |
| **Regras de Transição** | Garante que uma transação `CONCLUIDA` nunca volte para `PENDENTE` |

---

## Implementação no Projeto

O `FinanceiroService` será refatorado para que a lógica de "avançar status" fique no
**objeto de estado**, não no serviço.

### 1. A Interface de Estado

```java
public interface TransacaoState {
    void validar(Transacao transacao);
    void processar(Transacao transacao);
    void cancelar(Transacao transacao);
}
```

### 2. Estados Concretos

Criar uma classe para cada estado possível:

| Classe | Comportamento especial |
|--------|------------------------|
| `StatusPendente` | Permite avançar para `PROCESSANDO` após validação de saldo |
| `StatusProcessando` | Permite avançar para `CONCLUIDA` ou `FALHA` |
| `StatusConcluido` | `cancelar()` lança exceção — pagamento finalizado não pode ser cancelado |
| `StatusFalha` | Permite retry ou registro de auditoria |

#### Exemplo — `StatusConcluido`

```java
public class StatusConcluido implements TransacaoState {

    @Override
    public void cancelar(Transacao transacao) {
        throw new IllegalStateException(
            "Transação já concluída não pode ser cancelada."
        );
    }

    // processar() e validar() não fazem sentido neste estado
}
```

---

## Fluxo de Transição de Estados

```
PENDENTE
   │
   ▼ (validação de saldo OK)
PROCESSANDO
   │           │
   ▼           ▼
CONCLUIDA    FALHA
                │
                ▼ (opcional)
            ESTORNADA
```

---

## Requisitos da Entrega

| Requisito | Detalhe |
|-----------|---------|
| **Refatoração da `Transacao`** | A entidade delega mudanças de estado para o objeto de estado atual |
| **Validação de Transição** | Transferência só sai de `PENDENTE` para `PROCESSANDO` após validação atômica |
| **Encapsulamento** | `FinanceiroService` chama apenas `transacao.avancarEstado()` — sem saber as regras internas |

---

## Dica do Mentor

> "O State Pattern resolve um problema que todo dev cria na Fase 03: a bagunça de `if`s
> dentro do service. Quando você distribui o comportamento entre classes, o código fica
> auto-documentado — o nome da classe já explica as regras que ela aplica."

---

[← Fase 04](fase-04-segregacao-dominio.md) | [← Wiki](README.md) | [Próxima fase →](fase-06-performance-consultas.md)
