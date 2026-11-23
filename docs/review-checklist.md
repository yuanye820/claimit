# Pull Request Review Checklist

Use this checklist for every pull request to `main`. An approval means the reviewer has checked each item, not that the code merely compiles.

## 1. Scope and traceability

- [ ] The PR links an issue and the issue has testable acceptance criteria.
- [ ] The change does what the issue asks, and nothing unrelated.
- [ ] The PR is at most ~300 changed lines, or the description explains why not.
- [ ] The base branch is correct; stacked PRs state their dependency.

## 2. Behaviour and tests

- [ ] New behaviour has a test that would fail without the change.
- [ ] Error paths are tested (unauthorised, not found, conflict, invalid input).
- [ ] The test suite passes locally, not only in CI.
- [ ] The claim invariant still holds: `remaining >= 0`, and rejected claims write no state.

## 3. Security

- [ ] Every new or changed write endpoint requires authentication.
- [ ] Authorization is checked for the specific object, not only for “a logged-in user”.
- [ ] No secret, token, password or real credential is added to the repository, tests, logs or screenshots.
- [ ] SQL uses bound parameters; user input is not interpolated into queries.
- [ ] New response models do not expose password hashes, tokens or other users' emails.
- [ ] CORS origins are explicit; wildcards are justified.

## 4. Data and compatibility

- [ ] Schema changes are safe for existing data, or a migration/backfill is included.
- [ ] API changes are reflected in the clients that consume them.
- [ ] Time zones and ordering are defined for timestamps.

## 5. Operability and evidence

- [ ] The change can be diagnosed from logs without leaking sensitive values.
- [ ] Documentation and the evidence index are updated when behaviour changes.
- [ ] CI is green for the latest commit.
- [ ] The reviewer leaves at least one substantive comment: a risk, a question, a missing test or a suggested simplification.

## Review focus for Iteration 2

- **PR #16:** event ordering when timestamps tie; no event on a failed claim/release; the join must not expose emails or hashes.
- **PR #18:** every token-rejection path; identical login errors; list/item creation requires a token; no plaintext password in storage.
