# Smart Commute & Route Intelligence System

A multimodal journey planner: given a source, destination, time constraint and personal
constraints (budget, max walking distance, transfers, preferred modes), it computes and **ranks**
realistic multimodal routes (walk / bus / metro / cab and combinations) over a city transport
network.

The engineering core is a **constrained k-shortest-paths search over a time-dependent, mode-expanded
transport graph** (Dijkstra / A* + Yen), wrapped in a clean Spring Boot backend with provider
abstraction, Redis caching, JWT auth, and a full test suite. It runs end-to-end **with no paid API
keys** using a bundled synthetic city dataset.

> This is a portfolio project built design-first. All design documents live in [`docs/`](docs/).
> Read them in order (01 → 12) to understand the reasoning behind every decision.

## Tech stack

- **Backend:** Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA
- **Data:** PostgreSQL (Flyway migrations), Redis (route cache + rate limiting)
- **Resilience:** Resilience4j circuit breaker around the optional ML ETA call
- **Testing:** JUnit 5, AssertJ, Testcontainers (real Postgres + Redis)
- **Ops:** Docker multi-stage build, docker-compose, GitHub Actions CI
- **Stretch (optional):** Python ML ETA service, React frontend, NL→constraints parser

## Architecture at a glance

Modular monolith with a strict dependency direction: `web → application → domain`, with external
systems behind ports/adapters. The **domain** (graph, routing algorithms, ranking) has zero
framework dependencies and is unit-tested in isolation. See [`docs/02-architecture.md`](docs/02-architecture.md).

## Prerequisites

- **JDK 21** (only requirement for building — Maven is provided via the wrapper `mvnw`)
- **Docker** (for `docker compose` and for the Testcontainers-based integration tests)

## Run it

### Option A — Docker Compose (everything)

```bash
cp .env.example .env
# edit .env and set a JWT_SECRET of at least 32 characters
docker compose up --build
```

Backend comes up on http://localhost:8080. Health: http://localhost:8080/actuator/health

### Option B — Run the backend locally

Start Postgres + Redis (e.g. via `docker compose up postgres redis`), then:

```bash
cd backend
./mvnw spring-boot:run           # Linux/macOS
mvnw.cmd spring-boot:run         # Windows
```

## Try the API

```bash
# 1. Register
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"S3curePass!","displayName":"Asha"}'

# 2. Login (grab the token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"S3curePass!"}' | jq -r .token)

# 3. Plan a route
curl -s -X POST http://localhost:8080/api/v1/routes/plan \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"Central","destination":"TechPark","departAt":"08:30",
       "maxWalkingMeters":800,"objective":"BALANCED","k":3}'
```

Known stop names in the sample city: `Central, Park, Market, University, TechPark, Riverside,
Airport, Stadium`.

## Build & test

```bash
cd backend
./mvnw -B verify        # compiles, runs unit tests + Testcontainers integration tests
```

Integration tests require Docker to be running (they start real Postgres and Redis containers).

## Documentation map

| Doc | Contents |
|---|---|
| [01-requirements](docs/01-requirements.md) | Problem, users, functional & non-functional requirements |
| [02-architecture](docs/02-architecture.md) | C4 diagrams, modules, caching, providers, auth |
| [03-hld](docs/03-hld.md) / [04-lld](docs/04-lld.md) | High- and low-level design, classes, patterns |
| [05-database](docs/05-database-design.md) / [06-api](docs/06-api-design.md) | Schema and REST contract |
| [07-algorithms](docs/07-algorithms.md) | Graph model, Dijkstra/A*/Yen, complexity, trade-offs |
| [08-ai-ml](docs/08-ai-ml-design.md) | ETA prediction, fallback strategy, NLP guardrails |
| [09-development-plan](docs/09-development-plan.md) | Milestones and task breakdown |
| [10-interview-preparation](docs/10-interview-preparation.md) | How to explain and defend the project |
| [11-resume](docs/11-resume.md) | Honest resume bullets |
| [12-validation-report](docs/12-validation-report.md) | What was verified and how |
| [learning/](docs/learning/) | Per-module deep-dive learning docs + decision log |

## Scope

This is the **Focused Core**: backend + routing engine + providers + persistence + caching + auth +
tests + Docker/CI. ML service, frontend, and NL parsing are designed and stubbed as **stretch
goals**. Data is **synthetic and clearly labelled** — no fabricated scale or metrics.
