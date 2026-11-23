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
| Only authenticated users can claim | [#9](https://github.com/yuanye820/claimit/issues/9) | [#10](https://github.com/yuanye820/claimit/pull/10) | `test_claim_requires_bearer_token`, `test_data_endpoints_require_authentication` |
| Claim/release history is attributable | [#15](https://github.com/yuanye820/claimit/issues/15) | [#16](https://github.com/yuanye820/claimit/pull/16) | `test_claim_history_returns_events` |
| Invalid or unauthenticated requests are rejected | [#6](https://github.com/yuanye820/claimit/issues/6), [#17](https://github.com/yuanye820/claimit/issues/17) | [#7](https://github.com/yuanye820/claimit/pull/7), [#18](https://github.com/yuanye820/claimit/pull/18) | `test_claim_requires_bearer_token`, `test_claim_rejects_zero_quantity`, `test_claim_unknown_item_returns_404`, `test_release_without_claim_returns_409`, `test_invalid_token_is_rejected`, `test_expired_token_is_rejected` |
| Forged or tampered tokens are rejected | [#17](https://github.com/yuanye820/claimit/issues/17) | [#18](https://github.com/yuanye820/claimit/pull/18) | `test_tampered_token_is_rejected`, `test_token_signed_with_wrong_secret_is_rejected`, `test_unsigned_token_is_rejected`, `test_token_for_unknown_user_is_rejected` |
| Passwords are salted and hashed | [#17](https://github.com/yuanye820/claimit/issues/17) | [#18](https://github.com/yuanye820/claimit/pull/18) | `test_password_is_stored_as_a_salted_hash` |
| List and item creation require authentication | [#17](https://github.com/yuanye820/claimit/issues/17) | [#18](https://github.com/yuanye820/claimit/pull/18) | `test_data_endpoints_require_authentication` |
| Per-list membership (non-members receive 403) | planned | — | Residual risk in `security-evaluation.md` |
| Claim deadlines and per-person limits | planned | — | Deferred |

Update this table whenever an issue or PR is opened or merged.
