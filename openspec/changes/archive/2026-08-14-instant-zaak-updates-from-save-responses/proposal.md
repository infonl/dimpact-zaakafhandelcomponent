## Why

Saving a zaak in ZAC does not update the screen for 5–20 seconds. The write itself is fast and synchronous: OpenZaak is a plain REST API whose `PATCH /zaken/{uuid}` returns 200 with the full updated `Zaak` (`ZrcClient.zaakPartialUpdate`), and ZAC's own REST layer already forwards it — `ZaakRestService.updateZaak` returns `restZaakConverter.toRestZaak(updatedZaak, …)` built from OpenZaak's own PATCH response, not from a re-read.

The delay is entirely on the read side. The frontend discards that response body and instead waits for a websocket event that originates from Open Notificaties: OpenZaak → Open Notificaties → `POST /rest/notificaties` → `NotificationReceiver.handleWebsockets` → `EventingService` → websocket → `ZaakViewComponent.updateZaak()` → `GET /rest/zaken/zaak/{uuid}`. The authoritative new state was already in the response to the save; the round trip exists only to learn *that* something changed.

Two defects follow from the same design. `WebsocketService.suspendListener` exists to swallow the echo of your own change, but its window is 5 seconds while notifications arrive at 5–20 — so the suspension usually expires first and users get a "zaak is gewijzigd" snackbar for their own edit. And `ZaakViewComponent` holds `zaak` as a plain mutable field fed by a route resolver, with no cache entry and no key, so nothing but the component itself can update it; child components resort to mutating it in place.

## What Changes

- `ZakenService` gains `readZaakQuery(uuid)` (the single construction point for the zaak query key) and `cacheZaak(zaak)` (writes an authoritative `RestZaak` into that cache entry). `QueryClient` is injected as a **required** dependency, so a missing provider fails loudly at injection rather than degrading to a silent no-op
- `ZaakViewComponent.zaak` changes from a mutable field to a getter over an `injectQuery` keyed on `["/rest/zaken/zaak/{uuid}", { path: { uuid } }]`. The template is untouched — it references the bare `zaak` identifier 96 times across 73 lines, and a getter keeps every one of them working
- The side effects previously run together by `init()` are split into three Angular `effect()`s scoped by what they actually depend on: `loadBagObjecten` on uuid only, `loadOpschorting` on `isOpgeschort`, and `setupMenu`/`setDateFieldIconSet`/`invalidateZaakHistorie`/`ViewResourceUtil` on any content change. `untracked()` narrows the first two, because the methods they call read the whole `zaak` getter internally and would otherwise widen their own dependency sets
- Every mutation that already receives a full `RestZaak` writes it into the cache instead of discarding it: zaakgegevens wijzigen, locatie, verlengen, initiator toevoegen/wijzigen/ontkoppelen, betrokkene toevoegen/verwijderen
- `ZaakRestService.closeZaak`, `reopenZaak` and `terminateZaak` change from returning `Unit` to returning `RestZaak`. Each re-reads the zaak after mutating it (`zgwApiService.closeZaak`/`createStatusForZaak` mutate state in OpenZaak, so the local object is stale) and evaluates the response's `rechten` on that re-read zaak, while `assertPolicy` keeps deciding authorisation on the pre-mutation state the user acted on
- Websocket echo suppression becomes content-based: the `ZAAK` listener refetches and compares object identity, relying on TanStack's structural sharing returning the previous reference for a deep-equal payload. All eight `suspendListener`/`doubleSuspendListener` calls are removed from `ZaakViewComponent`; `WebsocketService` itself is unchanged, since other components still use timer-based suspension
- The `ZAAK_ROLLEN` listener instead invalidates the betrokkenen query and always notifies, because `RestZaak` structurally cannot carry the betrokkenen list and content comparison would silently swallow every rollen change

## Capabilities

### New Capabilities

- `zaak-update-propagation`: how a zaak change reaches the screen — the mutation response as the authoritative source, the shared cache entry as the single write target, and the content-based rules for when a websocket event is announced versus silently absorbed

### Modified Capabilities

_None._

## Impact

- `src/main/app/src/app/zaken/zaken.service.ts` — gains `readZaakQuery`/`cacheZaak` and a required `QueryClient` injection. Four specs that construct `ZakenService` without a `QueryClient` gain `provideQueryClient`
- `src/main/app/src/app/zaken/zaak-view/zaak-view.component.ts` — `zaak` becomes a query-backed getter; `init()` is deleted and its side effects split across three scoped effects; the websocket handler becomes refetch-and-compare; eight suspension calls removed. The `.html` template is unchanged
- `zaak-details-wijzigen`, `zaak-locatie-wijzigen`, `zaak-verlengen-dialog`, `zaak-afhandelen-dialog`, `zaak-betrokkene-list` — each writes its mutation response into the cache. Three distinct shapes are needed, because the dialogs differ in what they close with
- `src/main/app/src/app/zaken/is-rest-zaak.ts` (new) — narrows `RestZaak | boolean` from `MatDialogRef.afterClosed()`, since `ZaakDialogService`'s callbacks are typed `Observable<unknown>` and its dialogs close with `false` on error
- `src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt` — three endpoints gain a `RestZaak` return type and a post-mutation re-read. The OpenAPI spec and the generated frontend types change accordingly; both are build artifacts and are not committed
- No database schema change, no new dependencies, no change to the ZGW API contract. Solr-backed screens (werkvoorraad, zoeken, dashboard) are deliberately untouched
