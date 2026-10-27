---
status: proposed
date: 2024-08-15
decision-makers: Team Genève
tags: [feature-flags, architecture]
---

# Use Flipt as feature flag framework

## Context and Problem Statement

We want to hide new features (epics) on specific environments until they are ready, while continuing to do
trunk-based development. Which feature flag solution do we use?
See [PZ-3208](https://dimpact.atlassian.net/browse/PZ-3208).

## Decision Drivers

* Open source and forever free to self-host
* Robust and proven solution with a large, active open source community
* Feature flags can be turned on or off in real time, without restarting ZAC
* Fit for purpose: basic feature flags without many features we do not need
* Docker- and Kubernetes-based hosting, for both local development and the PodiumD Azure Kubernetes environments
* Good SDKs, preferably supporting the [OpenFeature](https://openfeature.dev/) standard to minimise the dependency on
  the product

## Considered Options

* Flipt
* Unleash
* PostHog

## Decision Outcome

Chosen option: "Flipt", used through the [OpenFeature](https://openfeature.dev/) API so that ZAC has no direct code
dependency on Flipt.

### Consequences

* Good, because Flipt is open source, mature, well documented and fit for purpose, without becoming a bloated product
  that is harder to upgrade or troubleshoot.
* Good, because Flipt has Helm charts and good GitOps support.
* Bad, because feature flags are not strictly needed yet, since ZAC is not yet used in production.
* Bad, because a future PodiumD-wide feature flag solution may require changes to our solution.

### Confirmation

Not checked automatically.

## Pros and Cons of the Options

### Unleash and PostHog

* Good, because they also do what we need.
* Bad, because they provide far more functionality than we need.
* Bad, because some similar products are SaaS only, while self-hosting is the basis of the PodiumD platform.

## More Information

* [Best open source feature flag tools](https://posthog.com/blog/best-open-source-feature-flag-tools)

Migrated from the Confluence decision log, where it had the status DRAFT:
[Use Flipt as feature flag framework in ZAC - DRAFT](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/157253633).
