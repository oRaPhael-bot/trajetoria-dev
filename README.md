# trajetoria-dev — Wiki do Projeto

Documentação das fases de evolução da API. Cada fase representa um marco de aprendizado,
partindo de um CRUD básico até padrões de design e performance em nível especialista.

---

## Fases

| # | Fase | Nível | Tempo estimado |
|---|------|-------|----------------|
| [01](fase-01-crud-clientes.md) | Operações de CRUD de Clientes | Iniciante | 2–4h |
| [02](fase-02-refinamento-dominio.md) | Refinamento de Domínio e Integridade de Dados | Intermediário | 4–6h |
| [03](fase-03-transferencias.md) | Sistema de Transferências e Consistência Financeira | Especialista Júnior | 6–8h |
| [04](fase-04-segregacao-dominio.md) | Segregação de Domínio e Confiabilidade Transacional | Especialista | 6–8h |
| [05](fase-05-state-pattern.md) | Design Patterns — Gerenciamento de Estados | Especialista | 6–10h |
| [06](fase-06-performance-consultas.md) | Performance e Consultas Avançadas | Especialista+ | 5–7h |
| [07](fase-07-testes-unitarios.md) | Cobertura de Testes Unitários | Especialista+ | 6–10h |
| [08](fase-08-notificacao-event-driven.md) | Notificação Pós-Transação e Microserviços | Especialista+ | 8–12h |

---

## Visão Geral da Jornada

```
Fase 01 ──► Fase 02 ──► Fase 03 ──► Fase 04 ──► Fase 05 ──► Fase 06 ──► Fase 07 ──► Fase 08
 CRUD       DTOs &      PIX &        ContaCli.   State        Queries      Testes       Events &
 básico     Validação   Saldo        @Transact.  Pattern      & Perf.      Unitários    RabbitMQ
```

Cada fase constrói sobre a anterior. Não pule etapas — os conceitos se acumulam.
