## 1. Zaak query key and cache writer

- [x] 1.1 In `src/main/app/src/app/zaken/zaken.service.ts`, add `readZaakQuery(uuid: string)` returning `zacQueryClient.GET("/rest/zaken/zaak/{uuid}", { path: { uuid } })`, so the query key `["/rest/zaken/zaak/{uuid}", { path: { uuid } }]` has one construction point — matching the existing `listBetrokkenenVoorZaakQuery`/`listHistorieVoorZaakQuery` precedent
- [x] 1.2 Add `cacheZaak(zaak)` writing the zaak into the cache entry for its own uuid, with `QueryClient` injected as a **required** dependency (not `{ optional: true }`, and no optional chaining — a missing provider must fail loudly rather than silently skip the write)
- [x] 1.3 Add `provideQueryClient` to the four specs that construct `ZakenService` without one (`zaak-ontkoppelen-dialog`, `zaak-opschorten-dialog`, `extern-advies-mail-task-form`, `aanvullende-informatie-task-form`), which is what 1.2 requires
- [x] 1.4 Extend the existing `zaken.service.spec.ts` (do not overwrite it) with tests for the key shape and for `cacheZaak` writing the exact object

## 2. Back `ZaakViewComponent.zaak` with the cached query

- [x] 2.1 Replace the `zaak!: GeneratedType<"RestZaak">` field with a `zaakUuid` signal, an `injectQuery` over `readZaakQuery`, and a public `get zaak()` returning `zaakQuery.data()!` — leaving `zaak-view.component.html` untouched
- [x] 2.2 In the `route.data` subscription, call `cacheZaak(zaak)` **before** `zaakUuid.set(zaak.uuid)` so the query activates onto fresh data and issues no initial fetch
- [x] 2.3 Delete `init()` and inline its remaining statements; its `this.zaak = zaak` assignment is not inlined, since the query owns that now
- [x] 2.4 Convert every `this.zaak = …` write to `cacheZaak(...)` — the getter makes each one a compile error, so the compiler drives the checklist. Includes three call sites the plan had missed (`openZaakOpschortenDialog`, `openZaakVerlengenDialog`, `updateZaak`)
- [x] 2.5 Convert the one write the compiler cannot catch: `loadBesluiten`'s in-place `this.zaak.besluiten = besluiten` becomes a functional `cacheZaak({ ...this.zaak, besluiten })`
- [x] 2.6 Fix the spec's direct `component.zaak = …` assignment, which no longer compiles against a getter

## 3. Scope the side effects to what they depend on

- [x] 3.1 Split `init()`'s side effects into three effects: `loadBagObjecten` on uuid; `loadOpschorting` on `isOpgeschort`; `setupMenu`/`setDateFieldIconSet`/`ViewResourceUtil` on any content change
- [x] 3.2 Wrap the uuid and `isOpgeschort` effects' calls in `untracked()`, because those methods read the whole `zaak` getter internally and would otherwise widen the effects' dependency sets; add a `computed()` for `isOpgeschort` so it compares on the boolean rather than the object reference
- [x] 3.3 Cover both directions: BAG must not reload on a content-only change, and the action menu must rebuild when `rechten` change

## 4. Feed the edit forms' responses into the cache

- [x] 4.1 `zaak-details-wijzigen` — `updateZaakMutation.onSuccess` caches the response before closing the sidenav
- [x] 4.2 `zaak-locatie-wijzigen` — caches the response, keeping `this.locatie.emit()` (the parent's handler does more than refresh)
- [x] 4.3 `zaak-verlengen-dialog` — caches the response, keeping `this.dialogRef.close(result)`
- [x] 4.4 Assert all three cache writes against the exact response object, not `expect.anything()`

## 5. Suppress the websocket echo by content

- [x] 5.1 Add `onZaakChanged(event)` — capture `getQueryData(key)`, `refetchQueries`, and stay quiet only when the refetch **succeeded** and the reference is unchanged (TanStack's structural sharing returns the previous object for a deep-equal payload). A failed refetch still announces
- [x] 5.2 Reproduce `addListenerWithSnackbar`'s four-part translated message from the `ScreenEvent`, so the wording users see is unchanged
- [x] 5.3 Switch the `ZAAK` listener to `addListener` with that handler; leave `ZAAK_BESLUITEN` on `addListenerWithSnackbar`
- [x] 5.4 Point the `ZAAK_ROLLEN` listener at `invalidateBetrokkenen()` and keep it announcing unconditionally — `RestZaak` cannot carry the betrokkenen list, so content comparison would swallow every rollen change
- [x] 5.5 Remove all eight `suspendListener`/`doubleSuspendListener` calls from `ZaakViewComponent`, and the last one from `zaak-betrokkene-list`. Leave `WebsocketService` and its timeout alone — other components still use them

## 6. Return the updated zaak from the lifecycle endpoints

- [x] 6.1 `ZaakRestService.closeZaak` returns `RestZaak`, re-reading the zaak after `zgwApiService.closeZaak` (which mutates state in OpenZaak, leaving the local object stale)
- [x] 6.2 `reopenZaak` likewise, after the status change and resultaat deletion
- [x] 6.3 `terminateZaak` likewise, with the return **after** the `zaaktypeConfiguration?.let` block so a null configuration still returns the zaak; both early `throw` paths and both `assertPolicy` calls unchanged
- [x] 6.4 Evaluate the response's `rechten` on the re-read zaak — `zaak.open` gates 20 rules in `zaak-rechten.rego`, so stale rights would leave edit affordances live on a closed zaak. `assertPolicy` keeps using the pre-mutation evaluation, since authorisation is decided on the state the user acted on
- [x] 6.5 Cover each endpoint with a test that fails if the implementation converts the stale object — stub `toRestZaak` on the post-operation object only, so `checkUnnecessaryStub()` catches the regression

## 7. Wire the lifecycle responses into the screen

- [x] 7.1 Regenerate the OpenAPI spec and the frontend types (build artifacts, not committed — `/src/generated/*` is git-ignored and CI regenerates it)
- [x] 7.2 Add `is-rest-zaak.ts` to narrow `RestZaak | boolean` from `afterClosed()`, since `ZaakDialogService`'s callbacks are typed `Observable<unknown>` and its dialogs close with `false` on error
- [x] 7.3 `afbreken` and `heropenen` — guard `afterClosed()` with `isRestZaak` and cache the response, falling back to `updateZaak()` for a confirmation-only result
- [x] 7.4 `afsluiten` — cache inside `ZaakAfhandelenDialogComponent.afsluitenMutation.onSuccess` instead. That dialog closes with `true`, discarding the response at the source, so no guard on `afterClosed()` could recover it. Keep it closing with a truthy value
- [x] 7.5 `deleteBetrokkene` in `zaak-betrokkene-list` — guard and cache, keeping its betrokkenen `invalidateQueries` (a separate query the zaak response does not carry)

## 8. Verify

- [x] 8.1 `./gradlew spotlessCheck detekt test --tests "nl.info.zac.app.zaak.*"` — green
- [x] 8.2 `ng test --test-path-pattern="src/app/zaken"` — 33 suites, 503 tests green (pre-change baseline 32 suites, 480 tests)
- [ ] 8.3 End-to-end against a running stack: edit a zaak and confirm the screen updates immediately; watch 30 seconds and confirm no "zaak is gewijzigd" snackbar for your own change; confirm a second user's change still notifies; run afsluiten, heropenen and afbreken and confirm status, resultaat and einddatum update immediately. **Not done** — every test above runs against mocks, so the premise that OpenZaak's response is authoritative and arrives before the notification is not yet proven against the real stack
