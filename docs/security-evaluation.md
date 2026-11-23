# Security evaluation — Iteration 2

**Scenario:** an unauthenticated attacker and an authenticated but malicious user attack the ClaimIt API.
**Scope:** the FastAPI service (`server/app/`), its authentication dependency, database access and browser-facing CORS policy. Clients, deployment TLS and the host operating system are out of scope.
**Question:** can the service preserve claim integrity and user-data confidentiality when requests are forged, replayed, malformed or injected?

## Method

1. **Threat hypotheses** were derived from the endpoints and the data flow: identity forgery, token replay after expiry, credential disclosure, injection through item/list names, cross-origin reads, and anonymous writes.
2. **Each hypothesis became an executable test.** Tests exercise the real FastAPI app through `TestClient`, with the real SQLAlchemy models, a per-test database reset, and real JWT encoding/decoding. No mocks replace the security boundary.
3. **A failing test was treated as a finding.** The security tests were first run against `main` at `cfa35c6` (before the fix) to confirm that the suspected anonymous-write defect was real; the same test was then run against the fixed branch.
4. **Results were recorded** with the command, environment, expected status and actual status. Residual risks that were not fixed in this iteration are listed explicitly rather than omitted.

## Environment

| Item | Value |
|---|---|
| Python | 3.12.8 |
| Local database | SQLite (`server/test_claimit.db`) |
| CI database | PostgreSQL 16 (service container) |
| Test runner | pytest |
| API framework | FastAPI + Starlette `TestClient` |
| Commit under test | `76ff55e` (PR #18), compared with `cfa35c6` (pre-fix `main`) |

## Hypotheses and results

| # | Hypothesis | Test | Expected | Result |
|---|---|---|---|---|
| S1 | Anonymous callers can create lists or items | `test_data_endpoints_require_authentication` | `401` on `POST /lists` and `POST /lists/{id}/items` | **Failed before fix (201), passes after fix (401)** |
| S2 | A malformed bearer token is accepted | `test_invalid_token_is_rejected` | `401` for `/auth/me` and `/items/{id}/claims` | Pass |
| S3 | A modified token signature is accepted | `test_tampered_token_is_rejected` | `401` | Pass |
| S4 | An expired token is accepted | `test_expired_token_is_rejected` | `401` | Pass |
| S5 | A token signed with another secret is accepted | `test_token_signed_with_wrong_secret_is_rejected` | `401` | Pass |
| S6 | An unsigned (`alg: none`) token is accepted | `test_unsigned_token_is_rejected` | `401` | Pass |
| S7 | A token for a deleted/unknown user is accepted | `test_token_for_unknown_user_is_rejected` | `401` | Pass |
| S8 | Passwords are stored in plaintext or with a reused salt | `test_password_is_stored_as_a_salted_hash` | PBKDF2-SHA256 prefix, no plaintext, different hashes for equal passwords | Pass |
| S9 | Email case allows duplicate accounts | `test_duplicate_email_is_case_insensitive` | `409` for `ALICE@example.com` after `alice@example.com` | Pass |
| S10 | Login responses disclose whether an email exists | `test_login_does_not_reveal_which_part_was_wrong` | identical `401` detail for wrong password and unknown email | Pass |
| S11 | Item names allow SQL injection | `test_sql_injection_payload_is_stored_literally` | payload round-trips literally; table remains usable | Pass |
| S12 | Any origin can read API responses | `test_cors_allows_only_the_configured_origin` | `Access-Control-Allow-Origin` only for `http://localhost:8080` | Pass |

## Reproduction

Pre-fix failure (detached worktree at `cfa35c6`, security test copied in):

```text
$ python -m pytest tests/test_security.py::test_data_endpoints_require_authentication -q
FAILED tests/test_security.py::test_data_endpoints_require_authentication
1 failed
```

The failing assertion was the first one: anonymous `POST /lists` returned `201`, not `401`.

Fixed branch:

```text
$ cd server && python -m pytest -q
27 passed, 1 warning in 3.76s
```

The remaining warning is a Starlette deprecation notice about the `httpx` test client, not a security finding.

## Findings

### F1 — Anonymous list and item creation (High, fixed)

`POST /lists` and `POST /lists/{id}/items` had no `current_user` dependency. An unauthenticated caller could create unlimited lists and items, consuming storage and polluting any list whose id was known. The fix adds the bearer dependency to both endpoints. The regression test fails on the pre-fix commit and passes on the fixed commit.

### F2 — Security behaviour was untested (Medium, fixed)

The JWT dependency rejected invalid tokens by construction, but no test prevented a future refactor from weakening it. Twelve tests now cover the rejection paths, including the `alg: none` case.

### F3 — Password-storage evidence was missing (Medium, fixed)

`hash_password` used PBKDF2-SHA256 with a random salt, but no test asserted that plaintext was absent or that equal passwords produced different hashes. The new test does both.

### F4 — No per-list membership or object-level authorization (Medium, open)

Read endpoints (`GET /lists/{id}/items`, `GET /items/{id}`) are public, and any authenticated user can read any list id. The product currently relies on unguessable/private ids rather than membership checks. This is a broken-object-level-authorization risk if lists are shared beyond a trusted group. It is documented as a residual risk, not fixed in this iteration because it requires a membership model and a product decision about invitation flow.

### F5 — No rate limiting on login or registration (Low, open)

An attacker can attempt passwords or create accounts without a server-side limit. Suitable mitigations are per-IP and per-account throttling at the reverse proxy or in the API. Out of scope for the iteration.

### F6 — Development JWT secret has a usable default (Medium, open)

`JWT_SECRET` falls back to a development string when the environment variable is absent. Tests and local runs need a default, but deployment must set a unique 32-byte secret. This is recorded as a deployment requirement and should be enforced by a startup check in a future iteration.

## Limitations of this evaluation

- It is **specification-based**, not coverage-based: it tests the threats the team identified, not every path. No line/branch coverage tool was used.
- It exercises the app in-process, so proxy behaviour, TLS configuration, header stripping and real network timing are not covered.
- It does not include dependency vulnerability scanning (`pip-audit`), fuzzing, or a penetration test.
- SQLite and PostgreSQL behave differently for some constraints; the CI run against PostgreSQL is the stronger signal for database behaviour.
- The test for CORS checks the response header, not browser enforcement; it is a proxy for the browser's behaviour, not a substitute for an end-to-end browser security test.

## Conclusion

The evaluation found one high-severity defect (anonymous writes), fixed it, and converted every identified hypothesis into a repeatable regression test. The suite now contains 27 passing tests, 12 of them security-focused. The remaining risks are authorization scope, rate limiting and deployment secret management; they are recorded so that the next iteration starts from evidence rather than from an assumption that “authentication was added, therefore the API is secure”.
