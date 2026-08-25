# LoanPro

Maker-checker loan processing: apply, verify documents, then a checker approves or rejects. AI is advisory only — it never decides.

**Stack:** React + Vite · Spring Boot 4.1 · PostgreSQL · FastAPI + LangGraph + Groq

## Architecture

LoanPro is three services plus two managed stores. The SPA never talks to the AI service or the database. Spring Boot is the only authority for auth, workflow, and the final approve/reject decision.

```mermaid
flowchart TB
  subgraph clients["People"]
    C[Customer]
    M[Maker]
    K[Checker]
    A[Admin]
  end

  subgraph vercel["Vercel — frontend"]
    UI["React + Vite SPA<br/>JWT in localStorage"]
  end

  subgraph render["Render"]
    API["Spring Boot API<br/>/api/v1<br/>JWT + RBAC + Flyway"]
    AI["FastAPI AI service<br/>LangGraph + Groq"]
  end

  subgraph data["Data"]
    DB[(Neon PostgreSQL)]
    CDN[Cloudinary]
    LLM[Groq LLM]
  end

  C --> UI
  M --> UI
  K --> UI
  A --> UI

  UI -->|"HTTPS + Bearer JWT"| API
  API -->|"JDBC"| DB
  API -->|"upload / fetch docs"| CDN
  API -->|"GET /ai-analysis<br/>server-side only"| AI
  AI -->|"structured JSON"| LLM

  API -.->|"never writes approve/reject"| AI
```

### Runtime request path

1. Browser loads the SPA from Vercel (`VITE_API_URL` is baked in at build time).
2. Login/register hits `POST /api/v1/auth/*`. API issues access + refresh JWTs (HMAC-SHA, `JWT_SECRET` ≥ 256 bits).
3. All later calls go to `https://<api>/api/v1/...` with `Authorization: Bearer`. CORS must list the Vercel origin (`CORS_ORIGINS`).
4. Documents are stored on Cloudinary. Postgres keeps application, review, audit, and notification rows.
5. Maker/Checker/Admin open an application → API calls `POST {AI_SERVICE_URL}/api/ai/analyze-loan`. If AI is down, the loan workflow still works; the UI shows unavailable.

```mermaid
sequenceDiagram
  actor User
  participant SPA as Vercel SPA
  participant API as Spring Boot
  participant DB as Neon
  participant Store as Cloudinary
  participant AI as FastAPI
  participant Groq as Groq

  User->>SPA: Login
  SPA->>API: POST /auth/login
  API->>DB: Verify user + roles
  API-->>SPA: accessToken + refreshToken

  User->>SPA: Apply / upload docs
  SPA->>API: POST /applications + documents
  API->>Store: Store file
  API->>DB: DRAFT then SUBMITTED

  User->>SPA: Maker/Checker opens file
  SPA->>API: GET /applications/{id}/ai-analysis
  API->>DB: Load application + docs
  API->>AI: POST /api/ai/analyze-loan
  AI->>Groq: 4-agent graph
  Groq-->>AI: Structured JSON
  AI-->>API: Advisory summary only
  API-->>SPA: Show summary (not a decision)

  User->>SPA: Checker Approve / Reject
  SPA->>API: POST /workflow/checker/...
  API->>DB: APPROVED or REJECTED + audit
```

### Maker-checker workflow

Same person cannot act as both maker and checker on one application. Checker can decide only in `CHECKER_REVIEW` / `MAKER_VERIFIED`. AI never changes status.

```mermaid
stateDiagram-v2
  [*] --> DRAFT: Customer creates application
  DRAFT --> SUBMITTED: Customer submits
  SUBMITTED --> MAKER_REVIEW: Maker claims
  MAKER_REVIEW --> INFO_REQUESTED: Maker needs more info
  INFO_REQUESTED --> MAKER_REVIEW: Customer updates / maker resumes
  MAKER_REVIEW --> MAKER_VERIFIED: Docs + info + financials verified
  MAKER_VERIFIED --> CHECKER_REVIEW: Send to checker
  CHECKER_REVIEW --> APPROVED: Checker approves
  CHECKER_REVIEW --> REJECTED: Checker rejects
  CHECKER_REVIEW --> RETURNED_TO_MAKER: Send back
  RETURNED_TO_MAKER --> MAKER_REVIEW: Maker re-verifies
  APPROVED --> [*]
  REJECTED --> [*]
```

| Role | What they do |
| --- | --- |
| **CUSTOMER** | Profile, apply, upload docs (`IDENTITY`, `ADDRESS_PROOF`, `INCOME_PROOF`, `BANK_STATEMENT`, `PHOTO`), submit, respond to info requests |
| **MAKER** | Queue → claim → verify customer / documents / financials → request info or send to checker |
| **CHECKER** | Queue → read maker notes + AI summary → approve, reject (reason required), or return to maker |
| **ADMIN** | Users, products, all applications, audit log |

Send-to-checker rules: every document `VERIFIED`, and maker checklist (customer, documents, financials) all true.

### AI analysis pipeline (advisory only)

Triggered by `GET /api/v1/applications/{id}/ai-analysis` (`MAKER` / `CHECKER` / `ADMIN`). EMI and DTI are computed in Python, not by the model. Invalid LLM JSON retries once, then a deterministic fallback. Disclaimer is always attached: a human must decide.

```mermaid
flowchart LR
  START([START]) --> D[document_agent]
  D --> E[eligibility_agent]
  E --> R[risk_agent]
  R --> S[verification_summary_agent]
  S --> END([END])
```

| Agent | Output used by makers/checkers |
| --- | --- |
| **document_agent** | Completeness, missing types, mismatches |
| **eligibility_agent** | Amount/tenure vs product; EMI/DTI metrics |
| **risk_agent** | Risk level + manual checks |
| **verification_summary_agent** | Short briefing; never approve/reject |

## Run locally

Copy `.env.example` → `.env` (Neon/Cloudinary). Copy `ai-service/.env.example` → `ai-service/.env` (`GROQ_API_KEY`).

```bash
docker compose up -d postgres mailhog

cd backend && ./mvnw spring-boot:run          # http://localhost:8080
cd frontend && npm install && npm run dev     # http://localhost:5173
cd ai-service && python3.12 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Or: `docker compose --profile app up --build` → UI at http://localhost:8081

## Demo

| Role | Email | Password |
| --- | --- | --- |
| Customer | customer@loanpro.com | Customer@12345 |
| Maker | maker@loanpro.com | Maker@12345 |
| Checker | checker@loanpro.com | Checker@12345 |
| Admin | admin@loanpro.com | Admin@12345 |

Flow: Customer submits → Maker claims and verifies → Send to checker → Checker decides. AI is a briefing only.

## Deploy

**Backend → [Render](https://render.com)** (Web Service, Docker, root `backend`)

Health: `/actuator/health`

Env:
```
DATABASE_URL=jdbc:postgresql://...neon.tech/neondb?sslmode=require
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=                           # 32+ random chars
CORS_ORIGINS=https://YOUR-APP.vercel.app
STORAGE_TYPE=cloudinary
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_FOLDER=loanpro
```

**Frontend → [Vercel](https://vercel.com)** (Import GitHub, root `frontend`)

Env:
```
VITE_API_URL=https://YOUR-API.onrender.com/api/v1
```

Redeploy frontend after setting `VITE_API_URL`. Then set `CORS_ORIGINS` on Render to the Vercel URL and restart the API.

Demo logins stay the same if `APP_SEED_ENABLED` is true (default).

