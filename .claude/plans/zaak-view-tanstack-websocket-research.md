/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Research: TanStack Query + WebSocket strategy for ZaakViewComponent

This document captures all findings and decisions from the architectural investigation
into migrating `ZaakViewComponent`'s zaak data fetching to TanStack Query, and how to
handle WebSocket-triggered invalidations correctly.

---

## Context

`ZaakViewComponent` currently:
- Loads zaak via Angular route resolver (`ZaakIdentificatieResolver`) which blocks navigation
- Stores zaak as a mutable field `this.zaak`
- Refreshes via `updateZaak()` which calls `zakenService.readZaak().subscribe(zaak => this.init(zaak))`
- Uses `suspendListener` / `doubleSuspendListener` on WebSocket listeners to prevent double-fetches after own mutations
- Has 3 WebSocket listeners:
  - `zaakListener` (ANY, ZAAK, uuid) → `updateZaak()`
  - `zaakRollenListener` (UPDATED, ZAAK_ROLLEN, uuid) → `updateZaak()`
  - `zaakBesluitenListener` (UPDATED, ZAAK_BESLUITEN, uuid) → `loadBesluiten()`

---

## All zaak refresh / reassignment call sites

| Location | How zaak is refreshed | Notes |
|---|---|---|
| `updateZaak()` ×14 call sites | `zakenService.readZaak().subscribe(zaak => init(zaak))` | All safe to replace with `invalidateQueries` |
| `openZaakOpschortenDialog` afterClosed | `this.init(result)` — dialog returns updated zaak directly | Can discard result, use `invalidateQueries` instead |
| `openZaakVerlengenDialog` afterClosed | `this.init(result)` — dialog returns updated zaak directly | Can discard result, use `invalidateQueries` instead |
| `handleNewInitiator` line 978 | `this.zaak = zaak` — returned zaak used for snackbar naam | Extract naam FIRST, then `setQueryData(result)` |
| `deleteInitiator` lines 1015–1017 | Manual `readZaak().subscribe(zaak => { this.zaak = zaak; loadHistorie() })` | Replace with `invalidateQueries` |
| `betrokkeneGeselecteerd` line 1036 | `this.zaak = zaak` — returned zaak not used for anything | Discard, use `setQueryData(result)` |
| `deleteBetrokkene` lines 1079–1082 | Manual `readZaak().subscribe(zaak => { this.zaak = zaak; loadHistorie(); loadBetrokkenen() })` | Replace with `invalidateQueries` |

**Summary:** All call sites are replaceable. One complication: `handleNewInitiator` reads
`zaak.initiatorIdentificatie` from the mutation response for a snackbar — extract that
before calling `setQueryData`/`invalidateQueries`.

---

## WebSocket architecture findings

### Message payload (what the frontend receives)

```
{ opcode, objectType, objectId: { resource, detail? }, timestamp }
```

**No actor / user ID in the message.** The `AbstractEvent` only carries `opcode`, `objectId`,
and a `timestamp` (epoch seconds). There is no "who made this change" field.

### Can we add user ID?

- **For ZAC-own mutations:** technically yes — the backend has the session/JWT. Could populate
  `ScreenEventId.detail` with the acting user ID.
- **For external mutations via OpenNotificaties:** NO — the ZGW Notificaties API standard
  does not carry actor information. Outside ZAC's control.

**Decision:** Not worth adding. A partial solution (works for own, not for external) creates
an inconsistent API. Use `dataUpdatedAt` threshold approach instead.

### Backend delay — IMPORTANT FINDING

The `AbstractEvent.delay()` method exists and `ScreenEventObserver` calls `event.delay()`
before sending. **However: `delay` is never set (defaults to 0) for notification-triggered
screen events.** The `setDelay()` method is never called in the notification path.

The comment in `WebsocketService`:
```typescript
// This must be bigger than the SECONDS_TO_DELAY defined in ScreenEventObserver.java
private static DEFAULT_SUSPENSION_TIMEOUT = 5; // seconds
```
…is **outdated and misleading**. There is no `SECONDS_TO_DELAY` constant in
`ScreenEventObserver.java`. The 5-second suspension timeout is purely a safety buffer
for own-mutation deduplication, calibrated against OpenNotificaties round-trip time.

### Actual timing flow for own mutations

```
t=0ms:      Frontend sends mutation to ZAC backend
t=~100ms:   ZAC processes, writes to Open Zaak, returns response to frontend
t=~100ms:   Frontend receives mutation response (fresh zaak data)
t=~200ms–2s: Open Zaak fires notification to OpenNotificaties
t=~500ms–3s: OpenNotificaties delivers webhook to ZAC
t=~500ms–3s: ZAC fires ScreenEvent (no delay) → WS arrives at frontend
```

### Actual timing flow for external mutations

```
t=0ms:      External user/system writes to Open Zaak
t=~100ms:   Open Zaak commits data AND fires notification to OpenNotificaties
t=~500ms–2s: OpenNotificaties delivers to ZAC
t=~500ms–2s: ZAC fires ScreenEvent → WS arrives at frontend
t=~600ms–3s: Frontend invalidates query → fetches from ZAC → reads from Open Zaak
```

By the time the OpenNotificaties chain completes, Open Zaak has long since committed.
Open Zaak uses a single PostgreSQL instance → read-after-write consistent.
**This is the same timing risk as today's `updateZaak()`.** TanStack doesn't change it.

---

## Timing concern: WS arrives late or not at all

**NEW OPEN QUESTION (raised at end of conversation, not yet resolved):**

The user raised the inverse concern: what if the WebSocket event takes too long to arrive
(or never arrives — network blip, WS reconnect, etc.)? Should we have a fallback that
invalidates after a timeout?

This is a valid reliability concern. Possible approaches:

**Option A — `refetchInterval` as safety net**
TanStack supports `refetchInterval: 30_000` (e.g. 30s). If the WS event never arrives,
the query auto-refreshes every 30s as a backstop. Low overhead, no extra logic.

**Option B — WS reconnect re-invalidates**
On WebSocket reconnect (already handled by `retryWhen` in the service), force an
`invalidateQueries` for the zaak. Handles the "WS was down, missed events" case.

**Option C — Timestamp-based: if WS hasn't arrived within N seconds after mutation,
force a refetch**
More complex, probably overkill given Option A covers it simply.

**Recommendation (not yet decided):** Combine `setQueryData` (own mutations) +
`dataUpdatedAt` threshold (WS deduplication) + `refetchInterval: 30_000` (safety net
for missed WS events). To be confirmed.

---

## Proposed TanStack migration strategy

### For own mutations: `setQueryData`

When the mutation response contains the updated zaak (opschorten, verlengen, initiator
update, betrokkene add), write directly to the cache — no HTTP refetch needed:

```typescript
queryClient.setQueryData(
  this.zakenService.readZaakQuery(this.zaak.uuid).queryKey,
  mutationResult
)
```

### For own mutations: `invalidateQueries`

When the mutation does not return a zaak (afbreken, heropenen, deleteBetrokkene,
bagObjectVerwijderen, etc.):

```typescript
queryClient.invalidateQueries({
  queryKey: this.zakenService.readZaakQuery(this.zaak.uuid).queryKey
})
```

### For WebSocket events: `dataUpdatedAt` threshold

```typescript
private invalidateIfStale() {
  const key = this.zakenService.readZaakQuery(this.zaak.uuid).queryKey
  const state = this.queryClient.getQueryState(key)
  const age = Date.now() - (state?.dataUpdatedAt ?? 0)
  if (age > 5_000) {  // 5s matches former suspendListener timeout
    void this.queryClient.invalidateQueries({ queryKey: key })
  }
}
```

Wire all three WS listener callbacks to `invalidateIfStale()`.

**Why 5000ms:** Own mutation completes at ~100ms, WS arrives at ~500ms–3s. A 5s threshold
safely covers the worst-case OpenNotificaties round-trip while being short enough to
always react to external changes. Same value as the existing `DEFAULT_SUSPENSION_TIMEOUT`.

### Consequence: remove all `suspendListener` calls

Once `invalidateIfStale()` handles deduplication via `dataUpdatedAt`, all
`suspendListener(this.zaakListener)` and `suspendListener(this.zaakRollenListener)`
calls throughout the component can be deleted. There are ~8 of them.

---

## Route resolver situation

Two resolvers exist:
- `ZaakUuidResolver` — resolves by UUID param, **not used in current routing**
- `ZaakIdentificatieResolver` — resolves by identificatie param, used for `/:zaakIdentificatie`

Route config:
```typescript
{
  path: ":zaakIdentificatie",
  component: ZaakViewComponent,
  resolve: { zaak: ZaakIdentificatieResolver },
}
```

`ZaakIdentificatieResolver` does: `zakenService.readZaakByID(zaakID)` → blocks navigation
until zaak loads.

**With TanStack:** resolver is removed. Component reads `zaakIdentificatie` from route
params, calls `readZaakByID` via `injectQuery`. Navigation no longer blocks; loading
state handled by Angular + TanStack. `ZaakUuidResolver` can be deleted (unused).

Note: `readZaakByID` (by identificatie) differs from `readZaak` (by UUID). The component
currently gets the UUID from the resolved zaak and uses it for all subsequent calls.
With TanStack, after the initial `readZaakByID` query resolves, the UUID is available
from `zaakQuery.data().uuid` for use in WS listeners and sub-queries.

---

## Phase 0 implementation steps (concrete)

### Step 1 — Add `queryOptions` methods to `ZakenService`
```typescript
readZaakQuery(uuid: string) {
  return this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}", { path: { uuid } })
}

readZaakByIdQuery(identificatie: string) {
  return this.zacQueryClient.GET("/rest/zaken/zaak/id/{identificatie}", {
    path: { identificatie }
  })
}
```

### Step 2 — Remove `ZaakIdentificatieResolver` from route config
Remove `resolve: { zaak: ZaakIdentificatieResolver }` from `zaken-routing.module.ts`.
Delete `ZaakUuidResolver` (unused). Keep `ZaakIdentificatieResolver` file temporarily
until all usages confirmed gone.

### Step 3 — Add `injectQuery` to `ZaakViewComponent`
```typescript
private readonly zaakIdentificatie = toSignal(
  this.route.paramMap.pipe(map(p => p.get("zaakIdentificatie")!))
)

protected readonly zaakQuery = injectQuery(() =>
  this.zakenService.readZaakByIdQuery(this.zaakIdentificatie())
)

// Convenience getter — avoids rewriting the entire template
protected get zaak() { return this.zaakQuery.data()! }
```

### Step 4 — Replace `init()` with `effect()`
```typescript
constructor() {
  super()
  effect(() => {
    const zaak = this.zaakQuery.data()
    if (!zaak) return
    this.setupMenu()
    this.loadOpschorting()
    this.loadNotitieRechten()
    ViewResourceUtil.actieveZaak = zaak
    this.utilService.setTitle("title.zaak", { zaak: zaak.identificatie })
  })
}
```

Note: `loadHistorie`, `loadBetrokkenen`, `loadBagObjecten`, `setDateFieldIconSet` are
removed from `init()` here — they move to child tab components in Phase 1.

### Step 5 — Wire WebSocket listeners after zaak UUID is available
WS listeners need `zaak.uuid`. With TanStack, UUID is available from the query signal.
Wire listeners inside the `effect()` on first load, or use a separate effect that only
fires once:

```typescript
private zaakListenersRegistered = false

// Inside the effect():
if (!this.zaakListenersRegistered && zaak.uuid) {
  this.zaakListenersRegistered = true
  this.registerZaakListeners(zaak.uuid)
}
```

### Step 6 — Implement `invalidateIfStale()` and wire to WS callbacks
(See code above in strategy section.)

### Step 7 — Replace all `updateZaak()` call sites
- Mutations returning zaak → `setQueryData(mutationResult)` (extract snackbar data first)
- Mutations not returning zaak → `invalidateQueries`

### Step 8 — Delete `updateZaak()` method

### Step 9 — Delete all `suspendListener` / `doubleSuspendListener` calls for zaak + rollen listeners

### Step 10 — Handle `zaakBesluitenListener`
Currently calls `loadBesluiten()` which sets `zaak.besluiten` directly. With TanStack,
`zaak.besluiten` is part of the zaak query — just call `invalidateIfStale()` here too,
no special `loadBesluiten()` needed.

---

## What disappears after Phase 0

| Removed | Replaced by |
|---|---|
| `ZaakIdentificatieResolver` blocking navigation | `injectQuery` in component |
| `this.zaak` mutable field | `zaakQuery.data()` signal via getter |
| `updateZaak()` method | `invalidateQueries` at each call site |
| `this.zaak = zaak` direct assignments | `setQueryData(result)` |
| Manual `readZaak().subscribe()` in component | automatic via query |
| All `suspendListener` for zaak/rollen (~8 calls) | `invalidateIfStale()` threshold |
| `init(zaak)` method | `effect()` reacting to query data signal |
| `loadBesluiten()` partial update | `invalidateIfStale()` in zaakBesluitenListener |

---

## Open questions / decisions pending

1. **WS late arrival / safety net:** should we add `refetchInterval: 30_000` as a backstop
   for missed WS events? (User raised this at end of conversation — not yet resolved.)

2. **`readZaakByIdQuery` vs `readZaakQuery`:** the route uses identificatie (human-readable
   ID like "ZAAK-2024-001"), not UUID. After first load we have the UUID. Sub-queries
   (loadHistorie, loadBetrokkenen etc.) use UUID. Consider whether to cache both queryKeys
   or only UUID-based ones.

3. **`loadNotitieRechten`:** currently a separate `policyService.readNotitieRechten()` call,
   stored as `notitieRechten` field, passed as `@Input` to `ZaakAlgemeenTabComponent`.
   Could become its own `injectQuery` — separate ticket.

4. **`loadOpschorting`:** only fetched when `zaak.isOpgeschort`. Could become a conditional
   `injectQuery(() => zaak?.isOpgeschort ? opschortingOptions(zaak.uuid) : skipToken)`.
   Currently stays in parent since `zaakOpschorting` is passed to `ZaakAlgemeenTabComponent`
   as `@Input` AND used in `openZaakHervattenDialog`.

---

## Relationship to tab extraction plan

The tab extraction plan (see `zaak-view-details-extraction.md`) and Phase 0 (TanStack
migration) are **independent and can be sequenced either way**, but Phase 0 first is
cleaner because:

- Tab components start life with `injectQuery` patterns from day one
- No `@Output() zaakUpdated` events needed — tabs call `invalidateQueries` directly
- The parent wiring is simpler from the start

**Recommended order:**
1. Phase 0: zaak → TanStack (this document)
2. Phase 1–6: tab extractions (zaak-view-details-extraction.md)
3. Phase 7: `ZaakDetailsComponent` wrapper

---

## Key files touched in Phase 0

| File | Change |
|---|---|
| `zaken.service.ts` | Add `readZaakQuery`, `readZaakByIdQuery` |
| `zaken-routing.module.ts` | Remove resolver from route config |
| `zaak-view.component.ts` | Replace everything described above |
| `zaak-identificatie-resolver.service.ts` | Delete |
| `zaak-uuid.resolver.ts` | Delete (already unused) |
