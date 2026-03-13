# Loopy Backend — Reading Guide

> **How to read this Spring Boot codebase in a logical order.**
> 68 Java source files organized into 7 modules.

---

## Architecture at a Glance

```
HTTP Request
  │
  ▼
SecurityConfig (filter chain)
  │
  ▼
JwtAuthenticationFilter (extract + validate JWT)
  │
  ▼
Controller (parse request, call service)
  │
  ▼
Service (business logic)
  │
  ▼
Repository (Spring Data JPA → PostgreSQL)
  │
  ▼
Entity (DB row ↔ Java object)
```

Every resource has an explicit `userId` FK — multi-tenant by design.

---

## Data Model

```
User
├── RefreshToken          (auth sessions)
├── Topic                 (organizational folder)
│   └── Concept           (a thing to learn)
│       ├── Card          (flashcard — SM2 or FSRS scheduling)
│       │   └── ReviewLog (immutable review record)
│       └── TeachBack     (immutable teach-back session)
```

---

## Reading Order

### Phase 1 — Bootstrap & Config

| # | File | What you'll learn |
|---|------|-------------------|
| 1 | `LoopyApplication.java` | Entry point. `@SpringBootApplication` + `@EnableJpaAuditing` |
| 2 | `application.yml` + `application-dev.yml` | Jackson settings, JPA config, Flyway, JWT/Anthropic properties |
| 3 | `pom.xml` | All dependencies: Spring Boot starters, JJWT, PostgreSQL, Flyway |

### Phase 2 — Security & Authentication

Read these to understand how every request is authenticated.

| # | File | What you'll learn |
|---|------|-------------------|
| 4 | `config/AppConfig.java` | Bean wiring: UserDetailsService, BCryptPasswordEncoder, AuthenticationManager |
| 5 | `config/SecurityConfig.java` | Filter chain: CORS, CSRF disabled, stateless sessions, endpoint rules |
| 6 | `auth/entity/User.java` | Implements `UserDetails`. Fields: UUID id, email, passwordHash, role |
| 7 | `auth/entity/Role.java` | Enum: `USER`, `ADMIN` |
| 8 | `auth/entity/RefreshToken.java` | Opaque UUID token with expiry + revocation tracking |
| 9 | `auth/service/JwtService.java` | HMAC-SHA256 JWT generation/validation, 15-min expiry |
| 10 | `auth/filter/JwtAuthenticationFilter.java` | Per-request: extract Bearer token → validate → set SecurityContext |
| 11 | `auth/service/AuthService.java` | Register, login, refresh (token rotation), logout |
| 12 | `auth/controller/AuthController.java` | Endpoints: `/api/auth/register`, `/login`, `/refresh`, `/logout`, `/me` |
| 13 | `auth/dto/*` | Request/response shapes: RegisterRequest, LoginRequest, TokenResponse, etc. |

### Phase 3 — Core CRUD (Topics → Concepts → Cards)

The hierarchy: a user has topics, each topic has concepts, each concept has cards.

| # | File | What you'll learn |
|---|------|-------------------|
| 14 | `topic/entity/Topic.java` | Fields: name, description, colorHex. FK to userId |
| 15 | `topic/repository/TopicRepository.java` | Queries: findByUserId, findByIdAndUserId |
| 16 | `topic/service/TopicService.java` | CRUD + card count per topic |
| 17 | `topic/controller/TopicController.java` | REST endpoints for `/api/topics` |
| 18 | `card/entity/Concept.java` | Fields: title, notes, referenceExplanation, status (enum) |
| 19 | `card/entity/ConceptStatus.java` | Enum: LEARNING → REVIEW → MASTERED ↔ TEACH_BACK_REQUIRED |
| 20 | `card/service/ConceptService.java` | CRUD for concepts |
| 21 | `card/controller/ConceptController.java` | REST endpoints for `/api/concepts` |
| 22 | `card/entity/Card.java` | The core learning unit. Dual scheduling fields (SM2 + FSRS) |
| 23 | `card/entity/CardType.java` | Enum: STANDARD, CODE_OUTPUT, SPOT_THE_BUG, FILL_BLANK, EXPLAIN_WHEN, COMPARE |
| 24 | `card/service/CardService.java` | CRUD + algorithm switching |
| 25 | `card/controller/CardController.java` | REST endpoints for `/api/cards` |

### Phase 4 — Spaced Repetition & Reviews

The heart of the learning system.

| # | File | What you'll learn |
|---|------|-------------------|
| 26 | `review/service/SM2Service.java` | Pure SM-2 algorithm: easeFactor, interval, repetition count |
| 27 | `review/service/SM2Result.java` | Result record from SM-2 calculation |
| 28 | `review/service/FSRSService.java` | FSRS-5 algorithm: stability, difficulty, forgetting curves |
| 29 | `review/service/SchedulingResult.java` | Unified result from either algorithm |
| 30 | `review/service/SchedulingAlgorithm.java` | Enum: SM2, FSRS |
| 31 | `review/entity/ReviewLog.java` | Immutable record: cardId, rating (0-5), confidence (1-5), responseTimeMs |
| 32 | `review/service/ReviewService.java` | getDueCards, submitReview (routes to SM2 or FSRS), practice mode |
| 33 | `review/controller/ReviewController.java` | `/api/reviews/today`, `/api/reviews/practice`, POST `/api/reviews/{cardId}` |

### Phase 5 — Stats & Analytics

| # | File | What you'll learn |
|---|------|-------------------|
| 34 | `review/service/StatsService.java` | Overview stats, streaks, heatmap (365 days), fragile card detection |
| 35 | `review/controller/StatsController.java` | `/api/stats/overview`, `/heatmap`, `/fragile` |
| 36 | `review/dto/*` | StatsOverview, HeatmapEntry, FragileCard shapes |

### Phase 6 — Teach-Back System

Concepts are escalated here when the user fails cards repeatedly.

| # | File | What you'll learn |
|---|------|-------------------|
| 37 | `teachback/service/EscalationService.java` | Auto-escalates concepts after 3+ failures or low-confidence reviews |
| 38 | `teachback/entity/TeachBack.java` | Immutable record: userExplanation, selfRating, gapsFound |
| 39 | `teachback/service/TeachBackService.java` | Get pending, submit session, auto-promote back to REVIEW if selfRating ≥ 4 |
| 40 | `teachback/controller/TeachBackController.java` | `/api/teach-back/pending`, POST, `/history` |

### Phase 7 — AI Integration (Claude API)

| # | File | What you'll learn |
|---|------|-------------------|
| 41 | `ai/service/AnthropicClient.java` | HTTP client for Claude Messages API |
| 42 | `ai/service/AIService.java` | Card generation (Haiku) + teach-back evaluation (Sonnet) |
| 43 | `ai/controller/AIController.java` | `/api/ai/status`, `/generate-cards`, `/evaluate-teach-back` |
| 44 | `ai/dto/*` | GenerateCardsRequest, TeachBackEvaluation, etc. |

### Phase 8 — Cross-Cutting Concerns

| # | File | What you'll learn |
|---|------|-------------------|
| 45 | `config/GlobalExceptionHandler.java` | Consistent JSON error responses for all exceptions |
| 46 | `config/ResourceNotFoundException.java` | Custom 404 exception |
| 47 | `config/CorsConfig.java` | Allows frontend at localhost:5173 |

---

## Key Patterns to Notice

| Pattern | Where |
|---------|-------|
| **Layered arch** | Controller → Service → Repository → Entity (every module) |
| **User isolation** | Every query includes `userId` — no cross-user data leaks |
| **Immutable logs** | `ReviewLog` and `TeachBack` are write-once, never updated |
| **Algorithm flexibility** | Cards store both SM2 and FSRS fields; `schedulingAlgorithm` enum picks which to use |
| **Service composition** | `ReviewService` delegates to `SM2Service` or `FSRSService` |
| **Escalation pipeline** | `EscalationService` monitors reviews → auto-triggers teach-back |
| **JWT + refresh rotation** | Stateless access tokens + database-backed refresh tokens with rotation |

---

## Quick Reference: All Endpoints

| Method | Path | Controller |
|--------|------|------------|
| POST | `/api/auth/register` | AuthController |
| POST | `/api/auth/login` | AuthController |
| POST | `/api/auth/refresh` | AuthController |
| POST | `/api/auth/logout` | AuthController |
| GET | `/api/auth/me` | AuthController |
| GET/POST/PUT/DELETE | `/api/topics` | TopicController |
| GET/POST/PUT/DELETE | `/api/concepts` | ConceptController |
| GET/POST/PUT/DELETE | `/api/cards` | CardController |
| GET | `/api/reviews/today` | ReviewController |
| GET | `/api/reviews/practice` | ReviewController |
| POST | `/api/reviews/{cardId}` | ReviewController |
| GET | `/api/stats/overview` | StatsController |
| GET | `/api/stats/heatmap` | StatsController |
| GET | `/api/stats/fragile` | StatsController |
| GET | `/api/teach-back/pending` | TeachBackController |
| POST | `/api/teach-back` | TeachBackController |
| GET | `/api/teach-back/history` | TeachBackController |
| GET | `/api/ai/status` | AIController |
| POST | `/api/ai/generate-cards` | AIController |
| POST | `/api/ai/evaluate-teach-back` | AIController |
