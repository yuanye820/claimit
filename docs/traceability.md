# Requirement Traceability

| Backlog item | GitHub issue | PR | Test / evidence |
|---|---|---|---|
| Create a list and items | [#1](https://github.com/yuanye820/claimit/issues/1) | — | `creates an item with the full quantity remaining` |
| Add items with quantities | [#2](https://github.com/yuanye820/claimit/issues/2) | — | `rejects an invalid total`, API `POST /lists/{id}/items` |
| List items in a list | [#5](https://github.com/yuanye820/claimit/issues/5) | [#8](https://github.com/yuanye820/claimit/pull/8) | `test_list_items_returns_items_in_id_order`, `test_list_items_unknown_list_returns_404` |
| Claim a quantity atomically | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `claim decrements remaining and records the claim`; `test_claim_last_unit_twice_one_conflicts` |
| Release a claimed quantity | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `release restores the claimed quantity`; `test_release_restores_the_claimed_quantity` |
| Two claims for the last unit: one conflict | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `two claims for the last unit: one succeeds, one conflicts`; `test_claim_last_unit_twice_one_conflicts` |
| Invariant `claimed + remaining = total` | [#3](https://github.com/yuanye820/claimit/issues/3) | [#4](https://github.com/yuanye820/claimit/pull/4) | `random claim/release sequences preserve the invariant` |
| Only list members can claim | planned #5 | — | JWT + API test (Iteration 2) |
| Invalid or unauthenticated requests are rejected | [#6](https://github.com/yuanye820/claimit/issues/6) | [#7](https://github.com/yuanye820/claimit/pull/7) | `test_claim_requires_user_header`, `test_claim_rejects_zero_quantity`, `test_claim_unknown_item_returns_404`, `test_release_without_claim_returns_409` |
| Claim deadlines and per-person limits | planned #7 | — | Planned for Iteration 2 |

Update this table whenever an issue or PR is opened or merged.
