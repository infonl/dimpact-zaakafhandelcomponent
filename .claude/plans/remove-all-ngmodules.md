/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Remove all 18 NgModules

Goal: fully standalone Angular frontend — zero `@NgModule` in `src/main/app/src/app`.

## Progress — 9 of 18 modules removed once step 6 merges; 5a merged, 5b pending

- [x] **Step 1** — zaken routes + lazy mount + `loadComponent` (commit `713c964`)
- [x] **Step 1b** — klanten mount points; delete `ZakenModule` + `KlantenModule` (commit `a5a4c31`)
- [x] **Step 2** — `fout-afhandeling` + `informatie-objecten` routes; `InformatieObjectenModule` deleted
- [x] **Step 3** — ngx-editor out of the eager graph (PZ-12707) — **−77 kB** (merged, #7088)
- [x] **Step 4** — dissolve `PipesModule` — pure deletion, −0.6 kB
- [ ] **Step 5** — dissolve `MaterialModule`, split in two PRs:
  - [x] **5a** — all 20 specs that import one of our NgModules (PZ-12820, merged, #7205)
  - [ ] **5b** — 14 non-spec files (local branch `temp-ng20-step5b`, commit `155f23db3`; applies cleanly on main `231146b26`, re-measure on top of step 6)
- [x] **Step 6** — `MaterialFormBuilderModule` removed — **−9.8 kB** (PZ-12845, PR pending; independent of 5b)
- [ ] **Step 7** — dissolve `SharedModule` (8 non-spec, 0 specs after 5a) — last, it re-exports the others
- [ ] **Step 8** — `loadChildren` targets: NgModule -> `Routes` (`taken` incl. `TakenModule`,
      `documenten`, `productaanvragen`)
- [ ] **Step 9** — `app-routing.module.ts` -> `app.routes.ts`
- [ ] **Step 10** — `bootstrapApplication` + delete `CoreModule`

Bundle so far: **672.06 -> 443.64 kB** initial transfer (**−34%**), the 77 kB of that in
the PZ-12707 PR (step 3) and the last 0.6 kB in step 4. Step 6 adds −9.8 kB on its own base (459.67 -> 449.84 kB).

**These figures are only comparable within the step that measured them.** Main moves underneath
the branch, so an absolute `Initial total` goes stale as soon as it is merged — the same branch
measured 459.77 kB after a later merge of main, with the step-4 delta unchanged. Always measure
before and after *on the same commit base* and record the delta; treat the absolutes as dated.

**Ordering criterion: measured bundle payoff.** An NgModule's `exports` are a live edge that
never tree-shakes; its `imports` are shaken away when nothing uses them. So the barrels only
cost what they *export*, and step 3 — the single export line that anchored ngx-editor — was the
biggest measured win. Step 4 confirmed the rest of the pattern: a barrel whose exports every
consumer already imports directly is worth ~0. Step 5 was expected to pay but measured a regression (+27 kB transfer), see step 5. Any remaining win needs steps 6 and 7 together. Everything else is bookkeeping toward zero `@NgModule`, ordered by risk, not payoff.

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
| [ ] | `shared/material/material.module.ts` | barrel | 5 | **+27 kB** transfer, −60 kB raw (measured) |
| [ ] | `shared/material-form-builder/material-form-builder.module.ts` | barrel | 6 | **−9.8 kB** (measured), on top of the −77 kB banked in step 3 |
| [ ] | `shared/shared.module.ts` | barrel | 7 | not yet measured |
| [x] | `shared/pipes/pipes.module.ts` | barrel | 4 | −0.6 kB (measured) |
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
them — at the time the only six in the app were `taken`, `admin`, `bag-objecten`,
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
  while edits are uncommitted; lint the touched files directly instead. `tsc --project .` is at 0
  errors on main (2026-09-29); the 4 former `date-range-filter.component.spec.ts` errors are gone.)
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
same factory itself now. Expect the same when dissolving the barrels in steps 5–7; 5a bore this out (`DateAdapter` and `HttpClient` inherited from `MaterialFormBuilderModule`).

Result: 538.75 kB -> 520.80 kB initial transfer.

## Step 3 — ngx-editor out of the eager graph — DONE (PZ-12707)

**The rule this step is built on:** an NgModule's `exports` are a live edge that never
tree-shakes; its `imports` are shaken away once nothing uses them. So a barrel costs only what
it *exports* — and `AppModule -> SharedModule -> MaterialModule / MaterialFormBuilderModule`
is what keeps Material eager. That remaining edge is cut in steps 5–7, where those barrels
are dissolved. Step 5 alone measured +27 kB (see step 5), so any win left needs 6 and 7.

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
into a lazy chunk. The same PR also drops 26 dead `imports` entries from the two barrels — no
bundle effect, measured. Verify the four screens in a browser: a missing import fails on screen,
not in the build.

## Steps 4–7 — Dissolve the barrels, one per PR

Every file that imports a barrel has to be given its own imports before the file can go — that is
the work, not the deletion. Order is forced by the dependencies: smallest first, `SharedModule`
last because it re-exports the other three. Step 5 measured +27 kB on its own; any Material win needs 6 and 7 together. Step 4 was cleanup, and mostly touched spec files, which ship to nobody.

**A barrel's `providers` are app-wide only because `AppModule -> SharedModule` imports it.** Each dissolving step must move those providers somewhere explicit (`CoreModule` until step 10) in the same PR, or they silently vanish at runtime while specs still pass. Step 5 did this for `MAT_SNACK_BAR_DEFAULT_OPTIONS`; steps 6 and 7 carry more (see there).

Shared cautions for all four: expect a tail of missing-import template errors, and expect specs to
lose providers they were inheriting through a barrel — step 2 hit exactly that with
`MatPaginatorIntl`. Importer specs still using `By.css` / `querySelector` must be migrated in the
PR that touches them, because `no-restricted-syntax` is an **error** on any spec a PR touches.
Step 4 cleared `shared/form/input`, `shared/form/radio` and `klanten/bedrijfsgegevens`;
`admin/bpmn-process-definitions` + its `-item` were cleared in 5a.

**Done in 5a (PZ-12820): no spec imports any of our NgModules any more.** On main 20 specs imported one (`MaterialModule` 14, `MaterialFormBuilderModule` 12, `SharedModule` 3, overlapping); 5a clears all 20, so steps 5b, 6 and 7 touch no spec. Specs lost two providers they had inherited from `MaterialFormBuilderModule` and now provide them themselves: the moment `DateAdapter` (`provideMomentDateAdapter()`, in `date`, `abstract-task-form`, `abstract-taak-formulier`) and `HttpClient` (`provideHttpClient()` + `provideHttpClientTesting()`, in the two abstract form specs). The 10 `querySelector` strict-lint errors in `abstract-taak-formulier`, `abstract-task-form` and `zaak-create` were migrated to Testing Library. In `zaak-create`, the sidenav content is inside a closed `mat-sidenav`, so `getByRole` sees an empty name even with `hidden: true`; `getByText` is used there.

## Step 4 — `PipesModule` — DONE

Removed from `shared/shared.module.ts` (imports + exports), from
`shared/indicaties/informatie-object-indicaties` and from 11 specs; file deleted. 11 -> 10 modules.
`Initial total` 444.27 -> 443.64 kB on a single commit base (**−0.6 kB**).

Note for `informatie-object-indicaties`: it keeps its `import { DatumPipe }` after the barrel is
gone, because it uses the pipe as `new DatumPipe("nl")` in the class body, not in its template.
A pipe import is not always a template import.

**No fan-out was needed, and that is the reusable finding.** All 40 components whose templates use
`datum` / `dagen` / `location` / `bestandsomvang` already listed the pipe in their own `imports`
array, so the barrel's `exports` edge fed nobody. Likewise the 12 specs: a standalone component
carries its own `imports`, so a spec that imports the component under test gets the pipes
transitively — a spec needs a pipe directly only when the *spec's own* inline template uses it,
and none did. Check both before assuming a barrel removal requires touching consumers.

Verification that makes this safe to repeat for steps 5-7: an AOT production build hard-errors on
an unresolvable pipe or directive, so a completing `ng build --configuration production` is the
real proof that no consumer was silently left behind. `ng test` alone is weaker.

**The real cost of this step was not the deletion — it was the touched-spec lint gate.** Removing
one import line from a spec makes that spec "changed", and `no-restricted-syntax` plus
`testing-library/no-node-access` turn from warnings into errors on it. Three of the 12 specs had
to be migrated to Testing Library queries before the branch could go for review (13 errors in
`shared/form/input`, `shared/form/radio`, `klanten/bedrijfsgegevens`). Two things learned while
doing it:

- `testing-library/no-node-access` is an error too, so `.closest()` / `.parentElement` are not an
  escape from `querySelector`. Where a component renders an unassociated `<label>` (as
  `zac-static-text` does) there is no role or label association to query at all; assert on the
  presence and absence of the distinct *values* instead of reaching for the field element.
- A vacuous assertion tends to hide inside a banned query. `input.spec.ts` asserted that
  `span[matSuffix]` existed — an element the template always renders, with nothing projected into
  it, so the test could not fail. Migrating it meant writing the test it was supposed to be: a
  `render()` host that projects a button and queries it by role. 5a did this for the remaining
  specs; it is per-spec work, not a mechanical find-and-replace.

### Step 5 — `MaterialModule` — 14 non-spec, 20 specs — IMPLEMENTED, split in two PRs

**Measured: a bundle regression, not a win.** On main `d4e8a2c3a`: `Initial total` 2.19 MB / 459.59 kB -> 2.13 MB / 486.59 kB (**+27 kB transfer**, −60 kB raw). Only stepper, tree and bottom-sheet left the initial set; the rest of Material stays eager through `MaterialFormBuilderModule` and `SharedModule`'s standalone components, and splitting it into small chunks gzips worse (initial chunks 61 -> 90). Verified by component selector in the initial chunks: `mat-stepper`, `mat-tree`, `mat-bottom-sheet-container` gone; `mat-mdc-table`, `mat-calendar`, `mat-datepicker-content`, `mat-mdc-tab-group`, `mat-expansion-panel`, `mat-mdc-paginator`, `mat-sort-header`, `mat-mdc-autocomplete`, `mat-mdc-chip` still there. The win expected here needs steps 6 and 7, or cutting `SharedModule` out of `AppModule`/`CoreModule`.

- **5a — 20 specs (PZ-12820):** drop every NgModule of ours from every spec (see above); among them `MaterialModule` from 14 specs; `klant-koppel.component.spec.ts` drops `SharedModule` and turns its override from `set` into `remove`/`add` of the two child components. Green on its own: 3326/3326 tests, `tsc --project .` at 0, strict touched-spec lint clean. With 5b on top also green: 3326/3326, `tsc` 0, production build 486.59 kB.
- **5b — 14 non-spec (`temp-ng20-step5b`):** direct Material imports in `admin/bpmn-process-definitions` + `-item`, `fout-afhandeling/dialog/fout-detailed-dialog`, `klanten/koppel/klanten/{klant-koppel,klant-koppel-betrokkene,klant-koppel-initiator}`, `shared/indicaties/{besluit,informatie-object,persoon,zaak}-indicaties`; `MaterialModule` out of `shared.module.ts`; `material.module.ts` deleted; `app.module.ts` and `core.module.ts` adjusted. Cannot go before 5a: specs on main still import the file it deletes. Rebase on main once 5a is merged.

Gotchas hit:
- `MaterialModule` also exported `ReactiveFormsModule`; `klant-koppel-betrokkene` needed it directly.
- `MAT_SNACK_BAR_DEFAULT_OPTIONS` was its only provider; moved to `CoreModule`.
- `AppComponent` needs `MatSidenavModule` in `AppModule`.
- The production `ng build` must run outside the sandbox (exit 134 otherwise).

### Step 6 — `MaterialFormBuilderModule` — DONE (PZ-12845, PR pending)

**Result.** On main `231146b26`: `Initial total` 2.19 MB / 459.67 kB -> 2.16 MB / 449.84 kB (**−9.8 kB transfer**). 3326/3326 tests, lint 0 errors, no spec file touched. 21 files, +96 / −148. Does not depend on 5b; 5b goes after it as its own PR.

**What was done.** 16 components import their `Zac*` fields and `EmptyPipe` directly (4 of them only lose the module line); `shared.module.ts` drops `forRoot()` and the export; the module file is deleted. The date providers became `provideZacDateAdapter()` in `shared/form/date/provide-zac-date-adapter.ts` (`provideMomentDateAdapter(ZAC_DATE_FORMATS, { strict: false })`), used by `CoreModule` and by `setupJest.ts`. The `parse`/`display` keys that sat in the old `MAT_MOMENT_DATE_ADAPTER_OPTIONS` were dropped: `MomentDateAdapter` reads only `strict` and `useUtc`. `withJsonpSupport()` and the barrel's `provideHttpClient` went without replacement; the app has no JSONP calls and no HTTP interceptors.

**Gotchas hit:**
- A template scan over `.html` files misses inline templates: `klant-koppel-betrokkene` uses `zac-select`/`zac-input` in `template:` and needed both. The production build caught it (NG8001); scan `.ts` files with `template:` too.
- The claim below that 5a made the specs provide the date adapter themselves was wrong for 4 specs (`zaak-brondatum-zetten-dialog`, `zaak-afhandelen-dialog`, `informatie-object-add`, `informatie-object-edit`, 12 tests). They got the app's `YYYY-MM-DD` format through the component's own barrel import, and fell back to `setupJest`'s default `provideMomentDateAdapter()`, whose format does not parse it, so the date stayed invalid and the submit button disabled. Fixed without touching a spec: `setupJest.ts` now uses `provideZacDateAdapter()`, so specs run on the app's real date config. `setupJest.ts` is not a `*.spec.ts`, so the touched-spec lint gate does not apply.
- `ng test` must run outside the sandbox too (watchman cannot write its LaunchAgent).

Original scope:

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
`providers: []` — a dead API, delete rather than port (its only caller is `shared.module.ts`). `withJsonpSupport()` in its `provideHttpClient(...)` is dead: no `.jsonp(` call exists in the app (verified 2026-09-29).

**Providers must move in this step, not step 10.** `DateAdapter` (`MomentDateAdapter`), `MAT_MOMENT_DATE_ADAPTER_OPTIONS` and `MAT_DATE_FORMATS` reach the app only through the barrel: via `SharedModule` (imported by `AppModule` and `CoreModule`) and via the components that import `MaterialFormBuilderModule` directly. Extract them into one `provideZacDateAdapter()` and add it to `CoreModule.providers`; without it every `mat-datepicker` throws "No provider found for DateAdapter" at runtime. The specs that relied on the barrel for it already provide it themselves (5a). Drop the barrel's `provideHttpClient(...)` rather than moving it; `app` and `core` already provide one.

Three components import both barrels (MFB + `SharedModule`): `klant-koppel-betrokkene`, `klant-koppel-initiator` and `parameters-edit-cmmn`. Step 6 removes only MFB from them; `SharedModule` stays until step 7.

### Step 7 — `SharedModule` — 8 non-spec, 0 specs (cleared by 5a)

- `admin/`: `bpmn-process-definitions` + its `-item`, `parameters-edit-cmmn`
- `klanten/koppel/klanten/`: `klant-koppel`, `klant-koppel-betrokkene`, `klant-koppel-initiator`
- `app.module.ts`, `core/core.module.ts`

Last, because until the other three are gone it is still the thing re-exporting them. Its own
exports are 21 standalone components, directives and pipes plus `CommonModule`, `FormsModule`, `TranslateModule` and `DragDropModule`, which consumers list directly instead.

**Its providers move in this step** (to `CoreModule.providers`), because step 10 comes after it: `Title`, the `MatPaginatorIntl` factory, the paginator-language `provideAppInitializer`, and `VertrouwelijkaanduidingToTranslationKeyPipe`. Watch the `MatPaginatorIntl` trap from step 2: specs inherit that provider transitively and lose their translated paginator accessible names when it moves; expect a few specs to need the factory provided locally.

## Step 8 — `loadChildren` targets: NgModule -> `Routes` (`taken`, `documenten`, `productaanvragen`)

All three already hang off a `loadChildren` in `app-routing.module.ts`, so they are lazy today.
Only the *shape* of the import target changes: it resolves to an NgModule instead of a plain
`Routes` array. No loading behaviour changes; this is what finally removes the modules.

- **`taken`** — the mount point imports `TakenModule`, not `TakenRoutingModule`. Pointing it at
  `taken.routes.ts` drops both modules in one move.
- **`documenten`** and **`productaanvragen`** — the mount point already imports the routing
  module itself, so only the import target and exported symbol change. Both hold eager
  `component:` refs; leave them. Route-level `loadComponent` inside an already lazy chunk was
  measured and rejected on 2026-09-10 (splitting klanten's chunk cost 4.33 kB through fragmentation).

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
  - Everything in `CoreModule.providers` and `AppModule.providers` -> bootstrap providers. By then
    that is: `LOCALE_ID`, `MAT_DATE_LOCALE`, `MAT_DIALOG_DEFAULT_OPTIONS`, `UtilService`,
    `MAT_SNACK_BAR_DEFAULT_OPTIONS` (step 5), `provideZacDateAdapter()` (step 6), `Title`,
    `MatPaginatorIntl`, the paginator initializer and `VertrouwelijkaanduidingToTranslationKeyPipe`
    (step 7), `provideRouter(APP_ROUTES)` (step 9), `APP_BASE_HREF`, `LocationStrategy`,
    `RouteReuseStrategy`, `provideTanStackQuery(...)` with devtools and `provideStartupPrefetch()`.
  - `AppComponent`'s own `imports`: `ToolbarComponent`, `ZoekComponent`, `MatSidenavModule` (step 5)
    and whatever else its template uses that `AppModule` supplies today.
  - `BrowserAnimationsModule` -> `provideAnimations()`. Handle with care: this repo
    has a history of NG05100 from animation providers being imported more than once.
    `provideAnimationsAsync()` is worth 11.7 kB but breaks 9 tab specs — see the parked
    findings before reaching for it here.
  - `provideHttpClient(withInterceptorsFromDi())` appears in `app` and `core` (MFB's copy goes in
    step 6). Collapse to one.
- Rehome `AppModule`'s constructor side effects — icon registry default font set,
  `window.__TANSTACK_QUERY_CLIENT__`, `persistQueryClient` with its
  session-storage persister — into `provideAppInitializer(...)` or
  `AppComponent`'s constructor. This is the most substantive piece of the step.
- `AppModule.injector` is assigned but **read nowhere**. Confirmed dead; delete it
  rather than porting it.

## Order summary

Step 3: done, −77 kB, merged (#7088).
Step 4: done, −0.6 kB — no consumer needed touching; the work was migrating 3 touched specs to
Testing Library.
Steps 5–7: order forced by the barrels' own dependencies. 5a merged; 5b measured +27 kB transfer on its own and waits for re-measurement on top of step 6. Step 6 done, −9.8 kB.
Steps 8–9: low risk, sequential, no behaviour change, no win.
Step 10: the gate — all of the risk, none of the payoff, so last.

## Findings parked outside this plan

Measured while hunting for bundle wins on 2026-09-16. None of these are migration steps; each
is its own ticket or a dead end worth recording so nobody re-measures it.

- **`provideAnimationsAsync()` (−11.7 kB, blocked).** Replacing `BrowserAnimationsModule` gives
  444.27 -> 432.56 kB, but breaks 9 `klant-koppel` tab specs: the animations engine arrives a
  microtask late and `MatTabGroup` no longer renders its tabs synchronously in a spec. Worth
  doing only together with fixing those specs. Mind this repo's NG05100 history. The
  `klant-koppel` spec has been rewritten since (#7154), so the 9 failures need re-measuring.
- **Dead `imports` entries cost nothing.** Four modules carried `imports` that nothing used
  (`core`: 3, MFB: 24, `shared`: 2, `taken`: 1). Pruning the 26 safe ones changed the bundle by
  0.04 kB — esbuild already shakes them. Already pruned in step 3 (#7088); nothing left to plan. Two of the four are
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
