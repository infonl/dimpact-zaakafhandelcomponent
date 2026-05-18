# Dashboard cards — migrate to TanStack Query

## Why we are doing this

### The current situation

The dashboard cards currently use two different approaches to fetch data:

- Three cards (`taak-zoeken-card`, `zaak-zoeken-card`, `zaken-card`) already use TanStack Query — but with hand-written query keys and inline `queryFn` definitions, bypassing the `ZacQueryClient` pattern the rest of the codebase uses.
- Three cards (`taken-card`, `zaak-waarschuwingen-card`, `informatieobjecten-card`) still use plain `.subscribe()` with a custom reload mechanism built into the base class.

This means the dashboard is maintained in two different mental models simultaneously, making it harder to reason about, test, and extend.

### The custom reload mechanism is solving a problem TanStack already solves

The base class (`DashboardCardComponent`) contains a hand-rolled reload system:
- a 60-second interval timer (`refreshTimed`)
- a websocket listener that emits on signalering events (`refreshOnSignalering`)
- a `reloader` subscription that calls `onLoad()` on every tick
- an abstract `onLoad()` that each subclass implements

This is essentially a cache invalidation system built from scratch. TanStack Query provides exactly this — and does it better:
- automatic background refetching
- stale-while-revalidate behaviour
- deduplication of concurrent requests
- cache invalidation by query key via `invalidateQueries`
- built-in loading/error state without manual flags

Every time we need new behaviour (cancel in-flight requests, deduplicate rapid reloads, vary stale time per card) we would have to extend the hand-rolled mechanism. With TanStack it is already there.

### Inconsistency makes invalidation fragile

The three cards that already use TanStack bypass `ZacQueryClient` and define their own query keys as plain strings like `"aan mij toegekende zaken signaleringen"`. This means:

- Invalidating these queries from another part of the application requires knowing and spelling that exact string correctly — no type safety, no autocomplete.
- `ZacQueryClient` derives query keys from the API URL (`["/rest/signaleringen/taken/{type}", ...]`), so invalidation by URL prefix is predictable and consistent with how every other query in the codebase works.

As the application grows and mutations in one domain need to invalidate dashboard cards in another (e.g. assigning a zaak should refresh the taken-card), having inconsistent keys becomes a real maintenance problem.

### The `ZacQueryClient` pattern is the established standard

Across the rest of the codebase, the pattern is:
1. Service exposes `queryOptions` via `ZacQueryClient.GET(url, params)`
2. Component calls `injectQuery(() => service.method())`
3. Invalidation uses the URL as the key — predictable, type-safe, no magic strings

The dashboard is the largest remaining area that does not follow this pattern. Migrating it brings the entire frontend into a single, consistent data fetching model that every developer on the team already knows.

### What we gain

| | Now | After migration |
|---|---|---|
| Data fetching | Mixed `.subscribe()` + `injectQuery` | Uniform `injectQuery` via `ZacQueryClient` |
| Reload on signalering | Hand-rolled websocket → `onLoad()` | `invalidateQueries` by URL key |
| 60s polling | Manual `interval()` subscription | TanStack `refetchInterval` per card |
| In-flight cancellation | Not handled | Automatic via TanStack |
| Loading state | Not exposed | `query.isLoading()` available in template |
| Query keys | Magic strings in components | URL-derived, type-safe, in services |
| Base class complexity | Reload mechanism, subscriptions, lifecycle management | Reduced to dataSource + ViewChild wiring |
| Testability | Mock service + call `onLoad()` directly | Standard TanStack testing patterns |

---

## Step-by-step plan

Work through the steps in order. Each step has its own test requirement — do not move to the next step until tests are green.

---

### Step 1 — Extend `ZacQueryClient` with `PUT_AS_QUERY`

**File:** `src/main/app/src/app/shared/http/zac-query-client.ts`

The Solr search endpoint (`/rest/zoeken/list`) uses HTTP `PUT` with a request body, but behaves semantically as a read (it is a search query, not a mutation). `ZacQueryClient.PUT` currently returns `mutationOptions`, which is wrong for a query. Add a dedicated method:

```typescript
public PUT_AS_QUERY<Path extends PathsWithMethod<Paths, "put">>(
  url: Path,
  body: PutBody<Path, "put">,
) {
  return queryOptions<Response<Path, "put">, HttpErrorResponse>({
    queryKey: [url, body],
    queryFn: () => lastValueFrom(this.httpClient.PUT(url, body)),
    retry: (failureCount, error) => {
      if (failureCount >= DEFAULT_RETRY_COUNT) return false;
      return error.status === 0 || error.status >= 500;
    },
    refetchOnWindowFocus: false,
    staleTime: StaleTimes.Instant,
  });
}
```

> **Why `staleTime: Instant`?** Search results depend on the full request body (filters, sort, page). Caching them longer than the current render is not useful — when the body changes (e.g. user clicks a new page), a new request must always fire.

**Test:** add a unit test in `zac-query-client.spec.ts` verifying that `PUT_AS_QUERY` returns an object with a `queryKey` containing both the URL and the body, and a callable `queryFn`.

---

### Step 2 — Migrate services to `ZacQueryClient`

For each service below, replace the existing Observable-returning method with one that returns `queryOptions` via `ZacQueryClient`.

**`SignaleringenService`** — methods used by dashboard cards:

```typescript
// before
listTakenSignalering(type: string) {
  return this.zacHttpClient.PUT("/rest/signaleringen/taken/{type}", ...);
}

// after
listTakenSignalering(type: string) {
  return this.zacQueryClient.GET("/rest/signaleringen/taken/{type}", {
    path: { type }
  });
}
```

Do the same for:
- `listZakenSignalering` (used by `zaken-card`)
- `listInformatieobjectenSignalering` (used by `informatieobjecten-card`)

**`ZakenService`:**

```typescript
// before
listZaakWaarschuwingen() {
  return this.zacHttpClient.GET("/rest/zaken/waarschuwing");
}

// after
listZaakWaarschuwingen() {
  return this.zacQueryClient.GET("/rest/zaken/waarschuwing");
}
```

**`ZoekenService`:**

```typescript
// before
list(body: PutBody<"/rest/zoeken/list">) {
  return this.zacHttpClient.PUT("/rest/zoeken/list", body);
}

// after
list(body: PutBody<"/rest/zoeken/list">) {
  return this.zacQueryClient.PUT_AS_QUERY("/rest/zoeken/list", body);
}
```

**Test:** update existing service specs. They should now verify that the returned object has a `queryKey` and a callable `queryFn` — not that it is an Observable.

> **Note:** other callers of these service methods outside the dashboard may exist. Search the codebase for each method name before changing its signature and update all call sites.

---

### Step 3 — Migrate the three `.subscribe()` cards

For each of `taken-card`, `zaak-waarschuwingen-card`, `informatieobjecten-card`, replace the `.subscribe()` pattern with `injectQuery` and an `effect` to push data into the dataSource.

**Example — `taken-card`:**

```typescript
export class TakenCardComponent extends DashboardCardComponent<...> {
  private readonly signaleringenService = inject(SignaleringenService);

  private readonly takenQuery = injectQuery(() =>
    this.signaleringenService.listTakenSignalering(
      this.data.signaleringType!
    )
  );

  constructor(...) {
    super(...);
    effect(() => {
      this.dataSource.data = this.takenQuery.data() ?? [];
    });
  }

  protected onLoad(): void {
    this.takenQuery.refetch();
  }
}
```

Apply the same pattern to `zaak-waarschuwingen-card` and `informatieobjecten-card`.

**Test:** update specs — remove direct `component["onLoad"]()` calls. Instead mock the service's `queryOptions` return and verify that `dataSource.data` reflects `query.data()`. Keep the existing sort/paginator wiring tests.

---

### Step 4 — Clean up the two halfway cards

`taak-zoeken-card` and `zaak-zoeken-card` already use `injectQuery` but define their query keys as plain strings inline in the component. Replace with the service method from Step 2:

```typescript
// before
protected readonly zoekQuery = injectQuery(() => ({
  queryKey: ["taak zoeken dashboard", this.zoekParameters()],
  queryFn: () => firstValueFrom(this.zoekenService.list(this.zoekParameters())),
}));

// after
protected readonly zoekQuery = injectQuery(() =>
  this.zoekenService.list(this.zoekParameters())
);
```

And `zaken-card`:

```typescript
// before
zakenQuery = injectQuery(() => ({
  queryKey: ["aan mij toegekende zaken signaleringen", this.parameters()],
  queryFn: () => firstValueFrom(this.signaleringenService.listZakenSignalering(...)),
}));

// after
zakenQuery = injectQuery(() =>
  this.signaleringenService.listZakenSignalering(
    this.parameters().signaleringType!,
    { page: this.parameters().page, rows: this.parameters().pageSize }
  )
);
```

**Test:** existing specs should still pass after this change. The only difference is where the `queryKey` is defined — behaviour is unchanged.

---

### Step 5 — Replace the custom reload mechanism with invalidation

Now that all cards use TanStack Query, the hand-rolled reload mechanism in the base class can be replaced. For websocket-driven cards, move the listener into the component and call `invalidateQueries` instead of `onLoad`.

**Invalidation strategy per card:**

| Card | Reload trigger | Invalidation |
|---|---|---|
| `taken-card` | Websocket signalering event | `invalidateQueries({ queryKey: ["/rest/signaleringen/taken/{type}"] })` |
| `zaak-waarschuwingen-card` | Websocket signalering event | `invalidateQueries({ queryKey: ["/rest/zaken/waarschuwing"] })` |
| `informatieobjecten-card` | Websocket signalering event | `invalidateQueries({ queryKey: ["/rest/signaleringen/informatieobjecten/{type}"] })` |
| `taak-zoeken-card` | User sort/page interaction | Automatic — signal change triggers new `queryKey`, TanStack fetches |
| `zaak-zoeken-card` | User sort/page interaction | Automatic — same as above |
| `zaken-card` | User page interaction | Automatic — same as above |

**Example — websocket invalidation in `taken-card`:**

```typescript
private readonly queryClient = inject(QueryClient);

constructor(...) {
  super(...);
  this.queryClient
    .ensureQueryData(this.identityService.readLoggedInUser())
    .then(({ id }) => {
      this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.SIGNALERINGEN,
        id,
        (event: ScreenEvent) => {
          if (event.objectId.detail === this.data.signaleringType) {
            this.queryClient.invalidateQueries({
              queryKey: ["/rest/signaleringen/taken/{type}"]
            });
          }
        },
      );
    });
}
```

> **Why invalidate by URL prefix only (without path params)?** Passing just the URL to `invalidateQueries` invalidates all cached variants of that endpoint at once. This is intentional — when a signalering event fires, we want all signalering type variants to refresh, and we do not need to reconstruct the exact key used at query time.

**Test:** add a test per card that calls `queryClient.invalidateQueries` and asserts that the query refetches and `dataSource.data` updates accordingly.

---

### Step 6 — Delete the now-empty base class reload mechanism

Once all cards handle their own data fetching and invalidation via TanStack, the following can be removed from `DashboardCardComponent`:

- `private reloader: Subscription`
- `protected reload: Observable<unknown> | null`
- `refreshTimed()`
- `refreshOnSignalering()`
- The `reloader` subscription setup in `ngAfterViewInit`
- `this.reloader?.unsubscribe()` in `ngOnDestroy`
- `protected abstract onLoad(): void`

What remains in the base class: the shared `dataSource`, `@ViewChild(MatSort)`, `@ViewChild(MatPaginator)`, and the wiring of those in `ngAfterViewInit`. If nothing else uses the base class after this, consider whether it is still worth keeping as an abstract class or whether each card can stand alone.

**Test:** run the full dashboard test suite — all 55+ tests should still be green. The base class spec should be updated to reflect the removed API.

---

## Order of execution

```
Step 1 (ZacQueryClient.PUT_AS_QUERY)
    ↓
Step 2 (services)
    ↓
Step 3 + Step 4 (cards — can be done per card in parallel)
    ↓
Step 5 (invalidation)
    ↓
Step 6 (base class cleanup)
```

Never skip ahead. Each step has a compilable, testable state. Committing after each step makes review and rollback easier.

---

## Definition of done

- [ ] All dashboard cards use `injectQuery` via `ZacQueryClient`
- [ ] No magic string query keys remain in component files
- [ ] Websocket-driven cards invalidate via `queryClient.invalidateQueries`
- [ ] The hand-rolled reload mechanism is deleted from the base class
- [ ] All existing dashboard specs pass
- [ ] New invalidation specs added per websocket-driven card
- [ ] `npm run lint` passes
