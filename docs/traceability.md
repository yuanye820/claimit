# Requirement Traceability

| Backlog item | GitHub issue | PR | Test / evidence |
|---|---|---|---|
| Create a list and items | [#1](https://github.com/yuanye820/claimit/issues/1) | — | `creates an item with the full quantity remaining` |
| Add items with quantities | [#2](https://github.com/yuanye820/claimit/issues/2) | — | `rejects an invalid total` |
| Claim a quantity atomically | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `claim decrements remaining and records the claim` |
| Release a claimed quantity | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `release restores the claimed quantity` |
| Two claims for the last unit: one conflict | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `two claims for the last unit: one succeeds, one conflicts` |
| Invariant `claimed + remaining = total` | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `random claim/release sequences preserve the invariant` |
| Non-member cannot read or write a list | planned #5 | — | Firestore emulator test (Iteration 2) |
| Claim deadlines and per-person limits | planned #7 | — | Planned for Iteration 2 |

Update this table whenever an issue or PR is opened or merged.
