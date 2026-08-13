## ADDED Requirements

### Requirement: Rol equality contract
The system SHALL treat two `Rol` instances as equal only when they are of the same runtime class, have the same `roltype` and `betrokkeneType`, and their betrokkene identity (as defined per subclass) is equal.

#### Scenario: Different runtime classes are never equal
- **WHEN** a `RolMedewerker` is compared to a `RolNatuurlijkPersoon`
- **THEN** `equals` returns `false`

#### Scenario: Same roltype, betrokkeneType, and identity are equal
- **WHEN** two `Rol` instances of the same subclass have the same `roltype`, `betrokkeneType`, and equal betrokkene identity
- **THEN** `equals` returns `true` and `hashCode` is equal for both instances

#### Scenario: Different roltype makes instances unequal
- **WHEN** two `Rol` instances of the same subclass have equal betrokkene identity but different `roltype`
- **THEN** `equals` returns `false`

### Requirement: RolNatuurlijkPersoon identity resolution
The system SHALL resolve a `RolNatuurlijkPersoon`'s identity by checking `anpIdentificatie`, then `inpANummer`, then `inpBsn`, in that order, using the first field that is populated on either side of the comparison.

#### Scenario: Identity resolves via BSN when only BSN is populated
- **WHEN** two `NatuurlijkPersoonIdentificatie` values have equal `inpBsn` and no `anpIdentificatie`/`inpANummer`
- **THEN** `equalBetrokkeneIdentificatie` returns `true`

#### Scenario: getIdentificatienummer returns the BSN
- **WHEN** `getIdentificatienummer()` is called on a `RolNatuurlijkPersoon` with a populated `inpBsn`
- **THEN** it returns that `inpBsn` value

#### Scenario: getNaam falls back to identificatienummer
- **WHEN** `getBetrokkeneIdentificatie().getVoorvoegselGeslachtsnaam()` is blank
- **THEN** `getNaam()` returns the same value as `getIdentificatienummer()`

### Requirement: RolMedewerker naam and identity resolution
The system SHALL build a `RolMedewerker`'s display naam from `voorletters`, `voorvoegselAchternaam`, and `achternaam` when `achternaam` is non-blank, and fall back to `identificatie` otherwise; identity SHALL be based on `identificatie`.

#### Scenario: Naam is composed from name parts when achternaam is present
- **WHEN** a `MedewerkerIdentificatie` has `voorletters = "J."`, `voorvoegselAchternaam = "van"`, and `achternaam = "Berg"`
- **THEN** `getNaam()` returns `"J. van Berg"`

#### Scenario: Naam falls back to identificatie when achternaam is blank
- **WHEN** a `MedewerkerIdentificatie` has a blank `achternaam`
- **THEN** `getNaam()` returns `getIdentificatie()`

#### Scenario: Identity is based on identificatie
- **WHEN** two `MedewerkerIdentificatie` values have equal `identificatie`
- **THEN** `equalBetrokkeneIdentificatie` returns `true` regardless of other fields

### Requirement: RolNietNatuurlijkPersoon identity number precedence
The system SHALL resolve a `RolNietNatuurlijkPersoon`'s identificatienummer by preferring a KVK-nummer without a vestigingsnummer, then an RSIN (`innNnpId`), then a vestigingsnummer, in that order.

#### Scenario: KVK-only initiator returns the KVK-nummer
- **WHEN** `kvkNummer` is populated and `vestigingsNummer` is blank
- **THEN** `getIdentificatienummer()` returns the `kvkNummer` value

#### Scenario: Legacy RSIN-only initiator returns the RSIN
- **WHEN** `kvkNummer` is blank and `innNnpId` is populated
- **THEN** `getIdentificatienummer()` returns the `innNnpId` value

#### Scenario: Vestiging-type initiator with both KVK and vestigingsnummer returns the vestigingsnummer
- **WHEN** both `kvkNummer` and `vestigingsNummer` are populated
- **THEN** `getIdentificatienummer()` returns the `vestigingsNummer` value

#### Scenario: Naam falls back to identificatienummer when statutaireNaam is blank
- **WHEN** `statutaireNaam` is blank
- **THEN** `getNaam()` returns the same value as `getIdentificatienummer()`

### Requirement: RolOrganisatorischeEenheid naam and identity resolution
The system SHALL resolve a `RolOrganisatorischeEenheid`'s identity and identificatienummer from `identificatie`, and fall back naam to `identificatienummer` when `naam` is blank.

#### Scenario: Naam falls back to identificatienummer when naam is blank
- **WHEN** `getBetrokkeneIdentificatie().getNaam()` is blank
- **THEN** `getNaam()` returns the same value as `getIdentificatienummer()`

#### Scenario: Identity is based on identificatie
- **WHEN** two `OrganisatorischeEenheidIdentificatie` values have equal `identificatie`
- **THEN** `equalBetrokkeneIdentificatie` returns `true`

### Requirement: RolVestiging naam and identity resolution
The system SHALL join all `handelsnaam` entries of a `RolVestiging` with `"; "` to form its naam, falling back to `identificatienummer` (the `vestigingsNummer`) when the joined result is blank.

#### Scenario: Naam joins multiple handelsnamen
- **WHEN** `handelsnaam` contains `["Bakkerij Jansen", "Jansen Beheer"]`
- **THEN** `getNaam()` returns `"Bakkerij Jansen; Jansen Beheer"`

#### Scenario: Naam falls back to vestigingsnummer when handelsnaam is absent
- **WHEN** `handelsnaam` is `null` or empty
- **THEN** `getNaam()` returns the same value as `getIdentificatienummer()`, i.e. the `vestigingsNummer`

#### Scenario: Identity is based on vestigingsnummer
- **WHEN** two `VestigingIdentificatie` values have equal `vestigingsNummer`
- **THEN** `equalBetrokkeneIdentificatie` returns `true`

### Requirement: ZaakInformatieobject zaak UUID extraction
The system SHALL derive a `ZaakInformatieobject`'s zaak UUID from the trailing UUID segment of its `zaak` URI.

#### Scenario: Extract UUID from the zaak URI
- **WHEN** `getZaakUUID()` is called on a `ZaakInformatieobject` whose `zaak` URI ends in a valid UUID
- **THEN** it returns that UUID

### Requirement: Zaakobject BAG object classification
The system SHALL classify a `Zaakobject` as a BAG object when its `objectType` is `ADRES`, `PAND`, `OPENBARE_RUIMTE`, or `WOONPLAATS`, or when `objectType` is `OVERIGE` and `objectTypeOverige` equals the nummeraanduiding "overige" marker; all other combinations SHALL be classified as not a BAG object.

#### Scenario: ADRES is always a BAG object
- **WHEN** `objectType` is `ADRES`
- **THEN** `isBagObject()` returns `true`

#### Scenario: OVERIGE with matching marker is a BAG object
- **WHEN** `objectType` is `OVERIGE` and `objectTypeOverige` equals `ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE`
- **THEN** `isBagObject()` returns `true`

#### Scenario: OVERIGE without matching marker is not a BAG object
- **WHEN** `objectType` is `OVERIGE` and `objectTypeOverige` does not equal `ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE`
- **THEN** `isBagObject()` returns `false`

#### Scenario: PRODUCTAANVRAAG is not a BAG object
- **WHEN** `objectType` is any value other than `ADRES`, `PAND`, `OPENBARE_RUIMTE`, `WOONPLAATS`, or `OVERIGE`
- **THEN** `isBagObject()` returns `false`

### Requirement: Zaakobject equality contract
The system SHALL treat two `Zaakobject` instances as equal only when they are of the same runtime class and have equal `zaak`, `object`, `objectType`, and `objectTypeOverige`.

#### Scenario: Equal core fields on the same subclass are equal
- **WHEN** two `Zaakobject` instances of the same subclass have equal `zaak`, `object`, `objectType`, and `objectTypeOverige`
- **THEN** `equals` returns `true`

#### Scenario: Different runtime classes are never equal
- **WHEN** a `ZaakobjectAdres` is compared to a `ZaakobjectPand`
- **THEN** `equals` returns `false`

### Requirement: Zaakobject waarde delegation
The system SHALL derive each `Zaakobject*` leaf class's `getWaarde()` from a specific field of its wrapped object-identificatie value.

#### Scenario: ZaakobjectAdres waarde is the adres identificatie
- **WHEN** `getWaarde()` is called on a `ZaakobjectAdres` wrapping an `ObjectAdres`
- **THEN** it returns that `ObjectAdres`'s `identificatie`

### Requirement: ZaakListParameters enum and set query-parameter mapping
The system SHALL map `ZaakListParameters`' enum and set-typed fields to query-string values: single enums render via `toString()` or `null` when unset, and enum sets render as a comma-joined list of `toString()` values or `null` when empty.

#### Scenario: Single enum renders via toString
- **WHEN** `archiefnominatie` is set to a non-null `ArchiefnominatieEnum` value
- **THEN** `getArchiefnominatie()` returns that value's `toString()`

#### Scenario: Unset single enum renders as null
- **WHEN** `archiefnominatie` is unset
- **THEN** `getArchiefnominatie()` returns `null`

#### Scenario: Non-empty enum set renders as a comma-joined string
- **WHEN** `archiefnominatieIn` contains multiple `ArchiefnominatieEnum` values
- **THEN** `getArchiefnominatieIn()` returns their `toString()` values joined with `,`

#### Scenario: Empty enum set renders as null
- **WHEN** `archiefnominatieIn` is `null` or empty
- **THEN** `getArchiefnominatieIn()` returns `null`
