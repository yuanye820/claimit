# ClaimIt

[![CI](https://github.com/yuanye820/claimit/actions/workflows/ci.yml/badge.svg)](https://github.com/yuanye820/claimit/actions/workflows/ci.yml)

A shared list with conflict-safe claims. Android app and responsive web app, backed by a self-hosted FastAPI service and PostgreSQL.

**Status:** Iteration 2. The service supports JWT authentication, conflict-safe claims, claim/release audit history and a 27-test API suite with security regression coverage. Android and web clients use the same API.

## Problem

Groups coordinate shared lists in chat. A claim is only a message: it scrolls away, two people can claim the same item, and nobody can see how many units remain. ClaimIt keeps one stateful list in the cloud and records each claim once.

## Current increment

- Register and log in; claims are attributed to a user.
- Create a list and an item with an integer total quantity.
- Claim or release a quantity.
- Two concurrent claims for the last unit: exactly one succeeds; the other receives `409`.
- Every successful claim or release appends an attributable event; `GET /items/{id}/claims` returns the history.
- Invariant: `claimed + remaining = total`, and `remaining >= 0`.

## Run the tests

### Domain rule (Node.js 20+)

```bash
npm test
```

### API (Python 3.12+)

```bash
cd server
python -m venv .venv
# Windows: .venv\Scripts\activate
source .venv/bin/activate
pip install -r requirements.txt
pytest tests
```

The API tests use SQLite locally and PostgreSQL in CI.

## Run the API

```bash
docker compose up --build
```

Open http://127.0.0.1:8000/docs for the generated OpenAPI UI.

## Repository structure

```
src/claim-engine.js        Framework-free claim rule and concurrency lock
test/claim-engine.test.js  Domain unit and concurrency tests
server/                    FastAPI service, PostgreSQL models and API tests
clients/android/           Android client (Java, min SDK 26)
clients/web/               Responsive web client (HTML/CSS/JavaScript)
docker-compose.yml         API + PostgreSQL for local development
docs/traceability.md       Requirement -> issue -> PR -> test
docs/evidence.md           Evidence index for the report
docs/working-agreement.md  3-person team agreement
docs/review-checklist.md   Mandatory review criteria
docs/iterations/           Iteration records and retrospectives
docs/security-evaluation.md Security scenario and residual risks
docs/system-overview.md    System diagram and design decisions
report/                    Assignment reports (PDF and DOCX)
.github/                   Issue/PR templates and CI
```

## Clients

- `clients/android/` — Android APK (Java, min SDK 26).
- `clients/web/` — responsive web app.

Both clients use the same OpenAPI contract. The API is served by FastAPI behind Nginx with TLS in deployment.

## Evidence

- Backlog: [GitHub Issues](https://github.com/yuanye820/claimit/issues)
- Traceability: [docs/traceability.md](docs/traceability.md)
- Evidence index: [docs/evidence.md](docs/evidence.md)
- Working agreement: [docs/working-agreement.md](docs/working-agreement.md)
- Iteration records: [Iteration 1](docs/iterations/iteration-1.md), [Iteration 2](docs/iterations/iteration-2.md)
- Security evaluation: [docs/security-evaluation.md](docs/security-evaluation.md)

## Reports

- Assignment 1 report (PDF): `report/ClaimIt_Assignment1_Report.pdf`
- Assignment 1 report (DOCX): `report/ClaimIt_Assignment1_Report.docx`
- Project proposal (DOCX): `report/ClaimIt_Proposal.docx`
- Submission tag: `assignment1-submission`

## Team

- Liye Zheng (`yuanye820`) — Product Owner; Android client
- Xirui Li (`lr20040416-ship-it`) — Tech Lead; PostgreSQL schema and claim endpoint
- Junyu Chen (`xixiyu268-cmd`) — QA/DevOps; web client, CI and evidence

## Design notes

- Quantities are integers; the invariant needs no floating-point tolerance.
- A claim is one SQL conditional update: `UPDATE items SET remaining = remaining - :qty WHERE id = :id AND remaining >= :qty`. If no row is updated, the API returns `409 Conflict`.
- Claim and release append a `claim_events` row in the same transaction as the quantity change, so the audit trail cannot drift from the state.
- The service is self-hosted on an Ubuntu server: FastAPI behind Nginx with TLS, PostgreSQL for storage, and `pg_dump` for backups.
- Authentication uses JWT bearer tokens (HS256) and PBKDF2-SHA256 password hashes. Deployment must set `JWT_SECRET`.
- The domain rule has no framework import, so it can be tested without a database; the API tests run against SQLite locally and PostgreSQL in CI.
- `main` is protected: one approving review, `test` and `api` checks, linear history, no direct pushes.
