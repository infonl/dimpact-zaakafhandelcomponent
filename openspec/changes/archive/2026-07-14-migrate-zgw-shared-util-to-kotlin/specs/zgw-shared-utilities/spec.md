## ADDED Requirements

### Requirement: Date conversion
The system SHALL convert a `LocalDate` to a `ZonedDateTime` at start-of-day in the system default time zone.

#### Scenario: Convert a local date to a zoned date-time
- **WHEN** a `LocalDate` is converted via the date-time utility
- **THEN** the result is a `ZonedDateTime` at `00:00:00` in the JVM's system default time zone on that date

### Requirement: Historie waarde formatting
The system SHALL format audit-trail "waarde" values as human-readable Dutch strings, returning `null` when the input is `null`.

#### Scenario: Format a date
- **WHEN** a non-null `LocalDate` is formatted as a historie waarde
- **THEN** the result is the date formatted as `dd-MM-yyyy`

#### Scenario: Format a date-time
- **WHEN** a non-null `ZonedDateTime` is formatted as a historie waarde
- **THEN** the result is the date-time formatted as `dd-MM-yyyy HH:mm` in the system default time zone

#### Scenario: Format a boolean
- **WHEN** a non-null `Boolean` is formatted as a historie waarde
- **THEN** `true` formats as `"Ja"` and `false` formats as `"Nee"`

#### Scenario: Format a null value
- **WHEN** any supported nullable type is `null`
- **THEN** the formatted historie waarde is `null`

### Requirement: JSON-B field-only visibility
The shared `Jsonb` instance SHALL serialize and deserialize using field visibility only, ignoring method visibility.

#### Scenario: Field is visible regardless of accessor visibility
- **WHEN** a value is (de)serialized with the shared `Jsonb` instance
- **THEN** all fields are considered visible and no getter/setter method is considered visible

### Requirement: Lenient URI deserialization
The system SHALL deserialize a JSON string value to a `URI`, returning `null` for blank or JSON-null input instead of throwing.

#### Scenario: Deserialize a non-empty URI string
- **WHEN** the JSON parser's current value is a non-empty URI string
- **THEN** the deserializer returns the parsed `URI`

#### Scenario: Deserialize a blank string
- **WHEN** the JSON parser's current value is an empty string
- **THEN** the deserializer returns `null`

#### Scenario: Deserialize a JSON null in a state that raises IllegalStateException
- **WHEN** reading the parser value raises `IllegalStateException` because the parser is in the `VALUE_NULL` state
- **THEN** the deserializer returns `null` instead of propagating the exception

### Requirement: JSON-B context resolver wiring
The system SHALL expose a single configured `Jsonb` instance, wired with the ZGW-specific custom deserializers and serializers, through a JAX-RS `ContextResolver<Jsonb>`.

#### Scenario: Resolve the shared Jsonb instance
- **WHEN** the context resolver's `getContext` is called for any class
- **THEN** it returns the same configured `Jsonb` instance with the ROL, ZaakObject, and URI deserializers and the delete-GeoJSON-geometry serializer registered
