## Context

ZAC writes to OpenZaak synchronously and reads back asynchronously. `PATCH /zaken/{uuid}` returns 200 with the full updated `Zaak`, `POST /zaken` returns 201 with the created one, and `ZaakRestService` already converts those responses into a fully hydrated `RestZaak` — including a fresh `listRollen` call inside `RestZaakConverter.toRestZaak`, so even the assignment endpoints that convert from a pre-update object return correct `groep`/`behandelaar`.

The frontend threw that away. Every mutation's response was discarded, and the screen waited for a websocket event that only exists because Open Notificaties eventually tells ZAC something changed. That path also drives Solr reindexing (`NotificationReceiver.handleIndexing`), which is why it cannot be removed wholesale.

That split matters for scope. Reads that go straight to OpenZaak — `GET /rest/zaken/zaak/{uuid}`, documents, betrokkenen, historie — are immediately consistent, so they can be driven from a mutation response today. Solr-backed reads (werkvoorraad, zoeken, dashboard cards) are only consistent once reindexing has run; the existing workaround for that, a literal `this.dataSource.load(5_000)` in `zaken-werkvoorraad.component.ts`, is evidence of the problem rather than a thing to build on.

`ZaakViewComponent` was the structural obstacle. Its `zaak` was a plain mutable field fed by a route resolver, so there was no key to write to and no way for a child component to update the parent except through an output binding per child. Children mutated it in place (`this.zaak.besluiten = besluiten`).

## Goals / Non-Goals

**Goals:**
- Make the zaak screen reflect a save immediately, using the response the backend already returns, without waiting for a webhook
- Give the zaak a single cache entry with one key-construction point, so any component can update the screen without knowing who renders it
- Stop announcing the user's own change back to them 5–20 seconds later, without losing announcements of changes made by other users
- Give `afsluiten`, `heropenen` and `afbreken` — the most visible actions, and the ones with no response body to work from — the same treatment

**Non-Goals:**
- Optimistic updates (`onMutate` + rollback). Unnecessary here: the write has already succeeded before the cache is written, so there is nothing to roll back and no risk of showing a change that did not happen
- Solr-backed list and search screens. A cache write there would be reverted by the next refetch until reindexing catches up; those keep their notification-driven refresh
- `taak-view`, `zaak-documenten` and besluiten. The same recipe applies and is now cheap, but they are a separate change
- Changing `WebsocketService` or its `DEFAULT_SUSPENSION_TIMEOUT`. This change removes zaak-view's *need* for timer-based suspension; other components still rely on it
- Migrating the rest of the app off `ZacHttpClient` onto TanStack Query. Roughly a quarter of components had been migrated when this change started; this is not the change that finishes that

## Decisions

### The mutation response is the source of truth, not a trigger to refetch

Every endpoint in scope already returns a complete `RestZaak` built from OpenZaak's own response. Feeding that into the cache is not an optimistic update — the write is already committed and durable when the response arrives — so no rollback path, no reconciliation, and no risk of displaying state that does not exist. This is what makes the change small: the fix is to stop discarding data, not to predict it.

### `cacheZaak` lives on `ZakenService`, not on the parent component

This is the load-bearing choice. Putting the cache writer on the service means `CaseDetailsEditComponent.onSuccess` calls `zakenService.cacheZaak(response)` and the zaak screen updates with no output binding and no knowledge of who edited what. Had it lived on `ZaakViewComponent`, every one of the ~10 mutation sites would have needed its own binding through the component tree, and each future surface would need another.

`QueryClient` is injected as a **required** dependency. An earlier attempt used `inject(QueryClient, { optional: true })` with `this.queryClient?.setQueryData(...)` to keep four specs green that construct `ZakenService` without a provider. That was rejected: `cacheZaak`'s only purpose is the write, and optional chaining turns a missing provider into a silent no-op — the screen simply never updates, with no error to trace, which is the exact failure class this change exists to remove. The four specs got a provider instead.

### `zaak` becomes a getter, not a `computed` signal

The template references the bare `zaak` identifier 96 times across 73 lines. A `computed` would have meant 96 mechanical `zaak()` edits with no upside. The component uses default change detection, and Angular tracks signal reads made during template evaluation regardless of call depth, so a getter that reads `zaakQuery.data()` both registers the reactive dependency and re-evaluates each cycle. Getters called from templates are already an established pattern here (`column-picker.component.ts`, `side-nav.component.ts`). A useful side effect: every `this.zaak = …` assignment became a compile error, which turned the write-path conversion into a checklist the compiler enforced.

The getter keeps the non-null assertion the old `zaak!:` field already carried. Improving on that would mean a template guard and the `@ViewChild` timing risk that comes with it, which this change does not take on.

### The query is keyed on uuid, and the route subscription seeds it

The route resolves by **identificatie** (`/rest/zaken/zaak/id/{identificatie}`) while every refresh uses **uuid**. The uuid key is canonical: `readZaakByID` performs a filtered list query against OpenZaak before reading, so keying on identificatie would make every refetch pay for a search, and any existing uuid read would create a competing entry for the same zaak.

Seeding happens in the existing `route.data` subscription — `cacheZaak(zaak)` *before* `zaakUuid.set(zaak.uuid)`, so the query activates onto already-fresh data and issues no initial fetch. Reading `route.snapshot` once was rejected: route data genuinely re-emits, and freezing on the first zaak would break both the existing specs and any navigation that reuses the component.

### Effects are scoped by what the called methods actually read, not by what they logically depend on

The side effects formerly run together by `init()` depend on different things, and one effect for all of them breaks something whichever guard is chosen: uuid-guarded freezes the action menu and the opschorting panel after a save, content-guarded re-fires BAG loading on every write.

The subtlety that decided the implementation: `invalidateZaakHistorie`, `loadBagObjecten` and `loadOpschorting` all read the whole `zaak` getter internally for its `.uuid`, which silently widens the tracked dependency set of whatever effect calls them. `untracked()` narrows the uuid and `isOpgeschort` effects, with a `computed()` for `isOpgeschort` so it compares on the boolean rather than the object reference (`cacheZaak` always produces a new reference). Note that `untracked()` suppresses dependency *registration*, not *execution* — an early attempt at historie invalidation un-wrapped it inside the uuid effect, which made that effect content-dependent and re-ran `loadBagObjecten` on every write.

### Echo suppression compares content, not elapsed time

The websocket event carries only a uuid: `ScreenEventType` calls `addEvent(events, resource, null)` for zaak events, so `ScreenEventId.detail` is empty, and although the frontend's `SocketMessage` type declares `timestamp?: number`, the backend `ScreenEvent` has no such field and never sends one. There is nothing in the event to diff.

So the comparison happens one step later. TanStack's `replaceData` applies `replaceEqualDeep` whenever `structuralSharing !== false` (the default), and `replaceEqualDeep` returns the **previous** object when payloads are deep-equal. Comparing `getQueryData(key)` by reference identity across a refetch is therefore a valid "nothing actually changed" test, needing no deep-equal helper. Suppression only applies when the refetch actually succeeded, so a failed GET still announces rather than silently doing nothing.

Populating an event payload server-side was rejected: Open Notificaties does not send the zaak body either, so ZAC would have to fetch it itself — moving the same fetch to the backend and adding a websocket contract change for no gain.

The `ZAAK_ROLLEN` listener is deliberately exempt. `RestZaak` does not carry the betrokkenen list, so a rollen change from another user produces a deep-equal zaak and would be suppressed forever. That listener always notifies and invalidates the betrokkenen query instead.

### Lifecycle endpoints re-read, and evaluate response rights on the re-read

`zgwApiService.closeZaak` and `createStatusForZaak` mutate status and resultaat inside OpenZaak, so the local `zaak` is stale when the function returns — converting it would return data that looks right in tests and is wrong at runtime.

The `rechten` must come from the re-read too. `zaak.open` gates 20 rules in `zaak-rechten.rego`, so rights genuinely change when a zaak closes or reopens; returning fresh zaak fields alongside pre-mutation rights would leave `isOpen=false` next to `rechten.wijzigen=true`, keeping edit affordances live for 5–20 seconds that 403 when used. `assertPolicy` still evaluates on the pre-mutation state, because authorisation must be decided on the state the user acted on.

The same pre-mutation-rechten pattern exists in `suspendZaak`, `resumeZaak`, `verlengenZaak` and `updateZaakLocatie`. Those did not regress — their responses already drove the screen — but the fix arguably belongs on all of them, and is left as a follow-up.

### No null-guard in `cacheZaak`

All endpoints in scope return a non-null `RestZaak` by their Kotlin signatures, so an empty body is a backend contract violation rather than a case to model. Guarding would contradict both the project convention of handling nulls at the source and the decision above that misconfiguration should fail loudly. If an endpoint ever returns an empty body, `onSuccess` throws and the dialog stays open with a console error — visible and traceable, which is the failure mode to prefer over a silent no-op.
