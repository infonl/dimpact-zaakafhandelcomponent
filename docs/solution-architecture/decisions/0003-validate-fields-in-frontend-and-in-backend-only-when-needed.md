---
status: accepted
date: 2024-01-22
decision-makers: Team Genève, Dimpact
tags: [frontend, backend, validation, security]
---

# Always validate fields in the frontend and in the backend only when needed

## Context and Problem Statement

ZAC passes many input fields on to underlying components, for example to Open Zaak using the ZGW APIs. Users need an
optimal user experience, and the backend must counter possible security threats. Where do we validate these fields?

## Decision Drivers

* Optimal user experience
* Protection against hacking attempts
* Avoiding duplicate validation that adds no value

## Considered Options

* Validate all fields in both the frontend and the backend
* Validate all fields in the frontend, and in the backend only when it adds value

## Decision Outcome

Chosen option: "Validate all fields in the frontend, and in the backend only when it adds value", because frontend
validation gives the best user experience, while backend validation of fields that the underlying components already
validate only adds value against hacking attempts.

* The ZAC frontend validates all fields, including fields that are defined by, for example, the ZGW APIs and that
  ZAC passes on to Open Zaak.
* The ZAC backend validates a field that an underlying component such as Open Zaak already validates only when that
  adds value to prevent hacking attempts. A friendly error message is not needed; it is better to return little
  technical detail to a possible attacker.
* ZAC does not log errors from these validations as errors, so that ZAC monitoring does not raise false alarms.
  These are HTTP responses with status 400.

### Consequences

* Good, because users get immediate, clear feedback on their input.
* Bad, because ZAC needs in-depth knowledge of the field properties of the ZGW APIs and other APIs. When ZAC upgrades
  to a new version of such an API, the field validations must be checked by hand, using a diff of the OpenAPI specs.

### Confirmation

Not checked automatically.

## More Information

Decided in the design sprint session of 18 January 2024:
[20240118 - Ontwerpsprint sessie - input van Lifely INFO](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/32604251/20240118+-+Ontwerpsprint+sessie+-+input+van+Lifely+INFO).

Migrated from the Confluence decision log:
[Always perform field validation in ZAC frontend and only in backend when needed](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/34734095).
