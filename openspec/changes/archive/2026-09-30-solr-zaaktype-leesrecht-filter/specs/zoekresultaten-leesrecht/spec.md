## Purpose

Ensures that zoekresultaten, werklijsten and the Koppelen search list only show zaken, taken and documenten
of zaaktypen the user is allowed to read, so users never see cases they cannot open.

## ADDED Requirements

### Requirement: Search results only contain zaaktypen the user holds a read role for

A werklijst, zoekresultaat or Koppelen search query SHALL only return zaken, taken and documenten of a
zaaktype for which the requesting user holds at least one read role (`raadpleger`, `behandelaar`,
`coordinator`, `recordmanager` or `beheerder`), either assigned for that zaaktype or held as an overall role
(a role not scoped to a specific zaaktype). Holding only other application roles for a zaaktype, such as
`brp_zoeken` or `zaakspecifiek_geautoriseerd`, SHALL NOT make that zaaktype's zaken, taken or documenten
appear in the results. The existing exclusion of zaakspecifiek geautoriseerde zaken SHALL continue to apply
on top of this rule.

#### Scenario: A user with only a non-read role for a zaaktype does not find its zaken
- **WHEN** a user holds only `brp_zoeken` for zaaktype A and `behandelaar` for zaaktype B, and performs a
  search or requests a werklijst that would otherwise match zaken of both zaaktypes
- **THEN** zaken of zaaktype B are present in the results and zaken of zaaktype A are absent

#### Scenario: A user with only a non-read role for a zaaktype does not find its taken and documenten
- **WHEN** a user holds only `zaakspecifiek_geautoriseerd` for zaaktype A and performs a search or requests a
  werklijst that would otherwise match taken or documenten of zaken of zaaktype A
- **THEN** those taken and documenten are absent from the results

#### Scenario: A user with a read role for a zaaktype keeps seeing its zaken
- **WHEN** a user holds `raadpleger` (or `behandelaar`, `coordinator`, `recordmanager` or `beheerder`) for
  zaaktype A, possibly together with non-read roles, and performs a search that would match zaken of
  zaaktype A
- **THEN** those zaken are present in the results, exactly as before this change

#### Scenario: A read role held as an overall role admits the zaaktypen the user has roles for
- **WHEN** a user holds `raadpleger` as an overall role and only `brp_zoeken` for zaaktype A, and performs a
  search that would match zaken of zaaktype A
- **THEN** those zaken are present in the results

#### Scenario: A user without any read role finds nothing
- **WHEN** a user holds only non-read roles, for every zaaktype they have roles for and as overall roles, and
  performs a search
- **THEN** the results are empty

### Requirement: The Koppelen search list does not offer zaken the user cannot read

The list of zaken offered when relating a zaak (Koppelen) SHALL NOT contain zaken of a zaaktype for which the
user holds no read role, and SHALL therefore not report "not authorised to read" as a reason why a listed
zaak cannot be linked. Relating two zaken SHALL still be refused when the user cannot read the target zaak,
regardless of how the request reached the server.

#### Scenario: A zaak the user cannot read is absent from the Koppelen list
- **WHEN** a user who holds only `brp_zoeken` for zaaktype A opens a zaak of zaaktype B, chooses Koppelen with
  relation type Gerelateerd and searches for a zaak of zaaktype A
- **THEN** that zaak of zaaktype A is absent from the list

#### Scenario: No not-authorised-to-read reason is reported
- **WHEN** the Koppelen search returns zaken that cannot be linked
- **THEN** none of them carries a "not authorised to read" reason

#### Scenario: Relating to an unreadable zaak is still refused
- **WHEN** a user requests to relate a zaak to a zaak they are not allowed to read
- **THEN** the request is refused with an authorisation error and no relation is created
