# Iteration 2 — Auditability, security and review enforcement

**Dates:** 23 November 2026
**Goal:** close the two highest-risk gaps from Iteration 1 — disputed claims with no audit trail, and a review process that existed on paper only — and evaluate security with repeatable evidence.

## Scope and selection

| Issue | Increment | Source |
|---|---|---|
| [#15](https://github.com/yuanye820/claimit/issues/15) | Claim audit history | Iteration 1 retrospective: a claim changes a number but leaves no attributable record. |
| [#17](https://github.com/yuanye820/claimit/issues/17) | Security hardening and regression tests | Quality-evaluation scenario chosen for Assignment 2; also exposes the unauthenticated-write finding. |
| — | Mandatory review enforcement | Process improvement selected from the Iteration 1 retrospective (review coverage 0/3). |

## Delivered

| PR | Author | Change | Changed lines | Files | CI | Review |
|---|---|---|---:|---:|---|---|
| [#16](https://github.com/yuanye820/claimit/pull/16) | `yuanye820` | `claim_events` table, event writes in claim/release, `GET /items/{id}/claims`, web History panel, API test | 147 | 7 | `test`, `api` green | 1 approval (Xirui) |
| [#18](https://github.com/yuanye820/claimit/pull/18) | `yuanye820` | Authentication on list/item creation, 12 security tests, `db_session` fixture | 295 | 6 | `test`, `api` green | 0 approvals — merged into its base branch, not `main` |
| [#20](https://github.com/yuanye820/claimit/pull/20) | `yuanye820` | Iteration records, security evaluation, review checklist, evidence index | — | 7 | `test`, `api` green | 1 approval (Xirui) |
| [#22](https://github.com/yuanye820/claimit/pull/22) | `yuanye820` | Replays the security commit on `main` after the merge-path error | 295 | 6 | `test`, `api` green | 1 approval required before merge |

PR #18 was stacked on #16 because both touch the API test suite. It was merged into `feat/15-claim-history`, whose branch was **not protected**, instead of into `main`. That merge bypassed review and left the security change outside `main`. PR #22 replays the same commit on a branch based on `main` and is the reviewed path for the security change.

## Audit history behaviour

- A successful claim appends `(item_id, user_id, +quantity, created_at)`.
- A successful release appends `(item_id, user_id, -quantity, created_at)`.
- The event write shares the transaction with the quantity change. A `409` path raises before the event is added, so rejected attempts leave no history.
- `GET /items/{item_id}/claims` requires a bearer token, returns `404` for an unknown item, and joins the user table for `display_name` only. It does not return emails or password hashes.
- The web client renders the event stream under each item; timestamps are formatted in the browser's local time zone.
- The API test claims one unit, releases it, and asserts the sequence `[("Alice", +1), ("Alice", -1)]`.

## Process improvement: mandatory review

**Baseline (Iteration 1):** 3 PRs, 0 with an approval, review coverage **0/3 (0%)**, mean size **695 changed lines**.

**Intervention (applied to `main` on 23 November 2026):**

- Require a pull request before merging.
- Require **one approving review**; dismiss stale approvals when new commits are pushed.
- Require the `test` and `api` status checks.
- Disallow direct pushes, force pushes and branch deletion.
- Require linear history and conversation resolution.
- Apply the rules to administrators as well.

A review checklist was added at [review-checklist.md](../review-checklist.md). PR authors state a review focus; reviewers must leave at least one substantive comment, not only an approval.

**Iteration 2 measurements (recorded after review):**

| Measure | Baseline | Iteration 2 |
|---|---|---|
| PRs with at least one approval | 0/3 (0%) | 2/3 (67%) |
| Mean changed lines per PR | 695 | 221 (147 and 295) |
| PRs at or below 300 changed lines | 0/3 | 2/2 |
| CI pass rate at submission | 3/3 | 2/2 |
| Time to first review (PR #16) | not measured | about 65 minutes |

The size target was met, which makes a substantive review feasible. Review coverage did **not** reach 100%: #16 and #20 were approved, but #18 merged into an unprotected base branch and received no review. That is a process failure, not a success story. It exposed a gap in the intervention: branch protection was applied to `main` only, so a stacked PR could merge into its base and bypass the rule. PR #22 is the correction; the next iteration should either protect the base of every stacked PR or require all PRs to target `main`.

## Quality evaluation: security

The full scenario, inputs, expected/actual results and residual risks are in [security-evaluation.md](../security-evaluation.md). Summary:

| Finding | Severity | Status |
|---|---|---|
| `POST /lists` and `POST /lists/{id}/items` accepted anonymous requests | High | Fixed; replayed on `main` by PR #22 |
| Token rejection paths had no regression tests | Medium | Fixed; replayed on `main` by PR #22 |
| Password storage had no test proving plaintext absence or unique salts | Medium | Fixed; replayed on `main` by PR #22 |
| Read endpoints have no per-list membership check (IDOR) | Medium | Documented as residual risk |
| No rate limiting on login/registration | Low | Documented as residual risk |
| Development JWT secret has a usable default | Medium | Deployment requirement documented |

The suite grew from 15 to **27 passing tests**; the 12 security tests run in the existing PostgreSQL `api` CI job as well as locally on SQLite.

## What did not go to plan

1. **The security review found a real defect.** The iteration started as “add tests for authentication” and became a fix when anonymous list creation was confirmed. This is a useful outcome, but it also shows that the Iteration 1 definition of done was not applied to every write endpoint.
2. **A stacked PR bypassed the protection rule.** PR #18 was merged into `feat/15-claim-history`, not `main`. The protected branch was not the base branch, so the merge needed no approval and the security change did not reach `main`. The team detected this during the submission audit; PR #22 replays the commit on a branch based on `main`. The lesson is that protecting `main` is necessary but not sufficient when PRs can target another long-lived branch.
3. **Stacked PRs add coordination cost.** #18 could not merge until #16 did, but its base also had to be retargeted at the right moment. The dependency was documented in the PR body but not enforced by tooling.
4. **Android remains outside CI.** The new API contract is exercised by the web client and tests, but no automated Android build runs on pull requests.
5. **Review latency is the remaining process risk.** Branch protection can block a merge, but it cannot make a reviewer respond. If reviews arrive late, the team needs a rotation or a response-time target in the next iteration.

## Retrospective actions

| Action | Owner | Measure |
|---|---|---|
| Merge #22 through a reviewed PR and verify `main` contains the security tests | Liye | `server/tests/test_security.py` on `main`; 27 tests pass |
| Require every PR to target `main`, or protect the base of a stacked PR | All | No merge into an unprotected long-lived branch |
| Keep the 300-line target for the next increment | All | PR size at submission |
| Add an Android build job to CI | Junyu | APK assembles on every PR |
| Add membership checks before sharing lists beyond the demo group | Xirui | 403 for non-members on list reads |
| Set `JWT_SECRET` and add login rate limiting before public deployment | Liye | Deployment checklist item verified |

## Evidence

- Issues: [#15](https://github.com/yuanye820/claimit/issues/15), [#17](https://github.com/yuanye820/claimit/issues/17), [#21](https://github.com/yuanye820/claimit/issues/21)
- Pull requests: [#16](https://github.com/yuanye820/claimit/pull/16), [#18](https://github.com/yuanye820/claimit/pull/18), [#20](https://github.com/yuanye820/claimit/pull/20), [#22](https://github.com/yuanye820/claimit/pull/22)
- Reviews: [PR #16 approval](https://github.com/yuanye820/claimit/pull/16#pullrequestreview-6017512051), [PR #20 approval](https://github.com/yuanye820/claimit/pull/20#pullrequestreview-6017522928)
- Screenshot: [iteration-2-claim-history.png](../evidence/iteration-2-claim-history.png)
- Security scenario: [security-evaluation.md](../security-evaluation.md)
- CI: [Actions](https://github.com/yuanye820/claimit/actions)
