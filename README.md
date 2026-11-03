# ClaimIt

[![CI](https://github.com/yuanye820/claimit/actions/workflows/ci.yml/badge.svg)](https://github.com/yuanye820/claimit/actions/workflows/ci.yml)

A shared list with conflict-safe claims. Android app and responsive web app, backed by Firebase Authentication and Cloud Firestore.

**Status:** Assignment 1 walking skeleton. The claim domain and its tests are implemented on `main` (PR #4); the Android and web clients are scaffolded in Iteration 1.

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
report/                    Assignment 1 report (PDF and DOCX)
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

## Report

- [Assignment 1 report (PDF)](report/ClaimIt_Assignment1_Report.pdf)
- [Assignment 1 report (DOCX)](report/ClaimIt_Assignment1_Report.docx)

## Team

- Liye Zheng (`yuanye820`) — Product Owner; Android client
- Xirui Li (`lr20040416-ship-it`) — Tech Lead; Firestore model, transaction and security rules
- Junyu Chen (`xixiyu268-cmd`) — QA/DevOps; web client, CI and evidence

## Design notes

- Quantities are integers; all money-like floating-point problems are avoided.
- A claim is a read-validate-decrement-write operation. In production it runs in a Cloud Firestore transaction; the prototype serialises per-item operations with a lock so the same rule can be tested locally.
- The domain module has no Firebase import, so the rule can be tested in CI without credentials or emulator time.
