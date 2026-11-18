## MODIFIED Requirements

### Requirement: Date conversion
The system SHALL convert a `LocalDate` to a `ZonedDateTime` at start-of-day in the ZAC time zone `Europe/Amsterdam`,
independent of the JVM's system default time zone.

#### Scenario: Convert a local date to a zoned date-time
- **WHEN** a `LocalDate` is converted via the date-time utility
- **THEN** the result is a `ZonedDateTime` at `00:00:00` in `Europe/Amsterdam` on that date

#### Scenario: Convert a local date on a server in UTC
- **WHEN** the JVM's system default time zone is UTC and the `LocalDate` 30-07-2026 is converted via the date-time
  utility
- **THEN** the result is 30-07-2026 `00:00:00` in `Europe/Amsterdam`
