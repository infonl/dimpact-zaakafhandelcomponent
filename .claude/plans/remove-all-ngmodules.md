/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Remove all 18 NgModules

Goal: fully standalone Angular frontend — zero `@NgModule` in `src/main/app/src/app`.

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
| `zaken/zaken-routing.module.ts` | routing (eager `forChild`) | 1 |
| `klanten/klanten-routing.module.ts` | routing (eager `forChild`) | 2 |
| `informatie-objecten/informatie-objecten-routing.module.ts` | routing (eager `forChild`) | 2 |
| `fout-afhandeling/fout-afhandeling-routing.module.ts` | routing (eager `forChild`) | 2 |
| `taken/taken-routing.module.ts` | routing (lazy) | 3 |
| `documenten/documenten-routing.module.ts` | routing (lazy) | 3 |
| `productaanvragen/productaanvragen-routing.module.ts` | routing (lazy) | 3 |
| `zaken/zaken.module.ts` | container | 4 |
| `klanten/klanten.module.ts` | container | 4 |
| `taken/taken.module.ts` | container | 4 |
| `informatie-objecten/informatie-objecten.module.ts` | container + provider | 4 |
| `app-routing.module.ts` | root routing | 5 |
| `app.module.ts` | root | 6 |
| `core/core.module.ts` | providers | 6 |
| `shared/shared.module.ts` | barrel | 7 |
| `shared/material/material.module.ts` | barrel | 7 |
| `shared/pipes/pipes.module.ts` | barrel | 7 |
| `shared/material-form-builder/material-form-builder.module.ts` | barrel | 7 |

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
- Gate every step on: `ng test`, `tsc --project .`, `./scripts/lint-changed-files.sh`.
  (`tsconfig.app.json` is `strict: false` — the real type gate is `tsc --project .`.)
- Route arrays are order-sensitive. Never reorder entries while moving a file.

---

## Step 1 — Zaken slice: routes + lazy mount + `loadComponent`

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

## Step 2 — The other three eager `forChild` modules

`klanten`, `informatie-objecten`, `fout-afhandeling` get the same treatment as
step 1: `.routes.ts` + real `loadChildren` mount point.

Watch out:
- `klanten` mounts **two** top-level prefixes (`persoon` and `bedrijf`) plus
  helper `buildBedrijfRouteLink`, which moves to a sibling file. Two mount points
  or one shared parent — decide explicitly.
- `klanten` has **two routes on the identical path** `:temporaryPersonId`: the
  first guarded by `canMatch: [PersoonResolverGuard]`, the second an unguarded
  `ErrorCardComponent` fallback. Order is load-bearing; reordering silently breaks
  the person-not-found page.
- `ErrorCardComponent` is configured entirely through route `data`
  (`title`/`text`/`iconName`). Carry that across verbatim.

## Step 3 — The three already-lazy routing modules

`taken`, `documenten`, `productaanvragen` -> `.routes.ts`. Purely mechanical: the
`loadChildren` entries already exist in app routing, only the import target and
the exported symbol change. `documenten` and `productaanvragen` also hold eager
`component:` refs worth flipping to `loadComponent` while in there.

## Step 4 — Delete the four feature container modules

`ZakenModule`, `KlantenModule`, `TakenModule`, `InformatieObjectenModule`. After
steps 1–3 these hold nothing but re-exports of standalone components.

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
