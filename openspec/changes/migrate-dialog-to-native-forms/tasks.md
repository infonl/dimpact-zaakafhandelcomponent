## 1. Generalize the shared action-buttons component

- [x] 1.1 In `shared/form/form-actions/form-actions.component.ts`, make `mutation` optional (`input<{ isPending: Signal<boolean> }>()`, was `input.required`) and add `loading = input(false)`; add `isPending()` as a plain method returning `this.mutation()?.isPending() ?? this.loading()` (method, not `computed`, so it re-reads a swapped signal each CD).
- [x] 1.2 In `form-actions.component.html`, replace the three `mutation().isPending()` reads with `isPending()`.
- [x] 1.3 Extend `form-actions.component.spec.ts` with a "without a mutation, using the loading input" block asserting submit enabled when `loading=false` and submit+cancel disabled when `loading=true`.

## 2. Presentational shell: `zac-generic-dialog`

- [x] 2.1 Create `shared/dialog/generic-dialog/generic-dialog.component.ts` — standalone `zac-generic-dialog`, signal inputs (`titleKey`, `icon`, `melding`, `uitleg`) and a single `cancelled` output (named `cancelled`, not `cancel`, to satisfy `@angular-eslint/no-output-native`); imports `MatToolbar`, `MatIcon`, `MatIconButton`, `MatDivider`, `MatDialogContent`, `TranslateModule`; SPDX `2026 INFO.nl`. **No footer buttons** — those come from `<zac-form-actions>`.
- [x] 2.2 Create `generic-dialog.component.html` — toolbar (icon/title/close→`cancelled`), `<mat-divider>`, `mat-dialog-content` with optional `melding`/`uitleg` and `<ng-content />`.
- [x] 2.3 Create `generic-dialog.component.spec.ts` (standalone host, no `NO_ERRORS_SCHEMA`, no `any`): renders `titleKey` + projected content + `melding`; emits `cancelled` on the close (X) button.

## 3. Dedicated dialog: `RedenDialogComponent`

- [x] 3.1 Create `shared/dialog/reden-dialog/reden-dialog.component.ts` — standalone; exported `RedenDialogData` (titleKey, icon?, label?, multiline?, maxlength?, melding?, uitleg?, confirmButtonActionKey?, cancelButtonActionKey?, callback?: `(reden: string) => Observable<unknown>`); `inject()` for `MatDialogRef`/`MAT_DIALOG_DATA`/`FormBuilder`; single `reden` control with `Validators.required` (+ `Validators.maxLength` when `maxlength` set); `loading` flag; `submitLabel`/`cancelLabel` defaulting to `actie.ja`/`actie.annuleren`.
- [x] 3.2 Create `reden-dialog.component.html` — `<form [formGroup] (ngSubmit)="submit()" (cancel)="close()">` wrapping `<zac-generic-dialog (cancelled)="close()">` (projecting `zac-input` or `zac-textarea` on `multiline`) followed by `<zac-form-actions [form] [loading] wrapper="dialog" [submitLabel] [cancelLabel] />`.
- [x] 3.3 Implement `submit()` mirroring `DialogComponent.confirm()` (no-callback → `close(true)`; else loading + `callback(reden)` → `close(result ?? true)` / `close(false)` on error) and `close()` → `close(false)`.
- [x] 3.4 Create `reden-dialog.component.spec.ts` (no `NO_ERRORS_SCHEMA`, no `any`): input vs textarea variant; submit disabled until valid+dirty; callback invoked with entered reden and closes with result; nullish→`true`; error→`false`; no-callback→`true`; `close()`→`false`.

## 4. Dedicated dialog: `ZaakAfbrekenDialogComponent`

- [x] 4.1 Create `zaken/zaak-afbreken-dialog/zaak-afbreken-dialog.component.ts` — standalone; `ZaakAfbrekenDialogData` (`options: Observable<RestZaakbeeindigReden[]>`, `callback: (reden) => Observable<unknown>`); required `reden` control; `loading` flag.
- [x] 4.2 Create `zaak-afbreken-dialog.component.html` — `<form>` wrapping `<zac-generic-dialog titleKey="actie.zaak.afbreken" icon="thumb_down_alt" (cancelled)="close()">` around a `zac-select` (`optionDisplayValue="naam"`) + `<zac-form-actions submitLabel="actie.zaak.afbreken" [loading] wrapper="dialog" />`.
- [x] 4.3 Create `zaak-afbreken-dialog.component.spec.ts` (no `NO_ERRORS_SCHEMA`, no `any`): renders select; submit disabled until a reden is selected+dirty; callback invoked with the selected `RestZaakbeeindigReden`, closes with result; `close()`→`false`.

## 5. Migrate call sites — zaak-view (7)

- [x] 5.1 `openZaakAfbrekenDialog` → `open<ZaakAfbrekenDialogComponent, ZaakAfbrekenDialogData>` with options + callback; keep existing `afterClosed()` handling.
- [x] 5.2 `openZaakHeropenenDialog` → `RedenDialogComponent` (input, maxlength 100, label `actie.zaak.heropenen.reden`, confirm `actie.zaak.heropenen`, icon `restart_alt`).
- [x] 5.3 `openZaakHervattenDialog` → `RedenDialogComponent` (input, maxlength 200, label `reden`, melding, confirm `actie.zaak.hervatten`, icon `play_circle`); callback → `resumeZaak({ reason })`.
- [x] 5.4 `initiatorGeselecteerd` (change) → `RedenDialogComponent` (textarea, melding, confirm `actie.initiator.wijzigen`, icon `link`).
- [x] 5.5 `deleteInitiator` → `RedenDialogComponent` (textarea, melding, confirm `actie.initiator.ontkoppelen`, icon `link_off`).
- [x] 5.6 `deleteBetrokkene` → `RedenDialogComponent` (textarea, melding, confirm `actie.betrokkene.ontkoppelen`, icon `link_off`).
- [x] 5.7 `bagObjectVerwijderen` → `RedenDialogComponent` (input, maxlength 80, uitleg, confirm `actie.bagObject.ontkoppelen`, icon `link_off`).
- [x] 5.8 Update imports (add `RedenDialogComponent`/`RedenDialogData`, `ZaakAfbrekenDialogComponent`/`ZaakAfbrekenDialogData`; remove `DialogComponent`, `DialogData`, the three `*FormFieldBuilder`s, and the now-unused `Validators`).

## 6. Migrate call sites — zaak-documenten & informatie-object-view (2)

- [x] 6.1 `zaak-documenten` ontkoppelInformatieObject → `RedenDialogComponent` (textarea, maxlength 200, melding, confirm `actie.document.ontkoppelen`, icon `link_off`); drop `Textarea*Builder`/`DialogComponent`/`DialogData`/`Validators` imports.
- [x] 6.2 `informatie-object-view` verwijderen → `RedenDialogComponent` when `this.zaak` (input, maxlength 100), else fall back to the existing `ConfirmDialogComponent` (no field); drop `Input*Builder`/`DialogComponent`/`DialogData`/`Validators` imports.

## 7. Delete the ATOS-backed shared dialog

- [ ] 7.1 Delete `shared/dialog/dialog.component.{ts,html,less,spec.ts}` and `dialog-data.ts`.
- [ ] 7.2 Remove the `DialogComponent` import + declaration/export from `shared/shared.module.ts`.
- [ ] 7.3 Grep-verify no remaining references to `shared/dialog/dialog.component`, `DialogComponent` (the shared confirm dialog), or `DialogData`.

## 8. Verify

- [x] 8.1 `cd src/main/app && npm run lint` — 0 errors (no `any`, no `NO_ERRORS_SCHEMA`).
- [ ] 8.2 `cd src/main/app && npx tsc --noEmit -p tsconfig.app.json` — no type errors in new/changed code (blocked until generated `zac-openapi-types.d.ts` is regenerated).
- [ ] 8.3 `cd src/main/app && ng test` — new dialog specs (29 passing) + updated host specs green (host specs blocked until generated types regenerated).
- [ ] 8.4 Confirm `shared/dialog` has zero references to `MaterialFormBuilderModule`, `<mfb-form-field>`, and `AbstractFormField`.
