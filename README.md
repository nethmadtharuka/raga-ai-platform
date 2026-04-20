# AI Research Platform (RAGA)

A production-oriented **Retrieval-Augmented Generation (RAG)** system: ingest text, store **semantic embeddings** in **ChromaDB**, and answer questions with **Google Gemini** using only retrieved context—plus automation-friendly webhooks and a **React** web UI.

---

## 1. Overview

| | |
|---|---|
| **What it is** | A Spring Boot backend and Vite/React frontend for document ingestion, vector search, and grounded Q&A. |
| **Problem it solves** | Teams need **accurate, source-grounded answers** over private or domain text without fine-tuning a model—classic **enterprise knowledge** and **research assistant** use cases. |
| **How it works** | Text is **chunked** → **embedded** → stored in ChromaDB; questions are embedded → **similarity search** → top chunks feed a **Gemini** prompt so answers stay tied to your data. |

---

## 2. Key Features

Features below are **implemented in this repository** (see packages `controller`, `service`, `security`, `frontend`).

### AI & retrieval

- **RAG Q&A** — Retrieves top-k similar chunks (`VectorStoreService` + `RAGService`), builds a context-only prompt, returns an answer with **source titles** and chunk counts.
- **Semantic embeddings** — `EmbeddingService` calls Gemini **embedContent** (`gemini-embedding-001`) with `x-goog-api-key`; vectors stored in ChromaDB.
- **Text generation** — `GeminiService` calls **Gemini 2.5 Flash** (`generateContent`); failures surface as errors (no silent “fake success” strings).
- **Chunking pipeline** — `ChunkingService`: sentence-oriented splits, **configurable max chunk size and overlap** (`chunking.*` in config).
- **Research & summarize (LLM-only)** — `ResearchService` + `ResearchController`: topic **research** and **summarization** via Gemini (not RAG retrieval for those endpoints).
- **Optional persist from research** — Webhook research can **save generated research** back into the vector store when `save` is true.

### Data & APIs

- **Document REST API** — JSON ingest, **`.txt` upload**, **vector search**, **RAG ask**, **collection stats**, **chunking preview** metadata (`DocumentController`).
- **Webhook API** — Dedicated routes for **add-document**, **ask**, **research**; unified **`/api/webhooks/n8n`** dispatcher with actions: `add_document`, `research_topic`, `ask_question`, `get_stats` (`WebhookController`).
- **Structured responses** — `WebhookResponse` wrapper (`success`, `message`, `result`).

### Security

- **API key authentication** — `ApiKeyAuthenticationFilter` + Spring Security: **`X-API-Key`** or **`Authorization: Bearer <key>`**.
- **Separate webhook key (optional)** — `WEBHOOK_API_KEY` accepted for `/api/webhooks/**` in addition to `APP_API_KEY` when configured.
- **Public endpoints** — `/api/health/**`, Swagger/OpenAPI, `/actuator/health`, static UI (no key).
- **Stateless security** — CSRF disabled for API usage; **session-free** JWT-style statelessness without JWT (keys per request).

### Validation & errors

- **Bean Validation** on request DTOs (`jakarta.validation`); `GlobalExceptionHandler` for **`MethodArgumentNotValidException`**, **`ConstraintViolationException`**, **`IllegalArgumentException`**, **`WebClientResponseException`**, and a generic 500 handler with optional **debug** fields via `app.errors.debug`.

### Observability & docs

- **Spring Boot Actuator** — Health, info, metrics exposed (see `management.*` in config).
- **OpenAPI 3 / Swagger UI** — `springdoc-openapi`; API key security scheme for interactive calls.

### Operations

- **Docker** — Multi-stage `Dockerfile` (non-root runtime user), **healthcheck** on `/api/health`; **docker-compose** for app + **ChromaDB** + **n8n**.
- **CI** — GitHub Actions: **npm build** (frontend) then **`mvn verify`** (`.github/workflows/ci.yml`).

### Frontend

- **Vite + React + TypeScript** — Add document and ask-question flows; **`fetch`** wrapper sends API key; model text rendered with **`textContent`** (XSS-safe for answers).
- **Dev proxy** — Vite proxies `/api` to the backend; production build can output into Spring `static/`.

### Testing (automated)

- **JUnit 5** — Context load, **WebMvc** tests (`HealthController`, `WebhookController` with API key headers), **unit tests** for `ChunkingService`, `EmbeddingService` (cosine similarity), **`RAGService`** (mocked dependencies).

---

## 3. System Architecture

```text
Clients (React, n8n, curl)
        │
        ▼
Spring Boot REST (Controllers)
        │
        ├── VectorStoreService ──► ChunkingService
        │         │                      │
        │         ├── EmbeddingService ──┼──► Gemini (embeddings)
        │         └── ChromaDBService ─────► ChromaDB (HTTP API v2)
        │
        ├── RAGService ──► VectorStoreService + GeminiService (generation)
        └── ResearchService ──► GeminiService
```

- **Layering** — Controllers delegate to **stateless `@Service` beans**; HTTP clients use **Spring WebClient** (`WebClientConfig`) for Gemini and Chroma.
- **Vector store** — ChromaDB holds **chunk text + embeddings + metadata** (title, source, chunk index, timestamps). No relational DB in this codebase: persistence for search is **Chroma only**.
- **Concurrency** — `ChromaDBService` uses **synchronized** initialization so collection setup is safe under concurrent requests.

---

## 4. Tech Stack

| Layer | Technologies |
|--------|----------------|
| **Backend** | Java 17, Spring Boot 4, Spring Web MVC, Spring Security, Spring Validation, Spring WebFlux (WebClient), Lombok |
| **Frontend** | React 18, TypeScript, Vite 5 |
| **Vector DB** | ChromaDB (HTTP client from app) |
| **AI** | Google Gemini API (embeddings + **Gemini 2.5 Flash** generation) |
| **Automation** | n8n (optional, via Docker Compose) |
| **Build** | Maven, npm |
| **Quality** | JUnit 5, Mockito, Spring Security Test |
| **Docs** | springdoc-openapi (Swagger UI) |
| **Ops** | Docker, Docker Compose, GitHub Actions |

---

## 5. Security & Performance

### Security (as implemented)

- **Not JWT** — Authentication is **API keys** (service-to-service / automation style), not user JWT sessions.
- **Secrets via environment** — `GEMINI_API_KEY`, `APP_API_KEY`, optional `WEBHOOK_API_KEY`; **do not commit** real keys (see `.env.example`).
- **CORS** — `CorsConfigurationSource` bean; permissive patterns suitable for development—**tighten origins** for production.
- **Gemini calls** — API key via **`x-goog-api-key`** header (not query strings for generation/embeddings in current code).

### Performance-related behavior

- **Chunking** reduces token load and improves retrieval precision; overlap preserves boundary context.
- **WebClient** for non-blocking-capable outbound calls; blocking `block()` used where synchronous service APIs are simpler.
- **Thread-safe** lazy Chroma collection initialization.
- **Rate limiting** — A `RateLimitExceededException` type exists for future use; **no rate limiter is wired** to controllers in the current codebase.

---

## 6. AI Features (Detail)

| Capability | Role in the system |
|------------|---------------------|
| **Embeddings** | `gemini-embedding-001` vectors for chunks and queries; similarity search in ChromaDB. |
| **Generation** | `gemini-2.5-flash` for RAG answers, research text, and summaries. |
| **RAG** | Top-3 chunks by default for Q&A; prompt instructs the model to use **only** supplied context. |
| **Heuristic fallback** | If no chunks match, the API returns a clear message asking the user to add documents first. |

---

## 7. Setup & Installation

### Prerequisites

- Java 17+, Maven 3.8+
- Node.js 20+ (for the UI build/dev)
- Docker (optional: ChromaDB / full stack)
- **Google Gemini API key**

### Environment variables

Copy `ai-research-platform/.env.example` and set at least:

| Variable | Purpose |
|----------|---------|
| `GEMINI_API_KEY` | Gemini REST API |
| `APP_API_KEY` | Required for protected `/api/**` calls |
| `WEBHOOK_API_KEY` | Optional second key for webhook routes |
| `CHROMA_URL` | Chroma base URL (default `http://localhost:8000`) |

### ChromaDB (local)

```bash
docker run -d --name chromadb -p 8000:8000 chromadb/chroma:latest
```

### Backend

```bash
cd ai-research-platform
export GEMINI_API_KEY=... APP_API_KEY=...   # Windows: set in Environment Variables
mvn spring-boot:run
```

Health (no key): `GET http://localhost:8080/api/health`

### Frontend

```bash
cd ai-research-platform/frontend
npm install
# Optional: .env with VITE_API_KEY matching APP_API_KEY
npm run dev
```

Vite dev server proxies `/api` → `http://localhost:8080` (see `vite.config.ts`).

**Production UI in the JAR:**

```bash
npm run build
```

Output is written to `src/main/resources/static/` (configured in Vite).

### Docker Compose

From `ai-research-platform` (same directory as `docker-compose.yml`):

```bash
export GEMINI_API_KEY=... APP_API_KEY=...
docker compose up --build
```

---

## 8. API Overview

Protected routes require **`X-API-Key: <APP_API_KEY>`** (or Bearer), except where noted.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Liveness |
| GET | `/api/health/detailed` | App + Chroma + vector store checks |
| GET | `/api/health/metrics` | Stats + JVM memory |
| POST | `/api/documents` | Add JSON document (chunk + embed + store) |
| POST | `/api/documents/upload` | Upload `.txt` file |
| GET | `/api/documents/search` | Semantic search (`query`, `limit`) |
| POST | `/api/documents/ask` | RAG question |
| GET | `/api/documents/stats` | Collection stats |
| POST | `/api/documents/chunking-preview` | Chunking metadata for text |
| GET | `/api/research/{topic}` | LLM research on a topic |
| POST | `/api/summarize` | Summarize provided text |
| POST | `/api/webhooks/n8n` | n8n-style action dispatcher |
| POST | `/api/webhooks/add-document` | Webhook: add document |
| POST | `/api/webhooks/research` | Webhook: research (+ optional save) |
| POST | `/api/webhooks/ask` | Webhook: RAG ask |

**Interactive docs:** `http://localhost:8080/swagger-ui.html` — use **Authorize** with your API key.

---

## 9. Future Improvements

- **User auth** — JWT or OAuth2 for interactive users; keep API keys for automation.
- **Rate limiting & quotas** — Redis or gateway-level limits; wire `RateLimitExceededException` to a filter.
- **Circuit breakers / retries** — Resilience4j around Gemini and Chroma.
- **Async ingestion** — Queue long documents; return job IDs and poll status.
- **Streaming answers** — SSE/WebSocket from Gemini to the React app.
- **Evaluation** — Golden Q&A sets and retrieval quality metrics.
- **Stricter CORS & CSP** — Lock to known front-end origins in production.

---

## 10. Project Impact

- **Business value** — Reduces time to find answers in **policies, runbooks, research notes, and internal docs** while **grounding** responses in retrieved text (lower hallucination risk than a raw chatbot).
- **Engineering value** — Demonstrates **end-to-end RAG**, **secure API design** (keys, stateless security), **vector DB integration**, **validated REST APIs**, **observability hooks**, **containerized dependencies**, **CI**, and a **modern SPA**—aligned with how many teams ship internal AI assistants.

---

## License

Specify your license here (repository currently has a placeholder in `pom.xml`).
