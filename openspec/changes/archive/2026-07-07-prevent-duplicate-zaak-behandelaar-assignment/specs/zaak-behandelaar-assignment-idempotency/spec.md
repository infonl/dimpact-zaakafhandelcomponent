## ADDED Requirements

### Requirement: At most one Behandelaar MEDEWERKER role per zaak

After any call to `assignZaak` or `assignZaakToLoggedInUser`, OpenZaak SHALL contain exactly zero or one role with `betrokkeneType=MEDEWERKER` and `omschrijvingGeneriek=BEHANDELAAR` for the targeted zaak.

#### Scenario: Single assignment creates exactly one Behandelaar role
- **WHEN** `assignZaak` is called for a zaak with a valid `userName`
- **THEN** OpenZaak contains exactly one MEDEWERKER Behandelaar role for that zaak linked to the specified user

#### Scenario: Reassignment replaces the existing Behandelaar role
- **WHEN** a zaak already has one MEDEWERKER Behandelaar role and `assignZaak` is called with a different `userName`
- **THEN** OpenZaak contains exactly one MEDEWERKER Behandelaar role for that zaak linked to the new user, and the previous role no longer exists

#### Scenario: Assigning with no user removes all Behandelaar MEDEWERKER roles
- **WHEN** `assignZaak` is called for a zaak with `userName` null or empty
- **THEN** OpenZaak contains zero MEDEWERKER Behandelaar roles for that zaak

### Requirement: Concurrent assignment calls for the same zaak are serialized

The system SHALL serialize concurrent invocations of `assignZaak` (or `assignZaakToLoggedInUser`) for the same zaak UUID so that the final state in OpenZaak reflects exactly one of the concurrent requests — not a combination of both.

#### Scenario: Two concurrent assignments result in exactly one Behandelaar role
- **WHEN** two requests call `assignZaak` for the same zaak UUID concurrently within the same JVM
- **THEN** OpenZaak contains exactly one MEDEWERKER Behandelaar role for that zaak after both requests complete

#### Scenario: Concurrent assignments do not interfere with other zaken
- **WHEN** two requests call `assignZaak` concurrently for different zaak UUIDs
- **THEN** each zaak independently has exactly one MEDEWERKER Behandelaar role after both requests complete

### Requirement: Pre-existing duplicate Behandelaar roles are cleaned up on assignment

If OpenZaak already contains more than one MEDEWERKER Behandelaar role for a zaak (due to a prior race), a subsequent call to `assignZaak` or `assignZaakToLoggedInUser` for that zaak SHALL remove all duplicate roles and leave exactly one (or zero if no user is specified).

#### Scenario: Assignment cleans up pre-existing duplicates
- **WHEN** a zaak has two or more MEDEWERKER Behandelaar roles in OpenZaak
- **AND** `assignZaak` is called for that zaak with a valid `userName`
- **THEN** OpenZaak contains exactly one MEDEWERKER Behandelaar role for that zaak after the call

#### Scenario: Duplicate cleanup is logged as a warning
- **WHEN** `assignZaak` detects more than one existing MEDEWERKER Behandelaar role for a zaak before assigning
- **THEN** a WARNING-level log entry is emitted indicating the number of duplicate roles found and the zaak UUID
