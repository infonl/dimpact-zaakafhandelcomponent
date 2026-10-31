# Architecture Decision Records

This directory contains the Architecture Decision Records (ADRs) of ZAC, in the [MADR 4](https://adr.github.io/madr/)
format. An ADR records one architecturally significant decision: its context, the options considered, the outcome
and its consequences. See [ADR-0007](0007-record-architecture-decisions-in-git-as-madr.md) for why we use them.

## Index

| ADR                                                                      | Title                                                                    | Status   | Summary                                                                                                |
|--------------------------------------------------------------------------|--------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------|
| [0001](0001-use-mermaid-for-diagrams-as-code.md)                         | Use Mermaid as diagrams-as-code tool in our technical documentation      | accepted | Diagrams in the technical documentation are Mermaid code in Markdown.                                  |
| [0002](0002-automate-release-notes-and-versioning.md)                    | Automate release notes and versioning                                    | accepted | Releases are started by hand with a choice of version bump; tags and release notes are automated.      |
| [0003](0003-validate-fields-in-frontend-and-in-backend-only-when-needed.md) | Always validate fields in the frontend and in the backend only when needed | accepted | The frontend validates all fields; the backend only when it adds value against attacks.                |
| [0004](0004-use-trivy-for-docker-images-and-snyk-for-dependencies.md) | Use Trivy for Docker image scanning and Snyk for dependency scanning | accepted | Trivy scans the published Docker image every weekday; Snyk scans the dependencies every week.           |
| [0005](0005-use-renovate-for-dependency-updates.md)                      | Use Renovate for automated dependency updates                            | accepted | Renovate replaces Dependabot for all dependency updates.                                               |
| [0006](0006-accept-outdated-jackson-in-flowable-until-flowable-8-1.md)   | Accept the outdated Jackson component in Flowable until Flowable 8.1     | accepted | The Jackson warnings in Flowable 8.0.0 are an accepted risk until Flowable 8.1.                        |
| [0007](0007-record-architecture-decisions-in-git-as-madr.md)             | Record architecture decisions in Git as MADR                             | accepted | Architecture decisions are MADR files in `docs/solution-architecture/decisions`, reviewed in pull requests.                        |
| [0008](0008-update-the-ui-without-waiting-for-notifications-on-zac-initiated-changes.md) | Update the UI without waiting for notifications on changes that ZAC initiates | accepted | ZAC sends screen events as soon as an external system confirms a change that ZAC initiated.            |
| [0009](0009-use-helm-unittest-for-unit-tests-of-the-helm-chart.md) | Use helm-unittest for unit tests of the Helm chart | accepted | CI tests the rendered templates of the Helm chart with helm-unittest, without a cluster. |

## When to write an ADR

Write an ADR for a decision that is hard to reverse or that affects more than one part of ZAC. For example:

* a new framework, library or external service, or replacing one
* a new architectural layer, pattern or integration style
* a cross-cutting convention, such as how we validate, log, authorise or version
* an accepted risk, such as a known vulnerability we do not fix yet

Do not write an ADR for a decision that only affects one feature. Use an [OpenSpec](../../../openspec) change for that.

## How to write an ADR

1. Copy [the template](template.md) to `NNNN-short-title.md`, where `NNNN` is the next free number.
2. Fill in the front matter and the sections. Remove the optional sections you do not need: Decision Drivers,
   Confirmation, Pros and Cons of the Options and More Information.
3. Add a row to the index above.
4. Add the ADR to the pull request that implements the decision, or to a pull request of its own when the decision
   is taken before its implementation starts. Set the status to `accepted`, so that merging the pull request accepts
   the decision. Use `proposed` only to merge a decision that is not yet taken.

## Statuses

| Status       | Meaning                                                                           |
|--------------|-----------------------------------------------------------------------------------|
| `proposed`   | The decision is under discussion and is not binding yet.                          |
| `accepted`   | The decision is binding.                                                          |
| `rejected`   | The decision was considered and rejected.                                         |
| `deprecated` | The decision no longer applies, and no other decision replaces it.                |
| `superseded` | A newer ADR replaces the decision. The `superseded-by` front matter names it.     |

## Changing a decision

The body of an accepted ADR is never changed. To change a decision, write a new ADR with `supersedes: NNNN` in its
front matter, and change only the front matter of the old ADR: set `status: superseded` and `superseded-by: NNNN`.
To end a decision without a replacement, set its status to `deprecated`.

The `Check ADRs` workflow enforces these rules. Run it locally with:

```shell
python3 scripts/adr/check-adrs.py --base origin/main
```
