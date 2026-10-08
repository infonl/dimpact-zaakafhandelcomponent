## Why

`RestExceptionMapper` classifies every REST response by exception *type*, not by whose fault the
failure is. An audit of config-driven lookups (zaaktype config, reference tables, SmartDocuments
templates, BRP protocollering config, Keycloak group/user references, OPA policy) found that several
genuinely admin-caused misconfigurations currently surface as 400/404, or as a silent 200 with
fabricated data, rather than as a logged server error — while structurally identical
"not found" cases elsewhere already correctly return 500. This delays operators noticing broken
configuration, because it looks like ordinary client traffic (or nothing at all) rather than an
alert-worthy failure. A separate but related gap: the plain `WebApplicationException` passthrough
path in the mapper never logs anything, so some 4xx responses leave no trace even when they should.

## What Changes

- Add an unconditional `Level.FINE` log call to `RestExceptionMapper`'s `WebApplicationException`
  passthrough branch (`createResponse()`, currently lines 94-97 / 254-267), so every response taking
  that path is logged regardless of exception type.
- Reclassify `SmartDocumentsConfigurationException`, `SmartDocumentsDisabledException`, and
  `BrpProtocolleringConfigurationException` to extend `ServerErrorException` (500 + `Level.SEVERE`)
  instead of `InputValidationFailedException`/`ServerErrorException` mixed usage — no call site of
  these can be triggered by caller input, only by deployment/admin configuration.
- Split `ReferenceTableService`'s reference-table-not-found handling by call-site intent:
  - Keep `ReferenceTableNotFoundException` (404) for lookups where the code/id is a caller-supplied
    `@PathParam` (`ReferenceTableRestService.kt` get/update-by-id and get-by-code endpoints).
  - Introduce `SystemReferenceTableNotConfiguredException` (extends `ServerErrorException`, 500 +
    `Level.SEVERE`) for lookups where the code is a hardcoded `SystemReferenceTable` enum member
    with no caller-supplied parameter at all (the `afzender`, `communicatiekanaal`, and
    BRP-doelbinding endpoints in `ReferenceTableRestService.kt`, plus the equivalent lookups in
    `ZaaktypeConfigurationRestService.kt` and `HealthCheckService.kt`).
- Add a `Level.WARNING` log call in `IdentityService.readUser()`/`readGroup()` at the point where a
  configured groepId/userId no longer resolves in Keycloak. The existing fallback behavior (return a
  placeholder `Group(groupId)`/`User(userId)` and a 200 response) is intentional and unchanged — the
  gap is that this config drift is currently invisible to operators, not that the response is wrong.

**Out of scope**: `PolicyException` (OPA rule denial vs. a malformed/misconfigured OPA rule are
indistinguishable once the exception reaches `RestExceptionMapper`) requires a change in
`OpaEvaluationClient`/`PolicyService` to distinguish "evaluated to deny" from "failed to evaluate" at
the point the OPA response is read. That is a separate change.

## Capabilities

### New Capabilities
- `configuration-error-classification`: defines which REST-facing exceptions represent an
  admin/deployment misconfiguration versus a genuine caller error, and the logging guarantee that
  applies to each category (every REST exception response is logged at a level matching who is at
  fault; misconfiguration is always logged at `WARNING` or higher, regardless of the HTTP status
  returned to the caller).

### Modified Capabilities
_None — no existing spec in `openspec/specs/` currently documents REST exception-mapping or logging
behavior._

## Impact

- `nl/info/zac/app/exception/RestExceptionMapper.kt` — add logging in the passthrough branch.
- `nl/info/zac/smartdocuments/exception/SmartDocumentsConfigurationException.kt`,
  `SmartDocumentsDisabledException.kt`,
  `nl/info/zac/configuration/exception/BrpProtocolleringConfigurationException.kt` — base class change.
- `nl/info/zac/admin/ReferenceTableService.kt`, `nl/info/zac/admin/exception/` (new exception class),
  `nl/info/zac/app/admin/ReferenceTableRestService.kt`,
  `nl/info/zac/app/admin/ZaaktypeConfigurationRestService.kt`,
  `nl/info/zac/healthcheck/HealthCheckService.kt` — call sites re-pointed to the new exception type.
- `nl/info/zac/identity/IdentityService.kt` — logging added, no behavior change to callers.
- **Caller-visible change**: requests that hit an unseeded SmartDocuments template/group, a disabled
  SmartDocuments feature flag, an invalid BRP protocollering config, or an unconfigured system
  reference table now receive **500** instead of 400/404. Frontend error handling for these specific
  cases should be reviewed, since the response is no longer a validation-style 4xx.
- No database migrations. No changes to external API contracts (Open Zaak, Keycloak, OPA, SmartDocuments).
