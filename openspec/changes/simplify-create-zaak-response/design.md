## Context

`ZaakRestService.createZaak` (`src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt:198`) currently ends with:

```kotlin
val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)
```

This computes policy rights and runs the full `RestZaak` conversion purely so the frontend can read one field. Confirmed by reading `ZaakCreateComponent.createZaakMutation` (`src/main/app/src/app/zaken/zaak-create/zaak-create.component.ts:105-113`): its `onSuccess` handler only destructures `identificatie` to navigate to `/zaken/<identificatie>`. The only other consumer of `zakenService.createZaak()` is `ToolbarComponent`, which only reads `.mutationKey` (not response data) — confirmed via grep across `src/main/app/src`.

The ZGW `Zaak` object returned by `zgwApiService.createZaak(...)` (assigned to the local `zaak` val, line 217) already carries `identificatie: String` (non-null, confirmed in the generated `Zaak` model), so it can be returned directly with no extra service calls.

Precedent for bare-`String` JSON REST responses exists in this codebase (`ConfigurationRestService.readGemeenteCode()` / `readGemeenteNaam()`, both `@Produces(MediaType.APPLICATION_JSON)`, returning `String`). `openapi-typescript` generates these as `content: { "application/json": string }`, which the project's `GeneratedType<T>` helper unwraps to a plain `string` at call sites — the same mechanism will apply here with no new frontend typing infrastructure needed.

The itest suite has ~30 files that call `zacClient.createZaak(...)` (raw HTTP wrapper, no parsing) or `zaakHelper.createZaak(...)` (parses `identificatie` and `uuid` out of the response body, returns `Pair<String, UUID>`). Two itest files additionally assert large chunks of the full `RestZaak` JSON structure directly against the create response (`ZaakRestServiceTest.kt` itest, around lines 257 and 1267). All call sites that rely on any response field besides `identificatie` must change.

## Goals / Non-Goals

**Goals:**
- Endpoint returns only the zaak identification string; no unused computation (policy rights, full conversion) on the hot create path.
- Frontend create flow keeps working with a one-line change (no destructuring needed).
- All itest call sites that currently depend on fields other than `identificatie` from the create response continue to get that data through an explicit follow-up read, so no test coverage is lost.

**Non-Goals:**
- Not changing the response shape of `GET /rest/zaken/zaak/{uuid}` or `GET /rest/zaken/zaak/id/{identificatie}` — those keep returning full `RestZaak` and remain the way to fetch a zaak's full details after creation.
- Not touching `RestZaakCreateData`/`RestZaakAanmaakGegevens` (the request body) — only the response changes.

## Decisions

### Return a `CreateZaakResponse` wrapper data class, not a bare `String`
Initially implemented as a bare `String` response (matching an in-repo precedent for bare-string JSON responses, e.g. `ConfigurationRestService.readGemeenteCode()`). Superseded on explicit request: the endpoint now returns a small `data class CreateZaakResponse(var identificatie: String)` (`src/main/kotlin/nl/info/zac/app/zaak/model/CreateZaakResponse.kt`), following the project's `@NoArgConstructor` / `@AllOpen` convention for simple REST DTOs (matching e.g. `RestReden`). This restores object-shaped destructuring on the frontend (`{ identificatie }`) and gives the response room to grow an additional field later without a breaking shape change (object → object is additive; string → object is not).

### Drop the trailing policy/conversion calls entirely
`policyService.readZaakRechten(...)` and `restZaakConverter.toRestZaak(...)` at the end of `createZaak` are removed rather than kept-but-unused, since their only purpose was producing the now-discarded `RestZaak`. `zaakType` and `loggedInUser` remain used earlier in the function (permission checks, `addInitiator`, `startZaak`), so no other cleanup is needed there.

### itest UUID retrieval: follow-up GET instead of parallel response field
`ZaakHelper.createZaak` currently extracts `uuid` directly from the create response. Since `uuid` is no longer present, it will issue a follow-up call to the existing `GET /rest/zaken/zaak/id/{identificatie}` endpoint (already exposed via `zacClient.retrieveZaak(id, testUser)`) to obtain the UUID. This is preferred over, e.g., adding a `Location` header or a secondary UUID field to the create response, because:
- The read endpoint already exists and already returns the UUID; no backend change needed beyond what's already planned.
- It keeps the create endpoint's contract minimal (matches the spec: identification only).
- It mirrors how the frontend itself now works (create → navigate by identificatie → detail page loads full zaak via `readZaakById`).

Trade-off: every itest that needs the zaak UUID now costs one extra HTTP round-trip. Given itest suites already tolerate many sequential calls (Docker/TestContainers-backed), this is an acceptable, and correctly scoped, cost.

### itest files asserting full `RestZaak` structure on the create response
The two `ZaakRestServiceTest.kt` (itest) scenarios that assert broad `RestZaak` JSON structure (bronorganisatie, groep, besluiten, isOpen, etc.) directly against the create response are rewritten to: (1) assert the create response contains only `identificatie`, then (2) call the existing zaak-read endpoint and assert the detailed structure against that response instead. This preserves the original test intent (verifying the zaak was created with the right attributes) while adapting to the new contract. One of these rewrites also surfaced a genuine (pre-existing) timing quirk: the old synchronous create response reflected the zaak *before* the CMMN engine had asynchronously set its first status, so `isInIntakeFase` read as `false`; a fresh read after creation correctly shows `true` once that first status exists — the test's expected value was updated to match, matching an identical assertion already present elsewhere in the same file.

## Risks / Trade-offs

- [Risk] Any itest call site missed during migration will fail loudly (JSON parse error on a bare string, or missing "uuid" key) rather than silently — mitigated by running the full itest suite after the change and by the exhaustive file-by-file task list below.
- [Risk] External consumers of ZAC's REST API outside this repo (if any exist) that depend on the current full-`RestZaak` create response would break — mitigated by this being flagged **BREAKING** in the proposal; per CLAUDE.md conventions this is acceptable for an internal API change with an explicit Jira ticket, but should be called out in the PR description.
- [Trade-off] Slightly more HTTP calls in itests that need post-create zaak details — acceptable given itest suites are not latency-sensitive.

## Migration Plan

1. Update `ZaakRestService.createZaak` return type and body (backend).
2. Update backend unit tests (`ZaakRestServiceTest.kt`) for the two `createZaak` scenarios (CMMN and BPMN) to assert the returned string.
3. Regenerate the OpenAPI spec and generated clients (`./gradlew generateOpenApiSpec generateJavaClients`) and the frontend generated types.
4. Update `ZaakCreateComponent.createZaakMutation` and its spec.
5. Update `ZaakHelper.createZaak` to fetch the UUID via a follow-up read.
6. Update all itest call sites per the file-by-file audit (see tasks.md), split into: no-change files, files needing to switch to `ZaakHelper`/follow-up-read for `uuid`, and files needing a full assertion rewrite.
7. Run `./gradlew test` and `./gradlew itest --info` (after rebuilding the ZAC Docker image) to confirm green.

No feature flag or staged rollout is needed — this is a single-deploy internal API contract change; rollback is a plain revert if issues surface.

## Open Questions

- None outstanding; scope confirmed by direct inspection of `ZaakCreateComponent` and a grep across `src/main/app/src` for all `createZaak`/`createZaakMutation` consumers.
