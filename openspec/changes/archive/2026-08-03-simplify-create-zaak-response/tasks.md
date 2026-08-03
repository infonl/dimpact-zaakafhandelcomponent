## 1. Backend endpoint

- [x] 1.1 Change `ZaakRestService.createZaak` (`src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt:198`) return type from `RestZaak` to `String`, returning `zaak.identificatie` instead of calling `policyService.readZaakRechten(...)` + `restZaakConverter.toRestZaak(...)` at the end of the function.
- [x] 1.2 Remove any now-unused imports in `ZaakRestService.kt` if applicable.

## 2. Backend unit tests

- [x] 2.1 In `src/test/kotlin/nl/info/zac/app/zaak/ZaakRestServiceTest.kt`, update the "Creating a CMMN zaak" scenario (around line 388-417) to assert `restZaakReturned shouldBe zaak.identificatie` instead of `restZaakReturned shouldBe restZaak`, and remove the now-unnecessary `restZaakConverter.toRestZaak(...)` stub/verification for this scenario if it becomes unused.
- [x] 2.2 In the same file, update the "Creating a BPMN zaak" scenario (around line 539-573) the same way.
- [x] 2.3 Run `checkUnnecessaryStub()` (already present in `afterEach`) to confirm no stray `every { restZaakConverter... }` / `every { policyService.readZaakRechten... }` stubs remain unused for these two scenarios. (Also found and fixed 4 unrelated scenarios that stubbed `loggedInUserInstance.get()` only because the old code called it unconditionally as the first line of `createZaak`; that call is gone now, so those stubs were removed.)

## 3. OpenAPI spec and generated clients

- [x] 3.1 Run `./gradlew generateOpenApiSpec` to regenerate the OpenAPI spec for the `POST /rest/zaken/zaak` endpoint. (Confirmed response schema is now `{"type": "string"}`.)
- [x] 3.2 Run `./gradlew generateJavaClients` to regenerate backend-generated API client code if it references this endpoint's response type. (No-op here; this endpoint is ZAC's own API, not an external ZGW client.)
- [x] 3.3 Regenerate frontend TypeScript types (`src/main/app/src/generated/`) from the updated OpenAPI spec so `GeneratedType<"...">` reflects the new bare-string response for this endpoint. (`npm run generate:types:zac-openapi`; `createZaak` operation now types its 200 response as `content: { "application/json": string }`. This file is gitignored/regenerated at build time, so nothing to commit.)

## 4. Frontend

- [x] 4.1 Update `ZaakCreateComponent.createZaakMutation` (`src/main/app/src/app/zaken/zaak-create/zaak-create.component.ts:105-113`) so `onSuccess` receives the identification string directly (no more `{ identificatie }` destructuring) and navigates with it.
- [x] 4.2 Update `src/main/app/src/app/zaken/zaak-create/zaak-create.component.spec.ts` to mock/assert the new plain-string mutation response. (Simplified the mocked `mutationFn` to resolve a plain string; existing navigate-target assertion needed no change.)
- [x] 4.3 Confirm `ToolbarComponent` (`src/main/app/src/app/core/toolbar/toolbar.component.ts:91-92`) still compiles unchanged — it only reads `.mutationKey`, not response data. (Confirmed by reading the file; all 24 `zaak-create.component.spec.ts` tests pass via `ng test`.)

## 5. Itest shared helpers

- [x] 5.1 Update `ZaakHelper.createZaak` (`src/itest/kotlin/nl/info/zac/itest/client/ZaakHelper.kt:46-79`) so it no longer reads `uuid` from the create response body; instead, after creation, parse `identificatie` from the (now bare-string) create response, then call `zacClient.retrieveZaak(zaakIdentification, testUser)` (the existing `GET /rest/zaken/zaak/id/{identificatie}` wrapper) to obtain the `uuid`. Keep the function's public signature/return type (`Pair<String, UUID>`) unchanged so all bucket-A callers keep working with no changes. (Used `JSONTokener(bodyAsString).nextValue() as String` to parse the bare-string create response; `compileItestKotlin` succeeds.)
- [x] 5.2 Verify the 8 `zaakHelper.createZaak`-only callers still pass unchanged: `DetachedDocumentRestServiceTest.kt`, `ZaakKoppelenRestServiceTest.kt`, `SearchRestServiceTest.kt`, `CsvRestServiceTest.kt`, `MailRestServiceTest.kt`, `EnkelvoudigInformatieObjectRestServiceTest.kt`, `EnkelvoudigInformatieObjectRestServiceHistorieTest.kt`, and the `zaakHelper.createZaak` call sites in `ZaakRestServiceTest.kt` (lines 798, 883, 961, 1129, 1166). (No source changes needed — `ZaakHelper.createZaak`'s public signature is unchanged; these files compile against it as-is. Full itest run requires a live ZAC Docker stack, deferred to task 9.4.)
- [x] 5.3 Add a small itest helper (e.g. an extension on `ZacClient` or a local function) that wraps "create zaak, then GET by identificatie" for call sites in section 6 that call `zacClient.createZaak` directly and only need `uuid`/`identificatie` — to avoid repeating the same two-call pattern ~35 times. Reuse `ZaakHelper` where a suitable overload already fits; only add a new helper if `ZaakHelper`'s existing signature doesn't cover a call site's needs (e.g. it doesn't return the raw `ResponseContent`/HTTP status some tests still check). (Added `ZacClient.createZaakAndRetrieve(...)`, mirroring `createZaak`'s exact parameter list — since it returns the full `retrieveZaak(...)` `ResponseContent`, every bucket B/B.2 call site becomes a mechanical rename with zero downstream parsing changes, as they all only ever read `uuid`/`identificatie`/`zaakdata` fields that a full zaak GET still provides.)

## 6. Itest call sites — direct `zacClient.createZaak` extracting only uuid/identificatie (top-level fields)

- [x] 6.1 `PlanItemsRestServiceTest.kt:41-52`
- [x] 6.2 `ZaakRestServiceLinkParentChildZaken.kt:44-56` and `:57-69`
- [x] 6.3 `ZaakRestServiceHistoryTest.kt:37-48`, `:49-61`, `:96-109`
- [x] 6.4 `WebDavServletTest.kt:52-66`
- [x] 6.5 `NotificationZaakDestroyTest.kt:52-67`
- [x] 6.6 `AanvullendeInformatieTaskCompleteTest.kt:34-47`
- [x] 6.7 `TaskRestServiceCompleteTest.kt:39-52`
- [x] 6.8 `BagRestServiceTest.kt:1164-1173`
- [x] 6.9 `TaskRestServiceTest.kt:58-71`
- [x] 6.10 `ZaakRestServiceExtensionTest.kt:35-47`
- [x] 6.11 `ZaakRestServiceTest.kt:431-441` (uuid-only extraction; separate from the bucket-C assertions in the same file, see section 8)
- [x] 6.12 `DocumentCreationRestServiceTest.kt:54-67`
- [x] 6.13 `NotificationZaakUpdateWebSocketListenerTest.kt:46-57`
- [x] 6.14 `SignaleringRestServiceTest.kt:62-74`
- [x] 6.15 `IndexingAdminRestServiceTest.kt:39-51`
- [x] 6.16 `NoteServiceTest.kt:28-39`
- [x] 6.17 `SignaleringAdminRestServiceTest.kt:62-74`
- [x] 6.18 `KlantRestServiceTest.kt:126-140`
- [x] 6.19 `ZaakSuspendRestServiceTest.kt:34-46`

For each: replace the direct `JSONObject(response.bodyAsString).getString("uuid" | "identificatie")` extraction with the create+follow-up-read pattern from section 5. (Done via a mechanical `zacClient.createZaak(` → `zacClient.createZaakAndRetrieve(` rename in each file — call counts per file matched the research report exactly, and all downstream `uuid`/`identificatie` parsing needed no further changes since `createZaakAndRetrieve` returns the full zaak GET response. `./gradlew compileItestKotlin` passes.)

## 7. Itest call sites — direct `zacClient.createZaak` extracting nested `zaakdata.zaakUUID` / `zaakdata.zaakIdentificatie`

These currently read `RestZaak.zaakdata` (a map) instead of the top-level fields, but break identically since the response is no longer a JSON object at all. Migrate each to the same create+follow-up-read pattern, reading `identificatie`/`uuid` from the follow-up GET response's top level instead of a nested `zaakdata` map:

- [x] 7.1 `ZaakRestServiceBrondatumAfleidingswijzeVervaldatumBesluitArchiveTest.kt:47-61`
- [x] 7.2 `ZaakRestServiceBrondatumAfleidingswijzeIngangsdatumBesluitArchiveTest.kt:53-67`
- [x] 7.3 `ZaakRestServiceBrondatumAfleidingswijzeAfgehandeldArchiveTest.kt:46-60`
- [x] 7.4 `ZaakRestServiceBrondatumAfleidingswijzeEigenschapArchiveTest.kt:52-66`
- [x] 7.5 `ZaakRestServiceBrondatumAfleidingswijzeHoofdzaakArchiveTest.kt:53-68` (hoofdzaak) and `:96-111` (deelzaak)
- [x] 7.6 `ZaakRestServiceBrondatumAfleidingswijzeTermijnArchiveTest.kt:47-61`
- [x] 7.7 `ZaakRestServiceCompleteTest.kt:50-64` and `:214-228`
- [x] 7.8 `TaskRestServiceGoedkeurenTest.kt:55-69`
- [x] 7.9 `ZaakBesluitRestServiceTest.kt:52-64`
- [x] 7.10 `bpmn/BpmnSuspendResumeExtendRestServiceTest.kt:47-60`
- [x] 7.11 `bpmn/BpmnDelegateAuthorisationTest.kt:32-52`, `:73-93`, `:114-134`
- [x] 7.12 `bpmn/BpmnSignDocumentRestServiceTest.kt:50-66`
- [x] 7.13 `bpmn/BpmnUserGroupAssignTest.kt:75-95`
- [x] 7.14 `bpmn/BpmnZaakRestServiceTest.kt:56-72` and `:233-248`

Done via the same mechanical rename as section 6 — `retrieveZaak`'s full `RestZaak` response still includes the `zaakdata` map these tests read from, so no downstream parsing changes were needed. Spot-checked `bpmn/BpmnDelegateAuthorisationTest.kt` to confirm the `zaakdata.zaakUUID`/`zaakdata.zaakIdentificatie` extraction still lines up. `./gradlew compileItestKotlin` passes.

## 8. Itest call sites — structural `RestZaak` assertions on the create response (`ZaakRestServiceTest.kt` itest)

- [x] 8.1 In `src/itest/kotlin/nl/info/zac/itest/ZaakRestServiceTest.kt` around lines 230-334: split the current single `zacClient.createZaak(...)` call into (a) an assertion that the create response is exactly the identification JSON string, and (b) a follow-up `zacClient.retrieveZaak(...)` GET whose response the existing `shouldEqualJsonIgnoringOrderAndExtraneousFields` structural comparison (besluiten, bronorganisatie, groep, rechten, zaaktype, etc.) is moved to. Update the `zaak2UUID` extraction (currently `JSONObject(responseBody).getString("uuid")` at line ~334) to read from the follow-up GET response instead. (The create `then` now parses the bare-string response via `JSONTokener(...).nextValue() as String` into `zaakIdentification`. The existing "get zaak endpoint is called as a behandelaar" `when` block — which already did a follow-up GET — now switches to `retrieveZaak(id = zaakIdentification, ...)` and absorbs the full structural assertion (merged with its prior lighter checks) plus the `zaak2UUID` extraction. The "as a beheerder" block was updated to use `id = zaakIdentification` too. `compileItestKotlin` passes.)
- [x] 8.2 In the same file around lines 1254-1281: apply the same split — assert the create response is the plain identification string, then move the `groep`/`behandelaar`/`isOpen` structural assertion (currently `shouldEqualJsonIgnoringOrderAndExtraneousFields` at line ~1267) to a follow-up GET response. (Added a second `when`/`then` doing `retrieveZaak(id = zaakIdentification, ...)` for the structural assertion.)

## 9. Verification

- [x] 9.1 Run `./gradlew spotlessApply detektApply` on backend changes. (`detekt` initially failed with `LargeClass` on `ZacClient.kt` once `createZaakAndRetrieve` was added as a member — moved it to a new top-level extension function file `src/itest/kotlin/nl/info/zac/itest/client/ZacClientZaakExtensions.kt` instead, which required adding an explicit `import nl.info.zac.itest.client.createZaakAndRetrieve` to the 32 call-site files since Kotlin extension functions aren't visible cross-package without an import. `detekt` and `spotlessApply` both pass clean now.)
- [x] 9.2 Run `./gradlew test --tests "nl.info.zac.app.zaak.ZaakRestServiceTest"` to confirm the unit test changes pass.
- [x] 9.3 Run `cd src/main/app && npm run lint && npm test` for the frontend changes. (Lint: 0 errors, 377 pre-existing unrelated warnings. Test: 212 suites / 2531 tests, all passed.)
- [x] 9.4 Rebuild the ZAC Docker image (`./gradlew buildDockerImage`) and run `./gradlew itest --info` to confirm all migrated itest files pass, in particular the 33 files touched in sections 6-8. (First full run: 6 failures, all in `ZaakRestServiceTest.kt`, root-caused to two real bugs found by this run and fixed: (1) task 6.11's call site in that same file was never actually migrated off raw `zacClient.createZaak` — it had been excluded from the section 6/7 bulk sed since this file also contains the bucket-C special cases, and was missed; (2) the section 8.1 restructuring moved the heavy structural assertion — including `isInIntakeFase`, which reflects the CMMN case's live status — to a separate later GET, but the original synchronous create response had captured a transient pre-status-creation snapshot (`isInIntakeFase: false`); a genuine follow-up read correctly shows `true` once the CMMN engine has set its first status, matching an identical assertion already present elsewhere in the same file. Fixed both, then reran: full suite green, 0 failures, `BUILD SUCCESSFUL`.)
- [x] 9.5 Manually smoke-test the "create zaak" flow in the running app (`./start-docker-compose.sh` or dev server) to confirm navigation to the new zaak's detail page still works end-to-end.

## 10. Follow-up: wrap identification in a `CreateZaakResponse` data class

Sections 1-9 above shipped the endpoint returning a bare `String`. Superseded on explicit request: wrap the identification in a small `data class CreateZaakResponse(var identificatie: String)` instead, so the response is an object (`{"identificatie": "..."}`) rather than a bare JSON string.

- [x] 10.1 Add `src/main/kotlin/nl/info/zac/app/zaak/model/CreateZaakResponse.kt` (`@AllOpen @NoArgConstructor data class CreateZaakResponse(var identificatie: String)`, following the `RestReden` convention).
- [x] 10.2 Change `ZaakRestService.createZaak` return type from `String` to `CreateZaakResponse`; return `CreateZaakResponse(zaak.identificatie)`. Remove the KDoc comment on `createZaak` (no longer needed once the return type is self-descriptive).
- [x] 10.3 Update `ZaakRestServiceTest.kt` (unit): both `createZaak` assertions changed from `restZaakReturned shouldBe zaak.identificatie` to `restZaakReturned shouldBe CreateZaakResponse(zaak.identificatie)`.
- [x] 10.4 Regenerate the OpenAPI spec and frontend types (`npm run generate:types:zac-openapi`); confirm `CreateZaakResponse` appears as a proper schema component (`{ identificatie: string }`) and the `createZaak` operation's 200 response references it.
- [x] 10.5 Revert `ZaakCreateComponent.createZaakMutation`'s `onSuccess` back to destructuring `({ identificatie }) => ...`, and update `zaak-create.component.spec.ts`'s mocked `mutationFn` to resolve a `GeneratedType<"CreateZaakResponse">` object instead of a bare string.
- [x] 10.6 Revert the itest bare-string parsing (`JSONTokener(...).nextValue() as String`) back to `JSONObject(...).getString("identificatie")` in `ZacClientZaakExtensions.kt`, `ZaakHelper.kt`, and both call sites in `ZaakRestServiceTest.kt` (itest); remove the now-unused `JSONTokener` imports.
- [x] 10.7 Re-run full verification: `detekt` (clean, no `LargeClass` regression), backend unit test, full frontend test suite (2531 tests), and the full Docker-based itest suite (0 failures, `BUILD SUCCESSFUL`).
- [x] 10.8 Update `proposal.md`, `design.md`, and `specs/zaak-creation-rest-endpoint/spec.md` to describe the `CreateZaakResponse` object shape instead of a bare string.
