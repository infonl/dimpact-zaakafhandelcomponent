## ADDED Requirements

### Requirement: AbstractVerblijfplaatsJsonbDeserializer deserializes all supported verblijfplaats types
The deserializer SHALL return the correct concrete subtype of `AbstractVerblijfplaats` for each of the four supported `type` values.

#### Scenario: Deserialize VerblijfplaatsBuitenland
- **WHEN** `deserialize` is called with JSON whose `type` is `"VerblijfplaatsBuitenland"`
- **THEN** a `VerblijfplaatsBuitenland` instance is returned

#### Scenario: Deserialize Adres
- **WHEN** `deserialize` is called with JSON whose `type` is `"Adres"`
- **THEN** an `Adres` instance is returned

#### Scenario: Deserialize VerblijfplaatsOnbekend
- **WHEN** `deserialize` is called with JSON whose `type` is `"VerblijfplaatsOnbekend"`
- **THEN** a `VerblijfplaatsOnbekend` instance is returned

#### Scenario: Deserialize Locatie
- **WHEN** `deserialize` is called with JSON whose `type` is `"Locatie"`
- **THEN** a `Locatie` instance is returned

#### Scenario: Unknown verblijfplaats type throws
- **WHEN** `deserialize` is called with JSON whose `type` is an unknown string
- **THEN** `InputValidationFailedException` is thrown

### Requirement: AbstractDatumJsonbDeserializer deserializes all supported datum types
The deserializer SHALL return the correct concrete subtype of `AbstractDatum` for each of the four supported `type` values.

#### Scenario: Deserialize VolledigeDatum
- **WHEN** `deserialize` is called with JSON whose `type` is `"Datum"`
- **THEN** a `VolledigeDatum` instance is returned

#### Scenario: Deserialize DatumOnbekend
- **WHEN** `deserialize` is called with JSON whose `type` is `"DatumOnbekend"`
- **THEN** a `DatumOnbekend` instance is returned

#### Scenario: Deserialize JaarDatum
- **WHEN** `deserialize` is called with JSON whose `type` is `"JaarDatum"`
- **THEN** a `JaarDatum` instance is returned

#### Scenario: Deserialize JaarMaandDatum
- **WHEN** `deserialize` is called with JSON whose `type` is `"JaarMaandDatum"`
- **THEN** a `JaarMaandDatum` instance is returned

#### Scenario: Unknown datum type throws
- **WHEN** `deserialize` is called with JSON whose `type` is an unknown string
- **THEN** `InputValidationFailedException` is thrown
