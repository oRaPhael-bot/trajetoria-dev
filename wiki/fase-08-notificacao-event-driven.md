# Fase 08 — Notificação Pós-Transação e Introdução a Microserviços

> **Projeto:** trajetoria-dev | **Nível:** Especialista+ | **Tempo estimado:** 8–12h

---

## Visão Geral

O sistema processa pagamentos, mas quando uma transação conclui ou falha **nada notifica o
cliente**. Esta fase resolve isso em dois passos progressivos:

- **Passo 1 — Spring Events (monolito):** desacoplar a notificação dentro do próprio app
  sem nenhuma infraestrutura nova.
- **Passo 2 — Microserviço real:** extrair a notificação para um projeto independente,
  com RabbitMQ como broker de mensagens e rede Docker dedicada.

---

## Por que Notificação é a fronteira certa para o primeiro microserviço

`ClienteModel` e `ContaClienteModel` têm `@OneToOne` no mesmo banco. O `FinanceiroService`
usa `@Transactional` que abrange débito + crédito ao mesmo tempo. Quebrar isso exigiria
**Saga Pattern** e transações distribuídas — complexidade desproporcional ao momento.

Notificação **não tem nenhum desses problemas**:

| Critério | Notificação |
|----------|-------------|
| Compartilha banco com financeiro? | Não |
| Precisa de `@Transactional` cruzado? | Não |
| Precisa responder de forma síncrona? | Não — pode ser assíncrona |
| Tem lógica de domínio financeiro? | Não — só registra e confirma |

---

## Passo 1 — Spring Events (monolito, sem nova infraestrutura)

### Como funciona

Quando `StateConcluido` ou `StateFalha` é atingido na máquina de estados, o
`FinanceiroService` publica um **evento de domínio** via `ApplicationEventPublisher`.
Um listener dentro do mesmo app consome esse evento de forma completamente desacoplada.

```
[FinanceiroService]
       │
       ▼  publishEvent(TransacaoConcluidaEvent)
[ApplicationEventPublisher]   ← mesmo JVM
       │
       ▼
[NotificacaoListener]
   @EventListener / @Async
   → loga confirmação estruturada
   → (simulação) envia e-mail / push
```

### Evento de Domínio

Criar em `com.trajetoria.event`:

```java
public record TransacaoConcluidaEvent(
    Long   idTransacao,
    Long   idContaOrigem,
    Long   idContaDestino,
    BigDecimal valor,
    StatusTransacao status,       // CONCLUIDA ou FALHA
    LocalDateTime   dataHora
) {}
```

### Publicando no FinanceiroService

```java
// Após atualizarStatusTransacao() confirmar a mudança de estado:
eventPublisher.publishEvent(new TransacaoConcluidaEvent(
    transacao.getIdTransacao(),
    transacao.getContaOrigem().getIdConta(),
    transacao.getContaDestino().getIdConta(),
    transacao.getValorTransferencia(),
    transacao.getStatusTransacao(),
    LocalDateTime.now()
));
```

### NotificacaoListener

Criar em `com.trajetoria.listener`:

```java
@Component
@Slf4j
public class NotificacaoListener {

    @Async
    @EventListener
    public void onTransacaoConcluida(TransacaoConcluidaEvent event) {
        if (event.status() == StatusTransacao.CONCLUIDA) {
            log.info("[NOTIF] Pagamento R${} confirmado — transação #{}",
                event.valor(), event.idTransacao());
        } else {
            log.warn("[NOTIF] Falha no pagamento R${} — transação #{}",
                event.valor(), event.idTransacao());
        }
    }
}
```

Adicionar `@EnableAsync` na `TrajetoriaApplication` para o listener não bloquear
o HTTP response da transferência.

> O `FinanceiroService` não importa, não conhece e não depende de `NotificacaoListener`.
> Se remover o listener amanhã, o service não muda uma linha.

---

## Passo 2 — Microserviço com RabbitMQ

---

## Novo Desenho de Arquitetura

### Visão geral dos dois serviços

```
┌──────────────────────────────────────────────────────┐
│                  minha_rede_dev (Docker Network)     │
│                                                      │
│  ┌─────────────────────┐     ┌────────────────────┐  │
│  │   trajetoria-dev    │     │  notificacao-service│  │
│  │  (porta 8080)       │     │  (porta 8081)       │  │
│  │                     │     │                     │  │
│  │  ClienteController  │     │  NotificacaoConsumer│  │
│  │  FinanceiroController     │  NotificacaoService │  │
│  │  FinanceiroService  │     │  NotificacaoRepo    │  │
│  │  NotificacaoPublisher────►│                     │  │
│  │                     │     │  banco: notif_db    │  │
│  │  banco: trajetoria  │     └─────────┬──────────┘  │
│  └──────────┬──────────┘               │              │
│             │                          │              │
│  ┌──────────▼──────────┐   ┌───────────▼──────────┐  │
│  │  postgres (5432)    │   │  rabbitmq (5672)     │  │
│  │  trajetoria_dev_db  │   │  fila: pag.confirmado│  │
│  └─────────────────────┘   └─────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### Fluxo de uma transferência com notificação

```
1. POST /financeiro/transferir
        │
2. FinanceiroService valida e persiste (PENDENTE)
        │
3. POST /processar-transacao → StateMachine → CONCLUIDA
        │
4. FinanceiroService → NotificacaoPublisher.publicar(event)
        │
5. [RabbitMQ — fila: pagamento.confirmado]
        │                        ▲ assíncrono, não bloqueia HTTP
6. notificacao-service consome  │
        │
7. Persiste Notificacao no banco próprio
        │
8. GET /notificacoes/{idTransacao} confirma o envio
```

---

## Estrutura dos Projetos

### trajetoria-dev (aplicação atual — refatoração necessária)

```
src/main/java/com/trajetoria/
├── TrajetoriaApplication.java
├── configuration/
│   ├── SwaggerConfig.java
│   └── RabbitMQConfig.java          ← NOVO (Passo 2)
├── controller/
│   ├── ClienteController.java
│   └── FinanceiroController.java
├── dto/
│   ├── cliente/
│   └── transacao/
├── event/                           ← NOVO (Passo 1 e 2)
│   └── TransacaoConcluidaEvent.java
├── exception/
├── listener/                        ← NOVO (Passo 1)
│   └── NotificacaoListener.java     (removido no Passo 2, substituído pelo Publisher)
├── mapper/
├── model/
├── publisher/                       ← NOVO (Passo 2)
│   └── NotificacaoPublisher.java
├── repository/
├── service/
│   ├── ClienteService.java
│   └── FinanceiroService.java
└── state/
```

### notificacao-service (projeto novo)

```
src/main/java/com/trajetoria/notificacao/
├── NotificacaoApplication.java
├── configuration/
│   └── RabbitMQConfig.java          ← mesma fila, lado consumidor
├── consumer/
│   └── NotificacaoConsumer.java     ← @RabbitListener
├── controller/
│   └── NotificacaoController.java   ← GET /notificacoes/{idTransacao}
├── dto/
│   ├── TransacaoConcluidaEvent.java  ← cópia do record (sem dependência entre projetos)
│   └── NotificacaoResponseDTO.java
├── model/
│   └── Notificacao.java
├── repository/
│   └── NotificacaoRepository.java
└── service/
    └── NotificacaoService.java
```

> **Regra importante:** `notificacao-service` não importa nenhuma classe do
> `trajetoria-dev`. O `TransacaoConcluidaEvent` é copiado como um record simples —
> cada serviço é dono do seu modelo.

---

## Infraestrutura — docker-compose.yml

O projeto já possui `docker-compose.yml` com Postgres e a rede `minha_rede_dev`.
Abaixo está o arquivo completo com as adições necessárias para o RabbitMQ e o
`notificacao-service`:

```yaml
version: '3.8'

services:

  # ─── Banco de dados principal (já existente) ──────────────────────────────
  db:
    image: postgres:17.0
    container_name: container-api-postgres-db-trajetoria-dev
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
        reservations:
          memory: 256M
          cpus: '0.25'
    environment:
      POSTGRES_DB: trajetoria_dev_db_teste
      POSTGRES_USER: dev_trajetoria_dev_teste
      POSTGRES_PASSWORD: senha123
    ports:
      - "5432:5432"
    networks:
      - minha_rede_dev

  # ─── RabbitMQ — broker de mensagens ───────────────────────────────────────
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: container-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"     # porta AMQP — usada pelos serviços Spring Boot
      - "15672:15672"   # painel de administração — http://localhost:15672
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - minha_rede_dev

  # ─── Banco de dados exclusivo do notificacao-service ──────────────────────
  db-notificacao:
    image: postgres:17.0
    container_name: container-postgres-notificacao
    environment:
      POSTGRES_DB: notificacao_db
      POSTGRES_USER: dev_notificacao
      POSTGRES_PASSWORD: senha123
    ports:
      - "5433:5432"     # porta diferente para não conflitar com o banco principal
    networks:
      - minha_rede_dev

  # ─── notificacao-service ──────────────────────────────────────────────────
  notificacao-service:
    build:
      context: ../notificacao-service    # pasta irmã do trajetoria-dev
      dockerfile: Dockerfile
    container_name: container-notificacao-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db-notificacao:5432/notificacao_db
      SPRING_DATASOURCE_USERNAME: dev_notificacao
      SPRING_DATASOURCE_PASSWORD: senha123
      SPRING_RABBITMQ_HOST: rabbitmq     # nome do container = hostname na rede Docker
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    ports:
      - "8081:8081"
    depends_on:
      rabbitmq:
        condition: service_healthy       # espera o RabbitMQ estar pronto
      db-notificacao:
        condition: service_started
    networks:
      - minha_rede_dev

# ─── Rede compartilhada (já existente) ────────────────────────────────────
networks:
  minha_rede_dev:
```

### Por que usar a mesma rede `minha_rede_dev`?

Na rede Docker, cada container é acessível pelo **nome do serviço** como hostname.
Sem a rede compartilhada, os containers ficam isolados e não conseguem se comunicar:

| De | Para | Hostname usado |
|----|------|----------------|
| `trajetoria-dev` | RabbitMQ | `rabbitmq:5672` |
| `notificacao-service` | RabbitMQ | `rabbitmq:5672` |
| `notificacao-service` | seu banco | `db-notificacao:5432` |

> Se cada serviço estivesse em sua própria rede, `trajetoria-dev` e
> `notificacao-service` não conseguiriam publicar/consumir na mesma fila.
> A rede compartilhada é o que permite a comunicação sem expor IPs fixos.

---

## Configuração RabbitMQ — trajetoria-dev

### Dependência no pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### RabbitMQConfig.java

```java
@Configuration
public class RabbitMQConfig {

    public static final String FILA             = "pagamento.confirmado";
    public static final String EXCHANGE         = "pagamento.exchange";
    public static final String ROUTING_KEY      = "pagamento.confirmado";

    @Bean
    public Queue filaPagamento() {
        // durable = true: fila sobrevive a reinicialização do RabbitMQ
        return QueueBuilder.durable(FILA).build();
    }

    @Bean
    public DirectExchange exchangePagamento() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue filaPagamento, DirectExchange exchangePagamento) {
        return BindingBuilder
            .bind(filaPagamento)
            .to(exchangePagamento)
            .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // serializa o record como JSON na fila — legível no painel do RabbitMQ
        return new Jackson2JsonMessageConverter();
    }
}
```

### application.properties (trajetoria-dev)

```properties
# RabbitMQ
spring.rabbitmq.host=localhost         # em dev local
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### NotificacaoPublisher.java

```java
@Component
@RequiredArgsConstructor
public class NotificacaoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicar(TransacaoConcluidaEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            event
        );
    }
}
```

---

## notificacao-service — Projeto Novo

### Geração no Spring Initializr

| Campo | Valor |
|-------|-------|
| **Group** | `com.trajetoria` |
| **Artifact** | `notificacao-service` |
| **Name** | `notificacao-service` |
| **Java** | 21 |
| **Dependências** | Spring Web, Spring Data JPA, Spring AMQP, PostgreSQL Driver, Lombok |

### application.properties

```properties
server.port=8081

# Banco exclusivo
spring.datasource.url=jdbc:postgresql://localhost:5433/notificacao_db
spring.datasource.username=dev_notificacao
spring.datasource.password=senha123
spring.jpa.hibernate.ddl-auto=update

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### RabbitMQConfig.java (notificacao-service)

```java
@Configuration
public class RabbitMQConfig {

    // mesmo nome de fila declarado no trajetoria-dev
    public static final String FILA = "pagamento.confirmado";

    @Bean
    public Queue filaPagamento() {
        return QueueBuilder.durable(FILA).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
```

### Entidade Notificacao

```java
@Entity
@Table(name = "notificacoes")
@Data
@NoArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long   idTransacao;     // referência externa — sem FK entre bancos
    private Long   idContaOrigem;
    private Long   idContaDestino;

    @Column(precision = 19, scale = 2)
    private BigDecimal valor;

    private String        status;       // "CONCLUIDA" ou "FALHA"
    private LocalDateTime enviadoEm;
    private Integer       tentativas;
}
```

### NotificacaoConsumer.java

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoConsumer {

    private final NotificacaoService notificacaoService;

    @RabbitListener(queues = RabbitMQConfig.FILA)
    public void consumir(TransacaoConcluidaEvent event) {
        log.info("[notificacao-service] Recebendo evento da transação #{}", event.idTransacao());
        notificacaoService.processar(event);
    }
}
```

### NotificacaoService.java

```java
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public void processar(TransacaoConcluidaEvent event) {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdTransacao(event.idTransacao());
        notificacao.setIdContaOrigem(event.idContaOrigem());
        notificacao.setIdContaDestino(event.idContaDestino());
        notificacao.setValor(event.valor());
        notificacao.setStatus(event.status().name());
        notificacao.setEnviadoEm(LocalDateTime.now());
        notificacao.setTentativas(1);

        notificacaoRepository.save(notificacao);
    }

    public Optional<Notificacao> buscarPorTransacao(Long idTransacao) {
        return notificacaoRepository.findByIdTransacao(idTransacao);
    }
}
```

### NotificacaoController.java

```java
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping("/{idTransacao}")
    public ResponseEntity<Notificacao> buscar(@PathVariable Long idTransacao) {
        return notificacaoService.buscarPorTransacao(idTransacao)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

---

## Dockerfile — notificacao-service

Criar na raiz do projeto `notificacao-service`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/notificacao-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Como subir tudo

```bash
# 1. gerar o .jar do notificacao-service
cd notificacao-service
mvn clean package -DskipTests

# 2. subir a infraestrutura completa a partir do trajetoria-dev
cd ../trajetoria-dev
docker compose up -d

# 3. verificar se tudo subiu
docker compose ps
```

Serviços disponíveis após o `up`:

| Serviço | URL |
|---------|-----|
| trajetoria-dev API | `http://localhost:8080` |
| notificacao-service API | `http://localhost:8081` |
| RabbitMQ painel | `http://localhost:15672` (guest / guest) |
| Postgres principal | `localhost:5432` |
| Postgres notificação | `localhost:5433` |

---

## Requisitos da Entrega

| # | Passo | Requisito |
|---|-------|-----------|
| 1 | Passo 1 | `TransacaoConcluidaEvent` criado e publicado após mudança de estado |
| 2 | Passo 1 | `NotificacaoListener` com `@Async` logando confirmação estruturada |
| 3 | Passo 2 | RabbitMQ adicionado no `docker-compose.yml` com `healthcheck` e na rede `minha_rede_dev` |
| 4 | Passo 2 | Banco `db-notificacao` na porta `5433` adicionado ao compose |
| 5 | Passo 2 | `RabbitMQConfig` com fila durável, exchange e converter JSON em ambos os projetos |
| 6 | Passo 2 | `NotificacaoPublisher` no `trajetoria-dev` substituindo o `ApplicationEventPublisher` |
| 7 | Passo 2 | Projeto `notificacao-service` criado com estrutura completa |
| 8 | Passo 2 | `NotificacaoConsumer` com `@RabbitListener` persistindo no banco próprio |
| 9 | Passo 2 | `GET /notificacoes/{idTransacao}` retornando confirmação do processamento |
| 10 | Passo 2 | `Dockerfile` criado no `notificacao-service` e referenciado no compose |

---

## O que esta fase ensina

| Conceito | Onde aparece |
|----------|--------------|
| **Domain Events** | `TransacaoConcluidaEvent` como contrato entre serviços |
| **Desacoplamento** | `FinanceiroService` não sabe quem consome o evento |
| **Async processing** | `@Async` no Passo 1, RabbitMQ no Passo 2 |
| **Message broker** | RabbitMQ como intermediário assíncrono confiável |
| **Database per service** | Cada serviço tem seu banco — sem FK cruzada |
| **Docker networking** | Rede compartilhada como backbone da comunicação |
| **Fronteira de microserviço** | Extrair pelo que não tem acoplamento transacional |
| **Healthcheck no compose** | `depends_on` com `condition: service_healthy` |

---

## Dica do Mentor

> "A migração para microserviço não começa com Kubernetes. Começa com a pergunta:
> 'este pedaço de responsabilidade consegue viver de forma independente?'
> Notificação responde sim — ela não precisa saber como o saldo foi debitado, só precisa
> saber que aconteceu. O `ApplicationEventPublisher` do Passo 1 e o RabbitMQ do Passo 2
> são a **mesma ideia em escalas diferentes**. Quem entende o Passo 1, entende o Passo 2.
> A rede Docker é o que substitui o 'mesmo JVM' — continua sendo um canal de comunicação,
> só que agora entre processos independentes."

---

[← Fase 07](fase-07-testes-unitarios.md) | [← Wiki](README.md)
