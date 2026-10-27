---
status: accepted
date: 2024-11-11
decision-makers: Team Genève
tags: [dependencies, tooling, ci]
---

# Use Renovate for automated dependency updates

## Context and Problem Statement

We used Dependabot to keep the libraries of ZAC up to date. Our requirements now cover a broader range of dependencies,
not only libraries but also Docker images. Dependabot does not fully cover these, and we had already introduced
Renovate for the unsupported dependencies. See [PZ-4403](https://dimpact.atlassian.net/browse/PZ-4403).

## Decision Drivers

* Support for all types of dependencies we use, including Docker images
* Less tooling to maintain

## Considered Options

* Dependabot
* Renovate

## Decision Outcome

Chosen option: "Renovate", because it supports all the dependency types we use, while Dependabot alone does not.
Maintaining two tools costs time that we would rather spend on building features and fixing bugs.

### Consequences

* Good, because one tool handles all dependency updates, with flexible configuration.
* Bad, because migrating away from Dependabot costs time upfront.

### Confirmation

`renovate.json` configures Renovate, and the repository has no Dependabot configuration.

## Pros and Cons of the Options

### Dependabot

* Good, because it is integrated with GitHub, with security alerts and compatibility scores.
* Good, because it is cheap to set up.
* Bad, because its language support is limited.
* Bad, because it can produce overwhelming notifications and dependency conflicts.

### Renovate

* Good, because it supports a wide range of languages and package managers.
* Good, because it has customisable configuration, batch updates and detailed reporting.
* Bad, because the setup is more complex, with a learning curve.

## More Information

Follow-up story: [PZ-4502](https://dimpact.atlassian.net/browse/PZ-4502).

Migrated from the Confluence decision log:
[Use Renovate for automated dependency updates](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/222822484).
