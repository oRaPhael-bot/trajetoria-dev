# Fase 07 — Cobertura de Testes Unitários

> **Projeto:** trajetoria-dev | **Nível:** Especialista+ | **Tempo estimado:** 6–10h

---

## Visão Geral

Com o sistema funcional e otimizado, chegou a hora de **garantir que ele continue funcionando**.
Testes unitários validam o comportamento de cada camada de forma isolada, sem subir o servidor
ou conectar ao banco. O objetivo desta fase é atingir uma cobertura significativa nas camadas
de **Service** e **Controller**, e entender quando e por que mockar dependências.

---

## Tecnologias Utilizadas

| Tecnologia | Detalhe |
|-----------|---------|
| **JUnit 5** | Framework de testes (já incluso no Spring Boot Starter Test) |
| **Mockito** | Criação de mocks e verificação de interações |
| **AssertJ** | Assertions fluentes e legíveis |
| **Spring Boot Test** | Suporte a slices de teste (`@WebMvcTest`, `@DataJpaTest`) |
| **MockMvc** | Teste de endpoints HTTP sem servidor real |

---

## Conceitos Fundamentais

### O que é um Teste Unitário?

Um teste unitário verifica **uma unidade de código** (método ou classe) de forma **isolada**.
Todas as dependências externas (repositórios, outros services, APIs) são substituídas por
**mocks** — objetos que simulam o comportamento esperado.

```
[Teste] ──► [Service] ──► [Mock do Repository]
                               ↑
                    retorna dados controlados
```

### Pirâmide de Testes

```
         /\
        /  \  ← Testes E2E (poucos, lentos, caros)
       /────\
      / Integ \  ← Testes de Integração
     /──────────\
    /  Unitários  \  ← Muitos, rápidos, baratos (foco desta fase)
   /______________\
```

> **Regra prática:** A base da pirâmide deve ser sempre a maior. Unitários são rápidos e
> baratos — escreva muitos. Integração e E2E são lentos — escreva os essenciais.

---

## Estrutura de um Teste com JUnit 5 + Mockito

```java
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("deve lançar exceção ao cadastrar cliente menor de 18 anos")
    void deveLancarExcecaoParaMenorDeIdade() {
        ClienteRequest request = new ClienteRequest(
            "João", "joao@email.com", "12345678900",
            LocalDate.now().minusYears(16), "senha123"
        );

        assertThatThrownBy(() -> clienteService.cadastrar(request))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("menor de 18");
    }
}
```

---

## O que testar em cada camada

### Service — Regras de Negócio (prioridade máxima)

| Caso de Teste | Cenário |
|---------------|---------|
| Cadastro válido | Deve salvar e retornar `ClienteResponse` sem a senha |
| Menor de idade | Deve lançar exceção ao receber `dataNascimento` < 18 anos |
| E-mail duplicado | Deve lançar exceção quando `email` já existe |
| Soft delete | `PATCH /status` deve alterar o status e não deletar o registro |
| Transferência — saldo insuficiente | Deve lançar `SaldoInsuficienteException` |
| Transferência — conta bloqueada | Deve lançar `ContaInativaException` |
| Transferência — sucesso | Deve debitar origem, creditar destino e retornar `CONCLUIDA` |
| Depósito — cliente inativo | Deve rejeitar o depósito |
| State Pattern | `StatusConcluido.cancelar()` deve lançar `IllegalStateException` |

### Controller — Comportamento HTTP

Use `@WebMvcTest` + `MockBean` para testar apenas a camada web, sem subir o contexto completo.

| Caso de Teste | Verificação |
|---------------|-------------|
| `POST /clientes` válido | Retorna `201 Created` + corpo com `ClienteResponse` |
| `POST /clientes` inválido | Retorna `400 Bad Request` com mensagem de erro |
| `GET /clientes/{id}` inexistente | Retorna `404 Not Found` |
| `PATCH /clientes/{id}/alterar-senha` | Retorna `204 No Content` |
| `POST /financeiro/transferir` sem saldo | Retorna `422 Unprocessable Entity` |

```java
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService clienteService;

    @Test
    @DisplayName("POST /clientes deve retornar 201 com dados do cliente criado")
    void deveCadastrarClienteComSucesso() throws Exception {
        ClienteResponse response = new ClienteResponse(1L, "Ana", "ana@email.com", "ATIVO");
        given(clienteService.cadastrar(any())).willReturn(response);

        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Ana",
                      "email": "ana@email.com",
                      "cpf": "12345678900",
                      "dataNascimento": "2000-01-01",
                      "senha": "senha123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Ana"))
            .andExpect(jsonPath("$.senha").doesNotExist());
    }
}
```

---

## Boas Práticas de Nomenclatura

Use o padrão **`deve[Comportamento]Quando[Condição]`** ou **`@DisplayName`** descritivo:

```java
// Ruim
@Test
void test1() { }

// Bom
@Test
@DisplayName("deve retornar lista vazia quando não há clientes ativos")
void deveRetornarListaVaziaQuandoNaoHaClientesAtivos() { }
```

---

## Cobertura com JaCoCo

Adicione o plugin no `pom.xml` para gerar relatório de cobertura:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

Gere o relatório com:

```bash
mvn test jacoco:report
```

O relatório HTML será gerado em `target/site/jacoco/index.html`.

### Metas de Cobertura

| Camada | Meta mínima |
|--------|-------------|
| Services | 80% |
| Controllers | 70% |
| Entidades / Enums | 60% |
| Repositórios | Não testar unitariamente — use `@DataJpaTest` |

> **Atenção:** Cobertura alta não garante qualidade. 100% de cobertura com assertions fracas
> é pior do que 70% com casos bem pensados. Foque em cobrir **regras de negócio**, não getters.

---

## Requisitos da Entrega

| Requisito | Detalhe |
|-----------|---------|
| `ClienteServiceTest` | Cobrir todos os casos de uso das Fases 01 e 02 |
| `FinanceiroServiceTest` | Cobrir transferência (sucesso + todas as falhas), depósito |
| `TransacaoStateTest` | Validar transições válidas e inválidas do State Pattern (Fase 05) |
| `ClienteControllerTest` | Ao menos um teste por endpoint (happy path + erro) |
| `FinanceiroControllerTest` | Transferência e depósito via MockMvc |
| **Relatório JaCoCo** | Gerar e apresentar o relatório com cobertura ≥ 70% nos services |

---

## Dica do Mentor

> "Testes unitários não são sobre encontrar bugs agora — são sobre **não introduzir bugs
> amanhã**. Quando você refatorar o `FinanceiroService` na Fase seguinte, os testes vão
> gritar imediatamente se você quebrar algo. Pense em cada teste como uma rede de segurança.
> Quanto mais testes bem escritos, mais confiança você tem para evoluir o sistema."

---

[← Fase 06](fase-06-performance-consultas.md) | [← Wiki](README.md) | [Próxima fase →](fase-08-notificacao-event-driven.md)
