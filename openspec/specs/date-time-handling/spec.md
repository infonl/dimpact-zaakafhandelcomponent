# date-time-handling Specification

## Purpose
Defines how ZAC handles dates and times: one time zone for the whole application, one meaning of a date range
"van t/m", dates without a time between frontend and backend, and lint rules that keep date and time handling in one
backend module and one frontend module.

## Requirements

### Requirement: One time zone for ZAC
The backend SHALL use the time zone `Europe/Amsterdam` whenever it converts between a date and a point in time, and
whenever it determines the current date, independent of the time zone of the server or container.

#### Scenario: Today on a server in UTC
- **WHEN** the backend runs with the JVM time zone UTC and the time is 23:30 UTC on 29-07-2026
- **THEN** the backend takes 30-07-2026 as the current date

#### Scenario: Start of a day on a server in UTC
- **WHEN** the backend runs with the JVM time zone UTC and converts the date 30-07-2026 to the start of that day
- **THEN** the result is 30-07-2026 00:00 in `Europe/Amsterdam`, which is 29-07-2026 22:00 UTC

### Requirement: A date range includes the whole t/m day
A date filter with a van date and a t/m date SHALL match every value from the start of the van day up to, but not
including, the start of the day after the t/m day, in the time zone `Europe/Amsterdam`. A range without a van date has
no lower bound; a range without a t/m date has no upper bound. This SHALL hold for the work lists of zaken, taken,
inbox documents, detached documents and inbox productaanvragen, and for the date filters of the search screen.

#### Scenario: Van today t/m today on a field with a time
- **WHEN** a user filters the tasks on "Aangemaakt op" van today t/m today, and a task was created today at 09:15
- **THEN** the list shows that task

#### Scenario: The last moment of the t/m day
- **WHEN** a user filters on a range t/m 30-07-2026, and a value is 30-07-2026 23:59:59 in `Europe/Amsterdam`
- **THEN** the value matches

#### Scenario: The start of the day after the t/m day
- **WHEN** a user filters on a range t/m 30-07-2026, and a value is 31-07-2026 00:00 in `Europe/Amsterdam`
- **THEN** the value does not match

#### Scenario: The day before the van day
- **WHEN** a user filters on a range van 30-07-2026, and a value is 29-07-2026 23:59 in `Europe/Amsterdam`
- **THEN** the value does not match

#### Scenario: A field without a time
- **WHEN** a user filters zaken on "Startdatum" van 30-07-2026 t/m 30-07-2026, and a zaak has the startdatum 30-07-2026
- **THEN** the list shows that zaak

#### Scenario: The same filter on every list
- **WHEN** a user filters the inbox documents, the detached documents and the search screen documents on van 30-07-2026
  t/m 30-07-2026
- **THEN** each list shows exactly the documents of 30-07-2026 and none of 29-07-2026

### Requirement: Dates travel without a time
The frontend SHALL send a date that a user picks as a date without a time, in the form `yyyy-MM-dd`, to the backend.
When the backend receives a date field that holds a time, it SHALL use the date of that point in time in
`Europe/Amsterdam`.

#### Scenario: The frontend sends a picked date
- **WHEN** a user in the Netherlands picks 30-07-2026 as the van date of a date filter
- **THEN** the request holds `"van": "2026-07-30"`

#### Scenario: The backend receives a date with a time
- **WHEN** the backend receives `"van": "2026-07-29T22:00:00.000Z"` for a date field, for example from a saved search
- **THEN** the backend uses the date 30-07-2026

#### Scenario: A saved search with an old date shows the same date
- **WHEN** a user opens a saved search that holds the date `2026-07-29T22:00:00.000Z`
- **THEN** the date filter shows 30-07-2026

### Requirement: Date and time handling stays in one module per side
The build SHALL fail when code outside the backend date and time module reads the system clock or the system time
zone, or converts between a date and a point in time with a time zone of its own. The build SHALL fail when frontend
code outside the frontend date and time module imports a date library or parses or formats a date itself. Code that
broke these rules before the rules were added SHALL be listed as frozen exceptions, and the list SHALL only shrink.

#### Scenario: New backend code reads the system time zone
- **WHEN** a pull request adds a call to the system default time zone outside the backend date and time module
- **THEN** the architecture tests fail and name the new violation

#### Scenario: New frontend code imports a date library
- **WHEN** a pull request adds an import of the date library to a frontend file outside the frontend date and time
  module and outside the frozen list
- **THEN** the frontend lint fails

#### Scenario: Frozen code is moved into the module
- **WHEN** a pull request moves a frozen use into the date and time module
- **THEN** the build passes and the frozen exception is removed from the list
