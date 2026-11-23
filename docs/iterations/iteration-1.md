# Iteration 1 — Authenticated clients

**Dates:** 3–23 November 2026
**Goal:** turn the tested claim rule into a usable product: one shared list, authenticated users, and both clients reading the same API.

## Scope and selection

Iteration 1 was planned after the Assignment 1 walking skeleton (PR #4). The backlog was ordered by the shortest path to a demonstrable end-to-end flow:

| Issue | Increment | Why it came first |
|---|---|---|
| [#9](https://github.com/yuanye820/claimit/issues/9) | JWT authentication | Claims need a stable user identity before they can be attributed or released. |
| [#11](https://github.com/yuanye820/claimit/issues/11) | Android client | The group mostly coordinates from phones; this is the primary client. |
| [#13](https://github.com/yuanye820/claimit/issues/13) | Web client | Gives a zero-install path for the demo and for desktop users. |

Everything else in the backlog (audit history, per-person limits, deadlines) was deferred to Iteration 2.

## Delivered

| PR | Author | Change | Changed lines | Files | Reviews | CI |
|---|---|---|---:|---:|---:|---|
| [#10](https://github.com/yuanye820/claimit/pull/10) | `lr20040416-ship-it` | JWT register/login, bearer dependency, password hashing, auth tests | 355 | 10 | 0 | `test`, `api` green |
| [#12](https://github.com/yuanye820/claimit/pull/12) | `yuanye820` | Android client: register, login, create list, add item, claim/release | 1,131 | 14 | 0 | `test`, `api` green |
| [#14](https://github.com/yuanye820/claimit/pull/14) | `xixiyu268-cmd` | Responsive web client with the same flow | 598 | 5 | 0 | `test`, `api` green |

All three PRs were squash-merged into `main`. The API test count at the end of the iteration was 14.

## Acceptance outcomes

- A user can register, log in, and call `/auth/me`; passwords are stored as PBKDF2-SHA256 hashes.
- The Android client performs the full flow against the deployed API; the APK was installed and exercised on an Android 15 emulator.
- The web client performs the same flow in a desktop browser.
- The claim invariant still holds: `remaining` never goes below zero, and the second claimant of the last unit receives `409`.
- A user can release a claimed quantity and see `remaining` restored.

## What did not go to plan

1. **Reviews were skipped.** All three PRs had zero reviews and zero approvals, even though the working agreement requires one approving review. Review coverage for the iteration was **0/3 (0%)**. PR #4 in Assignment 1 had two approvals, so this was a regression in process, not a tooling limitation.
2. **PRs were too large.** The working agreement targets roughly 300 changed lines. The three PRs averaged 695 changed lines; the Android PR alone changed 1,131 lines across 14 files. Large PRs make substantive review less likely, which helps explain finding 1.
3. **CORS was found late.** The web client worked in isolation but the browser blocked calls to the API until `CORSMiddleware` was added. The issue was found during manual integration, not by a test.
4. **CI does not build the Android client.** The `test` job runs the Node domain tests and the `api` job runs the Python suite. Android compilation is verified only locally, so a broken Gradle configuration could merge with green checks.
5. **The claim endpoint was the only authenticated write.** List and item creation were still anonymous. This was not noticed until the Iteration 2 security review.

## Retrospective actions carried into Iteration 2

| Action | Measure |
|---|---|
| Enforce review instead of relying on the agreement | Enable `main` branch protection: one approval, `test` and `api` checks, no direct pushes, stale approvals dismissed. |
| Keep PRs reviewable | Target at most 300 changed lines and state the review focus in the PR body. |
| Evaluate one quality attribute with evidence | Run a security scenario: threat hypotheses, regression tests, recorded results, residual risks. |
| Close the audit gap | Add claim/release history so disputed claims can be reconstructed. |
| Make integration risk visible | Add a review checklist item for cross-origin and client/API contract changes. |

## Evidence

- Issues: [#9](https://github.com/yuanye820/claimit/issues/9), [#11](https://github.com/yuanye820/claimit/issues/11), [#13](https://github.com/yuanye820/claimit/issues/13)
- Pull requests: [#10](https://github.com/yuanye820/claimit/pull/10), [#12](https://github.com/yuanye820/claimit/pull/12), [#14](https://github.com/yuanye820/claimit/pull/14)
- CI: [Actions](https://github.com/yuanye820/claimit/actions)
- Requirement mapping: [traceability.md](../traceability.md)
