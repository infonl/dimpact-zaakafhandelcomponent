## ADDED Requirements

### Requirement: RestOpenbareRuimteConverter returns null for null inputs
All `convertToREST` overloads and `convertToZaakobject` SHALL return null when the primary input is null (or when its `objectIdentificatie` is null).

#### Scenario: convertToREST(OpenbareRuimteIOHalBasis, AdresIOHal) returns null for null input
- **WHEN** `convertToREST(null, adres)` is called
- **THEN** null is returned

#### Scenario: convertToREST(OpenbareRuimteIOHalBasis) returns null for null input
- **WHEN** `convertToREST(null as OpenbareRuimteIOHalBasis?)` is called
- **THEN** null is returned

#### Scenario: convertToREST(ZaakobjectOpenbareRuimte) returns null when objectIdentificatie is null
- **WHEN** `convertToREST` is called with a `ZaakobjectOpenbareRuimte` whose `objectIdentificatie` is null
- **THEN** null is returned

### Requirement: RestOpenbareRuimteConverter converts OpenbareRuimteIOHalBasis with an adres
When both `openbareRuimteIO` and `adres` are provided, `convertToREST` SHALL populate `woonplaatsNaam` from `adres.woonplaatsNaam`.

#### Scenario: Convert with adres present
- **WHEN** `convertToREST(openbareRuimteIO, adres)` is called with non-null adres
- **THEN** the returned `RESTOpenbareRuimte` has `woonplaatsNaam` from the adres

#### Scenario: Convert without adres falls back to openbareRuimte
- **WHEN** `convertToREST(openbareRuimteIO, null)` is called
- **THEN** `woonplaatsNaam` is taken from `openbareRuimteIO.openbareRuimte.ligtIn`

### Requirement: RestOpenbareRuimteConverter converts a ZaakobjectOpenbareRuimte to REST
`convertToREST(ZaakobjectOpenbareRuimte)` SHALL map `url`, `identificatie`, `naam`, and `woonplaatsNaam` from the zaakobject's `objectIdentificatie`.

#### Scenario: Convert ZaakobjectOpenbareRuimte
- **WHEN** `convertToREST` is called with a valid `ZaakobjectOpenbareRuimte`
- **THEN** the returned `RESTOpenbareRuimte` has all fields populated from the object identificatie

### Requirement: RestOpenbareRuimteConverter converts a RESTOpenbareRuimte to a ZaakobjectOpenbareRuimte
`convertToZaakobject` SHALL build a `ZaakobjectOpenbareRuimte` containing the zaak URL, the openbareRuimte URL, and an `ObjectOpenbareRuimte` with identificatie, naam, and woonplaatsNaam.

#### Scenario: Convert to zaakobject
- **WHEN** `convertToZaakobject` is called with a `RESTOpenbareRuimte` and a `Zaak`
- **THEN** the returned `ZaakobjectOpenbareRuimte` has the correct zaak URL, openbareRuimte URL, and object identificatie fields
