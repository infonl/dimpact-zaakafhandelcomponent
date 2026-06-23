## ADDED Requirements

### Requirement: addHistorieRegel extension functions add a HistoryLine only when values differ
All overloads of `addHistorieRegel` SHALL append a `HistoryLine` to the receiver list when the old and new values are not equal, and SHALL not modify the list when they are equal.

#### Scenario: String values differ
- **WHEN** `addHistorieRegel` is called with two different `String` values
- **THEN** one `HistoryLine` is added to the list

#### Scenario: String values are equal
- **WHEN** `addHistorieRegel` is called with two identical `String` values
- **THEN** the list remains empty

#### Scenario: Boolean values differ
- **WHEN** `addHistorieRegel` is called with old `false` and new `true`
- **THEN** one `HistoryLine` is added

#### Scenario: Boolean values are equal
- **WHEN** `addHistorieRegel` is called with `true` and `true`
- **THEN** the list remains empty

#### Scenario: Nullable LocalDate values differ
- **WHEN** `addHistorieRegel` is called with a non-null old date and a different non-null new date
- **THEN** one `HistoryLine` is added

#### Scenario: One nullable LocalDate is null
- **WHEN** `addHistorieRegel` is called with a non-null date and null
- **THEN** one `HistoryLine` is added

#### Scenario: ZonedDateTime values differ
- **WHEN** `addHistorieRegel` is called with two different `ZonedDateTime` values
- **THEN** one `HistoryLine` is added

#### Scenario: StatusEnum values differ
- **WHEN** `addHistorieRegel` is called with two different `StatusEnum` values
- **THEN** one `HistoryLine` is added

#### Scenario: VertrouwelijkheidaanduidingEnum values differ
- **WHEN** `addHistorieRegel` is called with two different `VertrouwelijkheidaanduidingEnum` values
- **THEN** one `HistoryLine` is added
