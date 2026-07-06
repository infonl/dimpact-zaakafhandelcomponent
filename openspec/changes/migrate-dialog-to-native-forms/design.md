# Design: Migrate the shared dialog to native forms

## Context

`shared/dialog/DialogComponent` is a generic confirm dialog whose only ATOS coupling is that it renders an arbitrary list of MFB `AbstractFormField`s and harvests their values:

- `dialog.component.ts` imports `MaterialFormBuilderModule`.
- `DialogData.options.formFields: AbstractFormField[]` — the payload.
- `dialog.component.html` renders `<mfb-form-field [field]="formField">` per field.
- `confirm()` builds `results[field.id] = field.formControl.value` and invokes `options.callback(results)`, closing the `MatDialogRef` with the result.
- Validity is `options.formFields.some(f => f.formControl.invalid)`.

There are exactly 9 call sites, all via `MatDialog.open(DialogComponent, { data })`:

| # | Location | Field | Result type |
|---|---|---|---|
| 1 | `zaak-view` openZaakAfbrekenDialog | **select** (`RestZaakbeeindigReden` options) | `{ reden: RestZaakbeeindigReden }` |
| 2 | `zaak-view` openZaakHeropenenDialog | input, required, max 100 | `{ reden: string }` |
| 3 | `zaak-view` resumeZaak (opschorting) | input, required, max 200 | `{ redenOpschortingField: string }` |
| 4 | `zaak-view` updateInitiator | textarea, required | `{ reden: string }` |
| 5 | `zaak-view` deleteInitiator | textarea, required | `{ reden: string }` |
| 6 | `zaak-view` deleteBetrokkene | textarea, required | `{ reden: string }` |
| 7 | `zaak-view` bagObjectVerwijderen | input, required, max 80 | `{ reden: string }` |
| 8 | `zaak-documenten` ontkoppelInformatieObject | textarea, required, max 200 | `{ reden: string }` |
| 9 | `informatie-object-view` (verwijderen) | input, required | `{ reden: string }` |

The codebase already contains the destination pattern: dedicated dialog components such as `ZaakOpschortenDialogComponent` and `BesluitIntrekkenDialogComponent` use `<form [formGroup]>`, native `zac-*` fields bound via `[form]` + `key`, and (in the newest one) `<zac-form-actions wrapper="dialog" [mutation]>` for the footer. But every one of them **copy-pastes the header chrome** (`<mat-toolbar mat-dialog-title>` + icon + title + close button + `<mat-divider>`).

## Goals / Non-Goals

**Goals:**
- Remove `MaterialFormBuilder`, `AbstractFormField`, and `<mfb-form-field>` from `shared/dialog`.
- Give developers a readable, typed dialog authoring model: one `FormGroup`, native `zac-*` fields in real HTML, one dedicated component per meaningfully-distinct dialog.
- Factor the duplicated header/footer chrome into one shared presentational shell (`zac-generic-dialog`) reusable by the new and existing dedicated dialogs.
- Behaviour parity: identical UX, translation keys, validation, and `afterClosed()` result contract for every migrated call site.

**Non-Goals:**
- Deleting the MFB infrastructure (other consumers remain — see proposal Non-Goals).
- Converting call-site business logic to TanStack mutations.
- Retro-fitting `zac-generic-dialog` into the pre-existing dedicated dialogs in the same change (can follow once proven here).

## Decisions

### Decision 1: `zac-generic-dialog` is a presentational shell using content projection

The React-style `<Generic><Dedicated/></Generic>` composition maps to Angular **content projection** (`<ng-content>` = React `children`). Because these dialogs are opened imperatively (`MatDialog.open(Component)`), there is no host template at the call site — so the shell is wrapped **inside each dedicated dialog's own template**, and only the dedicated component is passed to `MatDialog.open`.

`zac-generic-dialog` owns **header chrome only** (toolbar + content wrapper) and has zero form/ATOS knowledge. The action buttons are deliberately **not** part of the shell — they come from `<zac-form-actions>` (see Decision 2). The shell exposes one `(cancelled)` output for the toolbar close (X):

```html
<!-- generic-dialog.component.html -->
<mat-toolbar role="heading" class="gap-16" mat-dialog-title>
  @if (icon()) { <mat-icon>{{ icon() }}</mat-icon> }
  <span class="flex-grow-1">{{ titleKey() ?? "" | translate }}</span>
  <button mat-icon-button type="button" (click)="cancelled.emit()">
    <mat-icon>close</mat-icon>
  </button>
</mat-toolbar>
<mat-divider />

<div mat-dialog-content>
  @if (melding()) { <p [innerHTML]="melding()"></p> }
  @if (uitleg()) { <p class="uitleg" [innerHTML]="uitleg()"></p> }
  <ng-content />
</div>
```

Signal inputs: `titleKey`, `icon`, `melding`, `uitleg`. Output: `cancelled`. (The output is named `cancelled`, not `cancel`, because `@angular-eslint/no-output-native` forbids output names that collide with native DOM events.)

**Alternatives considered:**

| Alternative | Why rejected |
|---|---|
| Shell owns its own confirm/cancel buttons | Would duplicate button logic that `<zac-form-actions>` already provides; the codebase prefers a single action-buttons component. |
| Multi-slot projection (`select="[title]"`, `[actions]"`) | More flexible but more boilerplate per dialog; the header chrome is uniform across all 9 sites, so fixed inputs are simpler and more readable. |
| Keep `DialogComponent`, just swap `mfb-form-field` → a native field switch | Retains the `AbstractFormField[]`/string-keyed-result contract and its `any`-typed values; does not deliver typed dialogs or remove the ATOS types. |
| One bespoke component per call site, no shell | Re-introduces the chrome duplication the codebase already suffers from. |

### Decision 1b: Generalize `<zac-form-actions>` instead of a second action-buttons component

The footer buttons reuse the existing `<zac-form-actions>` (already used by `besluit-intrekken`), placed inside the host `<form>` alongside `<zac-generic-dialog>`. But `zac-form-actions` **required** a `mutation` input (`{ isPending: Signal<boolean> }`), which would force these callback-based dialogs onto TanStack mutations. Rather than introduce a second, dumber action-buttons component (two components to maintain), we **generalize the one that exists**:

```ts
// form-actions.component.ts
protected readonly mutation = input<{ isPending: Signal<boolean> }>();   // was input.required
protected readonly loading = input(false);
protected isPending() {                                                  // plain method, not computed
  return this.mutation()?.isPending() ?? this.loading();
}
```

- The template's three `mutation().isPending()` reads become `isPending()`.
- `isPending` is a **method**, not a `computed`: the existing spec swaps the `isPendingSignal` instance between assertions, and a memoized `computed` would keep tracking the old signal instance. A method re-evaluates each change-detection pass, matching the original direct-binding semantics.
- Fully backward compatible: existing mutation-driven callers pass `[mutation]` unchanged; the new dialogs pass `[loading]`.

**Alternative considered:** a new dumb `zac-dialog-actions` component (`[disabled]`/`[loading]` in, `confirm`/`cancel` out). Rejected: it would leave two action-buttons components in the codebase for the same job.

### Decision 2: `zac-reden-dialog` — one reusable dedicated dialog for the 8 single-reason sites

Eight of nine sites are a single required "reden" field differing only by: input vs textarea, maxlength, labels, `melding`/`uitleg`, icon, and the service call. Model that as typed dialog data and one component:

```ts
export type RedenDialogData = {
  titleKey: string;
  icon?: string;
  label?: string;               // default "reden"
  multiline?: boolean;          // false → zac-input, true → zac-textarea
  maxlength?: number;
  melding?: string;
  uitleg?: string;
  confirmButtonActionKey?: string | null;
  cancelButtonActionKey?: string | null;
  callback?: (reden: string) => Observable<unknown>;
};
```

The internal control is always named `reden` — no `controlKey` is needed, because the callback signature is `(reden: string) => …`, so callers no longer destructure a named field (site 3's old `redenOpschortingField` id was purely an MFB detail).

Template composes the shell for the header and `<zac-form-actions>` for the footer, both inside one `<form>`:

```html
<form [formGroup]="form" (ngSubmit)="submit()" (cancel)="close()">
  <zac-generic-dialog
    [titleKey]="data.titleKey" [icon]="data.icon"
    [melding]="data.melding" [uitleg]="data.uitleg"
    (cancelled)="close()">
    @if (data.multiline) {
      <zac-textarea [form]="form" key="reden" [label]="label" />
    } @else {
      <zac-input [form]="form" key="reden" [label]="label" />
    }
  </zac-generic-dialog>

  <zac-form-actions
    [form]="form" [loading]="loading" wrapper="dialog"
    [submitLabel]="submitLabel" [cancelLabel]="cancelLabel" />
</form>
```

`submitLabel`/`cancelLabel` default to the old `actie.ja`/`actie.annuleren` when the caller does not override them. Validators live in the dialog: `Validators.required` always, plus `Validators.maxLength(maxlength)` when a `maxlength` is supplied — so `zac-form-actions` disables submit until the reden is valid, and `zac-input`/`zac-textarea` read the same `maxLength` validator to render the character counter.

`submit()` mirrors `DialogComponent.confirm()`: guard when no callback (`close(true)`), else set `loading`, call `data.callback(reden)`, `dialogRef.close(result ?? true)` on next / `close(false)` on error. Because the callback is a synchronous-friendly Observable, no TanStack mutation is involved. The typed `FormGroup` replaces `AbstractFormField` and the string-keyed result harvesting; the returned value is a real string, not `any`.

### Decision 3: `zac-zaak-afbreken-dialog` — dedicated dialog for the one select site

Site 1 selects a `RestZaakbeeindigReden` from an options `Observable` and returns the chosen object. It is genuinely distinct (async options, object result), so it becomes its own dedicated component wrapping `zac-generic-dialog` around a `zac-select`, with `<zac-form-actions>` for the footer (same pattern as `zac-reden-dialog`). It receives the options observable and a typed callback via `MAT_DIALOG_DATA` and returns the callback result via `afterClosed()`, matching the current behaviour.

### Decision 4: Call-site migration keeps the `afterClosed()` contract

Each call site swaps `open(DialogComponent, { data: new DialogData({...}) })` for `open<RedenDialogComponent, RedenDialogData>(RedenDialogComponent, { data: … })` (or `ZaakAfbrekenDialogComponent`). The `.afterClosed().subscribe(result => ...)` handling is unchanged because the dedicated dialogs close with the same callback result. Site 3's callback maps the entered string straight into `resumeZaak({ reason })` — no named control key needed.

**One exception — `informatie-object-view`:** its reden field was conditional (`this.zaak ? [input] : []`) and `deleteEnkelvoudigInformatieObject$(reden?)` takes an optional reden. When a zaak is present it uses `RedenDialogComponent` (required reden); when absent (ontkoppelde documenten) it falls back to the existing `ConfirmDialogComponent` (no field, plain confirm with the delete observable). Both close with a truthy result, so the shared `afterClosed()` handler is unchanged.

### Decision 5: Domain dialog services keep the trigger components clean

Opening a dialog inline is verbose — `open<Component, Data>(Component, { data: { …15 lines… } })` — and zaak-view has seven of them. Two thin injectable services hold the presentation config (title/icon/message keys) so the trigger components pass only the dynamic values + the business callback and keep their own `afterClosed()` reaction:

- **`ZaakDialogService`** (`zaken/zaak-dialog.service.ts`): `afbreken`, `heropenen`, `hervatten`, `wijzigInitiator`, `ontkoppelInitiator`, `ontkoppelBetrokkene`, `verwijderBagObject`.
- **`DocumentDialogService`** (`informatie-objecten/document-dialog.service.ts`): `ontkoppelDocument`, `verwijderDocument` (the latter hides the has-zaak → reden vs no-zaak → `ConfirmDialogComponent` branch entirely).

Each method returns the `MatDialogRef` and injects `TranslateService` to resolve message keys internally (the shell renders `melding`/`uitleg` as pre-translated strings via `[innerHTML]`). A zaak-view call site drops from ~18 lines to ~4.

**Scope guard:** only the reason/confirm dialogs (heavy inline config) go through a service. The already-one-line dedicated dialogs (`opschorten`, `verlengen`, `afhandelen`, `ontkoppelen`, `actie-onmogelijk`, `intake-afronden`) stay inline — wrapping them would add indirection for no line savings. Two domain services (not one) keep the catalog domain-correct: standalone-document dialogs don't belong to a zaak service.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Behaviour drift for one of the 9 sites (labels, maxlength, validators, result shape) | Per-site parity table above; each migrated site keeps its existing translation keys and validators; component specs assert confirm-disabled-until-valid and correct result. |
| `[innerHTML]` for `melding`/`uitleg` carried over from the old dialog (XSS surface) | Behaviour-preserving — same bindings as today, same trusted `translate.instant(...)` sources; no new untrusted input. Noted, not expanded. |
| `zac-reden-dialog` grows into a mini form-builder if future sites add fields | Scope guard: it stays single-field. A second field ⇒ a new dedicated dialog, not a `fields[]` array (that would recreate the MFB anti-pattern). |
| Existing dedicated dialogs still duplicate chrome after this change | Out of scope but now trivially adoptable; follow-up can migrate them to `zac-generic-dialog`. |

## Migration Plan

1. Generalize `<zac-form-actions>` (optional `mutation` + `loading` input) and extend its spec.
2. Add `zac-generic-dialog` (+ spec).
3. Add `zac-reden-dialog` and `zac-zaak-afbreken-dialog` (+ specs), both using `<zac-form-actions>`.
4. Migrate the 9 call sites, updating each host component's imports/spec.
5. Delete `DialogComponent`, `DialogData`, their spec/template/style files, and the `shared.module.ts` declaration/export.
6. `cd src/main/app && npm run lint` (0 errors) + `tsc --noEmit -p tsconfig.app.json` + `ng test` green; confirm no remaining `mfb-form-field`/`MaterialFormBuilderModule`/`AbstractFormField` references under `shared/dialog`.

## Open Questions

1. ~~Shell location~~ — resolved: new `shared/dialog/generic-dialog/` subfolder; the old flat `dialog.component.*`/`dialog-data.ts` files are deleted.
2. Should the pre-existing dedicated dialogs be migrated to the shell in this change or a follow-up? Proposed: follow-up, to keep this change reviewable.
