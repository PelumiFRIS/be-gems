# BE-GEMS

Board Evaluation & Governance Effectiveness Management System — FirstRegistrars &
Investor Services' platform for running board evaluations, governance benchmarking,
and board effectiveness management for its Company Secretarial clients.

This is the MVP scaffold: the foundation (auth, org/board/director/committee CRUD,
the NCCG 2018 question bank) is built and tested end to end. The evaluation
lifecycle, scoring engine, findings/actions, dashboard, and reports are built module
by module in the sessions that follow — see the plan this scaffold was built from
for the full phased scope.

## Stack

Same approach as FRIS's board-portal product: Spring Boot 4.1 / Java 21 backend,
React 19 / TypeScript / Vite frontend, Postgres via Flyway migrations, JWT auth,
single repo with `backend/` and `frontend/` as siblings.

## Local development

```
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run    # http://localhost:8081
cd frontend && npm install && npm run dev  # http://localhost:5175
```

Or run the whole stack in Docker:

```
docker compose up --build
```

## Tests

```
cd backend && ./mvnw test      # needs docker compose's postgres running
cd frontend && npm test -- --run && npm run build
```

## Architecture notes

- **Roles**: trimmed to 5 for MVP (`SUPER_ADMIN`, `ORG_ADMIN`, `COMPANY_SECRETARY`,
  `EVALUATOR`, `DIRECTOR`) — see `Role.java` for how each SRS role maps onto one of
  these, so later splits are additive.
- **Governance framework**: NCCG 2018 is seeded via `V2__framework_and_questions.sql`
  — 18 dimensions (weights sum to exactly 100%) and both example questionnaires from
  the requirements memo (Board self-assessment, Director Peer-to-Peer), as real
  starter data rather than hardcoded logic. Not admin-editable yet; more frameworks
  are a later phase.
- **One board per organisation** in MVP — multi-board support is a later phase.
- **Deploy**: backend to Render (Docker), frontend to Vercel — matching board-portal.
  The frontend Dockerfile exists for local dev via docker-compose and stays available
  as a self-host path if a client ever needs on-prem.
- **Integration with board-portal** (pulling Board/Director rosters via its
  `ApiKeyController`) is an explicit later phase — this app runs standalone with
  manually-entered directors for now.
