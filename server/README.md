# ClaimIt API

FastAPI + PostgreSQL backend for the claim rule.

## Endpoints

- `GET /health`
- `POST /lists`
- `POST /lists/{list_id}/items`
- `GET /items/{item_id}`
- `POST /items/{item_id}/claims` — `X-User-Id` header required
- `POST /items/{item_id}/releases` — `X-User-Id` header required

The claim is one conditional update:

```sql
UPDATE items
SET remaining = remaining - :qty
WHERE id = :id AND remaining >= :qty
RETURNING remaining;
```

If no row is returned, the API answers `409 Conflict`. The losing request never
sees a successful claim and `remaining` cannot become negative.

## Run locally

```bash
cd server
python -m venv .venv
# Windows: .venv\Scripts\activate
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Open http://127.0.0.1:8000/docs for the generated OpenAPI UI.

By default the app uses `sqlite:///./claimit.db`. Set `DATABASE_URL` to a
PostgreSQL URL for the real deployment.

## Tests

```bash
cd server
pytest tests
```

The tests use SQLite by default. In CI they run against PostgreSQL through
`TEST_DATABASE_URL`.

## Docker Compose

From the repository root:

```bash
docker compose up --build
```

This starts PostgreSQL and the API on port 8000. The password in
`docker-compose.yml` is a local demo value; use a secret or environment file in
deployment.

## Authentication

`X-User-Id` is a prototype header so the claim rule can be tested without a
login flow. JWT authentication is planned for Iteration 1; do not expose this
header-based mode to the public internet.
