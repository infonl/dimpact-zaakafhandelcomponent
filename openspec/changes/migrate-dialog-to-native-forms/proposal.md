## Why

The shared confirm dialog `shared/dialog/DialogComponent` is the **last shared consumer of the legacy ATOS `MaterialFormBuilder` (MFB)**. It renders an arbitrary `AbstractFormField[]` via `<mfb-form-field>` and harvests results with string-keyed `formField.formControl.value`. Because it is reused across the app, it blocks the final deletion of the MFB infrastructure (`MaterialFormBuilderModule`, `<mfb-form-field>`, `AbstractFormField`, the `*-form-field-builder` cluster).

The coupling is small and mechanical: across all 9 real call sites the entire field surface is just three primitives — a single text input, a single textarea, or a single select. Eight of the nine are a single "geef een reden" field returning `{ reden: string }`; one (zaak afbreken) is a select returning a typed object.

The codebase already has the target pattern: dedicated dialog components (`ZaakOpschortenDialogComponent`, `BesluitIntrekkenDialogComponent`, …) built from native `zac-*` form fields + typed `FormGroup`s. But each of those **duplicates the dialog chrome** (toolbar, title, icon, close button, divider, content/actions layout). There is no shared presentational shell.

## What Changes

- Introduce **`zac-generic-dialog`** — a presentational dialog shell (Angular content projection, React "children" style). It owns the header chrome only: toolbar/title/icon/close button, divider, and the `mat-dialog-content` wrapper (with optional `melding`/`uitleg`). It has **no form knowledge and no ATOS dependency**. The action buttons are **not** part of the shell — they are provided by the existing `<zac-form-actions>` component inside the host `<form>`. The shell exposes a single `(cancelled)` output for the toolbar close (X) button.
- **Generalize the existing `<zac-form-actions>`** (used today by `besluit-intrekken` etc.) so it can drive the footer for these callback-based dialogs: make its `mutation` input optional and add a `loading` boolean input, with pending state derived from `mutation?.isPending() ?? loading`. This keeps a **single** action-buttons component in the codebase without forcing every dialog onto TanStack mutations. Fully backward compatible — existing mutation-driven callers are unchanged.
- Introduce **`zac-reden-dialog`** — a reusable dedicated dialog for the single-"reden" case (input or textarea), opened via `MatDialog.open()`. It owns a typed `FormGroup`, renders a native `zac-input`/`zac-textarea` **inside** `<zac-generic-dialog>`, and returns the callback result. Covers 8 of 9 call sites.
- Introduce **`zac-zaak-afbreken-dialog`** — a dedicated dialog for the one select-based case (zaak afbreken/beëindigen), returning the typed `RestZaakbeeindigReden`. Covers the 9th call site.
- Because these dialogs are launched via `MatDialog.open(Component)` (imperative, no host template), the `<zac-generic-dialog>…fields…</zac-generic-dialog>` composition lives **inside each dedicated dialog's own template**, not at the call site.
- Migrate all 9 call sites (`zaak-view` ×7, `zaak-documenten` ×1, `informatie-object-view` ×1) from `open(DialogComponent, { data: new DialogData({ formFields, callback }) })` to the new dedicated dialogs.
- Delete `shared/dialog/DialogComponent`, `DialogData`, and their specs; remove the `MaterialFormBuilderModule`/`AbstractFormField`/`<mfb-form-field>` usage from `shared/dialog`.

## Non-Goals

- Deleting the MFB infrastructure itself (`MaterialFormBuilderModule`, `<mfb-form-field>`, `AbstractFormField`, `*-form-field-builder`). Other leaf consumers remain (`shared/edit`, `material-form-builder/form/*`); this change only removes the **shared dialog** consumer. Removing the shell of MFB stays a later step.
- Converting the dedicated dialogs to TanStack `mutation`s. To keep the change focused on removing ATOS, the dialogs keep the current Observable-callback + `loading` behaviour of `DialogComponent`; they reuse `<zac-form-actions>` via its new `loading` input rather than adopting mutations. Migrating individual dialogs to mutations can follow.
- Migrating the pre-existing dedicated dialogs (`besluit-intrekken`, `zaak-opschorten`, …) onto `zac-generic-dialog` to drop their duplicated header chrome. Now trivially possible, but out of scope here.
- Touching backend / Kotlin. This is a frontend-only change.

## Capabilities

### New Capabilities

- `native-dialog-forms`: A shared, presentational `zac-generic-dialog` shell plus native (`zac-*`, typed `FormGroup`) dedicated confirm dialogs that replace the ATOS-`MaterialFormBuilder`-backed shared `DialogComponent`.

### Modified Capabilities

<!-- No existing spec-level capability requirements are changing. -->

## Impact

- New: `src/main/app/src/app/shared/dialog/generic-dialog/` (`zac-generic-dialog` component + spec), `src/main/app/src/app/shared/dialog/reden-dialog/` (`RedenDialogComponent` + spec), `src/main/app/src/app/zaken/zaak-afbreken-dialog/` (dedicated select dialog + spec).
- Modified: `shared/form/form-actions/form-actions.component.{ts,html,spec.ts}` (optional `mutation` + new `loading` input); `zaken/zaak-view/zaak-view.component.ts` (7 call sites); `zaken/zaak-documenten/zaak-documenten.component.ts` (1); `informatie-objecten/informatie-object-view/informatie-object-view.component.ts` (1, with the no-zaak branch falling back to the existing `ConfirmDialogComponent`).
- Deleted: `shared/dialog/dialog.component.{ts,html,less,spec.ts}`, `shared/dialog/dialog-data.ts`.
- No API contract changes, no database migrations, no new dependencies.
- Frontend translations reused (existing `actie.*`/`reden`/`msg.*` keys); no new i18n keys required.
