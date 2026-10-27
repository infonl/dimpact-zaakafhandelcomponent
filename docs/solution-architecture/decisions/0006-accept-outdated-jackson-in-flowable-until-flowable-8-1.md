---
status: accepted
date: 2026-10-13
decision-makers: Dimpact architecture team, Team Genève
tags: [security, dependencies, flowable, accepted-risk]
---

# Accept the outdated Jackson component in Flowable until Flowable 8.1

## Context and Problem Statement

Our security scan reports 8 warnings about an outdated component, Jackson 3.0.1. The component is not part of our own
code: it is bundled inside Flowable 8.0.0, the workflow engine that runs our case processes and tasks. Do we replace
the component ourselves, change our Flowable version, or accept the warnings?

## Decision Drivers

* The case processes and tasks that users depend on every day must keep working
* The makers of Flowable have not tested Flowable 8.0.0 with a newer Jackson version
* Flowable is not directly reachable from the internet: all user input first passes through ZAC, which checks it and
  filters out harmful data before it reaches the engine, so the weaknesses behind the warnings are hard to exploit
* Flowable has confirmed that version 8.1 includes the updated component
  ([flowable-engine#4304](https://github.com/flowable/flowable-engine/issues/4304))

## Considered Options

* Replace the Jackson component in Flowable ourselves
* Change our Flowable version
* Accept the warnings until Flowable 8.1

## Decision Outcome

Chosen option: "Accept the warnings until Flowable 8.1", because the risk of running an untested combination of
components is greater than the risk from the warnings.

### Consequences

* Good, because we do not run an untested combination of components that could break case processes and tasks.
* Bad, because the security scan keeps reporting these 8 warnings until we upgrade to Flowable 8.1.

### Confirmation

This acceptance expires when Flowable 8.1 is released. Flowable has not announced a release date; based on past
releases we expect it between late 2026 and early 2027. We then upgrade to Flowable 8.1 and mark this ADR as
deprecated.

## More Information

Migrated from the Confluence decision log:
[Accepted Risk: Outdated Jackson Component in Flowable Until Flowable 8.1](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/1336180838).
