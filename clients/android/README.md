# Android client

Planned for Iteration 1.

- Kotlin, minimum SDK 26.
- Calls the FastAPI REST endpoints described in the OpenAPI schema.
- Uses JWT authentication (planned) and the same item/claim contract as the web client.
- The claim request is a single POST; the server returns `409` when the last unit was taken.

This folder will contain the Gradle project. Until then, the shared domain rule is implemented and tested in `src/claim-engine.js`, and the API is scaffolded in `server/`.
