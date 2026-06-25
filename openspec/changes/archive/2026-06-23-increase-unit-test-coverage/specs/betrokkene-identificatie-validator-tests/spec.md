## ADDED Requirements

### Requirement: BetrokkeneIdentificatieValidator validates BSN type
For `IdentificatieType.BSN`, the identification SHALL be valid when `temporaryPersonId` is set and both `kvkNummer` and `vestigingsnummer` are blank or null.

#### Scenario: BSN valid — temporaryPersonId set, kvkNummer and vestigingsnummer absent
- **WHEN** `isValid` is called with a `BetrokkeneIdentificatie` of type BSN with a non-null `temporaryPersonId` and blank `kvkNummer` and `vestigingsnummer`
- **THEN** `true` is returned

#### Scenario: BSN invalid — temporaryPersonId missing
- **WHEN** `isValid` is called with a BSN identification where `temporaryPersonId` is null
- **THEN** `false` is returned

#### Scenario: BSN invalid — kvkNummer present
- **WHEN** `isValid` is called with a BSN identification where `kvkNummer` is non-blank
- **THEN** `false` is returned

### Requirement: BetrokkeneIdentificatieValidator validates VN type
For `IdentificatieType.VN`, the identification SHALL be valid when both `kvkNummer` and `vestigingsnummer` are non-blank and `temporaryPersonId` is null.

#### Scenario: VN valid
- **WHEN** `isValid` is called with a VN identification with non-blank `kvkNummer` and `vestigingsnummer` and null `temporaryPersonId`
- **THEN** `true` is returned

#### Scenario: VN invalid — kvkNummer blank
- **WHEN** `isValid` is called with a VN identification where `kvkNummer` is blank
- **THEN** `false` is returned

#### Scenario: VN invalid — temporaryPersonId set
- **WHEN** `isValid` is called with a VN identification where `temporaryPersonId` is non-null
- **THEN** `false` is returned

### Requirement: BetrokkeneIdentificatieValidator validates RSIN type
For `IdentificatieType.RSIN`, the identification SHALL be valid when `kvkNummer` is non-blank, `temporaryPersonId` is null, and `vestigingsnummer` is blank or null.

#### Scenario: RSIN valid
- **WHEN** `isValid` is called with an RSIN identification with non-blank `kvkNummer`, null `temporaryPersonId`, and blank `vestigingsnummer`
- **THEN** `true` is returned

#### Scenario: RSIN invalid — vestigingsnummer present
- **WHEN** `isValid` is called with an RSIN identification where `vestigingsnummer` is non-blank
- **THEN** `false` is returned

### Requirement: BetrokkeneIdentificatieValidator rejects null input
The validator SHALL return `false` when called with a null value.

#### Scenario: Null input
- **WHEN** `isValid` is called with `null`
- **THEN** `false` is returned
