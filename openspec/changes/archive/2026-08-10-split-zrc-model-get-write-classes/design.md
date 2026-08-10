## Context

`nl.info.client.zgw.zrc.model.ZaakInformatieobject` conflates a GET response shape with a POST/PUT/PATCH request shape in a single hand-written class, forcing GET-only fields (`url`, `uuid`) to be nullable and forcing `!!` at every call site that "knows" it's holding a GET result.

The generated OpenAPI client (`nl.info.client.zgw.zrc.model.generated`) already models the GET/write split correctly for this non-polymorphic schema: `ZaakInformatieObject`/`ZaakInformatieObjectRequest`. Since there's no polymorphism involved, the generated classes are strictly sufficient and there is no reason to keep a hand-written duplicate.

`Rol<T>` and `Zaakobject` in the same package have the identical dual-purpose problem, but are deliberately out of scope here — they involve polymorphism (a `oneOf` with a discriminator over several leaf shapes) that the OpenAPI Java generator flattens into fully independent per-leaf classes with no shared base type, so they can't simply be replaced by generated types the way `ZaakInformatieobject` can. That's a separate, larger piece of work for a future change.

## Goals / Non-Goals

**Goals:**
- Make `url`/`uuid`/`aardRelatieWeergave`/`registratiedatum` non-null on the generated GET-response type, and drop them entirely from the generated write-request type (already the case in the generated code — this change just adopts it).
- Remove the `!!` assertions in `ZrcClientService.kt`, `ZgwApiService.kt`, and `ZaakRestService.kt` that exist only to work around the old hand-written class's nullability.
- Delete the hand-written `ZaakInformatieobject` class.
- Keep JSON wire behavior unchanged: the ZGW API sees the same request/response bodies before and after.

**Non-Goals:**
- Splitting or otherwise touching `Rol<T>` or `Zaakobject` — tracked separately for a future change.
- Changing any ZGW API request/response wire format.

## Decisions

### `ZaakInformatieobject` → adopt generated classes, delete the hand-written class
Replace every use of `nl.info.client.zgw.zrc.model.ZaakInformatieobject` with `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject` (GET, via `ZrcClient.zaakinformatieobjectRead`/`zaakinformatieobjectList`) and `...generated.ZaakInformatieObjectRequest` (write, via `ZrcClient.zaakinformatieobjectCreate` — its parameter type changes from `ZaakInformatieobject` to `ZaakInformatieObjectRequest`). The `zaakUUID` derivation (`zaak.extractUuid()`) becomes a Kotlin extension property `val ZaakInformatieObject.zaakUUID: UUID` (in `nl.info.client.zgw.zrc.model.ZaakInformatieObjectExtensions.kt`), since it can't be a member of the generated Java class.

**Alternative considered:** keep the hand-written class but split it in two (mirroring what a `Rol`/`Zaakobject` fix would need). Rejected — there is no polymorphism problem here, so the generated classes are strictly sufficient and this avoids maintaining a duplicate.

## Risks / Trade-offs

- [Call-site surface spans many files — every constructor call and every place that reads a GET-only field must be updated in the same change, or the build won't compile] → Kotlin's compiler makes this a mechanical, compile-error-driven migration; no behavior can silently regress since the compiler forces every site to be visited.
- [Two legacy Java files (`Signalering.java`, `SignaleringEventObserver.java`) reference the old type and needed updating] → updated the type reference only (kept as Java), rather than converting them to Kotlin as a drive-by, to keep this change scoped to the model split.
- [Test fixtures (`ZrcFixtures.kt`) construct this class with the old dual-purpose constructors] → split into `createZaakInformatieobjectForReads` (generated `ZaakInformatieObject`) and `createZaakInformatieobjectForCreatesAndUpdates` (generated `ZaakInformatieObjectRequest`); updated every call site accordingly, including several that were passing the wrong side (e.g. echoing a request back as if it were a response) once the compiler surfaced the mismatch.

## Migration Plan

Single-repo, single-deploy change — no data migration, no wire-format change, no phased rollout needed. Update the model usage, then follow compiler errors through the rest of the codebase and tests. No rollback strategy beyond reverting the commit(s) — this is a compile-time-checked internal refactor with no runtime feature flag.

## Open Questions

None outstanding. `Rol`/`Zaakobject` follow-up: a future change should investigate the same GET/write split for those hierarchies, keeping in mind that the generated classes cannot be reused directly for them (no shared base type across leaf types).
