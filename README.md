# Payment Processor (simulado)

Processador de pagamentos assíncrono, com integração de gateway **simulada** (sem chamadas externas reais). Projeto de estudo focado em arquitetura: separação entre domínio de pagamento e adapters de gateway, mensageria assíncrona com RabbitMQ, idempotência e auditoria/event sourcing básico do histórico de transações.

## Arquitetura

```
POST /payments
      │
      ▼
PaymentService ──► valida idempotencyKey ──► salva Payment (PENDING)
      │                                              │
      │                                    grava PaymentEvent (auditoria)
      ▼
publica "PaymentRequested" ──► RabbitMQ (payments.exchange)
                                      │
                                      ▼
                     PaymentRequestedListener (consumer)
                                      │
                          resolve o PaymentGateway (Strategy)
                          e chama gateway.charge(payment)
                                      │
                     publica "PaymentConfirmed" ou "PaymentFailed"
                                      │
                                      ▼
                     PaymentOutcomeListener (consumer)
                     atualiza status do Payment + grava PaymentEvent
```

- **Domínio de pagamento** (`domain/`): `Payment` e `PaymentEvent`, independentes de qualquer gateway.
- **Strategy/Adapter de gateway** (`gateway/`): interface `PaymentGateway` com duas implementações simuladas (`stripe`, `pagseguro`), escolhidas em tempo de execução por `PaymentGatewayResolver`.
- **Mensageria assíncrona** (`messaging/`): fila `payment.requested.queue` processa o pagamento; o resultado volta como evento (`payment.confirmed.queue` / `payment.failed.queue`), simulando o callback de confirmação de um gateway real.
- **Idempotência**: `idempotencyKey` com constraint `UNIQUE` no banco — repetir a mesma requisição retorna o pagamento já existente em vez de cobrar duas vezes.
- **Auditoria / event sourcing básico**: toda transição de estado é gravada na tabela `payment_events`, reconstruindo o histórico completo de cada transação.
- **Resiliência**: falhas transitórias do gateway (simuladas) disparam retry com backoff exponencial (3 tentativas); se persistirem, a mensagem vai para uma dead-letter queue (`payment.requested.dlq`) em vez de se perder.

## Stack

- Java 25
- Spring Boot 4.1 (Web MVC, Data JPA, AMQP, Validation, Actuator)
- PostgreSQL 16
- RabbitMQ 3 (management plugin)
- Lombok

## Pré-requisitos

- JDK 25
- Docker + Docker Compose

## Como rodar

**1. Suba as dependências (Postgres + RabbitMQ):**

```bash
docker compose up -d
```

**2. Rode a aplicação:**

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Painel do RabbitMQ em `http://localhost:15672` (usuário/senha: `guest`/`guest`).

**3. Confirme que está de pé:**

```bash
curl http://localhost:8080/actuator/health
```

## Endpoints

| Método | Rota                     | Descrição                                              |
|--------|--------------------------|---------------------------------------------------------|
| POST   | `/payments`               | Cria um pagamento (status inicial `PENDING`)            |
| GET    | `/payments/{id}`          | Consulta o status atual do pagamento                    |
| GET    | `/payments/{id}/events`   | Consulta o histórico de eventos (auditoria) do pagamento |

### Exemplo — criar pagamento

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.00,
    "currency": "BRL",
    "gateway": "stripe",
    "idempotencyKey": "pedido-123"
  }'
```

`gateway` aceita `stripe` ou `pagseguro`. O status evolui de forma assíncrona: `PENDING` → `PROCESSING` → `CONFIRMED` ou `FAILED`.

## Configuração

Variáveis principais em `src/main/resources/application.properties` (valores padrão já compatíveis com o `docker-compose.yml`):

| Propriedade                    | Padrão                                      |
|---------------------------------|----------------------------------------------|
| `spring.datasource.url`         | `jdbc:postgresql://localhost:5432/payments`  |
| `spring.datasource.username`    | `payments`                                    |
| `spring.datasource.password`    | `payments`                                    |
| `spring.rabbitmq.host`          | `localhost`                                   |
| `spring.rabbitmq.port`          | `5672`                                        |

## Estrutura do projeto

```
src/main/java/com/glrtech/payment/
├── api/            # Controller + DTOs (camada HTTP)
├── domain/         # Payment, PaymentEvent e enums (núcleo do domínio)
├── gateway/        # Strategy/Adapter dos gateways simulados
├── messaging/       # Configuração RabbitMQ, publisher e listeners
├── repository/     # Spring Data JPA
├── service/        # Regras de negócio (idempotência, orquestração)
└── exception/      # Exceptions e tratamento global de erros
```
