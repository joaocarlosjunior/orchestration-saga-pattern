# Orchestration Saga Pattern - Event-Driven Microservices (Restaurante)

Implementação de um sistema de pedidos distribuído utilizando o **Saga Pattern (Orchestration-based)** para gerenciamento e sincronização de transações entre microsserviços. Um **orquestrador central** coordena as transações distribuídas, gerenciando o ciclo de vida do pedido e controlando os fluxos felizes e de compensação de forma resiliente, idempotente e orientada a eventos por meio do **Apache Kafka**.

---

## Desenho da Arquitetura

<img width="1279" height="703" alt="Image" src="https://github.com/user-attachments/assets/59d503e8-8502-4709-b0e9-97e8ce9a8f9b" />

## 📌 Índice

- [Tecnologias](#tecnologias)
- [Serviços](#serviços)
  - [Order Service](#order-service)
  - [Kitchen Service](#kitchen-service)
  - [Delivery Service](#delivery-service)
  - [Inventory Service](#inventory-service)
  - [Saga Orchestrator](#saga-orchestrator)
- [Fluxos da Saga](#fluxos-da-saga)
  - [Happy Path - Pedido Concluído com Sucesso](#happy-path---pedido-concluído-com-sucesso)
  - [Compensação - Falha/Rejeição na Cozinha](#compensação---falha-ou-rejeição-na-cozinha)
  - [Compensação - Falha na Entrega](#compensação---falha-na-entrega)
- [Máquina de Estados](#máquina-de-estados)
- [Tópicos Kafka](#tópicos-kafka)
- [Schemas de Banco de Dados](#schemas-de-banco-de-dados)
- [Patterns Implementados](#patterns-implementados)
- [API Endpoints](#api-endpoints)
- [Como Executar](#como-executar)

---

## 🛠️ Tecnologias

| Componente | Tecnologia |
|---|---|
| **Order Service** | Java 17, Spring Boot 3.3.1, JPA / Hibernate, H2 Database (Memória) |
| **Kitchen Service** | Java 17, Spring Boot 3.3.1, JPA / Hibernate, H2 Database (Memória) |
| **Delivery Service** | Java 17, Spring Boot 3.3.1, JPA / Hibernate, H2 Database (Memória) |
| **Inventory Service** | Java 17, Spring Boot 3.3.1, JPA / Hibernate, H2 Database (Memória) |
| **Saga Orchestrator** | Java 17, Spring Boot 3.3.1, JPA / Hibernate, H2 Database (Memória) |
| **Mensageria** | Apache Kafka (KRaft mode via Docker Compose) |
| **Idempotência** | Constraint de Banco de Dados (`idempotency_key UNIQUE`) |
| **Testes** | JUnit 5, Spring Embedded Kafka |

---

## 📦 Serviços

### Order Service
**Porta:** `8081` | **Banco:** H2 (`jdbc:h2:mem:orderdb`) | **Linguagem:** Java (Spring Boot)

Ponto de entrada do sistema para o cliente final. Gerencia a criação, edição e cancelamento de pedidos, além de manter o status atualizado de acordo com os comandos enviados pelo orquestrador da Saga.

**Responsabilidades:**
*   Registrar pedidos com status `CREATED`
*   Garantir a idempotência do envio de pedidos via header `X-Idempotency-Key`
*   Publicar eventos no tópico `orders-topic` ao criar novos pedidos
*   Consumir comandos de atualização de status do tópico `commands-order`
*   Permitir edição e cancelamento do pedido apenas no estado inicial `CREATED`

**Estrutura de Diretórios:**
```
order-service/
├── pom.xml
└── src/main/java/com/joaocarlos/order_service/
    ├── OrderServiceApplication.java
    ├── controller/                  # REST Controller (OrderController)
    ├── domain/                      # Entidades JPA (Order, OrderItem) e enums (OrderStatus)
    ├── dto/                         # Requests e Responses (CreateOrderRequest, OrderResponse, etc.)
    ├── exception/                   # Tratamento global de exceções
    ├── repository/                  # JPA Repository (OrderRepository)
    ├── service/                     # Regras de negócio do pedido
    └── messaging/                   # Integração com o Kafka
        ├── OrderProducer.java       # Publicador do orders-topic
        ├── OrderCommandConsumer.java# Consumidor de comandos commands-order
        └── dto/                     # DTOs de eventos de mensageria
```

---

### Kitchen Service
**Porta:** `8083` | **Banco:** H2 (`jdbc:h2:mem:kitchendb`) | **Linguagem:** Java (Spring Boot)

Controla o fluxo físico de preparação dos pratos na cozinha. Valida a disponibilidade de itens automaticamente e gerencia as confirmações de preparo manuais dos operadores de cozinha.

**Responsabilidades:**
*   Consumir ordens de produção do tópico `kitchen-topic`
*   Validar automaticamente a viabilidade do pedido (falha se `productId == "999"` ou `quantity > 100`)
*   Expor endpoints para controle manual de produção (`POST /confirm`, `POST /ready`, `POST /reject`)
*   Publicar atualizações de status (`KITCHEN_PREPARING`, `KITCHEN_CONFIRMED`, `KITCHEN_FAILED`) no tópico `commands-kitchen`

**Estrutura de Diretórios:**
```
kitchen-service/
├── pom.xml
└── src/main/java/com/joaocarlos/kitchen_service/
    ├── KitchenServiceApplication.java
    ├── controller/                  # Endpoints REST de produção na cozinha
    ├── domain/                      # Entidades JPA (KitchenOrder, KitchenOrderItem, KitchenOrderStatus)
    ├── dto/                         # DTOs para APIs REST (KitchenOrderResponse, etc.)
    ├── exception/                   # Exceptions customizadas e GlobalExceptionHandler
    ├── repository/                  # KitchenOrderRepository
    ├── service/                     # Lógica de confirmação, conclusão e rejeição
    └── messaging/                   # Integração com Kafka
        ├── KitchenOrderConsumer.java# Consumidor de ordens de produção
        └── dto/                     # DTOs dos eventos (KitchenCommandEvent, etc.)
```

---

### Delivery Service
**Porta:** `8084` | **Banco:** H2 (`jdbc:h2:mem:deliverydb`) | **Linguagem:** Java (Spring Boot)

Gerencia a distribuição e a logística de entrega dos pedidos prontos. Expõe endpoints para os entregadores interagirem com o sistema.

**Responsabilidades:**
*   Consumir ordens de entrega do tópico `delivery-topic`
*   Registrar novas entregas locais com status `PENDING`
*   Expor rotas para o entregador gerenciar a viagem (`POST /start`, `POST /complete`, `POST /fail`)
*   Validar falhas automáticas se o endereço de entrega contiver a palavra-chave `"FAIL"`
*   Publicar eventos de entrega (`DELIVERY_STARTED`, `DELIVERY_CONFIRMED`, `DELIVERY_FAILED`) no tópico `commands-delivery`

**Estrutura de Diretórios:**
```
delivery-service/
├── pom.xml
└── src/main/java/com/joaocarlos/delivery_service/
    ├── DeliveryServiceApplication.java
    ├── controller/                  # Endpoints de logística
    ├── domain/                      # Entidades JPA (Delivery, DeliveryStatus)
    ├── dto/                         # DTOs de integração HTTP
    ├── exception/                   # Exceptions e handlers locais
    ├── repository/                  # DeliveryRepository
    ├── service/                     # Controle de fluxo da entrega
    └── messaging/                   # Integração com Kafka
        ├── DeliveryOrderConsumer.java# Consumidor do delivery-topic
        └── dto/                     # DTOs de eventos de entrega
```

---

### Inventory Service
**Porta:** `8085` | **Banco:** H2 (`jdbc:h2:mem:inventorydb`) | **Linguagem:** Java (Spring Boot)

Controla o estoque físico de ingredientes (matéria-prima) necessários para a confecção dos pratos do restaurante e gerencia as receitas/composição de cada produto oferecido no cardápio.

**Responsabilidades:**
*   Registrar insumos/ingredientes e gerenciar seus saldos de estoque
*   Permitir reabastecimento via rota REST dedicada
*   Registrar receitas mapeando produtos do cardápio a um conjunto de insumos
*   Consumir comandos de baixa de estoque do tópico `inventory-topic`
*   Realizar a dedução de estoque de forma transacional e atômica
*   Publicar o resultado da baixa (`INVENTORY_DEDUCTED`, `INVENTORY_FAILED`) no tópico `commands-inventory`

**Estrutura de Diretórios:**
```
inventory-service/
├── pom.xml
└── src/main/java/com/joaocarlos/inventory_service/
    ├── InventoryServiceApplication.java
    ├── controller/                  # Endpoints REST de ingredientes e receitas
    ├── domain/                      # Entidades JPA (Ingredient, RecipeItem)
    ├── dto/                         # DTOs REST (IngredientRequest, RecipeRequest, etc.)
    ├── exception/                   # Exceptions locais e handler global
    ├── repository/                  # Repositories (IngredientRepository, RecipeItemRepository)
    ├── service/                     # Lógica de controle e dedução
    └── messaging/                   # Integração com Kafka
        ├── InventoryCommandConsumer.java
        └── dto/                     # DTOs de eventos (InventoryCommandEvent, etc.)
```

---

### Saga Orchestrator
**Porta:** `8082` | **Banco:** H2 (`jdbc:h2:mem:orchestratordb`) | **Linguagem:** Java (Spring Boot)

O motor central da Saga. Ele não expõe APIs públicas diretas; opera inteiramente reagindo a eventos de mensageria e coordenando o fluxo de comandos de avanço e compensação.

**Responsabilidades:**
*   Consumir novos pedidos do `orders-topic` e inicializar o ciclo de vida da Saga
*   Manter a persistência de estados da Saga na tabela `sagas`
*   Gerenciar a máquina de estados, transicionando entre passos de forma idempotente e segura
*   Emitir comandos para os microsserviços nos tópicos do Kafka correspondentes
*   Atualizar o `order-service` sobre o andamento e o status final (`SUCCESS`/`FAILURE`) do pedido

**Estrutura de Diretórios:**
```
orchestrator-service/
├── pom.xml
└── src/main/java/com/joaocarlos/orchestrator_service/
    ├── OrchestratorServiceApplication.java
    ├── config/                      # Configurações do Spring Kafka e tópicos automáticos
    ├── domain/                      # Entidade Saga, SagaStatus e passos SagaStep
    ├── repository/                  # SagaRepository
    ├── service/                     # Lógica da máquina de estados do orquestrador (OrchestratorService)
    └── messaging/                   # Consumidores de eventos de todos os serviços
        ├── OrderCreatedConsumer.java
        ├── KitchenCommandConsumer.java
        ├── DeliveryCommandConsumer.java
        └── dto/                     # Payload DTOs compartilhados na orquestração
```

---

## 🔄 Fluxos da Saga

### Happy Path - Pedido Concluído com Sucesso

O diagrama abaixo apresenta o caminho ideal, desde a requisição HTTP inicial do cliente até a confirmação de entrega concluída.

```
Cliente        order-service       orchestrator-service      kitchen-service      inventory-service     delivery-service
   │                 │                      │                       │                    │                     │
   │ POST /orders    │                      │                       │                    │                     │
   │────────────────>│                      │                       │                    │                     │
   │   201 Created   │                      │                       │                    │                     │
   │<────────────────│                      │                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │     orders-topic     │                       │                    │                     │
   │                 │   (Pedido Criado)    │                       │                    │                     │
   │                 │─────────────────────>│                       │                    │                     │
   │                 │                      │ Inicia Saga (STARTED) │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │(ORDER_WAITING_KITCHEN)│                      │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │     kitchen-topic     │                    │                     │
   │                 │                      │   (Ordem Produção)    │                    │                     │
   │                 │                      │──────────────────────>│                    │                     │
   │                 │                      │                       │ Salva PENDING      │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │                       │ Operador Cozinha   │                     │
   │                 │                      │                       │ POST /confirm      │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    commands-kitchen   │                    │                     │
   │                 │                      │  (KITCHEN_PREPARING)  │                    │                     │
   │                 │                      │<──────────────────────│                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    inventory-topic    │                    │                     │
   │                 │                      │   (Baixa Estoque)     │                    │                     │
   │                 │                      │───────────────────────────────────────────>│                     │
   │                 │                      │                       │                    │ Deduz insumos       │
   │                 │                      │                       │                    │                     │
   │                 │                      │   commands-inventory  │                    │                     │
   │                 │                      │  (INVENTORY_DEDUCTED) │                    │                     │
   │                 │                      │<───────────────────────────────────────────│                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │   (ORDER_PREPARING)  │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │                       │ Operador Cozinha   │                     │
   │                 │                      │                       │ POST /ready        │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    commands-kitchen   │                    │                     │
   │                 │                      │  (KITCHEN_CONFIRMED)  │                    │                     │
   │                 │                      │<──────────────────────│                    │                     │
   │                 │                      │ Transiciona p/        │                    │                     │
   │                 │                      │ KITCHEN_CONFIRMED     │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │     delivery-topic    │                    │                     │
   │                 │                      │    (Ordem Entrega)    │                    │                     │
   │                 │                      │─────────────────────────────────────────────────────────────────>│
   │                 │                      │                       │                    │                     │ Salva PENDING
   │                 │                      │                       │                    │                     │
   │                 │                      │                       │                    │                     │ Entregador
   │                 │                      │                       │                    │                     │ POST /start
   │                 │                      │                       │                    │                     │
   │                 │                      │   commands-delivery   │                    │                     │
   │                 │                      │   (DELIVERY_STARTED)  │                    │                     │
   │                 │                      │<─────────────────────────────────────────────────────────────────│
   │                 │    commands-order    │                       │                    │                     │
   │                 │  (ORDER_DELIVERING)  │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │ Entregador
   │                 │                      │                       │                    │                     │ POST /complete
   │                 │                      │                       │                    │                     │
   │                 │                      │   commands-delivery   │                    │                     │
   │                 │                      │  (DELIVERY_CONFIRMED) │                    │                     │
   │                 │                      │<─────────────────────────────────────────────────────────────────│
   │                 │                      │ Finaliza Saga         │                    │                     │
   │                 │                      │ (COMPLETED)           │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │    (ORDER_SUCCESS)   │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │  Pedido: SUCCESS     │                       │                    │                     │
```

---

### Compensação - Falha na Baixa de Estoque (Inventário)

Se a cozinha confirmar o preparo físico do pedido, mas a baixa de ingredientes no `inventory-service` falhar (por exemplo, por insuficiência de estoque), o orquestrador cancela o preparo na cozinha (compensação) e encerra o pedido como falha.

```
Cliente        order-service       orchestrator-service      kitchen-service      inventory-service     delivery-service
   │                 │                      │                       │                    │                     │
   │ POST /orders    │                      │                       │                    │                     │
   │────────────────>│                      │                       │                    │                     │
   │                 │     orders-topic     │                       │                    │                     │
   │                 │─────────────────────>│                       │                    │                     │
   │                 │                      │     kitchen-topic     │                    │                     │
   │                 │                      │──────────────────────>│                    │                     │
   │                 │                      │                       │ Salva PENDING      │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │                       │ Operador Cozinha   │                     │
   │                 │                      │                       │ POST /confirm      │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    commands-kitchen   │                    │                     │
   │                 │                      │  (KITCHEN_PREPARING)  │                    │                     │
   │                 │                      │<──────────────────────│                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    inventory-topic    │                    │                     │
   │                 │                      │   (Baixa Estoque)     │                    │                     │
   │                 │                      │───────────────────────────────────────────>│                     │
   │                 │                      │                       │                    │ Estoque             │
   │                 │                      │                       │                    │ Insuficiente        │
   │                 │                      │   commands-inventory  │                    │                     │
   │                 │                      │   (INVENTORY_FAILED)  │                    │                     │
   │                 │                      │<───────────────────────────────────────────│                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │   kitchen-commands    │                    │                     │
   │                 │                      │  (CANCEL_PREPARING)   │                    │                     │
   │                 │                      │──────────────────────>│                    │                     │
   │                 │                      │                       │ Transiciona p/     │                     │
   │                 │                      │                       │ FAILED             │                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │   (ORDER_FAILURE +   │                       │                    │                     │
   │                 │     justificativa)   │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │  Pedido: FAILURE     │                       │                    │                     │
```

---

### Compensação - Falha ou Rejeição na Cozinha

Se ocorrer uma falha de negócio automática (item inválido) ou uma rejeição manual por parte da equipe da cozinha *antes* de iniciar a confecção, a Saga é compensada e o pedido finalizado como falha (sem envolver o inventário).

```
Cliente        order-service       orchestrator-service      kitchen-service      inventory-service     delivery-service
   │                 │                      │                       │                    │                     │
   │ POST /orders    │                      │                       │                    │                     │
   │────────────────>│                      │                       │                    │                     │
   │                 │     orders-topic     │                       │                    │                     │
   │                 │─────────────────────>│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │(ORDER_WAITING_KITCHEN)│                      │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │     kitchen-topic     │                    │                     │
   │                 │                      │──────────────────────>│                    │                     │
   │                 │                      │                       │ Rejeição Manual ou │                     │
   │                 │                      │                       │ item "999" (Falha) │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │    commands-kitchen   │                    │                     │
   │                 │                      │    (KITCHEN_FAILED)   │                    │                     │
   │                 │                      │<──────────────────────│                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │    commands-order    │                       │                    │                     │
   │                 │    (ORDER_FAILURE)   │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │  Pedido: FAILURE     │                       │                    │                     │
```

---

### Compensação - Falha na Entrega

Se a cozinha concluir o prato com sucesso, mas o entregador registrar uma falha manual (cliente ausente) ou automática (endereço de simulação `"FAIL"`), a entrega é marcada como `FAILED` e a Saga encerra o pedido como `FAILURE` no `order-service` (ingredientes são considerados desperdiçados e não estornados).

```
Cliente        order-service       orchestrator-service      kitchen-service      inventory-service     delivery-service
   │                 │                      │                       │                    │                     │
   │                 │                      │ ... (Cozinha e        │                    │                     │
   │                 │                      │ estoque concluídos)   │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │                      │     delivery-topic    │                    │                     │
   │                 │                      │    (Ordem Entrega)    │                    │                     │
   │                 │                      │─────────────────────────────────────────────────────────────────>│
   │                 │                      │                       │                    │                     │ Salva PENDING
   │                 │                      │                       │                    │                     │
   │                 │                      │                       │                    │                     │ Entregador
   │                 │                      │                       │                    │                     │ POST /start
   │                 │                      │                       │                    │                     │
   │                 │                      │   commands-delivery   │                    │                     │
   │                 │                      │   (DELIVERY_STARTED)  │                    │                     │
   │                 │                      │<─────────────────────────────────────────────────────────────────│
   │                 │    commands-order    │                       │                    │                     │
   │                 │  (ORDER_DELIVERING)  │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │ Ocorre problema.
   │                 │                      │                       │                    │                     │ Entregador chama
   │                 │                      │                       │                    │                     │ POST /fail (motivo)
   │                 │                      │                       │                    │                     │
   │                 │                      │   commands-delivery   │                    │                     │
   │                 │                      │   (DELIVERY_FAILED)   │                    │                     │
   │                 │                      │<─────────────────────────────────────────────────────────────────│
   │                 │    commands-order    │                       │                    │                     │
   │                 │   (ORDER_FAILURE +   │                       │                    │                     │
   │                 │     justificativa)   │                       │                    │                     │
   │                 │<─────────────────────│                       │                    │                     │
   │                 │                      │                       │                    │                     │
   │                 │  Pedido: FAILURE     │                       │                    │                     │
```

---

## ⚙️ Máquina de Estados

O `orchestrator-service` armazena e altera o estado da transação de forma centralizada por meio do `OrchestratorService.java`. As transições são guiadas pela tabela de estados a seguir:

| Estado Atual (Saga) | Evento Consumido | Próximo Estado | Próximo Passo (`currentStep`) | Comando/Evento Emitido | Descrição |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `-` | Novo pedido no `orders-topic` | `STARTED` | `KITCHEN` | `ORDER_WAITING_KITCHEN` & `kitchen-topic` | Inicialização da Saga de Pedidos |
| `STARTED` | `KITCHEN_PREPARING` | `KITCHEN_PREPARING` | `KITCHEN` | `inventory-topic` | Preparação iniciada; orquestrador solicita baixa de estoque |
| `KITCHEN_PREPARING` | `INVENTORY_DEDUCTED` | `KITCHEN_PREPARING` | `KITCHEN` | `ORDER_PREPARING` | Estoque baixado com sucesso; notifica order-service de preparo |
| `KITCHEN_PREPARING` | `INVENTORY_FAILED` | `FAILED` | `KITCHEN` | `kitchen-commands-topic` & `ORDER_FAILURE` | Estoque insuficiente; cancela cozinha e falha pedido |
| `KITCHEN_PREPARING` | `KITCHEN_CONFIRMED` | `KITCHEN_CONFIRMED` | `DELIVERY` | `delivery-topic` | Comida pronta na cozinha; enviada para o delivery |
| `STARTED` / `KITCHEN_PREPARING` | `KITCHEN_FAILED` | `FAILED` | `KITCHEN` | `ORDER_FAILURE` | Cancelamento ou falha no preparo do pedido pela cozinha |
| `KITCHEN_CONFIRMED` | `DELIVERY_STARTED` | `KITCHEN_CONFIRMED` | `DELIVERY` | `ORDER_DELIVERING` | Pedido em trânsito de entrega com o entregador |
| `KITCHEN_CONFIRMED` | `DELIVERY_CONFIRMED`| `COMPLETED` | `DELIVERY` | `ORDER_SUCCESS` | Pedido entregue e Saga concluída com sucesso |
| `KITCHEN_CONFIRMED` | `DELIVERY_FAILED` | `FAILED` | `DELIVERY` | `ORDER_FAILURE` | Falha logística na entrega; compensação aplicada (perda de estoque) |

---

## ✉️ Tópicos Kafka

| Tópico | Origem (Produtor) | Destino (Consumidor) | Descrição |
|---|---|---|---|
| `orders-topic` | `order-service` | `orchestrator-service` | Notifica a criação de um pedido para início da Saga. |
| `kitchen-topic` | `orchestrator-service` | `kitchen-service` | Dispara a ordem de confecção do prato para a cozinha. |
| `commands-kitchen` | `kitchen-service` | `orchestrator-service` | Envia as atualizações de preparo da cozinha (`PREPARING`, `CONFIRMED`, `FAILED`). |
| `delivery-topic` | `orchestrator-service` | `delivery-service` | Dispara a ordem de logística de entrega. |
| `commands-delivery` | `delivery-service` | `orchestrator-service` | Envia as atualizações de logística (`STARTED`, `CONFIRMED`, `FAILED`). |
| `commands-order` | `orchestrator-service` | `order-service` | Envia as atualizações de status que serão expostas ao cliente. |
| `inventory-topic` | `orchestrator-service` | `inventory-service` | Dispara o comando para dedução de ingredientes do estoque. |
| `commands-inventory` | `inventory-service` | `orchestrator-service` | Notifica o resultado da baixa de estoque (`INVENTORY_DEDUCTED`, `INVENTORY_FAILED`). |
| `kitchen-commands-topic` | `orchestrator-service` | `kitchen-service` | Dispara comandos de compensação para a cozinha (como cancelamento de preparo). |

---

## 🗄️ Schemas de Banco de Dados

Schemas SQL DDL gerados automaticamente nos respectivos bancos H2 de cada microsserviço:

### 1. `order-service` (`orderdb`)
```sql
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    total_value DECIMAL(10, 2) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL, -- CREATED, WAITING_KITCHEN, PENDING, DELIVERING, SUCCESS, FAILURE
    idempotency_key VARCHAR(36) UNIQUE NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

### 2. `orchestrator-service` (`orchestratordb`)
```sql
CREATE TABLE sagas (
    saga_id VARCHAR(36) PRIMARY KEY, -- Equivale ao ID do Pedido
    status VARCHAR(30) NOT NULL,     -- STARTED, KITCHEN_PREPARING, KITCHEN_CONFIRMED, COMPLETED, FAILED
    current_step VARCHAR(20) NOT NULL, -- KITCHEN, DELIVERY
    delivery_address VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 3. `kitchen-service` (`kitchendb`)
```sql
CREATE TABLE kitchen_orders (
    order_id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL, -- PENDING, PREPARING, COMPLETED, FAILED
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE kitchen_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES kitchen_orders(order_id) ON DELETE CASCADE
);
```

### 4. `delivery-service` (`deliverydb`)
```sql
CREATE TABLE deliveries (
    order_id VARCHAR(36) PRIMARY KEY,
    delivery_address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, DELIVERING, DELIVERED, FAILED
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 5. `inventory-service` (`inventorydb`)
```sql
CREATE TABLE ingredients (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    available_quantity INT NOT NULL
);

CREATE TABLE recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE,
    UNIQUE (product_id, ingredient_id)
);
```


---

## ⚡ Patterns Implementados

### 1. Saga Orchestration
Centralização das decisões de fluxo e de compensação de erros em um único ponto coordenador (`orchestrator-service`). Isto desacopla as regras do fluxo distribuído dos microsserviços de negócio (cozinha e entrega), tornando o sistema extensível e fácil de auditar.

### 2. Idempotência
Proteção e consistência contra reprocessamento ou requisições HTTP duplicadas:
*   A rota de criação de pedido (`POST /api/v1/orders`) exige a inclusão do header `X-Idempotency-Key`.
*   O banco de dados do `order-service` contém uma restrição de unicidade (`UNIQUE`) para a coluna `idempotency_key`.
*   Caso uma chamada repetida com a mesma chave seja feita, o sistema ignora a criação e republicação do evento no Kafka, retornando imediatamente o pedido existente com status HTTP `200 OK`.

### 3. Resiliência e Políticas de Retentativa
Tolerância local a falhas transitórias em cada consumidor Kafka:
*   Política configurada de **3 tentativas** de reprocessamento em caso de erros de infraestrutura.
*   **Backoff exponencial** configurado de `1s`, `2s` e `4s` para aguardar a recuperação de sistemas parceiros.
*   Transição para falhas permanentes caso as retentativas expirem.

---

## 🔌 API Endpoints

### Order Service (Porta 8081)
| Método | Rota | Parâmetros/Headers | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Header: `X-Idempotency-Key` (UUID) | Cria um pedido na base como `CREATED` e publica no Kafka. |
| `PUT` | `/api/v1/orders/{id}` | - | Permite editar itens e endereço se o pedido estiver em `CREATED`. |
| `POST` | `/api/v1/orders/{id}/cancel`| - | Cancela o pedido (apenas se estiver em `CREATED`). |
| `GET` | `/api/v1/orders/{id}` | - | Retorna os detalhes do pedido e status atual (para polling do cliente). |

### Kitchen Service (Porta 8083)
| Método | Rota | Payload (JSON) | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/kitchen/orders/pending` | - | Lista todos os pedidos recebidos aguardando início de preparo. |
| `POST` | `/api/v1/kitchen/orders/{id}/confirm`| - | Confirma o início de preparo físico do pedido. |
| `POST` | `/api/v1/kitchen/orders/{id}/ready` | - | Finaliza a confecção do pedido na cozinha (comida pronta). |
| `POST` | `/api/v1/kitchen/orders/{id}/reject`| `{ "reason": "texto" }` | Rejeita manualmente o preparo por escassez ou indisponibilidade. |

### Delivery Service (Porta 8084)
| Método | Rota | Payload (JSON) | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/deliveries/pending` | - | Lista todas as entregas disponíveis que aguardam saída de motoboy. |
| `POST` | `/api/v1/deliveries/{id}/start` | - | Altera o status para `DELIVERING` (início da rota do entregador). |
| `POST` | `/api/v1/deliveries/{id}/complete`| - | Confirma a entrega concluída com sucesso no endereço. |
| `POST` | `/api/v1/deliveries/{id}/fail` | `{ "reason": "texto" }` | Reporta uma falha manual na tentativa de entrega com justificativa. |

### Inventory Service (Porta 8085)
| Método | Rota | Payload (JSON) | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/inventory/ingredients` | `{ "id": "insumo-x", "name": "Nome", "availableQuantity": 100 }` | Cadastra um novo insumo no estoque. |
| `POST` | `/api/v1/inventory/ingredients/{id}/add-stock` | `{ "quantity": 10 }` | Adiciona/reabastece unidades do estoque do insumo. |
| `POST` | `/api/v1/inventory/recipes` | `{ "productId": "prod-101", "ingredients": [ { "ingredientId": "insumo-x", "quantity": 2 } ] }` | Associa uma receita (composição) para confecção de um prato. |

---

## 🚀 Como Executar

### Pré-requisitos
*   **Java 17** SDK instalado
*   **Docker** e **Docker Compose**
*   **Maven** instalado no sistema

### 1. Inicializar a infraestrutura local (Kafka)
Na raiz do repositório, execute o Docker Compose para subir o container do Apache Kafka rodando em modo KRaft:
```bash
docker compose up -d
```

### 2. Executar os Microsserviços
Em terminais separados (ou pela sua IDE de preferência), rode o comando do Maven para inicializar cada projeto Spring Boot:

```bash
# Terminal 1: Iniciar Order Service (Porta 8081)
cd order-service
mvn spring-boot:run

# Terminal 2: Iniciar Orchestrator Service (Porta 8082)
cd orchestrator-service
mvn spring-boot:run

# Terminal 3: Iniciar Kitchen Service (Porta 8083)
cd kitchen-service
mvn spring-boot:run

# Terminal 4: Iniciar Delivery Service (Porta 8084)
cd delivery-service
mvn spring-boot:run

# Terminal 5: Iniciar Inventory Service (Porta 8085)
cd inventory-service
mvn spring-boot:run
```

### 3. Roteiro Fim a Fim de Teste Feliz

0.  **Cadastrar Estoque e Receita (Antes de criar o pedido)**:
    Primeiro, inicialize o estoque de ingredientes e associe-os a um prato (`prod-101`):
    ```bash
    # Cadastrar ingrediente: Massa de Pizza
    curl -X POST http://localhost:8085/api/v1/inventory/ingredients \
      -H "Content-Type: application/json" \
      -d '{
        "id": "ing-dough",
        "name": "Massa de Pizza",
        "availableQuantity": 10
      }'
      
    # Cadastrar ingrediente: Pepperoni
    curl -X POST http://localhost:8085/api/v1/inventory/ingredients \
      -H "Content-Type: application/json" \
      -d '{
        "id": "ing-pepperoni",
        "name": "Fatias de Pepperoni",
        "availableQuantity": 100
      }'

    # Cadastrar a receita do prato "prod-101" (Pizza de Pepperoni)
    # Requer 1 Massa e 20 Pepperonis por pizza
    curl -X POST http://localhost:8085/api/v1/inventory/recipes \
      -H "Content-Type: application/json" \
      -d '{
        "productId": "prod-101",
        "ingredients": [
          { "ingredientId": "ing-dough", "quantity": 1 },
          { "ingredientId": "ing-pepperoni", "quantity": 20 }
        ]
      }'
    ```

1.  **Criar o Pedido**:
    ```bash
    curl -X POST http://localhost:8081/api/v1/orders \
      -H "Content-Type: application/json" \
      -H "X-Idempotency-Key: c9d7e3a9-1122-3344-5566-abcdefabcdef" \
      -d '{
        "customerId": "8f8b8a8b-8a8b-8a8b-8a8b-8f8b8a8b8a8b",
        "deliveryAddress": "Rua das Laranjeiras, 456",
        "items": [
          {
            "productId": "prod-101",
            "productName": "Pizza de Pepperoni",
            "quantity": 1,
            "unitPrice": 45.00
          }
        ]
      }'
    ```
    *Anote o `orderId` retornado na resposta JSON.*

2.  **Confirmar Início de Preparo (Equipe de Cozinha)**:
    ```bash
    curl -X POST http://localhost:8083/api/v1/kitchen/orders/{orderId}/confirm
    ```

3.  **Finalizar Preparo (Equipe de Cozinha)**:
    ```bash
    curl -X POST http://localhost:8083/api/v1/kitchen/orders/{orderId}/ready
    ```

4.  **Iniciar Rota de Entrega (Logística/Entregador)**:
    ```bash
    curl -X POST http://localhost:8084/api/v1/deliveries/{orderId}/start
    ```

5.  **Finalizar Entrega (Entregador)**:
    ```bash
    curl -X POST http://localhost:8084/api/v1/deliveries/{orderId}/complete
    ```

6.  **Validar Status Final (Cliente - Polling)**:
    ```bash
    curl http://localhost:8081/api/v1/orders/{orderId}
    ```
    *A resposta exibirá o status `"status": "SUCCESS"`.*
