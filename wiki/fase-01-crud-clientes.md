# Fase 01 — Operações de CRUD de Clientes

> **Projeto:** trajetoria-dev | **Dificuldade:** Iniciante | **Tempo estimado:** 2–4h

---

## Visão Geral

Nesta primeira etapa construímos a fundação da API. O objetivo é configurar o ambiente com
Java 21 e garantir que consigamos salvar, ler, atualizar e excluir dados de um cliente no
banco de dados.

---

## Tecnologias Utilizadas

| Tecnologia | Detalhe |
|-----------|---------|
| Linguagem | Java 21 |
| Framework | Spring Boot 3+ |
| Persistência | Spring Data JPA |
| Banco de Dados | H2 (em memória) |
| Produtividade | Lombok |

---

## Definição da Entidade: `Cliente`

A classe deve ser criada no pacote de modelos/entidades.

> **Atenção:** Os campos `corFavorita` e `tamanhoSapato` foram adicionados propositalmente
> como débito técnico — serão removidos na Fase 02.

| Atributo | Tipo | Observação |
|----------|------|------------|
| `id` | `Long` | Chave primária (gerada automaticamente) |
| `nome` | `String` | Obrigatório |
| `email` | `String` | Único |
| `cpf` | `String` | Apenas números |
| `corFavorita` | `String` | Temporário — remoção na Fase 02 |
| `tamanhoSapato` | `Integer` | Temporário — remoção na Fase 02 |
| `criadoEm` | `LocalDateTime` | Data e hora do cadastro |

---

## Requisitos da Entrega

### 1. Configuração Inicial

- Gerar o projeto no [Spring Initializr](https://start.spring.io) com o nome `trajetoria-dev`.
- Configurar o `application.properties` para o banco H2 e habilitar o console do banco.

### 2. Desenvolvimento das Camadas

- **Repositório:** Criar a interface `ClienteRepository`.
- **Serviço:** Criar `ClienteService` com as regras de salvar, buscar, atualizar e deletar.
- **Controller:** Criar `ClienteController` com os endpoints abaixo.

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/clientes` | Cadastrar cliente |
| `GET` | `/clientes` | Listar todos |
| `GET` | `/clientes/{id}` | Buscar por ID |
| `PUT` | `/clientes/{id}` | Atualizar dados |
| `DELETE` | `/clientes/{id}` | Remover cliente |

### 3. Teste de Funcionamento

Validar todos os métodos via **Postman**, **Insomnia** ou extensão do VS Code/IntelliJ.

---

## Dica do Mentor

> "Foque em fazer o fluxo completo (o 'caminho feliz'). Não se preocupe se o código parecer
> simples demais agora. O objetivo desta fase é dominar a estrutura básica do Spring Boot.
> Nas próximas fases, vamos refinar cada detalhe!"

---

[← Wiki](README.md) | [Próxima fase →](fase-02-refinamento-dominio.md)
