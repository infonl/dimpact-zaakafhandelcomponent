---
status: accepted
date: 2026-10-27
decision-makers: Team Genève
tags: [documentation, process, ai]
---

# Record architecture decisions in Git as MADR

## Context and Problem Statement

Our technical decision log lived in Confluence, away from the code it governs. Decisions were not reviewed in the
pull requests of the code changes they caused, and AI agents working in this repository could not read them.
Where and how do we record architecture decisions?
See [PZ-13291](https://dimpact.atlassian.net/browse/PZ-13291).

## Decision Drivers

* Decisions are reviewed and versioned together with the code
* AI agents read and follow the decisions, and propose new ones when appropriate
* A strict, well-known format that is easy to check automatically

## Considered Options

* Keep the decision log in Confluence
* Architecture Decision Records in Git, using [MADR](https://adr.github.io/madr/) 4
* Architecture Decision Records in Git, using the short Nygard format

## Decision Outcome

Chosen option: "Architecture Decision Records in Git, using MADR 4", because MADR is a widely used Markdown format
with front matter that tools and agents can parse, and its sections (context, drivers, options, outcome, consequences,
confirmation) match what our Confluence decisions already contained.

* Every ADR is a file `docs/solution-architecture/decisions/NNNN-short-title.md`, based on [the template](template.md).
* An ADR is added in the same pull request as the change it governs, or in a pull request of its own when the
  decision is taken before its implementation starts. Merging the pull request accepts the decision.
* The body of an accepted ADR is never changed. A changed decision is a new ADR that supersedes the old one.
* [The index](README.md) lists every ADR.

### Consequences

* Good, because decisions are close to the code and reviewed in pull requests.
* Good, because AI agents can read the index and follow the relevant decisions.
* Bad, because readers without access to GitHub, such as stakeholders outside the team, lose the Confluence view.

### Confirmation

The workflow `.github/workflows/check-adrs.yml` runs `scripts/adr/check-adrs.py`, which checks the file names, front
matter, required sections and the index, and that the body of an accepted ADR is not changed.

## More Information

The Confluence decision log is replaced by a link to this directory:
[Technical decision log](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/10551350/Technical+decision+log).
