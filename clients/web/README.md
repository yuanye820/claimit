# Web client

Planned for Iteration 1.

- Responsive single-page app that calls the FastAPI REST endpoints.
- Uses the same OpenAPI contract and field names as the Android client.
- The claim request is a single POST; the server returns `409` when the last unit was taken.

This folder will contain the Vite project. Until then, the shared domain rule is implemented and tested in `src/claim-engine.js`, and the API is scaffolded in `server/`.
