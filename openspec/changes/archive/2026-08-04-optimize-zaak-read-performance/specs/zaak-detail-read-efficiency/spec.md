## ADDED Requirements

### Requirement: Single role list lookup per zaak detail read
When resolving `groep`, `behandelaar`, and `initiatorIdentificatie` for a single `RestZaak`
conversion, the system SHALL retrieve the roles ("rollen") for that zaak from the ZGW ZRC API
at most once, and derive all three fields from that single result set instead of issuing a
separate filtered role list call per field.

#### Scenario: Reading a zaak with a group, a behandelaar, and an initiator
- **WHEN** `readZaak` or `readZaakById` is called for a zaak that has a group role, a behandelaar
  role, and an initiator role
- **THEN** the ZRC "list rollen for zaak" operation is invoked exactly once for that request, and
  the returned `RestZaak` still contains the correct `groep`, `behandelaar`, and
  `initiatorIdentificatie` values

#### Scenario: Reading a zaak with no roles
- **WHEN** `readZaak` is called for a zaak that has no roles at all
- **THEN** the ZRC "list rollen for zaak" operation is invoked exactly once for that request, and
  `groep`, `behandelaar`, and `initiatorIdentificatie` are all `null` in the returned `RestZaak`

### Requirement: Single zaak variables lookup per zaak detail read
When resolving `zaakdata` and `heeftOntvangstbevestigingVerstuurd` for a single `RestZaak`
conversion, the system SHALL query the process engine (Flowable CMMN/BPMN) zaak variables for
that zaak at most once, and derive `heeftOntvangstbevestigingVerstuurd` from that same result
instead of issuing a second, separate variables query.

#### Scenario: Reading a zaak where the confirmation of receipt was sent
- **WHEN** `readZaak` or `readZaakById` is called for a zaak whose process variables include
  `ontvangstbevestigingVerstuurd = true`
- **THEN** the zaak variables are queried exactly once for that request, and the returned
  `RestZaak.heeftOntvangstbevestigingVerstuurd` is `true`

#### Scenario: Reading a zaak with no process variables set
- **WHEN** `readZaak` is called for a zaak that has no zaak variables set yet
- **THEN** the zaak variables are queried exactly once for that request, and the returned
  `RestZaak.heeftOntvangstbevestigingVerstuurd` is `false`
