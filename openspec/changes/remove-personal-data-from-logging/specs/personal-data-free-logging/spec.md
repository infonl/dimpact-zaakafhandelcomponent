## ADDED Requirements

### Requirement: Backend log records are typed, structured log events
The backend SHALL write every log record from production code as a `LogEvent` through a `ZacLogger`. A `LogEvent` has
a fixed description and typed fields. `ZacLogger` SHALL NOT accept a free-text message. The message of a log record
SHALL be the description of the event, and each field of the event SHALL be a separate key in the JSON log record,
together with the key `event` holding the name of the event type.

#### Scenario: A log event is written with its fields
- **WHEN** the backend logs a `ZaakAssigned` event with description `Zaak assigned` for zaak `ZAAK-2026-0000000001` and
  group `test-group`
- **THEN** the message of the log record is `Zaak assigned`
- **AND** the record has the fields `event=ZaakAssigned`, `zaakIdentificatie=ZAAK-2026-0000000001` and
  `groupId=test-group`

#### Scenario: Fields do not leak to the next log record
- **WHEN** the backend logs an event with the field `zaakUuid`, and the same thread then logs an event without that
  field
- **THEN** the second log record has no `zaakUuid` field

#### Scenario: A log event is written with an exception
- **WHEN** the backend logs a log event at `SEVERE` together with an exception
- **THEN** the log record contains the event, its fields and the exception, at log level `SEVERE`
- **AND** the record has the field `errorType` with the class name of the exception

#### Scenario: A disabled log level does not render the event
- **WHEN** the backend logs a log event at `FINE` and `FINE` is disabled for that logger
- **THEN** no log record is written and the fields of the event are not rendered

#### Scenario: Rendering a field writes a log record of its own
- **WHEN** the backend logs an event with the field `zaakUuid` and an `Employee` field, and resolving that employee
  writes a log record of its own
- **THEN** the inner record has none of the fields of the outer event
- **AND** the outer record has all of its own fields, and no field of the inner record

### Requirement: Personal data fields are masked
The backend SHALL mask the value of every field of a `LogEvent` that is annotated with `@Redacted` when it writes the
log record. The original value SHALL NOT appear anywhere in the log record.

#### Scenario: A mail is sent
- **WHEN** the backend sends a mail to `burger@example.com`
- **THEN** the message of the log record is `Mail send completed` and the field `recipient` is `*****`
- **AND** the record does not contain `burger@example.com`

#### Scenario: A redacted field is empty
- **WHEN** the backend logs a log event whose `@Redacted` field is `null`
- **THEN** the log record shows that field as `null`

### Requirement: Log fields use one vocabulary
A field of a `LogEvent` that names a shared concept SHALL use the standard field name for that concept from
`LogFieldNames`. An event for an operation with a start and an end SHALL have the fields `outcome` (`SUCCESS` or
`FAILURE`) and `durationMs`.

#### Scenario: The standard field names
- **WHEN** a `LogEvent` has a field for a zaak, zaaktype, taak, informatieobject, group, employee, process or case
  definition, outcome, duration, error or trace
- **THEN** the field is named `zaakUuid`, `zaakIdentificatie`, `zaaktypeUuid`, `taakId`, `informatieobjectUuid`,
  `informatieobjectIdentificatie`, `groupId`, `employee` (or a name ending in `Employee`), `processDefinitionKey`,
  `caseDefinitionKey`, `outcome`, `durationMs`, `errorType`, `errorCode`, `traceId` or `spanId`

#### Scenario: The same zaak in two events
- **WHEN** the backend logs a `MailSendCompleted` event and a `ZaakAssigned` event for the zaak with UUID
  `5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f`
- **THEN** both records hold that UUID in the field `zaakUuid`

#### Scenario: An event uses a synonym of a standard field name
- **WHEN** a `LogEvent` subclass has a field named `zaakId`, `zaakUUID`, `uuid`, `user`, `username` or `userId`
- **THEN** the unit test of the log field vocabulary fails and names that event

#### Scenario: An operation with a start and an end is logged
- **WHEN** ZAC sends a mail and the mail server accepts it after 120 ms
- **THEN** the record has `outcome=SUCCESS` and `durationMs=120`

#### Scenario: Sending a mail fails
- **WHEN** ZAC sends a mail for the zaak with UUID `5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f` to `burger@example.com` and
  the mail server refuses it
- **THEN** exactly one log record is written at `SEVERE`, with `zaakUuid=5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f`,
  `outcome=FAILURE`, `durationMs`, `errorType` and the field `recipient` as `*****`
- **AND** the record does not contain `burger@example.com`

### Requirement: One log record per operation
When several log statements in one function or class describe the same operation, the backend SHALL write one log
record for that operation when it ends. The intermediate choices of the operation SHALL be fields of that record. An
operation that runs for minutes and reports its progress, such as reindexing, keeps a record for its start, its
progress and its end.

#### Scenario: A confirmation mail is sent
- **WHEN** the `SendConfirmationEmailDelegate` sends the confirmation mail with template `Ontvangstbevestiging` for
  zaak `ZAAK-2026-0000000001` to the email address from the zaak-specific contact details
- **THEN** exactly one log record is written for that execution, with `zaakIdentificatie=ZAAK-2026-0000000001`,
  `templateName=Ontvangstbevestiging` and `emailSource=ZAAK_CONTACT_DETAILS`

#### Scenario: No email address for a confirmation mail
- **WHEN** the `SendConfirmationEmailDelegate` runs for zaak `ZAAK-2026-0000000001` and neither the zaak-specific
  contact details nor the initiator have an email address
- **THEN** exactly one log record is written, with `zaakIdentificatie=ZAAK-2026-0000000001` and `emailSource=NONE`
- **AND** no mail is sent

#### Scenario: An employee logs in after another user in the same session
- **WHEN** an employee logs in on an HTTP session that belonged to another employee
- **THEN** exactly one log record is written, with the employee and the previous employee as fields

#### Scenario: Reindexing keeps its start, progress and end records
- **WHEN** an administrator reindexes all zaken
- **THEN** a record is written when the reindex starts, for its progress and when it ends, with the counts that the
  `solr-reindexing-observability` spec requires

### Requirement: Log records carry the trace and span ID
When tracing is enabled and a span is active, every log record SHALL have the fields `traceId` and `spanId` of that
span. When tracing is disabled, a log record SHALL have neither field.

#### Scenario: A log record inside a traced request
- **WHEN** tracing is enabled and the backend logs an event while it handles a REST request
- **THEN** the record has the `traceId` and `spanId` of the active span

#### Scenario: Tracing is disabled
- **WHEN** tracing is disabled and the backend logs an event
- **THEN** the record has no `traceId` and no `spanId` field

### Requirement: Logging outside ZacLogger fails the build
The build SHALL fail when a production class outside the `nl.info.zac.log` package uses a logging API other than
`ZacLogger`, writes to `System.out` or `System.err`, or calls `printStackTrace()`. The `logFilter` source set, which
holds the WildFly masking filter, is not part of this check.

#### Scenario: A class uses another logging API
- **WHEN** a production class in `nl.info.zac` uses `java.util.logging.Logger`, SLF4J (`org.slf4j`), JBoss Logging
  (`org.jboss.logging`), Log4j (`org.apache.logging.log4j`, `org.apache.log4j`), Commons Logging
  (`org.apache.commons.logging`), kotlin-logging (`io.github.oshai.kotlinlogging`, `mu`) or `System.Logger`
- **THEN** the architecture test fails and names that class

#### Scenario: A class prints to the console
- **WHEN** a production class in `nl.info.zac` calls `println(...)` or `exception.printStackTrace()`
- **THEN** the architecture test fails and names that class

#### Scenario: Only ZacLogger is used
- **WHEN** all production classes log through `ZacLogger`
- **THEN** the architecture test passes

### Requirement: Outbound REST calls are logged without payloads
The backend SHALL log an outbound REST call that gets a response as one record with a correlation ID, the HTTP
method, the host, the invoked client operation, the response status, the outcome and the duration. The correlation
ID SHALL be the span ID of the active span when tracing is enabled, and a random UUID otherwise. The backend SHALL
NOT log the path, the query string, the headers or the body of any outbound REST call, at any log level.

#### Scenario: An outbound BRP call is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and ZAC sends a `POST` to
  `https://brp.example.com/haalcentraal/api/brp/personen` with a BSN in the body, and BRP answers `200`
- **THEN** exactly one record is logged for that call, with a correlation ID, `method=POST`,
  `host=brp.example.com`, `operation=PersonenApi.personen`, `status=200`, `outcome=SUCCESS` and `durationMs`
- **AND** the record does not contain the path, the body, the headers or the BSN

#### Scenario: An outbound call with an identifier in the path is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and ZAC sends a `GET` to
  `https://bag.example.com/lvbag/individuelebevragingen/v2/adressen/0363200000123456`
- **THEN** the record has `operation=AdresApi.bevraagAdressenMetNumId`
- **AND** the record does not contain `0363200000123456`

#### Scenario: An outbound call with a query string is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and ZAC sends a `GET` to
  `https://klant.example.com/klantinteracties/api/v1/partijen?partijIdentificator__objectId=999993896`
- **THEN** the record does not contain `999993896` or any other part of the path or the query string

#### Scenario: An outbound call without a client operation is logged
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and a REST client that is not a MicroProfile Rest Client
  sends a request
- **THEN** the record has the host and no `operation` field

#### Scenario: An outbound call fails before a response
- **WHEN** `FINE` logging is enabled for `JsonLoggingFilter` and the connection to the remote system is refused
- **THEN** `JsonLoggingFilter` writes no record for that call
- **AND** the exception reaches the caller, which logs or maps it as before

### Requirement: BRP queries are logged without person data
The backend SHALL log a BRP person query only with the type of the query. It SHALL NOT log the query parameters or
the response.

#### Scenario: A BRP query by BSN is logged
- **WHEN** ZAC queries the BRP for the person with BSN `999993896`
- **THEN** the log record contains the query type `RaadpleegMetBurgerservicenummer` and not the BSN or the response

### Requirement: Mail sending is logged without addresses
The backend SHALL NOT log the email address or the name of the sender or the recipient of a mail in plain text, nor
the subject of a mail. The BPMN mail delegates SHALL log only the zaak identification, the name of the mail template
and, for the confirmation mail, the source of the email address.

#### Scenario: A mail with personal data in its subject is sent
- **WHEN** ZAC sends a mail whose subject resolves to `Uw aanvraag, mevrouw Jansen`
- **THEN** the log record does not contain the subject or `Jansen`

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
- **AND** neither its fields, its message nor its exception contain `geen-email` or the reason
- **AND** the REST response still contains the reason

### Requirement: Employees are identified by their Keycloak user ID
When a log record identifies an employee, the backend SHALL use the Keycloak user ID of that employee (the `sub` claim
of the access token). It SHALL NOT log the username, the name or the email address of an employee. When the Keycloak
user ID cannot be determined, the log record SHALL write `unknown` for that employee.

#### Scenario: The Keycloak user ID of a logged-in employee is known without a Keycloak call
- **WHEN** an employee with username `jjansen` has logged in on a ZAC instance, and a later log record on that
  instance identifies `jjansen`
- **THEN** the log record contains the Keycloak user ID from the access token of `jjansen`
- **AND** ZAC does not call Keycloak to determine it

#### Scenario: The Keycloak user ID of an employee is looked up only once
- **WHEN** two log records on the same ZAC instance identify the employee `jjansen`, who has not logged in on that
  instance
- **THEN** both log records contain the Keycloak user ID of `jjansen`
- **AND** ZAC calls Keycloak for `jjansen` only for the first log record

#### Scenario: No Keycloak call for a log record that is not written
- **WHEN** a log event at `FINE` identifies the employee `jjansen`, and `FINE` is disabled for that logger
- **THEN** ZAC does not call Keycloak for `jjansen`

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
- **AND** the log record contains the zaak UUID, the group and `unknown` for the employee, and not `jjansen`
- **AND** a log record for `jjansen` within 5 minutes writes `unknown` without calling Keycloak
- **AND** the first log record for `jjansen` after 5 minutes calls Keycloak again

#### Scenario: Keycloak does not answer
- **WHEN** a productaanvraag assigns a new zaak to the employee `jjansen`, who has not logged in on that instance, and
  Keycloak accepts the connection but does not answer
- **THEN** the zaak is assigned and the log record contains `unknown` for the employee
- **AND** the lookup waits no longer than the 2 second read timeout of the Keycloak admin client

#### Scenario: A BPMN task assigns a zaak to an employee
- **WHEN** the `UpdateZaakAssignmentDelegate` assigns zaak `ZAAK-2026-0000000001` to the employee `jjansen`, and `FINE`
  is enabled for that delegate
- **THEN** the log record contains the Keycloak user ID of `jjansen` and not `jjansen`

### Requirement: Notification properties are not logged
The backend SHALL log a received notification with its channel, main resource URL, resource, resource URL, action and
creation date. It SHALL NOT log the `kenmerken` of the notification.

#### Scenario: A notification with kenmerken is received
- **WHEN** ZAC receives a notification on channel `objecten` with the kenmerk `naam` with value `Jan Jansen`
- **THEN** the log record contains the channel, the resource URL and the action
- **AND** it contains neither `naam` nor `Jan Jansen`

### Requirement: Personal data in log fields is masked by the WildFly filter
The WildFly masking filter (ADR-0011) SHALL mask BSNs and IBANs in the fields of a log record as well as in its
message and exceptions. When masking fails, the filter SHALL withhold the message, the parameters, the exception and
the fields of the record.

#### Scenario: A BSN ends up in a log field
- **WHEN** a log event has a field with the value `999993896`
- **THEN** the field is written as `******896`

#### Scenario: Masking a record fails
- **WHEN** masking the personal data of a log record with the field value `999993896` fails
- **THEN** the record is written with a message that its content is withheld
- **AND** it contains no field values, no parameters and no exception of the original record

### Requirement: Dates and places of birth are not logged
The backend SHALL NOT log the date or the place of birth of a person.

#### Scenario: A mail template names an initiator without a name
- **WHEN** a mail template resolves the name of an initiator who has no name in the BRP
- **THEN** the log record does not contain the date or the place of birth of the initiator
