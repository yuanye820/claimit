# ClaimIt

A shared list with conflict-safe claims. Android app and responsive web app, backed by Firebase Authentication and Cloud Firestore.

**Status:** Assignment 1 walking skeleton. The claim domain contract and its tests are in place; the Android and web clients are scaffolded in Iteration 1.

## Problem

Groups coordinate shared lists in chat. A claim is only a message: it scrolls away, two people can claim the same item, and nobody can see how many units remain. ClaimIt keeps one stateful list in the cloud and records each claim once.

## Current increment

- Create an item with an integer total quantity.
- Claim or release a quantity.
- Two concurrent claims for the last unit: exactly one succeeds; the other receives a conflict.
- Invariant: `claimed + remaining = total`, and `remaining >= 0`.

## Run the tests

Requires Node.js 20 or newer.

```bash
npm test
```

There are no runtime dependencies for the domain prototype.

## Repository structure

```
src/claim-engine.js        Shared domain contract and claim rules
test/claim-engine.test.js  Unit and concurrency tests
docs/traceability.md       Requirement -> issue -> PR -> test
docs/evidence.md           Evidence index for the report
docs/working-agreement.md  3-person team agreement
docs/system-overview.md    System diagram and design decisions
.github/                   Issue/PR templates and CI
```

## Clients

- `clients/android/` — Android APK (Kotlin, min SDK 26), planned for Iteration 1.
- `clients/web/` — responsive web app (Firebase JS SDK), planned for Iteration 1.

Both clients use the same item/claim contract and the same Cloud Firestore data model.

## Evidence

- Backlog: [GitHub Issues](https://github.com/yuanye820/claimit/issues)
- Traceability: [docs/traceability.md](docs/traceability.md)
- Evidence index: [docs/evidence.md](docs/evidence.md)
- Working agreement: [docs/working-agreement.md](docs/working-agreement.md)

## Team

- [Member 1] — Product Owner; Android client
- [Member 2] — Tech Lead; Firestore model, transaction and security rules
- [Member 3] — QA/DevOps; web client, CI and evidence

## Design notes

- Quantities are integers; all money-like floating-point problems are avoided.
- A claim is a read-validate-decrement-write operation. In production it runs in a Cloud Firestore transaction; the prototype serialises per-item operations with a lock so the same rule can be tested locally.
- The domain module has no Firebase import, so the rule can be tested in CI without credentials or emulator time.
