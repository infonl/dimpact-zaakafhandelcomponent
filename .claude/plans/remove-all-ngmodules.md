/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Remove all 18 NgModules

Goal: fully standalone Angular frontend — zero `@NgModule` in `src/main/app/src/app`.

## Progress — 7 of 18 modules removed

- [x] **Step 1** — zaken routes + lazy mount + `loadComponent` (commit `713c964`)
- [x] **Step 1b** — klanten mount points; delete `ZakenModule` + `KlantenModule` (commit `a5a4c31`)
- [x] **Step 2** — `fout-afhandeling` + `informatie-objecten` routes; `InformatieObjectenModule` deleted
- [ ] **Step 3** — cut the eager export edges into Material — IN PROGRESS (ngx-editor slice
      measured and green, awaiting a browser check)
- [ ] **Step 4** — dissolve `PipesModule` (3 non-spec, 11 specs)
- [ ] **Step 5** — dissolve `MaterialModule` (6 non-spec, 14 specs)
- [ ] **Step 6** — dissolve `MaterialFormBuilderModule` (17 non-spec, 12 specs)
- [ ] **Step 7** — dissolve `SharedModule` (8 non-spec, 3 specs) — last, it re-exports the others
- [ ] **Step 8** — `loadChildren` targets: NgModule -> `Routes` (`taken` incl. `TakenModule`,
      `documenten`, `productaanvragen`)
- [ ] **Step 9** — `app-routing.module.ts` -> `app.routes.ts`
- [ ] **Step 10** — `bootstrapApplication` + delete `CoreModule`

Bundle so far: **672.06 kB -> 520.80 kB** initial transfer (−22%), with step 3's first
slice measured at a further **520.80 -> 444.27 kB** and waiting in a branch.

**Ordering criterion: measured bundle payoff.** An NgModule's `exports` are a live edge that
never tree-shakes; its `imports` are shaken away when nothing uses them. So the barrels only
cost what they *export*, and step 3 — the single export line that anchors Material — is the
one remaining step with a measured win. Everything after it is bookkeeping toward zero
`@NgModule` and is ordered by risk, not payoff.

### Where the initial bundle stood before step 3 (measured 2026-09-15, production build)

Initial total: 2.48 MB raw / 520.97 kB transfer. Full build across all 112 chunks:
7.22 MB raw / ~1.72 MB transfer. Source-map attribution of the largest initial
chunk (1.54 MB raw / 297 kB transfer), by original source size:

| Source | Size |
|---|---|
| `@angular/material` | 1727 kB |
| `@angular/core` | 1720 kB |
| `@angular/cdk` | 657 kB |
| `@angular/common` + `router` + `forms` | 989 kB |
| TanStack + rxjs + ngx-translate | 212 kB |
| **own app code** | **~45 kB** |

What this attribution got wrong, corrected on 2026-09-16: it was read as "only ~45 kB of own
code is left, so nothing is left to win". That conclusion does not follow. The win was never in
own code — it is in *vendor* code that own code keeps eagerly reachable. Cutting one export edge
moved 77 kB of ngx-editor out of the initial bundle without touching a single line of own app
code. Read the attribution as a map of what the barrels are anchoring, not as a floor.

Note that the estimated transfer sizes only materialise behind nginx
(`charts/zac/templates/configmap-nginx.yaml`), which gzips. WildFly itself is not
configured to compress, so a local run on :8080 ships the full raw size.

## Starting position (verified 2026-09-10, `main`)

- **187 components, all already standalone.** Angular 19+ defaults `standalone: true`;
  not one module declares a component (`AppComponent` is the sole exception).
- **All 18 NgModules are pure containers**: routing wrappers, re-export barrels,
  or provider holders. None can block a component from compiling.
- Precedent for the target shape already exists: `admin/admin.routes.ts`,
  `bag/bag.routes.ts`, `signaleringen/signaleringen.routes.ts` are plain `Routes`
  arrays with the path prefix at the mount point, not inside the array.

### The 18 modules

| ✓ | Module | Kind | Step | Bundle gain |
|---|---|---|---|---|
| [x] | `zaken/zaken-routing.module.ts` | routing (eager `forChild`) | 1 | −134 kB with 1b |
| [x] | `zaken/zaken.module.ts` | container | 1b | (same) |
| [x] | `klanten/klanten-routing.module.ts` | routing (eager `forChild`) | 1b | (same) |
| [x] | `klanten/klanten.module.ts` | container | 1b | (same) |
| [x] | `fout-afhandeling/fout-afhandeling-routing.module.ts` | routing (eager `forChild`) | 2 | none (0.4 kB) |
| [x] | `informatie-objecten/informatie-objecten-routing.module.ts` | routing (eager `forChild`) | 2 | −18 kB with the container |
| [x] | `informatie-objecten/informatie-objecten.module.ts` | container + provider | 2 | (same) |
| [ ] | `shared/material/material.module.ts` | barrel | 3 + 5 | step 3, not yet measured |
| [ ] | `shared/material-form-builder/material-form-builder.module.ts` | barrel | 3 + 6 | **−77 kB** measured in step 3 |
| [ ] | `shared/shared.module.ts` | barrel | 3 + 7 | step 3, not yet measured |
| [ ] | `shared/pipes/pipes.module.ts` | barrel | 4 | none |
| [ ] | `taken/taken-routing.module.ts` | routing (lazy) | 8 | none |
| [ ] | `taken/taken.module.ts` | container | 8 | none |
| [ ] | `documenten/documenten-routing.module.ts` | routing (lazy) | 8 | none |
| [ ] | `productaanvragen/productaanvragen-routing.module.ts` | routing (lazy) | 8 | none |
| [ ] | `app-routing.module.ts` | root routing | 9 | none |
| [ ] | `app.module.ts` | root | 10 | none |
| [ ] | `core/core.module.ts` | providers | 10 | none |

### Key finding (steps 1–2, now resolved): four modules only *looked* lazy

`zaken`, `klanten`, `informatie-objecten` and `fout-afhandeling` used
`RouterModule.forChild(...)` but are reached eagerly through
`XxxModule -> AppModule`. There is **no `loadChildren` mount point** for any of
them — the only six in the app are `taken`, `admin`, `bag-objecten`,
`signaleringen`, `documenten`, `productaanvragen`.

Consequence: their routes self-register into the root config at startup, which is
why each carries its own `path: "zaken"` / `"persoon"` / `"informatie-objecten"`
prefix *inside* the array. Giving them real mount points is what steps 1–2 did, for −152 kB.
All four now have a real `loadChildren` mount point; the check itself stays relevant for any
module still to be deleted.

## Guiding rules

- One step per PR. Steps are ordered; do not skip ahead.
- No behaviour change in any step but 10. Any route that resolves today resolves after.
- Absolute URLs must be identical before and after. Moving a path segment from a
  child array to a mount point is a refactor of *where* the prefix is declared,
  never of the resulting URL.
- Gate every step on: `ng test`, `tsc --project .`, `ng lint`, and a production build with a
  before/after `Initial total`. (`tsconfig.app.json` is `strict: false` — the real type gate is
  `tsc --project .`. `lint-changed-files.sh` diffs against `main`'s tip, so it reports nothing
  while edits are uncommitted; lint the touched files directly instead. `tsc --project .` is not
  at zero: `date-range-filter.component.spec.ts` carries 4 pre-existing errors, already ticketed —
  compare against those 4, do not read them as your own.)
- **`exports` cost bundle, `imports` do not.** An NgModule's `exports` are a live edge that never
  tree-shakes; its `imports` are shaken away once nothing uses them. Measured: pruning 26 dead
  `imports` across four modules moved 0.04 kB, while cutting two export edges moved 77 kB. When
  hunting for a win, read the `exports` list.
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

Verified manually in a local DEV environment: `/zaken/*`, `/persoon/<id>`, and `/bedrijf/<id>`.

## Step 2 — The remaining two eager `forChild` modules — DONE

Split into two PRs. `fout-afhandeling` is done: `fout-afhandeling.routes.ts`
(`FOUT_AFHANDELING_ROUTES`, `loadComponent`), `loadChildren` mount at `path: "fout"`, module
import dropped from `AppModule`. No provider, no exported component, no reachability trap.
Bundle 538.75 kB -> 538.39 kB — structural only; `FoutAfhandelingService` and the error dialogs
stay eager because most of the app imports them directly.

`informatie-objecten` followed, and took its container module with it (step 4's half, done early
because the module turned out to be empty once the routing import was gone):

- `informatie-objecten.routes.ts` (`INFORMATIE_OBJECTEN_ROUTES`), both `:uuid` and `:uuid/:versie`
  on `loadComponent`, resolver unchanged; mounted at `path: "informatie-objecten"`.
- `InformatieObjectenModule` declared nothing, so every entry in its `imports` was dead weight
  (`SharedModule`, `DocumentIconComponent`, `InformatieObjectIndicatiesComponent`,
  `MimetypeToExtensionPipe`, `InformatieObjectEditComponent`). Deleted.
- Its two consumers, `inbox-documenten-list` and `ontkoppelde-documenten-list`, only ever used
  `<zac-informatie-object-link>`; they import that component directly now.
- `RouteReuseStrategy` moved to `AppModule.providers` — not lost.

**Trap that cost a test run:** `inbox-documenten-list.component.spec.ts` was inheriting
`SharedModule`'s `MatPaginatorIntl` provider transitively through `InformatieObjectenModule`, so
the paginator buttons lost their translated accessible names and two Testing Library queries
failed. Runtime was never affected (`AppModule` imports `SharedModule`). The spec provides the
same factory itself now. Expect the same when dissolving the barrels in steps 4–7.

Result: 538.75 kB -> 520.80 kB initial transfer.

## Step 3 — Cut the eager export edges into Material — IN PROGRESS

**The rule this step is built on:** an NgModule's `exports` are a live edge that never
tree-shakes; its `imports` are shaken away once nothing uses them. So a barrel costs only what
it *exports* — and `AppModule -> SharedModule -> MaterialModule / MaterialFormBuilderModule`
is what keeps Material eager. Cutting those edges is the whole remaining bundle win; deleting
the barrel files afterwards (step 4) adds nothing.

### Slice 1 — ngx-editor — done, measured, not yet merged

`MaterialFormBuilderModule` imported `NgxEditorModule` *and* exported `ZacHtmlEditor`, so every
first paint carried the whole WYSIWYG editor. It is used on four lazy screens only: mail-create,
ontvangstbevestiging, admin/mailtemplate, and the `htmlEditor` field type in taakformulieren.

Dropped `NgxEditorModule`, `ZacHtmlEditor` and `ZacComposedForm` from the barrel; the three
components import `ZacHtmlEditor` directly. Measured on `main` after #7075: **520.97 -> 444.27 kB**,
2940 tests green, `tsc` at its 4 pre-existing errors.

Both edges matter, and not equally:

| Change | Initial transfer | ngx-editor |
|---|---|---|
| baseline | 520.97 kB | eager |
| `NgxEditorModule` out of `imports` only | 521.02 kB | still eager |
| `ZacHtmlEditor` + `ZacComposedForm` out of `exports` only | −19.9 kB * | still eager |
| both | **444.27 kB** | lazy |

\* measured on the pre-#7075 tree (538.67 -> 518.80 kB); the delta carries over, the absolute
number does not.

The `imports` line alone buys nothing — esbuild shakes it. The `exports` alone buy ~20 kB but
leave the library eager, because the barrel still imports it. Only cutting both moves ngx-editor
into a lazy chunk. Still to do: a browser check of those four screens, since a missing import
fails on screen, not in the build.

### Slice 2 — the Material barrels themselves

`SharedModule.exports` lists `MaterialModule` and `MaterialFormBuilderModule`. Verified in the
production build: `mat-mdc-table`, `mat-calendar`, `mat-datepicker`, `mat-mdc-chip`,
`mat-stepper`, `mat-tree`, `mat-mdc-tab`, `mat-expansion`, `mat-mdc-paginator`, `mat-sort` and
`mat-mdc-autocomplete` all sit in the initial chunks, while the app shell renders only toolbar,
sidenav, icon, button, menu, dialog and snackbar.

Drop both from `SharedModule.exports` and give the consumers their own imports. Removing the
heavy entries and building lists exactly who those are — the compile errors are the worklist:
`bpmn-process-definitions` (`mat-tree`), `klant-koppel` (`mat-action-row`), and the rest of the
six lazy `SharedModule` consumers under `admin/` and `klanten/koppel/`.

The ceiling is not measured: probing it costs the same work as doing the step, because the build
does not complete until those consumers are fixed. Measure `Initial total` before and after.

`MAT_SNACK_BAR_DEFAULT_OPTIONS` (in `MaterialModule`) and the moment date adapter with
`MAT_DATE_FORMATS` / `MAT_MOMENT_DATE_ADAPTER_OPTIONS` (in `MaterialFormBuilderModule`) must land
somewhere explicit — they do not travel with the exports.

## Steps 4–7 — Dissolve the barrels, one per PR

Step 3 only fixes the six lazy `SharedModule` consumers. Every other file that imports a barrel
directly still has to be given its own imports before the file can go — that is the work, not the
deletion. No bundle win is left here: step 3 took it, and what remains is mostly spec files, which
ship to nobody. Order is forced by the dependencies, smallest first, `SharedModule` last because
it re-exports the other three.

Shared cautions for all four: expect a tail of missing-import template errors, and expect specs to
lose providers they were inheriting through a barrel — step 2 hit exactly that with
`MatPaginatorIntl`. Six importer specs still use `By.css` / `querySelector`
(`shared/form/input`, `shared/form/radio`, `klanten/bedrijfsgegevens`,
`admin/bpmn-process-definitions` + its `-item`) and `no-restricted-syntax` is an **error** on any
spec a PR touches, so migrate each in the PR it falls into.

### Step 4 — `PipesModule` — 3 non-spec, 11 specs

- `shared/indicaties/informatie-object-indicaties`
- `shared/material-form-builder/material-form-builder.module.ts`
- `shared/shared.module.ts`

The leaf: four standalone pipes, no providers, nothing transitive. Proves the pattern at near-zero
risk.

### Step 5 — `MaterialModule` — 6 non-spec, 14 specs

- `fout-afhandeling/dialog/fout-detailed-dialog.component.ts`
- `shared/indicaties/{besluit,informatie-object,persoon,zaak}-indicaties`
- `shared/shared.module.ts` (only if step 3 left the import behind)

Carries `MAT_SNACK_BAR_DEFAULT_OPTIONS`, which must land somewhere explicit.

### Step 6 — `MaterialFormBuilderModule` — 17 non-spec, 12 specs

- `admin/`: `mailtemplate`, `parameters-edit-bpmn`, `parameters-edit-cmmn`,
  `parameters-select-process-model-method`
- `informatie-objecten/`: `informatie-object-add`, `informatie-object-edit`
- `klanten/`: `klant-koppel-betrokkene`, `klant-koppel-initiator`, `bedrijf-zoek`, `persoon-zoek`
- `mail/`: `mail-create`, `ontvangstbevestiging`
- `taken/taken-verdelen-dialog`
- `zaken/`: `besluit-create`, `zaak-afhandelen-dialog`, `zaak-brondatum-zetten-dialog`
- `shared/shared.module.ts`

The biggest, and the one whose name has to go: it has nothing to do with the ATOS form builder
any more — what it exports is the modern `Zac*` form-field set. Carries the moment date adapter
with `MAT_DATE_FORMATS` / `MAT_MOMENT_DATE_ADAPTER_OPTIONS`. Its `forRoot()` returns
`providers: []` — a dead API, delete rather than port. `withJsonpSupport()` in its
`provideHttpClient(...)` is the only JSONP in the app and is probably dead too.

### Step 7 — `SharedModule` — 8 non-spec, 3 specs

- `admin/`: `bpmn-process-definitions` + its `-item`, `parameters-edit-cmmn`
- `klanten/koppel/klanten/`: `klant-koppel`, `klant-koppel-betrokkene`, `klant-koppel-initiator`
- `app.module.ts`, `core/core.module.ts`

Last, because until the other three are gone it is still the thing re-exporting them. Its own
exports are ~20 standalone components that consumers list directly instead.

## Step 8 — `loadChildren` targets: NgModule -> `Routes` (`taken`, `documenten`, `productaanvragen`)

All three already hang off a `loadChildren` in `app-routing.module.ts`, so they are lazy today.
Only the *shape* of the import target changes: it resolves to an NgModule instead of a plain
`Routes` array. No loading behaviour changes; this is what finally removes the modules.

- **`taken`** — the mount point imports `TakenModule`, not `TakenRoutingModule`. Pointing it at
  `taken.routes.ts` drops both modules in one move.
- **`documenten`** and **`productaanvragen`** — the mount point already imports the routing
  module itself, so only the import target and exported symbol change. Both also hold eager
  `component:` refs worth flipping to `loadComponent` while in there.

## Step 9 — `app-routing.module.ts` -> `app.routes.ts`

`RouterModule.forRoot(routes)` becomes `provideRouter(APP_ROUTES)`, staged into `AppModule`'s
providers so this step stands alone. Last routing module gone.

## Step 10 — `bootstrapApplication` + delete `CoreModule`

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
  - **Hoist `SharedModule`'s providers too**, even though the barrel itself is not
    dissolved until step 7. They live in `SharedModule` but are app-wide today
    because `AppModule` imports it: `Title`, the `MatPaginatorIntl` factory, the
    paginator-language `provideAppInitializer`, and
    `VertrouwelijkaanduidingToTranslationKeyPipe`. Moving them here leaves
    `SharedModule` a pure re-export barrel, which is what makes step 7 safe.
    Watch the `MatPaginatorIntl` trap from step 2: specs inherit that provider
    transitively, and lose their translated paginator accessible names when it
    moves. Expect a few specs to need the factory provided locally.
  - `BrowserAnimationsModule` -> `provideAnimations()`. Handle with care: this repo
    has a history of NG05100 from animation providers being imported more than once.
    `provideAnimationsAsync()` is worth 11.7 kB but breaks 9 tab specs — see the parked
    findings before reaching for it here.
  - `provideHttpClient(withInterceptorsFromDi())` currently appears in **three**
    modules (`app`, `core`, `material-form-builder`). Collapse to one and confirm
    whether MFB's `withJsonpSupport()` is still needed — it is the only JSONP in the app.
- Rehome `AppModule`'s constructor side effects — icon registry default font set,
  `window.__TANSTACK_QUERY_CLIENT__`, `persistQueryClient` with its
  session-storage persister — into `provideAppInitializer(...)` or
  `AppComponent`'s constructor. This is the most substantive piece of the step.
- `AppModule.injector` is assigned but **read nowhere**. Confirmed dead; delete it
  rather than porting it.

## Order summary

Step 3: the only step with bundle payoff left in the plan. Slice 1 is measured at −77 kB and
sits in a branch; slice 2's win is demonstrated but not yet quantified.
Steps 4–7: cleanup, no win, safe to defer; order forced by the barrels' own dependencies.
Steps 8–9: low risk, sequential, no behaviour change, no win.
Step 10: the gate — all of the risk, none of the payoff, so last.

## Findings parked outside this plan

Measured while hunting for bundle wins on 2026-09-16. None of these are migration steps; each
is its own ticket or a dead end worth recording so nobody re-measures it.

- **`provideAnimationsAsync()` (−11.7 kB, blocked).** Replacing `BrowserAnimationsModule` gives
  444.27 -> 432.56 kB, but breaks 9 `klant-koppel` tab specs: the animations engine arrives a
  microtask late and `MatTabGroup` no longer renders its tabs synchronously in a spec. Worth
  doing only together with fixing those specs. Mind this repo's NG05100 history.
- **Dead `imports` entries cost nothing.** Four modules carry `imports` that nothing uses
  (`core`: 3, MFB: 24, `shared`: 2, `taken`: 1). Pruning the 26 safe ones changed the bundle by
  0.04 kB — esbuild already shakes them. Do not plan a step for this. Two of the four are
  load-bearing anyway: `core`'s `TranslateModule.forRoot(...)` carries providers and `taken`'s
  `TakenRoutingModule` carries the routes.
- **`@angular/elements` is an unused dependency.** In `package.json`, imported nowhere. No
  bundle effect; removing it is pure upgrade-churn relief.
- **moment is eager (18.84 kB) and stays that way.** Pulled in by `toolbar.component`,
  `datum.pipe` and `dagen.pipe`, all in eager reach. Not a leak — replacing moment is a
  refactor across 30+ files, not a barrel problem.
- **Form.io, OpenLayers and proj4 are already lazy.** A grep suggesting otherwise was matching
  the i18n key `msg.error.formio.init`.
- **Bootstrap grid stays.** `styles.less` pulls `bootstrap-grid.min.css` and templates use
  `row`/`col-*` 450+ times. The separate `assets/vendor/bootstrap` copy is live too:
  `formio-bootstrap-loader.service.ts` fetches it for the Form.io shadow DOM.
