## Context

See `proposal.md` - Why. `RestExceptionMapper.toResponse()` is a single `when` expression that
dispatches purely on exception *type* (or, for `WebApplicationException`, on the status family
already attached to it). It has no way to ask "was this caused by the caller or by configuration" -
that question can only be answered at the point an exception is thrown, by whichever call site knows
where its input came from. Several exceptions used today are thrown from more than one call site with
different answers to that question (`ReferenceTableNotFoundException`), and several are thrown from
exactly one call site that is always configuration-caused (`SmartDocumentsConfigurationException`,
`SmartDocumentsDisabledException`, `BrpProtocolleringConfigurationException`).

## Goals / Non-Goals

**Goals:**
- Every response the mapper produces is logged, regardless of which branch handles it.
- Exceptions that can only be caused by misconfiguration are consistently mapped to 500 + `SEVERE`.
- Where one exception class is thrown for both a caller-caused and a configuration-caused reason,
  split it so classification is decided at the throw site, not guessed from the exception type later.
- No behavior change for exceptions that are already correctly classified.

**Non-Goals:**
- `PolicyException` (OPA denial vs. a malformed/misconfigured OPA rule) is not addressed here - the
  exception mapper has no information to distinguish the two; fixing it requires `OpaEvaluationClient`
  or `PolicyService` to read that distinction out of the OPA response itself. Separate change.
- No metrics/counters for 4xx rate. Useful for abuse/probing detection, but a monitoring concern, not
  an exception-logging one, and not requested by this change.
- No frontend changes. The proposal's Impact section flags that SmartDocuments- and BRP-config-related
  requests now return 500 instead of 400/404; updating any frontend logic that branches on those
  specific status codes is left to whoever owns that surface.

## Decisions

**Log unconditionally inside the shared passthrough function, not at each call site.**
`createResponse()` is the one function every plain-`WebApplicationException` response already flows
through. Adding one `log()` call there guarantees every current and future passthrough response is
logged, without relying on every REST resource remembering to log before throwing.
_Alternative considered_: log at each throw site. Rejected - there are dozens of `NotFoundException`/
`BadRequestException` throw sites across the codebase; a shared choke point is the only way to
guarantee coverage.

**Reclassify by changing the exception's base class, not by special-casing it in the mapper.**
`SmartDocumentsConfigurationException`, `SmartDocumentsDisabledException`, and
`BrpProtocolleringConfigurationException` become subtypes of `ServerErrorException`. Each needs an
`ErrorCode` (the constructor parameter `ServerErrorException` already requires), reusing or adding one
per exception as appropriate. No new branch needed in `RestExceptionMapper` since the existing
`is ServerErrorException` branch already produces 500 + `SEVERE`.
_Alternative considered_: keep them under `InputValidationFailedException` and override the log level
per instance. Rejected - log level in this mapper is a property of which branch handles the exception,
not of the exception instance; adding an instance-level override would let a future exception silently
opt out of the class-wide guarantee.

**Split `ReferenceTableNotFoundException` into two exception types at the throw site.**
Add `SystemReferenceTableNotConfiguredException` (extends `ServerErrorException`, needs its own
`ErrorCode`), thrown only from `ReferenceTableService`/call sites that look up a hardcoded
`SystemReferenceTable` enum member. `ReferenceTableNotFoundException` keeps its current behavior
(extends `NotFoundException`, 404) for the `@PathParam`-driven lookups in `ReferenceTableRestService`.
_Alternative considered_: keep one exception type and have the mapper inspect `exception.message` or
the call stack to decide. Rejected - string/stack-trace matching is exactly the kind of fragile,
drift-prone check this change is trying to remove (the existing `handleProcessingException` already
does this out of necessity for third-party client exceptions; it should not be added as a new pattern
where a type-safe alternative exists).

**Add logging directly at the `readUser()`/`readGroup()` fallback, no new exception, no behavior change.**
The 200-with-placeholder response is correct and must not change (see proposal.md). This is a pure
addition of a `log()` call at the existing `?:` fallback.

## Risks / Trade-offs

- [Risk] Reclassifying SmartDocuments/BRP exceptions to 500 changes the HTTP status any existing
  integration test or frontend branch expects for these specific error codes. → Mitigation: called out
  explicitly in the proposal's Impact section; update the affected tests as part of this change's
  tasks rather than discovering the break later.
- [Risk] `SystemReferenceTableNotConfiguredException` adds one more class to an already large
  exception hierarchy. → Mitigation: scope it narrowly to system-reference-table lookups only; the
  name states exactly when it applies, so it should not need a comment to explain itself.
- [Risk] The new `Level.FINE` log in the passthrough path adds one log statement to every plain 4xx
  response. → Mitigation: `FINE` is already the level used for most of the mapper's other 4xx
  branches, and is filtered out by default log configuration in this project's environments -
  consistent with, not additive to, existing log volume expectations.

## Migration Plan

No persisted state changes and no API contract changes for external systems (Open Zaak, Keycloak,
OPA, SmartDocuments) - this only changes which HTTP status ZAC itself returns and what it logs.
Deploy as a normal application release; rollback is a plain revert. Land the passthrough logging fix
first (independent, zero caller-visible impact), then the reclassifications and the reference-table
split together (both change caller-visible status codes, so worth coordinating with anyone who owns
frontend error handling for SmartDocuments/BRP-config/system-reference-table failures), then the
identity logging addition (independent, zero caller-visible impact).
