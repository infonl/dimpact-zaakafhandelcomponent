/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Remove all 18 NgModules

Goal: fully standalone Angular frontend — zero `@NgModule` in `src/main/app/src/app`.

## Progress — 4 of 18 modules removed

- [x] **Step 1** — zaken routes + lazy mount + `loadComponent` (commit `713c964`)
- [x] **Step 1b** — klanten mount points; delete `ZakenModule` + `KlantenModule` (commit `a5a4c31`)
- [ ] **Step 2** — `informatie-objecten` slice (+ `fout-afhandeling`) — NEXT
- [ ] **Step 3** — already-lazy routing modules (`taken`, `documenten`, `productaanvragen`)
- [ ] **Step 4** — delete `TakenModule` + `InformatieObjectenModule`
- [ ] **Step 5** — `app-routing.module.ts` -> `app.routes.ts`
- [ ] **Step 6** — `bootstrapApplication` + delete `CoreModule`
- [ ] **Step 7** — dissolve the four shared barrels

Bundle so far: **672.06 kB -> 538.14 kB** initial transfer (−20%).

## Starting position (verified 2026-09-10, `main`)

- **187 components, all already standalone.** Angular 19+ defaults `standalone: true`;
  not one module declares a component (`AppComponent` is the sole exception).
- **All 18 NgModules are pure containers**: routing wrappers, re-export barrels,
  or provider holders. None can block a component from compiling.
- Precedent for the target shape already exists: `admin/admin.routes.ts`,
  `bag/bag.routes.ts`, `signaleringen/signaleringen.routes.ts` are plain `Routes`
  arrays with the path prefix at the mount point, not inside the array.

### The 18 modules

| Module | Kind | Step |
|---|---|---|
| [x] `zaken/zaken-routing.module.ts` | routing (eager `forChild`) | done |
| [x] `klanten/klanten-routing.module.ts` | routing (eager `forChild`) | done |
| [ ] `informatie-objecten/informatie-objecten-routing.module.ts` | routing (eager `forChild`) | 2 |
| [ ] `fout-afhandeling/fout-afhandeling-routing.module.ts` | routing (eager `forChild`) | 2 |
| [ ] `taken/taken-routing.module.ts` | routing (lazy) | 3 |
| [ ] `documenten/documenten-routing.module.ts` | routing (lazy) | 3 |
| [ ] `productaanvragen/productaanvragen-routing.module.ts` | routing (lazy) | 3 |
| [x] `zaken/zaken.module.ts` | container | done |
| [x] `klanten/klanten.module.ts` | container | done |
| [ ] `taken/taken.module.ts` | container | 4 |
| [ ] `informatie-objecten/informatie-objecten.module.ts` | container + provider | 4 |
| [ ] `app-routing.module.ts` | root routing | 5 |
| [ ] `app.module.ts` | root | 6 |
| [ ] `core/core.module.ts` | providers | 6 |
| [ ] `shared/shared.module.ts` | barrel | 7 |
| [ ] `shared/material/material.module.ts` | barrel | 7 |
| [ ] `shared/pipes/pipes.module.ts` | barrel | 7 |
| [ ] `shared/material-form-builder/material-form-builder.module.ts` | barrel | 7 |

### Key finding: four modules only *look* lazy

`zaken`, `klanten`, `informatie-objecten` and `fout-afhandeling` use
`RouterModule.forChild(...)` but are reached eagerly through
`XxxModule -> AppModule`. There is **no `loadChildren` mount point** for any of
them — the only six in the app are `taken`, `admin`, `bag-objecten`,
`signaleringen`, `documenten`, `productaanvragen`.

Consequence: their routes self-register into the root config at startup, which is
why each carries its own `path: "zaken"` / `"persoon"` / `"informatie-objecten"`
prefix *inside* the array. Giving them real mount points is the single biggest
lazy-loading win in this migration, and it is what steps 1–2 are actually for.

## Guiding rules

- One step per PR. Steps are ordered; do not skip ahead.
- No behaviour change in steps 1–5. Any route that resolves today resolves after.
- Absolute URLs must be identical before and after. Moving a path segment from a
  child array to a mount point is a refactor of *where* the prefix is declared,
  never of the resulting URL.
- Gate every step on: `ng test`, `tsc --project .`, `ng lint`, and a production build with a
  before/after `Initial total`. (`tsconfig.app.json` is `strict: false` — the real type gate is
  `tsc --project .`. `lint-changed-files.sh` diffs against `main`'s tip, so it reports nothing
  while edits are uncommitted; lint the touched files directly instead.)
- **Route configs have no test coverage and cannot get any.** `jest.config.js`
  `testPathIgnorePatterns` deliberately excludes `*-routing.module.spec.ts` and `*.routes.spec.ts`
  (PR #6469, 2026-07-07: "Route specs assert exact paths/link arrays — brittle"). Do not add
  guard specs, and do not rename around the filename filter. Verify routes by build output and
  manual URL checks. Corollary: a spec matching those names is silently not running even if it
  tests something unrelated — check before touching one.
- **Before deleting any container module, ask what it transitively pulls in.** `AppModule`'s
  import list is the only eager root; a module can be the sole path by which an unrelated
  feature's routes or providers reach the app.
- Route arrays are order-sensitive. Never reorder entries while moving a file.

---

## Step 1 — Zaken slice: routes + lazy mount + `loadComponent` — DONE

Committed as "step 1". Delivered as written, plus one addition: `ZaakViewComponent` also had to
be removed from `ZakenModule`'s `imports`, or the eager `AppModule -> ZakenModule` edge would
have kept it in the initial bundle and made `loadComponent` a no-op.

Result: `zaak-view-component` split into its own 70.88 kB chunk; all four pre-existing werklijst
chunks unchanged. Initial total still 3.14 MB / 672.06 kB at this point — the children were still
eager, which motivated step 1b.

## Step 1b — Unlock the zaken children — DONE

Not in the original plan; added once it turned out `ZakenModule` was pure dead weight (its
exports' only consumer, `taak-view.component.ts`, imports both components directly, and
`AppComponent` needs nothing from it).

Blocked by a hidden dependency: `KlantenRoutingModule <- KlantenModule <- ZakenModule <-
AppModule`. `KlantenModule`'s **only** importer in the app was `ZakenModule`, so `/persoon` and
`/bedrijf` reached the router solely through it. Deleting `ZakenModule` naively would have
dropped both routes with no compile error and no test failure.

Done:
- `klanten-routing.module.ts` -> `klanten/klanten.routes.ts` (`PERSOON_ROUTES`, `BEDRIJF_ROUTES`),
  two `loadChildren` mount points. Duplicate `:temporaryPersonId` order preserved; repeated
  `ErrorCardComponent` `data` extracted to a `PERSOON_GEEN_DATA` const.
- `buildBedrijfRouteLink` -> `klanten/bedrijf-route-link.ts`. **Load-bearing:** the eager
  `betrokkene-link.component.ts` imports it; leaving it in the routes file would have pulled the
  lazy route graph back into the eager bundle. Its spec moved too, which incidentally started
  running it for the first time (+5 tests) — it had been silently excluded by its filename.
- Deleted `zaken.module.ts` and `klanten.module.ts`; `ZakenModule` removed from `AppModule`.

Result: **3.14 MB / 672.06 kB -> 2.54 MB / 538.14 kB (-20% transfer)**. `zaak-view-component`
grew 70.88 -> 234.35 kB absorbing its children; new `klanten-routes` chunk at 33.78 kB; every
pre-existing chunk byte-identical.

Not verified: the three klanten URLs were never exercised in a running app.

## Step 1 (original text, for reference)

Self-contained, delivers a measurable bundle win, and proves the pattern the next
two steps copy.

**Changes**
1. `zaken/zaken-routing.module.ts` -> `zaken/zaken.routes.ts`, exporting
   `ZAKEN_ROUTES: Routes`. Drop `@NgModule` / `RouterModule.forChild`.
2. Strip the wrapping `path: "zaken"` node; the four children (`mijn`,
   `werkvoorraad`, `create`, `afgehandeld`), the `""` -> `werkvoorraad` redirect
   and the `:zaakIdentificatie` route become the top level of the array.
3. Add the mount point to `app-routing.module.ts`:
   `{ path: "zaken", loadChildren: () => import("./zaken/zaken.routes").then(m => m.ZAKEN_ROUTES) }`
4. Flip `ZaakViewComponent` from a static `component:` to `loadComponent:`.
   It is the only route in that array not code-split, and it is the largest
   component in the app.
5. Remove `ZakenRoutingModule` from `zaken.module.ts`'s imports (the module itself
   survives until step 4).

**Why the eager `component:` matters**: `ZaakViewComponent` is statically imported
at the top of the route file today, so it lands in whatever bundle holds the route
config. Code-splitting it is also a prerequisite for the deferred lazy-historie
chunk work — there is no point `@defer`-ing a child of an eagerly loaded parent.

**Verify**
- `/zaken`, `/zaken/mijn`, `/zaken/werkvoorraad`, `/zaken/create`,
  `/zaken/afgehandeld` and `/zaken/<identificatie>` all resolve.
- The `""` -> `werkvoorraad` redirect still fires with `pathMatch: "full"`.
- `TabelGegevensResolver` and `ZaakIdentificatieResolver` still run.
- A zaak-view chunk appears separately in the build output.
- Zaak-view specs: expect harness timeouts, not assertion failures, if async work
  is pending on mount.

## Step 2 — The remaining two eager `forChild` modules — NEXT

`klanten` is done (step 1b). Remaining: `informatie-objecten` and `fout-afhandeling` — same
treatment, `.routes.ts` + real `loadChildren` mount point.

Watch out:
- Both are still eager-with-no-mount-point, so apply the reachability check first: what else
  reaches the app *only* through them?
- `InformatieObjectenModule` is imported directly by `AppModule` **and** was imported by the
  now-deleted `ZakenModule`, so it survived step 1b. It still carries the app-wide
  `RouteReuseStrategy` provider — that must move to bootstrap, not vanish.
- `informatie-objecten` has two routes on `:uuid` vs `:uuid/:versie` — different shapes, so
  order is not load-bearing there, unlike klanten's.

## Step 3 — The three already-lazy routing modules

`taken`, `documenten`, `productaanvragen` -> `.routes.ts`. Purely mechanical: the
`loadChildren` entries already exist in app routing, only the import target and
the exported symbol change. `documenten` and `productaanvragen` also hold eager
`component:` refs worth flipping to `loadComponent` while in there.

## Step 4 — Delete the remaining two feature container modules

`ZakenModule` and `KlantenModule` are gone (step 1b). Remaining: `TakenModule` and
`InformatieObjectenModule`. After step 3 these hold nothing but re-exports of standalone
components.

- `InformatieObjectenModule` has two real consumers —
  `documenten/inbox-documenten-list` and `documenten/ontkoppelde-documenten-list` —
  which must import the three exported components directly.
- `InformatieObjectenModule` also carries
  `{ provide: RouteReuseStrategy, useClass: RouteReuseStrategyService }`, an
  app-wide provider hiding in a feature module. It moves to bootstrap providers in
  step 6 — it must not silently disappear here.
- These four are imported eagerly by `AppModule`, so deleting them is what finally
  gets zaken/klanten/informatie-objecten out of the initial bundle.

## Step 5 — `app-routing.module.ts` -> `app.routes.ts`

`RouterModule.forRoot(routes)` becomes `provideRouter(APP_ROUTES)`, staged into
`AppModule`'s providers so this step stands alone. Last routing module gone.

## Step 6 — `bootstrapApplication` + delete `CoreModule`

The one step with genuine behavioural risk. Own PR, own smoke test.

- `main.ts`: `platformBrowserDynamic().bootstrapModule(AppModule)` ->
  `bootstrapApplication(AppComponent, { providers: [...] })`. Keep `alterMoment()`.
- Delete `app.module.ts`; `AppComponent` becomes standalone with its own `imports`.
- Delete `core/core.module.ts` and `core/ensure-module-loaded-once.guard.ts`
  (`EnsureModuleLoadedOnceGuard` has no other user).
- Provider consolidation:
  - `TranslateModule.forRoot({...})` -> `provideTranslateService({...})`, keeping
    the cache-busting loader and `fallbackLang: "nl"`.
  - `LOCALE_ID`, `MAT_DATE_LOCALE`, `MAT_DIALOG_DEFAULT_OPTIONS`, `UtilService`,
    `APP_BASE_HREF`, `LocationStrategy`, `Title`, `MatPaginatorIntl`,
    `RouteReuseStrategy` -> bootstrap providers.
  - `BrowserAnimationsModule` -> `provideAnimations()`. Handle with care: this repo
    has a history of NG05100 from animation providers being imported more than once.
  - `provideHttpClient(withInterceptorsFromDi())` currently appears in **three**
    modules (`app`, `core`, `material-form-builder`). Collapse to one and confirm
    whether MFB's `withJsonpSupport()` is still needed.
- Rehome `AppModule`'s constructor side effects — icon registry default font set,
  `window.__TANSTACK_QUERY_CLIENT__`, `persistQueryClient` with its
  session-storage persister — into `provideAppInitializer(...)` or
  `AppComponent`'s constructor. This is the most substantive piece of the step.
- `AppModule.injector` is assigned but **read nowhere**. Confirmed dead; delete it
  rather than porting it.

## Step 7 — Dissolve the four shared barrels

Independent of steps 1–6; can run in parallel or after. One barrel per PR.

| Barrel | Non-module importers |
|---|---|
| `MaterialFormBuilderModule` | 28 |
| `MaterialModule` | 19 |
| `PipesModule` | 12 |
| `SharedModule` | 9 |

Importing `SharedModule` today transitively pulls `MaterialModule` +
`MaterialFormBuilderModule` + `PipesModule` + ~20 components, so every consumer
gets the whole graph and nothing tree-shakes. Dissolving them means each consumer
lists what it actually uses. Roughly half the affected files are specs.

Do not batch this as one mechanical sweep. Expect a long tail of missing-import
template errors, and note that specs touched here must also be migrated to
Testing Library queries — `no-restricted-syntax` is an error on any spec a PR
touches.

Providers riding along inside these barrels must land somewhere explicit:
`MAT_SNACK_BAR_DEFAULT_OPTIONS` (Material), and the moment date adapter with
`MAT_DATE_FORMATS` / `MAT_MOMENT_DATE_ADAPTER_OPTIONS` (MFB).

## Order summary

Steps 1–5: low risk, sequential, no behaviour change.
Step 6: the gate.
Step 7: independent, longest tail, biggest bundle payoff.
