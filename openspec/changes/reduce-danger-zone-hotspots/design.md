## Context

`ZaakRestService` (1119 lines, 28 injected dependencies) is the top file of the churn vs complexity report. Its total complexity is high because it is large, not because one function is complex: the worst function
scores 6 and 20 functions score 1. Six groups of endpoints were already split off at the same `@Path("zaken")` root:
assign and release, besluit (archived change `2026-06-15-extract-zaak-besluit-rest-service`), brondatum, koppelen,
suspend and zaakspecifieke autorisatie. The zaaktypen for zaak creation moved to `ZaaktypeRestService` (#7919). This change continues that pattern, one group per phase.

The betrokkene group is phase 1 because it holds the most complexity of the remaining groups. It has five endpoints
and four private helpers, and `composeBetrokkeneIdentification` and `addBetrokkeneToZaak` are among the most complex
functions left. Moving it first also makes the queued fixes in this code smaller to review.

Call graph of the group, as it stands at the start of phase 1:

| Endpoint | Uses |
| --- | --- |
| `addBetrokkene` | `addBetrokkeneToZaak` → `composeBetrokkeneIdentification` → `createVestigingIdentificationString` |
| `deleteBetrokkene` | `removeBetrokkene`, `ROLTYPE_OMSCHRIJVINGEN_THAT_CANNOT_BE_DELETED_AS_BETROKKENE` |
| `deleteInitiator` | `removeInitiator` |
| `listBetrokkenenVoorZaak` | `toRestZaakBetrokkenen` |
| `updateInitiator` | `removeInitiator`, `composeBetrokkeneIdentification`, private `updateInitiator`, `ROL_VERWIJDER_REDEN` |

Shared with the create path: the private `updateInitiator` (policy check per identification type, then
`ZaakService.addInitiatorToZaak`) is also called from `addInitiator` in `createZaak`. `createVestigingIdentificationString`
is also called from `addInitiator`. `assertCanAddBetrokkene` and `addInitiator` are used only by creating and updating
a zaak, so they stay in `ZaakRestService` until phase 3.

## Goals / Non-Goals

**Goals:**

- Move the five endpoints and their exclusive helpers to `ZaakBetrokkeneRestService`, with identical URIs, method
  names, signatures, annotations and behaviour.
- One implementation of the policy check on adding an initiator, used by both the create path and the new service.
- Remove the two places where domain code reads a constant from `ZaakRestService`.
- Move the unit tests of the moved endpoints to `ZaakBetrokkeneRestServiceTest` without changing what they assert.

**Non-Goals:**

- Fixing defects in the moved code, even where they are known. A test that locks in today's behaviour stays as it is;
  the fix tickets change it.
- Unifying the two ways the initiator identification is built (`addInitiator` on create, `composeBetrokkeneIdentification`
  on update). They differ in validation, so merging them changes behaviour; that belongs to a fix ticket.
- Adding `@Valid` or other validation to request bodies.
- Removing dependencies from `ZaakRestService` beyond those the move leaves unused.
- Phases 2 to 6; each gets its own change.

## Decisions

### Decision 1: Same `@Path("zaken")` root, separate class

The same choice as for the besluit, brondatum and suspend services: JAX-RS merges the sub-paths of several resources
under one root, so every URI stays the same and neither the frontend nor the test-data scripts change.
*Alternative considered:* a `/zaken/betrokkenen` root. Rejected, because it changes the REST contract.

### Decision 2: Keep the endpoint method names

The OpenAPI generation uses `OperationIdStrategy.METHOD`, so the operation id is the method name. Keeping
`addBetrokkene`, `deleteBetrokkene`, `deleteInitiator`, `listBetrokkenenVoorZaak` and `updateInitiator` keeps the
generated OpenAPI specification, and the frontend types generated from it, identical. That makes a byte-for-byte
comparison of `openapi.json` before and after the move a cheap check on the REST contract.

### Decision 3: The shared initiator policy check becomes an extension on `ZaakRechten`

Both the create path and the new service check, per identification type, whether the user may add an initiator
(`canToevoegenInitiatorPersoon` for BSN, `canToevoegenBetrokkeneBedrijf` for VN and RSIN). An extension function
`ZaakRechten.assertCanAddInitiator(IdentificatieType)` in `nl.info.zac.app.zaak` holds that check once. Each caller
keeps its own call to `ZaakService.addInitiatorToZaak` with its own explanation text.
*Alternatives considered:*
- A shared helper bean that both services inject. Rejected: one more injected class for a pure function.
- Moving the check into `ZaakService`. Rejected: policy checks belong to the REST layer, and the domain must not
  depend on REST types.
- Injecting `ZaakBetrokkeneRestService` into `ZaakRestService`. Rejected: one REST resource calling another.

`createVestigingIdentificationString` is a pure function used by both as well, so it becomes a top-level function
next to the constant it uses (see decision 4).

### Decision 4: The constants that domain code reads move to the domain

`ZaakService` reads `VESTIGING_IDENTIFICATIE_DELIMITER` and `SuspensionZaakHelper` reads
`AANVULLENDE_INFORMATIE_TASK_NAME` from `ZaakRestService.Companion`. Both are `const val`s, so the compiler inlines
them and the ArchUnit rule "the domain code does not depend on the REST layer" cannot see the dependency. They move to
the domain package that uses them: the delimiter to `nl.info.zac.zaak`, the task name next to `SuspensionZaakHelper`.
The REST layer imports them from there. The values do not change.

### Decision 5: Close the gaps in the safety net first, then move the tests unchanged

Before anything moves, every moved endpoint gets a unit test and an integration test of its normal use. Today:

| Endpoint | Unit test | Integration test |
| --- | --- | --- |
| `addBetrokkene` | none | `ZaakRestServiceTest` (itest) |
| `deleteBetrokkene` | "Deleting a betrokkene" | none |
| `deleteInitiator` | "Deleting an initiator" | none |
| `listBetrokkenenVoorZaak` | "Listing betrokkenen for a zaak" | several |
| `updateInitiator` | "Updating an initiator" | several |

So the first pull request adds a unit test for `addBetrokkene`, and integration tests for deleting a betrokkene and
deleting the initiator, against the current `ZaakRestService`. They cover normal use with a valid reden only: a test
of a known defect would lock that defect in, and the fix tickets add those tests.

Then the `ZaakRestServiceTest` contexts "Deleting a betrokkene", "Deleting an initiator", "Listing betrokkenen for a
zaak", "Updating an initiator" and the new `addBetrokkene` tests move to `ZaakBetrokkeneRestServiceTest`. They get only
the mocks the new class needs, and `checkUnnecessaryStub()` stays. Their assertions do not change, including those
that describe today's behaviour of known defects. The integration tests in `src/itest` stay untouched; passing them
unchanged is the proof that the REST behaviour is the same.

### Decision 6: Measure each phase with the churn vs complexity report

Each phase records the file's numbers from the report before and after: commits, total and worst-function
complexity, number of functions and lines of code. For phase 1 the expected effect on `ZaakRestService.kt` is about 22
points of total complexity and 9 functions fewer than the baseline of task 1.2. The worst function stays at 6, because
`addInitiator` and `assertCanAddBetrokkene` belong to the create path (phase 3). The new file starts with no churn of
its own, because the report does not follow code that moves between files.

## Risks / Trade-offs

- [Two resources on one root path] → Already the case for seven resources; the integration tests cover every moved
  URI.
- [A moved endpoint loses an annotation, such as `@Valid` on `addBetrokkene`, which changes validation] → The
  endpoints move verbatim. The OpenAPI comparison and the unchanged integration tests catch a difference.
- [Merge conflicts with the fix tickets that touch the same methods] → Phase 1 is small and lands first; the fixes
  build on it. This is announced in the team before the pull request.
- [The move resets the history in the report] → Expected and documented: the report follows renamed files, not code
  that moves between files, so `ZaakBetrokkeneRestService.kt` starts as a new file with no churn.

## Migration Plan

Not applicable: no database, configuration or deployment change. Rollback is reverting the pull request.

## Open Questions

- The order of phases 2 to 6 is a proposal. Revisit it with the report when phase 1 is merged.
