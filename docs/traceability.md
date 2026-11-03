# Requirement Traceability

| Requirement | Issue | PR | Test / evidence |
|---|---|---|---|
| Create an item with an integer quantity | #1 | — | `creates an item with the full quantity remaining` |
| Reject invalid or duplicate items | #1 | — | `rejects an invalid total`, `rejects a duplicate item id` |
| Claim a quantity atomically | #3 | #4 | `claim decrements remaining and records the claim` |
| Release a claimed quantity | #3 | #4 | `release restores the claimed quantity` |
| Two claims for the last unit: one conflict | #3 | #4 | `two claims for the last unit: one succeeds, one conflicts` |
| Invariant `claimed + remaining = total` | #3 | #4 | `random claim/release sequences preserve the invariant` |
| Non-member cannot read or write a list | #5 | — | Firestore emulator test (Iteration 2) |
| Claim deadlines and per-person limits | #7 | — | Planned for Iteration 2 |

Update this table whenever an issue or PR is opened or merged.
