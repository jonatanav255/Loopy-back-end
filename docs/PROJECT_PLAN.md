# COMPLETE PROJECT PLAN — Hybrid Learning Engine

**Spaced Repetition + Teach-Back + Voice + Smart Intake**
From git init to production — 10 phases, every feature

- **Author:** Jonatan
- **Stack:** Spring Boot 3 · Java 21 · React · TypeScript · PostgreSQL
- **Date:** March 2026
- **Version:** 2.0 — Complete Edition

Items marked [OPTIONAL] are ideas worth considering but not required. Build them if they solve a real problem you're experiencing.

---

## Project Overview

A personal learning engine that combines spaced repetition, active recall, AI-powered teach-back evaluation, voice interaction, and smart content intake into a self-reinforcing cycle. You feed it concepts, it quizzes you on a schedule, and when you struggle, it escalates you from simple recall to active teaching — exposing and filling knowledge gaps automatically.

### The Core Loop

Intake → Card Generation → Spaced Review (SM-2/FSRS) → Confidence Signal → Escalation → Teach-Back → Gap Detection → New Cards → Repeat

### Principles

- **Ship incrementally.** Every phase delivers a usable product. No waiting months for a payoff.
- **Zero cost first.** Phases 1–4 have zero API costs. Prove the loop works before spending money.
- **Use it daily.** If you won't use it, don't build it. Each phase must earn its place in your routine.
- **Learn by building.** JWT, Spring Security, React patterns, system design — the project itself is a learning vehicle.

---

## PHASE 1 — Foundation & Auth

**Timeline:** 1–2 weeks | **Cost:** $0/month | **Status:** COMPLETE

Get the project running: Spring Boot backend, PostgreSQL, JWT authentication, and a React shell with protected routes. This is the skeleton everything else hangs on.

### Backend Scaffolding

| Task | Details | Time |
|------|---------|------|
| Spring Boot project | Spring Initializr: Web, Security, JPA, PostgreSQL, Validation, Flyway | 30 min |
| PostgreSQL in Docker | docker-compose.yml with Postgres container, volume for persistence | 30 min |
| Project structure | Package by feature: auth/, card/, review/, concept/, config/, common/ | 30 min |
| Base entity | BaseEntity with UUID id, createdAt, updatedAt via JPA auditing (@EntityListeners) | 1 hr |
| Global error handling | @ControllerAdvice, consistent ErrorResponse DTO, proper HTTP status codes | 1 hr |
| Dev profile config | application-dev.yml with local DB, debug logging, CORS for localhost:5173 | 30 min |

### JWT Authentication

| Task | Details | Time |
|------|---------|------|
| User entity | email (unique), passwordHash (BCrypt), role (USER/ADMIN), enabled flag, timestamps | 1 hr |
| Registration endpoint | POST /api/auth/register — validate input, hash password, save user, return tokens | 2 hrs |
| JWT service | Generate/validate tokens using JJWT. Access token (15 min), refresh token (30 days) | 3 hrs |
| Login endpoint | POST /api/auth/login — verify credentials, return access token body + refresh httpOnly cookie | 2 hrs |
| JWT auth filter | OncePerRequestFilter: extract Bearer token, validate, set SecurityContext | 3 hrs |
| Refresh endpoint | POST /api/auth/refresh — validate refresh token cookie, issue new access token | 2 hrs |
| Logout endpoint | POST /api/auth/logout — clear refresh token cookie, optionally blacklist token | 1 hr |

### React Frontend Shell

| Task | Details | Time |
|------|---------|------|
| Vite + React + TS | Initialize project, install Tailwind CSS, setup folder structure | 1 hr |
| Auth context | React context storing access token in memory (NOT localStorage), user state | 2 hrs |
| Login/Register pages | Forms with validation, error display, redirect on success | 2 hrs |
| Axios interceptor | Auto-attach Bearer token, auto-refresh on 401, redirect to login on refresh failure | 2 hrs |
| Protected routes | Route guard component, redirect unauthenticated users to /login | 1 hr |
| App layout shell | Sidebar nav, top bar with user info, main content area, responsive | 2 hrs |

**MILESTONE:** Register, login, see a protected dashboard. Tokens refresh silently. Logout works.

---

## PHASE 2 — Core Engine: Cards & SM-2 Algorithm

**Timeline:** 2–3 weeks | **Cost:** $0/month

The MVP. Create flashcards manually, review them daily with SM-2 scheduling. This is the version you start using every single day.

### Data Model

| Task | Details | Time |
|------|---------|------|
| Topic entity | name, description, colorHex. User has many Topics (Java, Spring, Design Patterns...) | 1 hr |
| Concept entity | Belongs to Topic. title, notes, masteryScore (calculated), status enum | 1 hr |
| Card entity | Belongs to Concept. front, back, cardType enum, hint (optional), sourceUrl (optional) | 1.5 hrs |
| Card types enum | STANDARD, CODE_OUTPUT, SPOT_THE_BUG, FILL_BLANK, EXPLAIN_WHEN, COMPARE | 30 min |
| SM-2 fields on Card | repetitionCount, easeFactor (2.5), intervalDays, nextReviewDate, lastReviewDate | 1 hr |
| ReviewLog entity | cardId, rating (0–5), reviewedAt, responseTimeMs. Immutable audit trail | 1 hr |
| Flyway migrations | V1–V6 scripts with indexes on nextReviewDate, userId, topicId | 1 hr |

### SM-2 Algorithm

Pure Java implementation. No dependencies. The entire algorithm in one service class.

- **Rating < 3 (fail):** Reset repetitionCount to 0, interval to 1 day. Ease factor decreases.
- **Rating 3 (hard):** Advance but conservatively. Ease factor holds or slightly decreases.
- **Rating 4 (good):** Standard progression. new_interval = old_interval × easeFactor.
- **Rating 5 (easy):** Ease factor increases, interval grows aggressively.
- **Ease factor floor:** Never below 1.3. Prevents cards from getting permanently stuck.
- **First review:** 1 day. Second: 6 days. Then: interval × easeFactor from there.

**Formula:** `EF' = EF + (0.1 - (5 - q) × (0.08 + (5 - q) × 0.02))` where q = rating

| Task | Details | Time |
|------|---------|------|
| SM2Service | Pure function: (currentState, rating) → newState. Stateless, fully unit-testable | 2 hrs |
| ReviewService | Orchestrates: validate card ownership, call SM2Service, save card, create ReviewLog | 2 hrs |
| Daily queue endpoint | GET /api/reviews/today — cards where nextReviewDate ≤ today, priority-ordered | 1 hr |
| Submit review | POST /api/reviews/{cardId} body: {rating} — update card, log review, return next card | 1 hr |
| Unit tests | SM-2 edge cases: first review, repeated fails, ease floor, interval math, date calc | 2 hrs |

### Card Management UI

| Task | Details | Time |
|------|---------|------|
| Topics list page | Grid/list of topics with card counts, due counts, mastery percentage bar | 2 hrs |
| Card creation form | Front/back inputs, topic/concept selectors, card type dropdown, optional hint | 2 hrs |
| Card type renderers | Different display for code cards (syntax highlight), fill-blank, compare, etc. | 3 hrs |
| Review session UI | Full-screen mode: show front → tap reveal → show back → rate 0–5. Progress bar | 3 hrs |
| Keyboard shortcuts | Space = reveal, 1–6 = rate, Enter = next. Power user speed | 1 hr |
| Session summary | After review: cards done, accuracy, time spent, weakest topic | 1 hr |

**MILESTONE:** Creating cards and doing daily reviews. SM-2 schedules them. You use this every day.

---

## PHASE 3 — Confidence Signal & Analytics Dashboard

**Timeline:** 1–2 weeks | **Cost:** $0/month

Add a confidence dimension to every review. Build a dashboard that reveals your learning patterns, weak spots, and progress over time.

| Task | Details | Time |
|------|---------|------|
| Confidence rating | After correctness rating, rate confidence 1–3 (guessing / unsure / confident). Stored in ReviewLog | 2 hrs |
| Fragile knowledge flag | Correct + low confidence = flagged. Service identifies these and adjusts scheduling | 2 hrs |
| Stats API | Endpoints: cards today/week/month, accuracy by topic, streak, mastery distribution | 3 hrs |
| Dashboard — overview | Today's progress, streak counter, cards due, overall mastery score | 2 hrs |
| Dashboard — charts | Recharts: accuracy over time, reviews per day, mastery by topic (bar chart) | 3 hrs |
| Dashboard — heatmap | GitHub-style contribution heatmap showing daily review activity | 2 hrs |
| Weakest topics view | Ranked list of topics/concepts by failure rate and low confidence frequency | 1 hr |

**MILESTONE:** Clear data on what you know and don't. Fragile knowledge is surfaced. Streak motivates daily use.

---

## PHASE 4 — Teach-Back: Text Only, Self-Evaluated

**Timeline:** 1–2 weeks | **Cost:** $0/month

When a card crosses the escalation threshold (3+ failures or persistent low confidence), the app challenges you to explain the concept. You evaluate yourself against a reference answer. The feedback loop works end-to-end with zero API costs.

| Task | Details | Time |
|------|---------|------|
| Escalation service | Detects cards crossing threshold, sets concept status to TEACH_BACK_REQUIRED | 2 hrs |
| TeachBack entity | conceptId, userExplanation, selfRating (1–5), gapsFound (JSON array), timestamp | 1 hr |
| Reference answers | Optional field on Concept: referenceExplanation. Shown after user writes theirs | 1 hr |
| Teach-back UI | Full-screen writing mode: concept prompt at top, large textarea, timer (optional) | 3 hrs |
| Self-eval screen | Side-by-side: your explanation vs reference. Rate yourself 1–5 on accuracy/completeness | 2 hrs |
| Gap marking | Checkboxes for common gap types. User marks what they missed | 1 hr |
| Card creation from gaps | Each marked gap offers "Create card" → pre-filled card form with gap as the question | 2 hrs |
| Mastery score update | Teach-back results feed back into concept mastery calculation | 1 hr |

**MILESTONE:** Complete feedback loop: weak cards → teach-back → gaps found → new cards generated. All free.

---

## PHASE 5 — AI Integration: Claude API

**Timeline:** 2–3 weeks | **Cost:** $5–15/month

Now that you've validated the core loop manually, add AI to supercharge card generation and teach-back evaluation. Use Haiku for simple tasks, Sonnet for evaluation.

### AI Card Generation

| Task | Details | Time |
|------|---------|------|
| Anthropic API client | Spring service wrapping HTTP client. Configurable model per task. Error handling + retry | 2 hrs |
| System prompts | Carefully crafted prompts for card generation. Output: structured JSON with front/back/type | 3 hrs |
| Paste & Generate UI | Textarea to paste content → "Generate Cards" button → preview/edit/save flow | 3 hrs |
| Multi-difficulty generation | From one input, create beginner/intermediate/advanced cards. User picks which to keep | 2 hrs |
| Prompt caching | Use Anthropic prompt caching for system prompt. Cuts input costs ~90% on repeated calls | 1 hr |
| Batch generation | Queue multiple inputs, process with Batch API (50% discount) overnight | 2 hrs |

### AI Teach-Back Evaluation

| Task | Details | Time |
|------|---------|------|
| Evaluation prompt | Send concept + user explanation → Sonnet scores clarity/accuracy/completeness (1–10 each) | 3 hrs |
| Follow-up questions | Claude returns 2–3 probing questions based on gaps detected in explanation | 2 hrs |
| Auto gap detection | AI identifies specific missing sub-concepts, suggests topics for new cards | 2 hrs |
| One-click card creation | From AI-detected gaps → AI writes card front/back → user confirms → saved | 2 hrs |
| Cost tracking dashboard | Log tokens per request, show running monthly cost, model usage breakdown in settings | 2 hrs |

**MILESTONE:** Cards auto-generate from pasted content. Teach-back gets real AI evaluation. Feedback loop fully automated.

---

## PHASE 6 — Voice Integration

**Timeline:** 2–3 weeks | **Cost:** +$3–8/month

Add voice input for teach-back (speak your explanation instead of typing) and voice output for AI responses (the app talks back). Transforms the experience from text chat to spoken conversation.

### Speech-to-Text (Voice Input)

| Task | Details | Time |
|------|---------|------|
| Audio recording component | React component using MediaRecorder API. Hold-to-talk button with visual waveform | 3 hrs |
| Audio upload endpoint | Backend receives audio file (WebM/MP3), validates size/duration limits | 1 hr |
| STT service | Integrate GPT-4o Mini Transcribe ($0.003/min). Send audio, receive transcript | 2 hrs |
| Voice teach-back flow | Record → transcribe → display text for review → send to Claude for eval | 2 hrs |
| Edit before submit | User can review/edit transcript before evaluation. Catches transcription errors | 1 hr |

### Text-to-Speech (Voice Output)

| Task | Details | Time |
|------|---------|------|
| TTS service | Integrate OpenAI TTS ($15/1M chars). Send Claude's eval text, receive audio stream | 2 hrs |
| Audio playback component | Custom player: play/pause, speed control (1x/1.25x/1.5x), progress bar | 2 hrs |
| Streaming playback | Start playing audio before full response is generated. Reduces perceived latency | 2 hrs |
| Voice selection | Let user pick from available TTS voices. Store preference | 1 hr |

### Conversational Mode

| Task | Details | Time |
|------|---------|------|
| Full voice loop | Speak → transcribe → evaluate → speak back → follow-up question → user responds → repeat | 4 hrs |
| Conversation history | Store multi-turn teach-back conversations. Track how understanding evolves across turns | 2 hrs |
| Auto-listen mode | After TTS finishes, automatically start recording for next user response | 1 hr |

**MILESTONE:** Speak your explanation out loud, hear AI feedback and follow-up questions. Full voice conversation loop.

---

## PHASE 7 — Smart Intake Pipeline

**Timeline:** 2–4 weeks | **Cost:** $0–5/month

Solve the intake problem. Make it effortless to feed the learning engine content from every source: browsing, coding, reading, conversations, and even your own mistakes.

### Browser Extension

| Task | Details | Time |
|------|---------|------|
| Chrome extension scaffold | Manifest V3, content script, popup UI, background service worker | 2 hrs |
| Highlight & send | Select text on any page → right-click → "Send to Learning Engine" with source URL | 3 hrs |
| Quick card creation | Popup lets you type front/back directly from the extension without opening the app | 2 hrs |
| Auth integration | Extension authenticates with your backend via stored JWT token | 1 hr |
| Intake queue badge | Extension icon shows count of items waiting in intake queue | 1 hr |

### Intake Queue System

| Task | Details | Time |
|------|---------|------|
| IntakeItem entity | rawContent, sourceUrl, sourceType enum (BROWSER/GITHUB/READING/MANUAL), status, timestamp | 1 hr |
| Intake queue page | List of pending items. For each: preview content, generate cards, or dismiss | 3 hrs |
| Bulk generate | Select multiple items → batch generate cards for all at once (uses Batch API discount) | 2 hrs |
| Auto-categorize | AI suggests which Topic each intake item belongs to based on content | 1 hr |

### GitHub Integration [OPTIONAL]

Watch your repos and auto-generate learning material from your actual work.

| Task | Details | Time |
|------|---------|------|
| GitHub webhook endpoint | Receive push/PR events. Extract new files, changed code, PR descriptions | 3 hrs |
| Code pattern detection | AI scans new code for patterns: new annotations, design patterns, unfamiliar APIs | 3 hrs |
| Auto-suggest cards | Generate suggested cards from detected patterns. User approves/dismisses in intake queue | 2 hrs |
| PR learning notes | When you open a PR, optionally generate cards from what you learned building the feature | 2 hrs |

### Reading Integration [OPTIONAL]

Import highlights from books and reading apps into the learning engine.

| Task | Details | Time |
|------|---------|------|
| Readwise API integration | Sync highlights from Kindle, Apple Books, web articles via Readwise API | 3 hrs |
| Highlight categorization | AI groups highlights by theme (leadership, decision-making, technical concepts) | 2 hrs |
| Cross-book connections | Surface patterns across different books: "This principle appeared in 3 biographies" | 2 hrs |

### Conversation Import [OPTIONAL]

Turn Claude conversations into learning material.

| Task | Details | Time |
|------|---------|------|
| Paste conversation | Paste a Claude conversation transcript → AI extracts key concepts | 2 hrs |
| Concept extraction | AI identifies distinct concepts discussed, generates cards for each | 2 hrs |
| Link to source | Cards reference the original conversation for future review context | 1 hr |

### Learning from Mistakes

When you fail a card or reveal gaps in teach-back, don't just create one replacement card — create a cluster of related cards.

| Task | Details | Time |
|------|---------|------|
| Gap cluster generation | One failure triggers AI to create 3–5 related cards covering connected sub-concepts | 3 hrs |
| Concept graph edges | Link related concepts so gaps in one surface prerequisites from another | 2 hrs |
| Prerequisite detection | If you fail an advanced card, check if the prerequisite concepts are solid first | 2 hrs |

**MILESTONE:** Content flows into the app from browsing, coding, reading, and mistakes. Intake is nearly frictionless.

---

## PHASE 8 — FSRS Algorithm & Advanced Scheduling

**Timeline:** 2–3 weeks | **Cost:** $0/month

Upgrade from SM-2 to FSRS (Free Spaced Repetition Scheduler). FSRS uses machine learning to model your personal forgetting patterns, scheduling reviews ~30% more efficiently than SM-2.

### FSRS Implementation

| Task | Details | Time |
|------|---------|------|
| Port FSRS to Java | Translate FSRS-5 reference implementation. Core model: Difficulty, Stability, Retrievability | 1 week |
| Personal model training | Use accumulated ReviewLog data to train FSRS parameters specific to your patterns | 3 days |
| A/B comparison mode | Run SM-2 and FSRS side by side on different topics. Compare review efficiency | 2 days |
| Full migration | Switch to FSRS as default once validated. Keep SM-2 as fallback option in settings | 1 day |

### Advanced Scheduling Features

| Task | Details | Time |
|------|---------|------|
| Context-aware scheduling | "What are you working on today?" → prioritize related cards for that morning's session | 2 hrs |
| Review format rotation | Same card shown differently: multiple choice, free text, code completion, one-sentence explain | 3 hrs |
| Load balancing | Spread reviews evenly across days. Prevent 50-card spikes after a weekend off | 2 hrs |
| Retention targets | Set desired retention rate (e.g. 90%). FSRS auto-adjusts intervals to hit target | 2 hrs |

**MILESTONE:** FSRS learns your personal forgetting patterns. Reviews are 30% more efficient than SM-2.

---

## PHASE 9 — Experience Polish & Advanced Features

**Timeline:** 3–4 weeks | **Cost:** Same

Features that make the app exceptional, not just functional. Each one is independent — build them in any order based on what you want most.

### Cross-Concept Intelligence

| Task | Details | Time |
|------|---------|------|
| Concept graph | Data model linking related concepts with relationship types (prerequisite, related, builds-on) | 3 hrs |
| AI connection surfacing | "Observer pattern relates to Spring Events you studied last week" — periodic AI analysis | 3 hrs |
| Graph visualization | Interactive D3 or force-graph showing your knowledge map with mastery colors | 4 hrs |

### Spaced Writing Prompts [OPTIONAL]

Periodically prompt you to write a short explanation of a topic. Writing forces synthesis in ways cards don't.

| Task | Details | Time |
|------|---------|------|
| Writing prompt service | Schedule prompts for concepts at high mastery — test if you can synthesize, not just recall | 2 hrs |
| Writing UI | Distraction-free editor with word count. Save as blog-post draft | 2 hrs |
| AI feedback on writing | Claude evaluates writing for depth, clarity, and technical accuracy | 2 hrs |
| Blog export | Export polished writing as Markdown for a portfolio blog or GitHub Pages | 1 hr |

### Daily Sprint Mode

| Task | Details | Time |
|------|---------|------|
| Sprint dashboard | Single view combining: today's review cards + one teach-back + one writing prompt | 3 hrs |
| 30-minute timer | Guided session: 15 min reviews, 10 min teach-back, 5 min writing. Configurable | 1 hr |
| Sprint summary | End-of-session report: cards done, concepts strengthened, gaps found, mastery deltas | 2 hrs |

### Spaced Prompts for New Topics [OPTIONAL]

The app proactively generates learning questions for topics you've tagged as "learning."

| Task | Details | Time |
|------|---------|------|
| Topic-based prompt generation | Tag a topic as "active learning." AI generates a new design/coding question every few days | 3 hrs |
| System design prompts | "How would you design a rate limiter?" User sketches answer, AI evaluates and generates cards | 3 hrs |

**MILESTONE:** Knowledge graph, writing prompts, daily sprints, and proactive learning. The app drives your growth.

---

## PHASE 10 — Deployment, DevOps & Mobile

**Timeline:** 1–2 weeks + ongoing | **Cost:** $10–30/month

Get the app live so you can use it from anywhere. Can start as early as Phase 2 and evolve alongside features.

### Containerization

| Task | Details | Time |
|------|---------|------|
| Backend Dockerfile | Multi-stage: build with Maven, run with JRE slim. Minimize image size | 1 hr |
| Frontend Dockerfile | Build React with Vite, serve with Nginx. Or bundle into Spring Boot static resources | 1 hr |
| Docker Compose | Full local stack: backend + frontend + PostgreSQL + pgAdmin (dev) | 1 hr |

### CI/CD & Cloud

| Task | Details | Time |
|------|---------|------|
| GitHub Actions pipeline | On push: run tests → build Docker images → push to ECR → deploy | 3 hrs |
| AWS deployment | ECS Fargate (or EC2 with Docker). RDS PostgreSQL. ALB for load balancing | 4 hrs |
| Environment configs | Separate dev/staging/prod Spring profiles. Secrets in AWS Secrets Manager | 2 hrs |
| Domain + HTTPS | Custom domain via Route 53, SSL via ACM, enforce HTTPS everywhere | 1 hr |
| Monitoring & alerts | CloudWatch logs, health checks, error rate alerts, basic uptime monitoring | 2 hrs |

### Mobile & PWA

| Task | Details | Time |
|------|---------|------|
| PWA configuration | Service worker, web app manifest, installable on home screen | 2 hrs |
| Offline card review | Cache today's review queue locally. Sync results when back online | 3 hrs |
| Mobile-optimized UI | Touch-friendly card review, swipe gestures for rating, responsive layouts | 3 hrs |
| Push notifications | Daily review reminder at configured time. "You have 12 cards due" | 2 hrs |

### OAuth2 (Future Auth Upgrade) [OPTIONAL]

Add social login so others can use the app without creating an account.

| Task | Details | Time |
|------|---------|------|
| Spring OAuth2 client | Add Google and GitHub as OAuth2 providers in Spring Security config | 3 hrs |
| Account linking | If OAuth email matches existing account, link them. Handle edge cases | 2 hrs |
| Login UI update | "Sign in with Google" / "Sign in with GitHub" buttons alongside email/password | 1 hr |

**MILESTONE:** App is live on the web, installable as PWA, works offline for reviews, accessible from anywhere.

---

## Complete Timeline & Cost Summary

Assuming 1–2 hours of focused work per day alongside a full-time job. Phases can overlap where noted.

| # | Phase | Duration | API Cost | Infra | Running Total |
|---|-------|----------|----------|-------|---------------|
| 1 | Foundation & Auth | 1–2 wk | $0 | $0 | $0/mo |
| 2 | Cards & SM-2 | 2–3 wk | $0 | $0 | $0/mo |
| 3 | Confidence & Analytics | 1–2 wk | $0 | $0 | $0/mo |
| 4 | Teach-Back (manual) | 1–2 wk | $0 | $0 | $0/mo |
| 5 | AI Integration | 2–3 wk | $5–15 | $0 | $5–15/mo |
| 6 | Voice Integration | 2–3 wk | +$3–8 | $0 | $8–23/mo |
| 7 | Smart Intake | 2–4 wk | +$0–5 | $0 | $8–28/mo |
| 8 | FSRS Algorithm | 2–3 wk | $0 | $0 | $8–28/mo |
| 9 | Polish & Advanced | 3–4 wk | +$0–5 | $0 | $8–33/mo |
| 10 | Deploy & Mobile | 1–2 wk | $0 | $10–30 | $18–63/mo |

**Total timeline:** 18–28 weeks at 1–2 hrs/day (4–7 months)
**Total monthly cost at full build:** ~$18–63/month (AI APIs + cloud hosting)
**First 4 phases:** Completely free. A usable app with zero external costs.

### What's Optional vs Essential

The core path (Phases 1–6 + 8 + 10) gives you a complete, voice-enabled, AI-powered learning engine. Everything marked [OPTIONAL] in the plan is genuinely optional — build it if you find yourself wanting it after using the core app daily.

- **Essential:** Auth, Cards, SM-2, Confidence, Teach-Back, AI generation, AI evaluation, Voice, FSRS, Deploy
- **High value optional:** Browser extension, daily sprint mode, analytics dashboard enhancements
- **Nice to have:** GitHub integration, reading app sync, conversation import, blog export, concept graph visualization, spaced writing prompts, system design prompt generator

---

## Build Order — Free First, Paid Second

### Part 1 — Free ($0/month)

Build and validate the entire core learning loop before spending on APIs.

| Order | Phase | What You Get |
|-------|-------|-------------|
| 1 | Phase 1 — Foundation & Auth | **COMPLETE** |
| 2 | Phase 2 — Cards & SM-2 | Daily flashcard reviews with spaced repetition |
| 3 | Phase 3 — Confidence & Analytics | Dashboard, fragile knowledge detection, streaks |
| 4 | Phase 4 — Teach-Back (manual) | Self-evaluated teach-back, gap→card loop |
| 5 | Phase 8 — FSRS Algorithm | Smarter scheduling, personal forgetting model |

At the end of Part 1 you have a fully functional learning engine with zero monthly costs.

### Part 2 — Paid (APIs + Hosting)

| Order | Phase | Cost | What You Get |
|-------|-------|------|-------------|
| 6 | Phase 5 — AI Integration | $5–15/mo | AI card generation, AI teach-back evaluation |
| 7 | Phase 6 — Voice | +$3–8/mo | Voice teach-back, TTS feedback, conversation mode |
| 8 | Phase 7 — Smart Intake | +$0–5/mo | Browser extension, intake queue, imports |
| 9 | Phase 9 — Polish & Advanced | +$0–5/mo | Concept graph, writing prompts, sprint mode |
| 10 | Phase 10 — Deploy & Mobile | $10–30/mo | AWS, PWA, offline, push notifications |
