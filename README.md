# ClaimIt

[![CI](https://github.com/yuanye820/claimit/actions/workflows/ci.yml/badge.svg)](https://github.com/yuanye820/claimit/actions/workflows/ci.yml)

A shared list with conflict-safe claims. Android app and responsive web app, backed by a self-hosted FastAPI service and PostgreSQL.

**Status:** Assignment 1 walking skeleton. The claim rule is implemented and tested in the framework-free domain module (PR #4). The FastAPI + PostgreSQL service is scaffolded under `server/`; the Android and web clients follow in Iteration 1.

## Problem

Groups coordinate shared lists in chat. A claim is only a message: it scrolls away, two people can claim the same item, and nobody can see how many units remain. ClaimIt keeps one stateful list in the cloud and records each claim once.

## Current increment

- Create a list and an item with an integer total quantity.
- Claim or release a quantity.
- Two concurrent claims for the last unit: exactly one succeeds; the other receives `409`.
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
docker-compose.yml         API + PostgreSQL for local development
docs/traceability.md       Requirement -> issue -> PR -> test
docs/evidence.md           Evidence index for the report
docs/working-agreement.md  3-person team agreement
docs/system-overview.md    System diagram and design decisions
report/                    Assignment 1 report (PDF and DOCX)
.github/                   Issue/PR templates and CI
```

## Clients

- `clients/android/` — Android APK (Kotlin, min SDK 26), planned for Iteration 1.
- `clients/web/` — responsive web app, planned for Iteration 1.

Both clients use the same OpenAPI contract. The API is served by FastAPI behind Nginx with TLS in deployment.

## Evidence

- Backlog: [GitHub Issues](https://github.com/yuanye820/claimit/issues)
- Traceability: [docs/traceability.md](docs/traceability.md)
- Evidence index: [docs/evidence.md](docs/evidence.md)
- Working agreement: [docs/working-agreement.md](docs/working-agreement.md)

## Report

- [Assignment 1 report (PDF)](report/ClaimIt_Assignment1_Report.pdf)
- [Assignment 1 report (DOCX)](report/ClaimIt_Assignment1_Report.docx)
- [Project proposal (DOCX)](report/ClaimIt_Proposal.docx)
- Submission tag: `assignment1-submission`

## Team

- Liye Zheng (`yuanye820`) — Product Owner; Android client
- Xirui Li (`lr20040416-ship-it`) — Tech Lead; PostgreSQL schema and claim endpoint
- Junyu Chen (`xixiyu268-cmd`) — QA/DevOps; web client, CI and evidence

## Design notes

- Quantities are integers; the invariant needs no floating-point tolerance.
- A claim is one SQL conditional update: `UPDATE items SET remaining = remaining - :qty WHERE id = :id AND remaining >= :qty`. If no row is updated, the API returns `409 Conflict`.
- The service is self-hosted on an Ubuntu server: FastAPI behind Nginx with TLS, PostgreSQL for storage, and `pg_dump` for backups.
- `X-User-Id` is a prototype header so the claim rule can be tested without a login flow. JWT authentication is planned for Iteration 1.
- The domain rule has no framework import, so it can be tested without a database; the API tests run against PostgreSQL in CI.
