---
status: accepted
date: 2024-03-11
decision-makers: Dimpact architecture team, Team Genève, PodiumD Zaak product owner
tags: [iam, security, architecture]
---

# Keep the current IAM architecture for the ZAC MVP

## Context and Problem Statement

For the upcoming ZAC/PodiumD Zaak MVP we had to decide whether to change the ZAC IAM architecture, in particular how
employees, their roles and their group memberships are managed.

## Decision Drivers

* Too much is still unclear about the registration of employees
* We want to learn from the KISS project first
* In the future there will be a standardised, PodiumD-wide registration of employees with a proper management interface

## Considered Options

* Keep the current IAM architecture
* Change the IAM architecture for the MVP

## Decision Outcome

Chosen option: "Keep the current IAM architecture", because there is too much uncertainty about the registration of
employees to change it before the MVP.

* Keycloak is used for authentication against AD and ADFS. No assumptions are made about the roles of employees; these
  are entered in Keycloak.
* The group memberships of employees stay in LDAP. They are managed in Keycloak and synchronised with LDAP.
* The Lifely test environment uses OpenLDAP for authentication, where the foreseen Dimpact PodiumD test environment
  uses AD/ADFS.

### Consequences

* Good, because the MVP is not delayed by an IAM redesign.
* Bad, because the ZAC IAM architecture will need to change in the future. Once the PodiumD-wide employee registration
  exists, ZAC no longer needs LDAP.
* Bad, because municipalities manage users twice, in Keycloak and in ADFS, similar to the e-Suite. Synchronisation of
  the username and email address reduces this somewhat.

### Confirmation

Not checked automatically.

## More Information

Decided in the ZAC IAM architecture meeting between Team Genève, the PodiumD Zaak product owner and the Dimpact
architecture team. See also [IAM architecture](../solution-architecture/iamArchitecture.md).

Migrated from the Confluence decision log:
[Use current ZAC IAM architecture for ZAC MVP](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/56426498).
