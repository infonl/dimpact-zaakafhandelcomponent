## ADDED Requirements

### Requirement: PersonenQueryResponseJsonbDeserializer deserializes all supported response types
The deserializer SHALL return the correct concrete subtype of `PersonenQueryResponse` for each of the six supported `type` values.

#### Scenario: Deserialize RaadpleegMetBurgerservicenummer
- **WHEN** `deserialize` is called with a JSON object whose `type` is `"RaadpleegMetBurgerservicenummer"`
- **THEN** a `RaadpleegMetBurgerservicenummerResponse` instance is returned

#### Scenario: Deserialize ZoekMetGeslachtsnaamEnGeboortedatum
- **WHEN** `deserialize` is called with JSON whose `type` is `"ZoekMetGeslachtsnaamEnGeboortedatum"`
- **THEN** a `ZoekMetGeslachtsnaamEnGeboortedatumResponse` instance is returned

#### Scenario: Deserialize ZoekMetNaamEnGemeenteVanInschrijving
- **WHEN** `deserialize` is called with JSON whose `type` is `"ZoekMetNaamEnGemeenteVanInschrijving"`
- **THEN** a `ZoekMetNaamEnGemeenteVanInschrijvingResponse` instance is returned

#### Scenario: Deserialize ZoekMetNummeraanduidingIdentificatie
- **WHEN** `deserialize` is called with JSON whose `type` is `"ZoekMetNummeraanduidingIdentificatie"`
- **THEN** a `ZoekMetNummeraanduidingIdentificatieResponse` instance is returned

#### Scenario: Deserialize ZoekMetPostcodeEnHuisnummer
- **WHEN** `deserialize` is called with JSON whose `type` is `"ZoekMetPostcodeEnHuisnummer"`
- **THEN** a `ZoekMetPostcodeEnHuisnummerResponse` instance is returned

#### Scenario: Deserialize ZoekMetStraatHuisnummerEnGemeenteVanInschrijving
- **WHEN** `deserialize` is called with JSON whose `type` is `"ZoekMetStraatHuisnummerEnGemeenteVanInschrijving"`
- **THEN** a `ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse` instance is returned

### Requirement: PersonenQueryResponseJsonbDeserializer throws on unknown type
The deserializer SHALL throw `InputValidationFailedException` when the `type` field contains an unrecognised value.

#### Scenario: Unknown type throws
- **WHEN** `deserialize` is called with JSON whose `type` is an unknown string (e.g. `"OnbekendType"`)
- **THEN** `InputValidationFailedException` is thrown
