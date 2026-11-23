# ClaimIt web client

Plain HTML/CSS/JavaScript client for ClaimIt.

## Run

Start the API from the repository root:

```bash
docker compose up --build
```

Or:

```bash
cd server
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Then serve this folder:

```bash
cd clients/web
python -m http.server 8080
```

Open http://localhost:8080.

The API base URL is editable in the page; the default is
`http://127.0.0.1:8000`. The FastAPI service must allow
`http://localhost:8080` through CORS.

## Notes

- The bearer token is stored in `localStorage` for the prototype. For
  production, prefer an HttpOnly cookie or a short-lived token strategy.
- The client uses `textContent` for item names, not `innerHTML`, to avoid
  injecting HTML.
- `409 Conflict` is shown as a message; it is the expected result when another
  user takes the last unit.
