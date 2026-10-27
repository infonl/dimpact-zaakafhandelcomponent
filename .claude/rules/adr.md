---
paths:
  - "docs/solution-architecture/decisions/**"
---

# Architecture Decision Record conventions

Follow [the ADR README](../../docs/solution-architecture/decisions/README.md).
Run `python3 scripts/adr/check-adrs.py --base origin/main` after every change in
`docs/solution-architecture/decisions`; the `Check ADRs` workflow runs the same check.

## Writing a new ADR
- Start from [the template](../../docs/solution-architecture/decisions/template.md) and use the next free four-digit
  number.
- One decision per ADR. Write in English, in present tense, and state facts that the code or the pull request
  confirms. Do not invent decision makers, dates, options or reasons; ask the user when they are unknown.
- Set `decision-makers` to the team or role, never to the names of individual people.
- Fill in Confirmation with the automated check that enforces the decision, such as an ArchUnit rule, a Detekt rule
  or a CI workflow. When none exists, write "Not checked automatically." and mention in the pull request whether a
  check could be added.
- Add the row to the index in `docs/solution-architecture/decisions/README.md`, with the title equal to the
  `# Title` heading.

## Changing an existing ADR
- An ADR with status `proposed` may be changed freely.
- Never change the body of an ADR with any other status, not even to fix a typo. Write a new ADR with
  `supersedes: NNNN`, and in the old ADR change only `status: superseded` and `superseded-by: NNNN`, plus its
  status in the index.
