/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Plan: Extract details card from ZaakViewComponent

## Goal

Decompose `zaak-view.component.ts` by extracting the details card (the left `<mat-card class="w50 flex-1">`, HTML lines 189–800) into isolated, testable components.

**Approach:** Extract each tab one by one (6 PRs), then wrap the shell in a `ZaakDetailsComponent` (1 final PR).

**Why tab-by-tab:** Smaller PRs, lower risk, independently reviewable. Each tab owns exactly one slice of data + methods.

---

## Progress

| # | Component | Status | PR |
|---|---|---|---|
| 1 | `ZaakGerelateerdeZakenTabComponent` | ⬜ not started | — |
| 2 | `ZaakHistorieTabComponent` | ⬜ not started | — |
| 3 | `ZaakBagObjectenTabComponent` | ⬜ not started | — |
| 4 | `ZaakBetrokkenenTabComponent` | ⬜ not started | — |
| 5 | `ZaakLocatieTabComponent` | ⬜ not started | — |
| 6 | `ZaakAlgemeenTabComponent` | ⬜ not started | — |
| 7 | `ZaakDetailsComponent` (wrapper) | ⬜ not started | — |

---

## Rules (apply to every tab component)

- **Standalone: true** — new components are standalone from birth; no migration ticket needed later
- **No SharedModule** in `imports[]` — import every directive/component/pipe individually
- **Register** in `ZakenModule.imports[]` (not `declarations[]`) — standalone components go in imports
- **SPDX header** — new files get `2026 INFO.nl` only
- **No `any`**, no `NO_ERRORS_SCHEMA`, no `useValue` mocks in specs
- **Access modifiers** — `protected` for template members, `private` for internal, `public` only for `@Input`/`@Output`
- **Specs** — `TestBed.inject` + `jest.spyOn`, harness > querySelector > By.directive, no trivial smoke tests
- **Mutation pattern** — after any successful mutation, emit `zaakUpdated` (parent calls `updateZaak()`)
- **WebSocket suspend** — child components do NOT call `websocketService.suspendListener()`; the WS reload after mutation is harmless

---

## Tab 1 — `ZaakGerelateerdeZakenTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-gerelateerde-zaken-tab/`
**HTML source:** zaak-view.component.html lines 401–458

### Inputs
```ts
@Input({ required: true }) zaak: GeneratedType<"RestZaak">
```
`zaak.gerelateerdeZaken` used directly as table dataSource — no separate MatTableDataSource.

### Outputs
```ts
@Output() zaakUpdated = new EventEmitter<void>()
```
Emitted after unlink dialog confirms.

### State owned by child
```ts
protected readonly gerelateerdeZaakColumns = [
  "identificatie", "zaaktypeOmschrijving", "statustypeOmschrijving", "startdatum", "relatieType"
] as const
protected readonly gerelateerdeZaakColumnsWithAction = [...this.gerelateerdeZaakColumns, "actions"]
```

### Methods moved from parent
- `startZaakOntkoppelenDialog(gerelateerdeZaak)` — opens `ZaakOntkoppelenDialogComponent`; after confirm: `utilService.openSnackbar` + emit `zaakUpdated`

### Services injected by child
`MatDialog`, `ZakenService`, `UtilService`

### Tab visibility guard (stays in parent)
`*ngIf="zaak.gerelateerdeZaken?.length"` — parent still has `zaak`, no change needed.

### Spec coverage
- Table renders rows from `zaak.gerelateerdeZaken`
- View button visible when `row.rechten.lezen`, navigates to correct route
- Unlink button visible when `row.rechten.wijzigen`
- `zaakUpdated` emits after dialog confirms

---

## Tab 2 — `ZaakHistorieTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-historie-tab/`
**HTML source:** zaak-view.component.html lines 460–577

### Inputs
```ts
@Input({ required: true }) zaakUuid: string
```
Child loads its own data — full `zaak` object not needed.

### Outputs
```ts
@Output() hasData = new EventEmitter<boolean>()
```
Emitted after `loadHistorie()` completes. Parent stores `historieHasData` flag and uses it for tab visibility: `*ngIf="historieHasData"`.

### State owned by child
```ts
protected readonly historieColumns = [
  "datum", "gebruiker", "wijziging", "actie", "oudeWaarde", "nieuweWaarde", "toelichting"
] as const
protected historie = new MatTableDataSource<GeneratedType<"RestTaskHistoryLine">>()
@ViewChild("historieSort") private historieSort!: MatSort
```

### Lifecycle
- `ngOnInit` → `loadHistorie()`
- `ngAfterViewInit` → wire `this.historie.sort = this.historieSort` + `sortingDataAccessor`
- `ngOnChanges` → reload when `zaakUuid` changes

### Methods moved from parent
- `loadHistorie()` — calls `zakenService.listHistorieVoorZaak(zaakUuid)`; after set: emit `hasData`
- `sortingDataAccessor` logic (datum → datumTijd, gebruiker → door)

### Public API
```ts
reload() { this.loadHistorie() }
```
Parent or sibling components can trigger a reload after mutations (e.g. after betrokkene delete).

### Services injected by child
`ZakenService`

### Spec coverage
- Table renders rows from mocked historie data
- MatSort wired in `ngAfterViewInit`
- `hasData` emits `true` when data > 0, `false` when empty
- `reload()` re-calls `loadHistorie()`

---

## Tab 3 — `ZaakBagObjectenTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-bag-objecten-tab/`
**HTML source:** zaak-view.component.html lines 719–797

### Inputs
```ts
@Input({ required: true }) zaak: GeneratedType<"RestZaak">
```
Needs `zaak.uuid` (load + delete) and `zaak.rechten.behandelen` (delete button guard).

### Outputs
```ts
@Output() hasData    = new EventEmitter<boolean>()   // after load: drives tab *ngIf in parent
@Output() zaakUpdated = new EventEmitter<void>()     // after delete: parent calls updateZaak()
```

### State owned by child
```ts
protected readonly bagObjectenColumns = ["identificatie", "type", "omschrijving", "actions"] as const
protected bagObjectenDataSource = new MatTableDataSource<GeneratedType<"RESTBAGObjectGegevens">>()
protected gekoppeldeBagObjecten: GeneratedType<"RESTBAGObject">[] = []
```

### Lifecycle
- `ngOnInit` → `loadBagObjecten()`
- `ngOnChanges` → reload when `zaak` changes

### Methods moved from parent
- `loadBagObjecten()` — calls `bagService.list(zaak.uuid)`; after set: emit `hasData`
- `bagObjectVerwijderen(bagObjectGegevens)` — dialog + `bagService.delete()`; after confirm: `loadBagObjecten()` + emit `zaakUpdated` + snackbar

### Tab visibility guard (in parent)
Parent stores `bagObjectenHasData = false`. Child emits `(hasData)="bagObjectenHasData = $event"`. `<mat-tab *ngIf="bagObjectenHasData">`.

### Services injected by child
`BAGService`, `MatDialog`, `UtilService`, `TranslateService`

### Spec coverage
- Table renders rows from mocked bag data
- View (navigate) button present
- Delete button visible when `zaak.rechten.behandelen`
- `zaakUpdated` emits after dialog confirms
- `hasData` emits correctly

---

## Tab 4 — `ZaakBetrokkenenTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-betrokkenen-tab/`
**HTML source:** zaak-view.component.html lines 579–717

### Inputs
```ts
@Input({ required: true }) zaak: GeneratedType<"RestZaak">
```
Needs `zaak.uuid`, `zaak.zaaktype.uuid`, `zaak.rechten.verwijderenBetrokkene`, `zaak.zaaktype.zaakafhandelparameters.betrokkeneKoppelingen`.

### Outputs
```ts
@Output() visibilityChanged = new EventEmitter<boolean>()  // tab *ngIf driven by parent
@Output() zaakUpdated       = new EventEmitter<void>()     // after delete mutation
```

### State owned by child
```ts
protected readonly betrokkenenColumns = [
  "roltype", "betrokkenegegevens", "betrokkeneidentificatie", "roltoelichting", "actions"
] as const
protected betrokkenen = new MatTableDataSource<GeneratedType<"RestZaakBetrokkene">>()
private readonly datumPipe = new DatumPipe("nl")
private readonly queryClient = inject(QueryClient)
```

### Lifecycle
- `ngOnInit` → `loadBetrokkenen()`
- `ngOnChanges` → reload when `zaak` changes

### Methods moved from parent
- `loadBetrokkenen()` — calls `zakenService.listBetrokkenenVoorZaak(zaak.uuid)`; after set: emit `visibilityChanged(showBetrokkeneKoppelingen())`
- `betrokkeneGegevensOphalen(betrokkene)` — async; sets `betrokkene["gegevens"] = "LOADING"` then resolves via `QueryClient.ensureQueryData` + `KlantenService`
- `deleteBetrokkene(betrokkene)` — dialog; after confirm: reload + emit `zaakUpdated` + snackbar
- `showBetrokkeneKoppelingen()` — checks `betrokkeneKoppelingen.brpKoppelen || kvkKoppelen` AND `betrokkenen.data.length > 0`

### Tab visibility guard (in parent)
Parent stores `betrokkenenTabVisible = false`. Child emits `(visibilityChanged)="betrokkenenTabVisible = $event"`.

### Services injected by child
`ZakenService`, `KlantenService`, `QueryClient`, `MatDialog`, `UtilService`, `TranslateService`

### Spec coverage
- Table renders betrokkenen rows
- Gegevens fetch: button shows `more_horiz` → click → `hourglass_empty` → data displayed
- Delete button visible when `zaak.rechten.verwijderenBetrokkene`
- `zaakUpdated` emits after delete confirms
- `visibilityChanged` emits `false` when no brp/kvk koppelingen or empty data
- `visibilityChanged` emits `true` when koppelingen enabled and data present

---

## Tab 5 (penultimate) — `ZaakLocatieTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-locatie-tab/`
**HTML source:** zaak-view.component.html lines 372–399

### Inputs
```ts
@Input({ required: true }) zaak: GeneratedType<"RestZaak">
```
Uses `zaak.zaakgeometrie` and `zaak.rechten.wijzigenLocatie`.

### Outputs
```ts
@Output() editLocationRequested = new EventEmitter<void>()
```
Parent handles: `(editLocationRequested)="editLocationDetails()"` → sets `activeSideAction = 'actie.zaak.locatie.koppelen'` + opens sidenav.

### State owned by child
None — purely presentational.

### Methods moved from parent
None — edit button simply emits output.

### Services injected by child
None.

### Tab visibility guard (stays in parent)
`*ngIf="zaak.zaakgeometrie"` — parent still has `zaak`, no change needed.

### Spec coverage
- Coordinates display renders `zaak.zaakgeometrie | location`
- `<zac-locatie-tonen>` receives `[currentLocation]="zaak.zaakgeometrie"`
- Edit button visible when `zaak.rechten.wijzigenLocatie`
- Edit button hidden when permission absent
- `editLocationRequested` emits on edit button click

---

## Tab 6 (last) — `ZaakAlgemeenTabComponent`

**Path:** `src/main/app/src/app/zaken/zaak-algemeen-tab/`
**HTML source:** zaak-view.component.html lines 210–371

### Inputs
```ts
@Input({ required: true }) zaak:            GeneratedType<"RestZaak">
@Input({ required: true }) zaakOpschorting: GeneratedType<"RESTZaakOpschorting">
@Input()                   notitieRechten:  GeneratedType<"RestNotitieRechten">
```
`zaakOpschorting` loaded by parent (also used in hervatten dialog which stays in parent) — passed as input.

### Outputs
```ts
@Output() editDetailsRequested        = new EventEmitter<void>()  // parent: activeSideAction = 'actie.zaak.wijzigen'
@Output() addOrEditInitiatorRequested = new EventEmitter<void>()  // parent: activeSideAction = 'actie.initiator.koppelen'
```

### State owned by child
```ts
protected readonly indicatiesLayout = IndicatiesLayout
protected dateFieldIconMap = new Map<string, TextIcon>()
protected readonly loggedInUser = injectQuery(() => this.identityService.readLoggedInUser())
```

### Lifecycle
- `ngOnInit` → `setDateFieldIconSet()`
- `ngOnChanges` → re-run `setDateFieldIconSet()` when `zaak` changes

### Methods moved from parent
- `setDateFieldIconSet()` — builds `dateFieldIconMap` entries for `einddatumGepland` and `uiterlijkeEinddatumAfdoening` using `DateConditionals.isExceeded` + `TextIcon`
- `showInitiator()` — checks `zaakSpecificContactDetails` + `betrokkeneKoppelingen`
- `initiatorViewType()` → `"PERSON" | "COMPANY" | "CONTACT_DETAILS" | "ADD"`
- `allowedToAddBetrokkene()`, `allowBedrijf()`, `allowPersoon()`
- `isAfterDate(datum)`

### Services injected by child
`IdentityService`

### Spec coverage
- All static-text fields render correct values from `zaak`
- `dateFieldIconMap` icons appear for exceeded dates
- Edit button visible when `zaak.rechten.wijzigen || zaak.rechten.toekennen`
- `editDetailsRequested` emits on edit click
- `addOrEditInitiatorRequested` emits on initiator edit click
- Initiator block visible/hidden per `showInitiator()`
- Opschorting/verlenging remark block visible only when data present

---

## Phase 2 — `ZaakDetailsComponent` wrapper

**Path:** `src/main/app/src/app/zaken/zaak-details/`

Once all 6 tab components are merged, the mat-card shell in `zaak-view.component.html` contains only structural markup + 6 `<zac-*-tab>` tags. This PR:

1. Creates `ZaakDetailsComponent` (standalone: true, selector `zac-zaak-details`)
2. Moves the mat-card shell + all 6 tab tags into `zaak-details.component.html`
3. `ZaakDetailsComponent` inputs: `zaak`, `zaakOpschorting`, `notitieRechten`
4. `ZaakDetailsComponent` outputs: `editDetailsRequested`, `editLocationRequested`, `addOrEditInitiatorRequested`, `zaakUpdated`
5. Replaces the entire block in `zaak-view.component.html` with `<zac-zaak-details .../>`
6. Registers in `ZakenModule.imports[]`

After this PR `ZaakViewComponent` contains only: route/lifecycle, websocket listeners, sidenav/menu orchestration, dialog openers for zaak-level actions (opschorten, verlengen, heropenen, etc.), and initiator/betrokkene/bag mutations that flow back from sidepanel actions.

---

## Parent wiring reference

After all tabs are extracted, the parent template tab-group block looks like:

```html
<mat-tab-group mat-stretch-tabs="false">
  <mat-tab>
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-algemeen-tab
      [zaak]="zaak"
      [zaakOpschorting]="zaakOpschorting"
      [notitieRechten]="notitieRechten"
      (editDetailsRequested)="editCaseDetails()"
      (addOrEditInitiatorRequested)="addOrEditZaakInitiator()"
    />
  </mat-tab>
  <mat-tab *ngIf="zaak.zaakgeometrie">
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-locatie-tab
      [zaak]="zaak"
      (editLocationRequested)="editLocationDetails()"
    />
  </mat-tab>
  <mat-tab *ngIf="zaak.gerelateerdeZaken?.length">
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-gerelateerde-zaken-tab
      [zaak]="zaak"
      (zaakUpdated)="updateZaak()"
    />
  </mat-tab>
  <mat-tab *ngIf="historieHasData">
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-historie-tab
      [zaakUuid]="zaak.uuid"
      (hasData)="historieHasData = $event"
    />
  </mat-tab>
  <mat-tab *ngIf="betrokkenenTabVisible">
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-betrokkenen-tab
      [zaak]="zaak"
      (visibilityChanged)="betrokkenenTabVisible = $event"
      (zaakUpdated)="updateZaak()"
    />
  </mat-tab>
  <mat-tab *ngIf="bagObjectenHasData">
    <ng-template mat-tab-label>...</ng-template>
    <zac-zaak-bag-objecten-tab
      [zaak]="zaak"
      (hasData)="bagObjectenHasData = $event"
      (zaakUpdated)="updateZaak()"
    />
  </mat-tab>
</mat-tab-group>
```

Parent adds these flags to its TS (temporary, removed in Phase 2):
```ts
protected historieHasData = false
protected betrokkenenTabVisible = false
protected bagObjectenHasData = false
```
