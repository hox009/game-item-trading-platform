# Game Item Trading Platform

A distributed marketplace for trading game virtual items (skins, accounts, top-up
cards) built as **Spring Cloud microservices**, with an **AI trading assistant**
(LangChain + RAG + function calling) as an independent Python service. The platform
supports catalog search, seller listings, inventory control, wallet payments,
order processing, notifications, and AI-assisted pricing and ordering.

## Architecture

```
                    ┌─────────────────────────────┐
   Web / k6 ───────▶│  API Gateway (8080)          │  JWT auth, routing,
   AI chat ────────▶│  Spring Cloud Gateway        │  Redis rate limiting
                    └──────────────┬──────────────┘
                                   │  (Nacos service discovery)
   ┌───────────────┬───────────────┼───────────────┬───────────────┐
   ▼               ▼               ▼               ▼               ▼
 user(8081)     item(8082)    inventory(8083)  order(8085)    payment(8084)
 JWT/BCrypt     SPU/SKU +     Redisson lock    state machine   wallet/ledger
                Redis cache   (anti-oversell)   Feign+Kafka     escrow charge
                                   ▲               │
                                   │  Kafka        │  Kafka events
                                   └───── order.paid / order.cancelled
                                                   │  RabbitMQ
                                            notification(8086)
                                                   │
                              ai-assistant(8087, Python/LangChain)
                              function-calls business APIs via gateway
```

## Modules

| Module | Port | Responsibility | Key tech |
|--------|------|----------------|----------|
| `common` | – | ApiResponse, ResultCode, BusinessException, JwtUtil, events | – |
| `common-web` | – | Global exception handling, gateway headers | – |
| `gateway` | 8080 | Routing, JWT auth, rate limiting | Spring Cloud Gateway, Redis |
| `user-service` | 8081 | Accounts, login, JWT issuance | JPA, BCrypt |
| `item-service` | 8082 | Catalog (SPU/SKU), search | JPA, Redis cache |
| `inventory-service` | 8083 | Stock freeze/deduct/release | Redisson lock, Kafka |
| `payment-service` | 8084 | Wallet, escrow charge, ledger | JPA |
| `order-service` | 8085 | Order orchestration & state machine | Feign, Kafka, RabbitMQ |
| `notification-service` | 8086 | Station inbox | RabbitMQ |
| `ai-assistant-service` | 8087 | AI trading assistant | Python, LangChain, RAG |
| `frontend` | 3001 | Web UI (catalog, orders, AI chat) | React, Vite, Tailwind |

## Prerequisites
- **Docker only** to run the whole stack (each service builds inside its container).
- For local development without Docker: JDK 17 (Java) and Node 20+ (frontend).
  No global Maven needed — use the bundled wrapper `./mvnw`.

## Build & test (local)
```bash
./mvnw -B verify                   # build + run all unit tests (Java), no global Maven needed
cd ai-assistant-service && pytest  # AI assistant offline tests
```

## Run the whole stack (Docker)
Everything (infra + services + frontend) comes up with one command. The Java
services use multi-stage Dockerfiles, so **you do not need Maven or the JDK** —
only Docker Desktop.
```powershell
./scripts/run-stack.ps1            # Windows: build + start; add -Seed for 100K SKUs
```
```bash
./scripts/run-stack.sh             # macOS/Linux; add --seed for 100K SKUs
```
or directly:
```bash
docker compose up -d --build
```
Stop with `./scripts/stop-stack.ps1` (or `docker compose down`; add `-v` to wipe data).

### Ports
| URL | What |
|-----|------|
| http://localhost:3001 | Frontend (web UI) |
| http://localhost:8080 | API gateway |
| http://localhost:8087 | AI assistant |
| http://localhost:9090 | Prometheus |
| http://localhost:3000 | Grafana (admin/admin) |
| http://localhost:15672 | RabbitMQ console (guest/guest) |
| http://localhost:8848/nacos | Nacos console |

> First build compiles all services in-container and downloads images, so it
> takes a few minutes. Subsequent runs are cached and fast.

## Frontend (standalone dev)
A React + Vite + Tailwind SPA (catalog browsing/search, place & pay orders,
seller listings, wallet, notifications, plus a floating AI assistant chat).
Served at http://localhost:3001 in the full stack; for hot-reload dev:
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxies /api -> :8080, /api/assistant -> :8087)
```

## Preview the UI without Docker
A zero-dependency mock backend serves the full API contract in-memory so the UI
can be demoed with just Node:
```bash
node mock-server/server.js   # :8080 + :8087 with seeded catalog
cd frontend && npm run dev    # then open http://localhost:5173
```

## Seed 100K+ SKUs
```bash
python scripts/seed/generate_seed.py --skus 100000        # -> scripts/db/seed-items.sql
docker compose exec -T mysql mysql -uroot -proot < scripts/db/seed-items.sql
```

## Observability
Every Java service exposes Prometheus metrics at `/actuator/prometheus`.
The stack ships Prometheus (http://localhost:9090) and Grafana
(http://localhost:3000, admin/admin) pre-wired via `monitoring/`.

## Quick smoke test
```bash
# register + login
curl -X POST localhost:8080/api/users/register -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"password\":\"secret123\",\"role\":\"SELLER\"}"
curl -X POST localhost:8080/api/users/login    -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"password\":\"secret123\"}"

# ask the assistant (offline fallback works without an OpenAI key)
curl -X POST localhost:8087/api/assistant/chat -H "Content-Type: application/json" -d "{\"message\":\"What is a fair price for a CS2 knife skin?\"}"
```
