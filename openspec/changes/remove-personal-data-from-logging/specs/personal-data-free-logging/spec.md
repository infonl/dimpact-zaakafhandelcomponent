## ADDED Requirements

### Requirement: Backend log records are typed log events
The backend SHALL write every log record from production code as a `LogEvent` through a `ZacLogger`. A `LogEvent` has
a fixed description and typed fields. `ZacLogger` SHALL NOT accept a free-text message. A log record SHALL be written
as the description of the event followed by its fields as `name=value` pairs.

#### Scenario: A log event is written with its fields
- **WHEN** the backend logs a `ZaakAssigned` event with description `Zaak assigned` for zaak `ZAAK-2026-0000000001` and
  group `test-group`
- **THEN** the log record reads `Zaak assigned [zaakIdentificatie=ZAAK-2026-0000000001, groupId=test-group]`

#### Scenario: A log event is written with an exception
- **WHEN** the backend logs a log event at `SEVERE` together with an exception
- **THEN** the log record contains the rendered event and the exception, at log level `SEVERE`

#### Scenario: A disabled log level does not render the event
- **WHEN** the backend logs a log event at `FINE` and `FINE` is disabled for that logger
- **THEN** no log record is written and the fields of the event are not rendered

### Requirement: Personal data fields are masked
The backend SHALL mask the value of every field of a `LogEvent` that is annotated with `@Redacted` when it writes the
log record. The original value SHALL NOT appear anywhere in the log record.

#### Scenario: A mail is sent
- **WHEN** the backend sends a mail with subject `Uw aanvraag` to `burger@example.com`
- **THEN** the log record reads `Mail sent [recipient=*****, subject=Uw aanvraag]` and does not contain
  `burger@example.com`

#### Scenario: A redacted field is empty
- **WHEN** the backend logs a log event whose `@Redacted` field is `null`
- **THEN** the log record shows that field as `null`

### Requirement: Logging outside ZacLogger fails the build
The build SHALL fail when a production class outside the `nl.info.zac.log` package uses `java.util.logging.Logger`.
The `logFilter` source set, which holds the WildFly masking filter, is not part of this check.

#### Scenario: A class creates its own Logger
- **WHEN** a production class in `nl.info.zac` calls `Logger.getLogger(...)`
- **THEN** the architecture test fails and names that class

#### Scenario: Only ZacLogger is used
- **WHEN** all production classes log through `ZacLogger`
- **THEN** the architecture test passes

### Requirement: Outbound REST calls are logged without payloads
The backend SHALL log an outbound REST call only with a correlation ID, the HTTP method, the host and path of the
URL, the response status and the duration. It SHALL NOT log the query string, the headers or the request or response
body of any outbound REST call, at any log level.

#### Scenario: An outbound BRP call is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and ZAC sends a `POST` to
  `https://brp.example.com/haalcentraal/api/brp/personen` with a BSN in the body, and BRP answers `200`
- **THEN** a request record and a response record are logged with the same correlation ID, the method `POST`, the
  host and path, the status `200` and the duration
- **AND** neither record contains the body, the headers or the BSN

#### Scenario: An outbound call with a query string is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and ZAC sends a `GET` to
  `https://klant.example.com/klantinteracties/api/v1/partijen?partijIdentificator__objectId=999993896`
- **THEN** the logged URL is `https://klant.example.com/klantinteracties/api/v1/partijen` without the query string

### Requirement: BRP queries are logged without person data
The backend SHALL log a BRP person query only with the type of the query. It SHALL NOT log the query parameters or
the response.

#### Scenario: A BRP query by BSN is logged
- **WHEN** ZAC queries the BRP for the person with BSN `999993896`
- **THEN** the log record contains the query type `RaadpleegMetBurgerservicenummer` and not the BSN or the response

### Requirement: Mail sending is logged without addresses
The backend SHALL NOT log the email address or the name of the sender or the recipient of a mail in plain text. The
BPMN mail delegates SHALL log only the zaak identification and the name of the mail template.

#### Scenario: The send email BPMN task sends a mail
- **WHEN** the `SendEmailDelegate` sends a mail with template `Ontvangstbevestiging` for zaak `ZAAK-2026-0000000001`
  from `zaken@gemeente.example.com` to `burger@example.com`
- **THEN** the log record contains `ZAAK-2026-0000000001` and `Ontvangstbevestiging`
- **AND** it contains neither `zaken@gemeente.example.com` nor `burger@example.com`

#### Scenario: The confirmation email BPMN task sends a mail
- **WHEN** the `SendConfirmationEmailDelegate` sends the confirmation mail for zaak `ZAAK-2026-0000000001` to the email
  address of the initiator
- **THEN** the log record contains `ZAAK-2026-0000000001` and does not contain the email address of the initiator or
  of the sender

### Requirement: Free-text assignment reasons are not logged
The backend SHALL NOT log the free-text reason of a zaak assignment.

#### Scenario: A BPMN task assigns a zaak with a reason
- **WHEN** the `UpdateZaakAssignmentDelegate` assigns zaak `ZAAK-2026-0000000001` to group `test-group` with the reason
  `Overleg met mevrouw Jansen`
- **THEN** the log record contains `ZAAK-2026-0000000001` and `test-group` and does not contain the reason

### Requirement: Rejected input values are not logged
The backend SHALL NOT log the values that a ZGW API rejected in a `ZgwValidationErrorException`. It SHALL log the
validation error code and the names of the invalid parameters.

#### Scenario: Open Zaak rejects an email address
- **WHEN** Open Zaak rejects a request because the parameter `emailadres` with value `geen-email` is invalid, with
  code `invalid` and the reason `Voer een geldig e-mailadres in: geen-email`
- **THEN** the log record contains the error code `invalid` and the parameter name `emailadres`
- **AND** it contains neither `geen-email` nor the reason

### Requirement: Employees are identified by their Keycloak user ID
When a log record identifies an employee, the backend SHALL use the Keycloak user ID of that employee (the `sub` claim
of the access token). It SHALL NOT log the username, the name or the email address of an employee. When the Keycloak
user ID cannot be determined, the log record SHALL NOT identify the employee.

#### Scenario: An employee logs in
- **WHEN** an employee with username `jan.jansen@gemeente.example.com` and Keycloak user ID
  `5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f` starts a new session
- **THEN** the log record contains `5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f`, the groups and the roles of the employee
- **AND** it does not contain `jan.jansen@gemeente.example.com`

#### Scenario: A websocket event is logged
- **WHEN** an employee subscribes to a websocket event
- **THEN** the log record contains the Keycloak user ID of the employee and not the username

#### Scenario: A policy check in a BPMN task fails
- **WHEN** an employee without the right to suspend zaak `ZAAK-2026-0000000001` runs the suspend zaak BPMN task
- **THEN** the policy denial record contains `ZAAK-2026-0000000001` and the Keycloak user ID of the employee, and not
  the username

#### Scenario: A productaanvraag assigns a zaak to an employee
- **WHEN** a productaanvraag assigns a new zaak to the employee with username `jjansen`, and Keycloak knows that user
- **THEN** the log record contains the Keycloak user ID of `jjansen` and not `jjansen`

#### Scenario: The Keycloak user ID of an assigned employee cannot be found
- **WHEN** a productaanvraag assigns a new zaak to the employee with username `jjansen`, and Keycloak does not know
  that user or cannot be reached
- **THEN** the zaak is still assigned
- **AND** the log record contains the zaak UUID and the group, and does not identify the employee

### Requirement: Dates and places of birth are not logged
The backend SHALL NOT log the date or the place of birth of a person.

#### Scenario: A mail template names an initiator without a name
- **WHEN** a mail template resolves the name of an initiator who has no name in the BRP
- **THEN** the log record does not contain the date or the place of birth of the initiator
