# Team Working Agreement (Initial)

**Project:** ClaimIt — shared list with conflict-safe claims
**Team:** 3 members
**Duration:** two two-week iterations

## Roles

| Member | Role | Primary responsibility |
|---|---|---|
| A | Product Owner | Backlog order, acceptance criteria, Android client |
| B | Tech Lead | Firestore model, claim transaction, security rules |
| C | QA/DevOps | Web client, CI, property and emulator tests, evidence index |

Everyone develops, reviews at least one pull request per iteration, and keeps their own evidence links current.

## Cadence

- Stand-up: Monday, Wednesday and Friday at 20:00, 15 minutes.
- Planning: first day of each iteration, 60 minutes.
- Review and retrospective: last day of each iteration, 45 minutes.
- Decisions are recorded in GitHub Issues; meeting notes go to `docs/meetings/`.

## Workflow

- `main` is protected; no direct pushes.
- Branches: `feat/<issue>-short-name` or `fix/<issue>-short-name`.
- Pull requests: at most ~300 changed lines where possible; use the PR template; one approving review; CI green; squash merge.
- Every PR links its issue. Every issue has testable Given/When/Then acceptance criteria.

## Definition of Done

- Acceptance criteria verified.
- Unit or integration tests added.
- Documentation updated.
- Reviewed by another member.
- CI green.
- No secrets committed.

## Evidence

Each member owns at least one issue per iteration and reviews at least one PR per iteration. Evidence includes issue assignment, PR authorship, review comments, test or documentation authorship, and meeting notes.

## Conflict resolution

Discuss in the issue. If there is no agreement within 24 hours, the Product Owner decides and records the rationale. Escalate to the lecturer if still blocked.

## AI use

AI tools may be used for boilerplate, debugging and explanation. All generated code must be reviewed and tested by its author. Disclose material AI assistance in the PR description and follow the module policy.
