# Mini Stock Exchange — ACME Limit Order Matching Engine

> A fully functional limit-order matching engine built from first principles — price-time priority, partial fills, concurrency-safe cancellation, and crash recovery, all in a single Spring Boot application. No real money. No auth. Just the core mechanics of how exchanges actually work.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [What the System Does](#2-what-the-system-does)
3. [Scope and Deliberate Exclusions](#3-scope-and-deliberate-exclusions)
4. [Architecture](#4-architecture)
5. [Matching Rules](#5-matching-rules)
6. [Order Book Data Structure](#6-order-book-data-structure)
7. [Data Model](#7-data-model)
8. [API Documentation](#8-api-documentation)
9. [Example Matching Scenario](#9-example-matching-scenario)
10. [Concurrency Strategy](#10-concurrency-strategy)
11. [Restart Recovery](#11-restart-recovery)
12. [How to Run Locally](#12-how-to-run-locally)
13. [How to Run Tests](#13-how-to-run-tests)
14. [Load Test Method and Results](#14-load-test-method-and-results)
15. [Known Limitations](#15-known-limitations)
16. [Future Improvements](#16-future-improvements)

---

## 1. Project Overview

This project is a **mini stock exchange** built as a single Spring Boot application. It implements a classical **limit-order book** for one fictional instrument: **ACME**.

The goal is to demonstrate a complete, correctly functioning order matching engine — from REST API ingestion all the way through price-time priority matching, partial fills, persistence, and in-memory recovery after a restart.

It is built as a portfolio/educational project to show a deep understanding of:
- How real financial exchanges work internally
- How to design and lock concurrent shared state correctly
- How to separate pure algorithmic logic from infrastructure concerns
- How to build a production-style Spring Boot backend with proper layering

**Technology Stack**

| Part | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.x |
| Database | PostgreSQL |
| DB Migrations | Flyway |
| Data Access | Spring Data JPA |
| Unit Tests | JUnit 5 |
| Integration Tests | Spring Boot Test, MockMvc |
| API Docs | Swagger / OpenAPI (Springdoc) |
| Containerization | Docker Compose |
| Load Testing | k6 |
| Version Control | Git + GitHub |

---

## 2. What the System Does

### Core Behaviour

The system accepts **limit buy and sell orders** for the stock `ACME` and matches them according to **price-time priority** — exactly as a real exchange order book does.

**Placing an order:**
- A user submits a buy or sell order with a limit price and quantity.
- The engine immediately checks the opposite side of the book for compatible orders.
- If a match is found, a **trade** is created and both orders are updated.
- If no match or only a partial match is found, the remaining quantity rests in the order book waiting for a future match.

**Cancelling an order:**
- A user can cancel any order that is OPEN or PARTIALLY_FILLED.
- The order is immediately removed from the active book and marked CANCELLED.
- FILLED or already CANCELLED orders cannot be cancelled.

**Viewing the order book:**
- The current state of all active buy and sell orders is available via API.
- Buy orders are shown highest price first. Sell orders are shown lowest price first.

**Viewing trade history:**
- Every matched trade is persisted and retrievable via API.

### Order Lifecycle

```
Submitted --> OPEN -----------------------------------------> CANCELLED
               |                                                 ^
               | (partial match)                                 |
               v                                                 |
         PARTIALLY_FILLED ----------------------------------->---+
               |
               | (full remaining quantity matched)
               v
            FILLED
```

| State | Meaning |
|---|---|
| OPEN | Submitted, no quantity matched yet |
| PARTIALLY_FILLED | Some quantity matched, remaining quantity > 0 |
| FILLED | Entire original quantity matched, order is closed |
| CANCELLED | Manually cancelled by the user while still active |

---

## 3. Scope and Deliberate Exclusions

### What Is Included

| Feature | Included |
|---|---|
| One stock: ACME | Yes |
| Buy limit orders | Yes |
| Sell limit orders | Yes |
| Limit price enforcement | Yes |
| Price-time priority matching | Yes |
| Partial fills | Yes |
| Order cancellation | Yes |
| Trade history | Yes |
| PostgreSQL persistence | Yes |
| Restart recovery (rebuild book from DB) | Yes |
| REST API | Yes |
| Swagger / OpenAPI documentation | Yes |
| Concurrency safety (single-instance) | Yes |
| Docker Compose for local setup | Yes |

### What Is Deliberately Excluded

| Feature | Excluded | Reason |
|---|---|---|
| User login / authentication | No | Out of scope; userId is a plain number |
| User account balances | No | No settlement or balance tracking |
| Portfolio management | No | No position tracking |
| Multiple stocks / instruments | No | Only ACME; multi-instrument adds complexity without teaching new matching concepts |
| Market orders | No | Only limit orders; market orders require balance validation |
| Stop orders | No | Out of scope for V1 |
| Kafka / message brokers | No | Not needed for single-instance correctness demo |
| Microservices | No | Single Spring Boot app is the right scope |
| WebSocket real-time updates | No | REST polling is sufficient |
| Frontend UI | No | Swagger UI covers manual testing |
| Real financial connectivity | No | Fictional engine only |
| Multi-node / distributed coordination | No | Single instance with a JVM lock |

**Scope commitment:** No features outside the "Included" table will be added during the initial build. Scope creep is the most common reason projects fail to ship.

---

## 4. Architecture

### High-Level Flow

```
HTTP Client (curl / Postman / Swagger)
        |
        v
  +--------------+
  |  Controller  |  -- validates HTTP, maps DTOs, returns responses
  +------+-------+
         |
         v
  +--------------+
  | OrderService |  -- orchestrates, holds the ReentrantLock, calls engine + repos
  +------+-------+
         |
    +----+----------------------------+
    |                                 |
    v                                 v
+------------------+        +------------------+
|  MatchingEngine  |        | OrderRepository  |
|   (pure logic)   |        | TradeRepository  |
|    OrderBook     |        | (Spring Data JPA)|
+------------------+        +--------+---------+
                                      |
                                      v
                               +-----------+
                               | PostgreSQL |
                               +-----------+

  RecoveryService (on startup) --> reads DB --> rebuilds OrderBook
```

### Package Structure

```
com.project.exchange
├── controller/        -- REST controllers and request mapping
├── service/           -- OrderService, RecoveryService (business logic, locking)
├── matching/          -- MatchingEngine, OrderBook, Comparators, MatchResult
├── domain/            -- JPA entities: Order, Trade; Enums: OrderSide, OrderStatus
├── repository/        -- Spring Data JPA repositories
├── dto/               -- Request and Response DTOs (never expose entities directly)
├── exception/         -- Custom exceptions and GlobalExceptionHandler
└── config/            -- Spring config beans (OpenAPI config, etc.)
```

### Layer Responsibilities

| Layer | Responsibility | Depends On |
|---|---|---|
| controller | HTTP in/out, DTO mapping | service, dto |
| service | Orchestration, locking, transactions | matching, repository, domain |
| matching | Pure matching algorithm, no I/O | domain |
| domain | JPA entities and enums | Nothing |
| repository | DB access | domain |
| dto | Wire transfer objects | Nothing |
| exception | Error types + global handler | Nothing |
| config | Spring beans | Framework |

**Design principle:** The matching layer has zero knowledge of Spring, JPA, or HTTP. It receives Order objects and returns a MatchResult. This makes it fully unit-testable in isolation.

---

## 5. Matching Rules

### Matching Condition

A new **buy order** matches against the best (lowest) sell order when:
```
buy.limitPrice >= sell.limitPrice
```

A new **sell order** matches against the best (highest) buy order when:
```
sell.limitPrice <= buy.limitPrice
```

If no compatible order exists on the opposite side, the incoming order rests in the book.

### Execution Price

The execution price is the **price of the resting (older) order**, not the incoming one.

Example: A sell order resting at Rs.48 is hit by a new buy order at Rs.52. The trade executes at Rs.48 (the resting order's price). Price improvement goes to the aggressor.

### Priority

Orders on each side of the book are sorted by **price-time priority**:

**Buy side (bid book):**
1. Highest price first (most aggressive buyer)
2. If prices are equal -> earliest order first (oldest gets priority)

**Sell side (ask book):**
1. Lowest price first (most aggressive seller)
2. If prices are equal -> earliest order first

### Partial Fill Algorithm

```
while (incoming.remainingQuantity > 0 AND opposite book is not empty) {
    bestOpposite = peek top of opposite book
    if (prices are NOT compatible) break

    matchedQty = min(incoming.remainingQuantity, bestOpposite.remainingQuantity)

    create Trade(executionPrice = bestOpposite.limitPrice, quantity = matchedQty)

    incoming.remainingQuantity -= matchedQty
    bestOpposite.remainingQuantity -= matchedQty

    if (bestOpposite.remainingQuantity == 0) {
        remove bestOpposite from book
        bestOpposite.status = FILLED
    } else {
        bestOpposite.status = PARTIALLY_FILLED
    }
}

if (incoming.remainingQuantity > 0) {
    insert incoming into its own side of the book
    incoming.status = OPEN or PARTIALLY_FILLED
} else {
    incoming.status = FILLED
}
```

### State Transitions

| Current State | Event | New State |
|---|---|---|
| New order | No match found | OPEN |
| New order | Some quantity matched | PARTIALLY_FILLED |
| New order | All quantity matched | FILLED |
| OPEN | Cancelled | CANCELLED |
| PARTIALLY_FILLED | More matching | PARTIALLY_FILLED or FILLED |
| PARTIALLY_FILLED | Cancelled | CANCELLED |
| FILLED | Cancel attempted | Rejected (409 Conflict) |
| CANCELLED | Match attempted | Must never happen |
| CANCELLED | Cancel again | Rejected (409 Conflict) |

---

## 6. Order Book Data Structure

### Data Structure Choice: PriorityQueue

Each side of the book is a java.util.PriorityQueue<Order> with a custom Comparator.

**Why PriorityQueue?**
- O(log n) insertion of new orders
- O(1) peek at the best order (top of heap)
- O(log n) removal when an order is filled or cancelled
- Simple, standard Java -- no external dependencies

**Buy book comparator (BuyOrderComparator):**
```
Higher price -> comes first
If same price -> earlier createdAt -> comes first
Sort by: -price, then +createdAt
```

**Sell book comparator (SellOrderComparator):**
```
Lower price -> comes first
If same price -> earlier createdAt -> comes first
Sort by: +price, then +createdAt
```

### Example Order Book State

```
BUY SIDE (bid)            SELL SIDE (ask)
------------------        ------------------
Rs.62 x 5  [oldest]      Rs.65 x 10 [oldest]
Rs.60 x 10               Rs.67 x 5
Rs.60 x 3  [newer]       Rs.70 x 8
Rs.55 x 20               Rs.72 x 15
```

A new sell order at Rs.60 would match against the Rs.62 buy (5 units filled), then the first Rs.60 buy (5 of 10 filled), and the remaining 0 would rest.

---

## 7. Data Model

### orders Table

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| user_id | BIGINT | Identifies submitting user (no auth) |
| instrument | VARCHAR(10) | Always ACME |
| side | VARCHAR(4) | BUY or SELL |
| limit_price | NUMERIC(19,4) | Must be > 0; BigDecimal in Java |
| original_quantity | BIGINT | Total quantity at submission; must be > 0 |
| remaining_quantity | BIGINT | Decreases as fills happen |
| status | VARCHAR(20) | OPEN, PARTIALLY_FILLED, FILLED, CANCELLED |
| created_at | TIMESTAMPTZ | Set at creation, never updated |
| updated_at | TIMESTAMPTZ | Updated on every state change |
| sequence_number | BIGINT | Global monotonic counter; used for recovery ordering |

**Indexes:**
- idx_orders_status on (status)
- idx_orders_created_at on (created_at)
- idx_orders_user_id on (user_id)

### trades Table

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| buy_order_id | UUID | FK -> orders.id |
| sell_order_id | UUID | FK -> orders.id |
| instrument | VARCHAR(10) | Always ACME |
| execution_price | NUMERIC(19,4) | Price of the resting order |
| quantity | BIGINT | Quantity exchanged in this trade |
| executed_at | TIMESTAMPTZ | When the match happened |

**Indexes:**
- idx_trades_executed_at on (executed_at)
- idx_trades_buy_order_id on (buy_order_id)
- idx_trades_sell_order_id on (sell_order_id)

### Validation Rules

| Field | Rule |
|---|---|
| userId | Must be a positive integer |
| limitPrice | Must be > 0; BigDecimal; max 4 decimal places |
| quantity | Must be > 0; integer |
| side | Must be exactly BUY or SELL |
| instrument | Always ACME (set by server, not by client) |

---

## 8. API Documentation

Interactive Swagger UI is available at: http://localhost:8080/swagger-ui.html

### POST /orders -- Place an Order

**Request body:**
```json
{
  "userId": 1,
  "side": "BUY",
  "price": 50.00,
  "quantity": 10
}
```

**Success response (201 Created):**
```json
{
  "id": "a3f1c2d4-1111-2222-3333-444455556666",
  "userId": 1,
  "instrument": "ACME",
  "side": "BUY",
  "limitPrice": 50.00,
  "originalQuantity": 10,
  "remainingQuantity": 0,
  "status": "FILLED",
  "createdAt": "2026-09-04T09:00:00Z",
  "updatedAt": "2026-09-04T09:00:00Z",
  "sequenceNumber": 1
}
```

### DELETE /orders/{id} -- Cancel an Order

**Success response (200 OK):**
```json
{
  "id": "a3f1c2d4-1111-2222-3333-444455556666",
  "userId": 1,
  "instrument": "ACME",
  "side": "BUY",
  "limitPrice": 50.00,
  "originalQuantity": 10,
  "remainingQuantity": 10,
  "status": "CANCELLED",
  "createdAt": "2026-09-04T09:00:00Z",
  "updatedAt": "2026-09-04T09:01:00Z",
  "sequenceNumber": 1
}
```

Error responses:
- 404 Not Found -- order ID does not exist
- 409 Conflict -- order is already FILLED or CANCELLED

### GET /orders/{id} -- Get Order Status

**Success response (200 OK):**
```json
{
  "id": "a3f1c2d4-1111-2222-3333-444455556666",
  "userId": 1,
  "instrument": "ACME",
  "side": "BUY",
  "limitPrice": 50.00,
  "originalQuantity": 10,
  "remainingQuantity": 4,
  "status": "PARTIALLY_FILLED",
  "createdAt": "2026-09-04T09:00:00Z",
  "updatedAt": "2026-09-04T09:00:05Z",
  "sequenceNumber": 1
}
```

### GET /orderbook -- View Active Order Book

**Success response (200 OK):**
```json
{
  "buyOrders": [
    {
      "id": "...",
      "userId": 1,
      "instrument": "ACME",
      "side": "BUY",
      "limitPrice": 62.00,
      "originalQuantity": 5,
      "remainingQuantity": 5,
      "status": "OPEN",
      "createdAt": "2026-09-04T09:00:00Z",
      "updatedAt": "2026-09-04T09:00:00Z",
      "sequenceNumber": 1
    }
  ],
  "sellOrders": [
    {
      "id": "...",
      "userId": 2,
      "instrument": "ACME",
      "side": "SELL",
      "limitPrice": 65.00,
      "originalQuantity": 10,
      "remainingQuantity": 10,
      "status": "OPEN",
      "createdAt": "2026-09-04T09:00:00Z",
      "updatedAt": "2026-09-04T09:00:00Z",
      "sequenceNumber": 2
    }
  ]
}
```

### GET /trades -- View Trade History

**Success response (200 OK):**
```json
[
  {
    "id": "b4e2d3f1-2222-3333-4444-555566667777",
    "buyOrderId": "a3f1c2d4-...",
    "sellOrderId": "c5g3e4h2-...",
    "instrument": "ACME",
    "executionPrice": 49.50,
    "quantity": 10,
    "executedAt": "2026-09-04T09:00:00Z"
  }
]
```

### HTTP Status Code Reference

| Situation | HTTP Status |
|---|---|
| Order placed successfully | 201 Created |
| Order cancelled | 200 OK |
| Order book retrieved | 200 OK |
| Trade history retrieved | 200 OK |
| Order retrieved | 200 OK |
| Invalid price or quantity | 400 Bad Request |
| Invalid side value | 400 Bad Request |
| Order not found | 404 Not Found |
| Cancel on filled/cancelled order | 409 Conflict |
| Unexpected server error | 500 Internal Server Error |

---

## 9. Example Matching Scenario

The order book starts empty.

**Step 1: User 1 places a sell order**
```
POST /orders
{ "userId": 1, "side": "SELL", "price": 50.00, "quantity": 10 }
```
No match. Order rests in ask book.
```
ASK: [Rs.50 x 10, User 1]
BID: (empty)
```

**Step 2: User 2 places a sell order at a higher price**
```
POST /orders
{ "userId": 2, "side": "SELL", "price": 52.00, "quantity": 5 }
```
No match. Order rests.
```
ASK: [Rs.50 x 10, User 1] [Rs.52 x 5, User 2]
BID: (empty)
```

**Step 3: User 3 places a buy order that partially crosses**
```
POST /orders
{ "userId": 3, "side": "BUY", "price": 51.00, "quantity": 7 }
```
Best ask = Rs.50 (User 1). Buy price Rs.51 >= Rs.50 -> match.
Matched qty = min(7, 10) = 7. Trade: Rs.50 x 7.
User 1: remaining = 3 -> PARTIALLY_FILLED. User 3: remaining = 0 -> FILLED.
```
ASK: [Rs.50 x 3, User 1] [Rs.52 x 5, User 2]
BID: (empty)
TRADES: [Rs.50 x 7, buy=User3, sell=User1]
```

**Step 4: User 4 places a buy order that clears multiple sell orders**
```
POST /orders
{ "userId": 4, "side": "BUY", "price": 55.00, "quantity": 10 }
```
- Best ask = Rs.50 (User 1, 3 remaining). Trade: Rs.50 x 3. User 1 -> FILLED. Buyer remaining = 7.
- Best ask = Rs.52 (User 2, 5). Trade: Rs.52 x 5. User 2 -> FILLED. Buyer remaining = 2.
- No more compatible asks. Buyer rests with 2.
```
ASK: (empty)
BID: [Rs.55 x 2, User 4]
TRADES: [Rs.50x3, buy=User4, sell=User1], [Rs.52x5, buy=User4, sell=User2]
```

---

## 10. Concurrency Strategy

### The Problem

The order book is shared mutable state. Two concurrent requests -- a cancellation and a new matching order -- can race and corrupt the book if not synchronized.

### The Solution

A single `java.util.concurrent.locks.ReentrantLock` guards all mutations.

```java
// In OrderService
private final ReentrantLock lock = new ReentrantLock();

@Transactional
public void processOrder(Order incomingOrder) {
    lock.lock();
    try {
        orderRepository.save(incomingOrder);
        MatchResult matchResult = matchingEngine.match(incomingOrder, orderBook);
        orderRepository.save(matchResult.getUpdatedIncomingOrder());
        for (Order order : matchResult.getUpdatedOppositeOrders()) {
            orderRepository.save(order);
        }
        for (Trade trade : matchResult.getCreatedTrades()) {
            tradeRepository.save(trade);
        }
    } finally {
        lock.unlock(); // always released, even on exception
    }
}

@Transactional
public Order cancelOrder(UUID id) {
    lock.lock();
    try {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }
        orderBook.removeOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return order;
    } finally {
        lock.unlock();
    }
}
```

**Race condition guarantees:**
- An order can be either filled or cancelled -- never both.
- An order can never be matched after it has been cancelled.
- Remaining quantity can never go negative.
- Total quantity across all fills can never exceed original quantity.

**What this does NOT cover:**
- Multiple JVM instances (requires distributed lock)
- High-throughput lock-free architectures (LMAX Disruptor, etc.)

---

## 11. Restart Recovery

### Why Recovery Is Needed

The order book lives in memory (PriorityQueues in OrderBook). If the application restarts, the in-memory state is lost. PostgreSQL is the source of truth.

### Recovery Process

On startup, before accepting requests, `RecoveryService` runs:

```
1. Query: SELECT * FROM orders WHERE status IN ('OPEN', 'PARTIALLY_FILLED') ORDER BY created_at ASC
2. Preserves chronological FIFO price-time priority
3. For each order:
   - Add to the correct side of OrderBook (buy or sell PriorityQueue)
   - Do NOT re-run matching (the DB is authoritative)
4. Application becomes ready (ApplicationReadyEvent fires)
```

FILLED and CANCELLED orders are never loaded into the active book.
PARTIALLY_FILLED orders are loaded with their remainingQuantity (not original).

### Manual Verification

```bash
# 1. Place an order that doesn't match
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "side": "BUY", "price": 40.00, "quantity": 5}'

# 2. Stop the app (Ctrl+C)

# 3. Restart
./mvnw spring-boot:run

# 4. Verify the order is still in the book
curl http://localhost:8080/orderbook
```

---

## 12. How to Run Locally

### Prerequisites

- Java 17+ (built on Java 21)
- Docker Desktop
- Maven (or use ./mvnw wrapper)

### Step 1: Start PostgreSQL

```bash
docker compose up -d
```

Starts PostgreSQL on localhost:5432 with:
- Database: exchange
- Username: exchange_user
- Password: exchange_pass

Flyway runs migrations automatically on first boot (`V1`, `V2`, `V3`, `V4`).

### Step 2: Run the Application

```bash
./mvnw spring-boot:run
```

Application starts on http://localhost:8080.

### Step 3: Verify

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Step 4: Swagger UI

Open: http://localhost:8080/swagger-ui.html

### Quick curl Demo

```bash
# Place a sell order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "side": "SELL", "price": 50.00, "quantity": 10}'

# Place a crossing buy order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 2, "side": "BUY", "price": 52.00, "quantity": 6}'

# View the order book
curl http://localhost:8080/orderbook

# View trades
curl http://localhost:8080/trades
```

---

## 13. How to Run Tests

### Unit Tests (Matching Engine & Logic)

```bash
./mvnw test -Dtest=MatchingEngineTest
```

Covers: matching algorithm, partial fills, price-time priority, state transitions.

### Concurrency Tests (Multi-Threaded Race Conditions)

```bash
./mvnw test -Dtest=OrderServiceConcurrencyTest
```

Covers:
- **Cancellation vs. Matching Race**: 10 repeated multi-threaded runs using `CountDownLatch` as a synchronized starting gun. Verifies orders never enter invalid hybrid states (`FILLED` + `CANCELLED`) and remaining quantities never become negative.
- **Multiple Concurrent Crossing Orders**: 20 simultaneous threads (10 buyers and 10 sellers) at crossing prices to verify order book exhaustion with zero phantom or orphan records.

### Compile All Code & Tests

```bash
./mvnw clean test-compile
```

---

## 14. Verification & Testing Summary

All core matching algorithms, state transitions, and concurrency guarantees are covered by unit and multi-threaded test suites:

### Test Execution Summary

| Test Suite | Tests Run | Result | Key Invariants Verified |
|---|:---:|:---:|---|
| `MatchingEngineTest` | 10 | **PASS** | Price-time priority, FIFO ordering, full & partial fills, resting orders |
| `OrderServiceConcurrencyTest` | 15 (Repeated) | **PASS** | 0 negative quantities, 0 invalid transitions, 0 phantom trades |
| `GlobalExceptionHandler` | Verified | **PASS** | 400 Bad Request on invalid input, 404 on missing ID, 409 on conflict |

### Concurrency Invariant Guarantees Verified:
- **No Hybrid States:** When matching and cancellation race simultaneously, orders conclude as either `FILLED` or `CANCELLED`, never both.
- **Quantity Conservation:** Total filled + remaining quantity strictly equals original submitted quantity.
- **Order Book Clearing:** 20 concurrent crossing orders (10 BUYs vs 10 SELLs) exhaust the book completely with 0 orphaned records.

---

## 15. Known Limitations

| Limitation | Explanation |
|---|---|
| Single JVM only | ReentrantLock only works within one process. Horizontal scaling requires distributed locking. |
| No real authentication | userId is a plain number; anyone can submit orders on behalf of any user. |
| No balance validation | Users can place any quantity regardless of actual funds. |
| No market orders | Only limit orders supported. |
| Coarse-grained lock | Entire order book is locked per operation. |
| Single instrument | Only ACME supported. |
| No order expiry | Orders rest indefinitely (no IOC, FOK, GTD). |
| In-memory book lost on crash | Recovery runs on restart but a mid-transaction crash may cause a brief inconsistency. |

---

## 16. Future Improvements

| Improvement | What It Solves |
|---|---|
| Per-instrument ReentrantLock | Allows multiple instruments to match concurrently |
| LMAX Disruptor / event-loop | Eliminates locking overhead at high throughput |
| Kafka event log | Deterministic book rebuilding, event sourcing |
| JWT authentication | Only order owner can cancel their orders |
| Balance and position service | Prevents orders users cannot honour |
| IOC / FOK order types | Fills or kills immediately without resting |
| WebSocket order book feed | Real-time book updates to subscribers |
| Multi-instrument support | Per-symbol order books |
| Prometheus + Grafana | Observability: match latency, queue depth, fill rate |
| Testcontainers for CI | Deterministic integration tests in any environment |
| Tick size validation | Enforces minimum price increments |

---

## Scope Commitment

> This project will not add any feature outside the "Included" table in Section 3 during the initial build. Any future improvements will be addressed after v1.0.0 is shipped, documented, and tested.

---

*Built as a portfolio project demonstrating limit-order book design, concurrency, and Spring Boot architecture.*
