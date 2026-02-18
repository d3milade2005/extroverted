# Extroverted

A location-aware social platform for discovering and sharing local events. Tell it where you are, tell it what you like — it finds what's happening around you.

Built with Java 17, Spring Boot 3, PostGIS, Keycloak, and Redis.

---

## What it does

Most event discovery apps show you everything. Extroverted narrows it down to what's actually relevant — events near you, in categories you care about, that people you know are attending. The further away an event is, or the less it matches your interests, the lower it ranks. New users get trending and hyper-local events by default until the platform learns their preferences.

---

## Services

The platform is split into six services, each responsible for a single domain.

**API Gateway** — the only public-facing entry point. All client traffic routes through here. Handles rate limiting (token bucket algorithm, backed by Redis) and global CORS policy before any request reaches a downstream service.

**Auth Service** — identity and access management via Keycloak. Handles OAuth2 login, JWT issuance, and role-based access control (USER / HOST / ADMIN). No service manages its own auth logic — they all verify tokens against Keycloak's JWK Set.

**User Service** — owns everything about a user: profile, interest tags, style preferences, social connections, and location (stored as a PostGIS point type for spatial queries).

**Event Service** — the core of the platform. Creates and manages events, and powers location-based discovery using PostGIS geography types. Queries account for earth curvature — this is not simple lat/long comparison.

**Recommendation Service** — scores every event against the current user and returns a ranked list. Uses a weighted algorithm across five signals: proximity, interest alignment, social proof, recency, and a cold-start strategy for new accounts. Results are cached in Redis to avoid recomputing on every request.

**Admin Service** — back-office tooling for content moderation and platform management.

---

## How recommendations work

Every event gets a score from 0.0 to 1.0 for each user. The score is a weighted combination of:

- **Proximity** — exponential decay by distance. An event 500m away scores far higher than one 5km away
- **Interest match** — tag overlap between the user's preference profile and the event's categories
- **Social proof** — events with high interaction rates and friends attending rank higher
- **Recency** — upcoming events are weighted over distant future ones
- **Cold start** — no history? The algorithm defaults to what's trending nearby until it learns more

---

## How location search works

Extroverted uses PostGIS geography types, not raw coordinates. This matters because the earth is curved — a naive lat/long query gets less accurate the further from the equator you are. PostGIS handles this correctly.

Queries use `ST_DWithin` (indexed with GiST spatial indexes) for fast radius lookups and `ST_Distance` at the database layer for distance-ranked results — so the application never has to sort by distance itself.

---

## Tech stack

| | Technology | |
|--|------------|--|
| Language | Java 17 | Records, pattern matching, LTS stability |
| Framework | Spring Boot 3.5.7 | Spring Cloud Gateway, native observability |
| Database | PostgreSQL 15 | ACID, JSONB, reliable |
| Spatial | PostGIS 3.3 | Earth-accurate geo queries, GiST indexes |
| Cache | Redis 7 | Rate limiting counters, hot recommendation sets |
| Auth | Keycloak 23 | OAuth2, OIDC, RBAC — production-grade out of the box |
| Infrastructure | Docker Compose | One command, entire platform |

---

## Running locally

You need Docker and Docker Compose. That's it.

```bash
git clone https://github.com/your-org/extroverted.git
cd extroverted
docker compose up -d --build
```

Give it about 30 seconds for Keycloak and Postgres to finish initializing.

| | URL |
|--|-----|
| API Gateway | `http://localhost:8090` |
| Keycloak Admin | `http://localhost:8080` |
| Health check | `http://localhost:8090/actuator/health` |

Keycloak default credentials: `admin / admin`

---

## Project layout

```
extroverted/
├── api-gateway-service/
├── auth-service/
├── user-service/
├── event-service/
├── recommendation-service/
├── admin-service/
├── docker-compose.yml
├── init-databases.sql
└── .env
```