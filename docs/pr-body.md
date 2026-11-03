## What

Implement atomic claim and release in the shared claim engine.

## Why

Closes #3. Without an atomic check-and-decrement, two users can claim the last unit and `remaining` becomes negative.

## How tested

`npm test` runs the unit tests, a `Promise.all` test for two claims on the last unit, and a randomized invariant test.

## Evidence

- Test output: [paste `npm test` output]
- CI run: [paste GitHub Actions run link]

## Checklist

- [x] Acceptance criteria verified
- [x] Tests added
- [x] Documentation updated
- [x] CI green
- [x] No secrets committed
