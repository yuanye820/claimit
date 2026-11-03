# Issue #3 — Claim and release a quantity

**Title:** Claim and release a quantity

## User story

As a group member, I want to claim or release a quantity on an item so that the list reflects who is bringing what.

## Acceptance criteria

- Given an item with `remaining = 1`, when two users claim one unit at the same time, then exactly one succeeds and the other receives a conflict.
- Given a successful claim of one unit, when it completes, then `remaining` decreases by one and `claimed + remaining = total`.
- Given a claim of one unit, when the same user releases one unit, then `remaining` increases by one and the user's claim is removed.
- Given a claim larger than `remaining`, when it is submitted, then it is rejected and `remaining` is unchanged.

## Out of scope

- UI, Firestore, deadlines and per-person limits.

## Definition of Done

- [ ] Implementation in `src/claim-engine.js`
- [ ] Unit and concurrency tests
- [ ] `npm test` green
- [ ] Pull request reviewed
