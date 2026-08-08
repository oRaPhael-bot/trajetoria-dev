# Fase 06 — Performance e Consultas Avançadas

> **Projeto:** trajetoria-dev | **Nível:** Especialista+ | **Tempo estimado:** 5–7h

---

## Visão Geral

Com o sistema complexo e funcional, o foco agora é **eficiência**. Consultas padrão do
Spring Data JPA (`findById`) podem ser lentas ou insuficientes para relatórios pesados.

---

## O que deve ser feito

### 1. Queries Customizadas com `@Query`

Parar de buscar entidades completas quando apenas um resumo é necessário.

**Projeções:** Criar uma consulta que retorna apenas `id`, `valor` e `data` de uma transação,
sem carregar os objetos `Cliente` inteiros — evitando o problema do **N+1**.

```java
@Query("SELECT t.id, t.valor, t.dataHora FROM Transacao t WHERE t.contaOrigem.id = :contaId")
List<TransacaoResumo> buscarResumoByContaOrigem(@Param("contaId") Long contaId);
```

---

### 2. Consultas de Relatório Complexas

Implementar um endpoint de **Relatório de Volume Financeiro** usando JPQL ou Native Query.

**Exemplo:** Somar todos os valores de transferências `CONCLUIDAS` de um cliente em um
intervalo de datas.

```java
@Query("""
    SELECT SUM(t.valor)
    FROM Transacao t
    WHERE t.contaOrigem.id = :contaId
      AND t.status = 'CONCLUIDA'
      AND t.dataHora BETWEEN :inicio AND :fim
    """)
BigDecimal somarTransferenciasConcluidas(
    @Param("contaId") Long contaId,
    @Param("inicio") LocalDateTime inicio,
    @Param("fim") LocalDateTime fim
);
```

---

### 3. Repository Customizado com `EntityManager`

Para consultas **dinâmicas** (onde o usuário pode ou não passar filtros como Data, Tipo ou
Status), implementar um Repository Customizado usando `EntityManager` e **Criteria API**
(ou **QueryDSL**, opcional).

---

## Requisitos da Entrega

| Requisito | Detalhe |
|-----------|---------|
| **Otimização do Extrato** | Implementar paginação com `Pageable` e filtros personalizados |
| **Query Nativa** | Pelo menos uma consulta usando **SQL puro** (`nativeQuery = true`) |
| **Data Integrity** | Consultas avançadas devem respeitar o status de atividade do cliente e da conta |

---

## Referência Rápida — Spring Data + Performance

| Técnica | Quando usar |
|---------|-------------|
| `@Query` (JPQL) | Consultas customizadas simples com objetos JPA |
| `@Query(nativeQuery=true)` | SQL puro, funções específicas do banco, performance máxima |
| Projeções (interface/record) | Buscar apenas os campos necessários — evita N+1 |
| `Pageable` | Listagens com paginação e ordenação |
| `Criteria API` | Filtros dinâmicos (número variável de parâmetros) |
| QueryDSL | Alternativa type-safe à Criteria API |

---

## Dica do Mentor

> "Performance não é sobre escrever código mais rápido — é sobre buscar apenas o que você
> precisa. Uma query que traz 50 campos quando você precisa de 3 é 16x mais lenta do que
> deveria. Projeções e paginação são o primeiro nível de otimização que qualquer sênior
> vai exigir em code review."

---

[← Fase 05](fase-05-state-pattern.md) | [← Wiki](README.md) | [Próxima fase →](fase-07-testes-unitarios.md)
