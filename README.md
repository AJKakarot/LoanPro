# LoanPro

Maker-checker loan processing: apply, verify documents, then a checker approves or rejects. AI is advisory only — it never decides.

**Stack:** React + Vite · Spring Boot 4.1 · PostgreSQL · FastAPI + LangGraph + Groq

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

Flow: Customer submits → Maker verifies → Checker decides.
