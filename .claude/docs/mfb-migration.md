# ATOS Material Form Builder (MFB) — Migration Plan

## Summary

| Component (§)                                                                                       | Blocked by                                             | Complexity  | Jira |
| --------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ----------- | ---- |
| `human-task-do` cleanup (§1)                                                                        | — _(taak formulieren migration complete)_              | Low         |      |
| `edit.component` (§2) + `edit-input.component` (§3) — one PR                                       | —                                                      | Medium-High |      |
| `informatie-object-verzenden` (§4)                                                                  | —                                                      | Medium      |      |
| `shared/dialog` — create `PromptDialogComponent` + batch a: migrate 5 `zaak-view` call sites (§5)  | —                                                      | Medium      |      |
| `shared/dialog` batch b: migrate remaining 5 call sites + clean up `DialogComponent` (§5)           | batch a                                                | Medium-High |      |
| `besluit-edit` (§6)                                                                                 | —                                                      | Medium-High |      |
| `besluit-view` (§7)                                                                                 | §5 dialog batch b                                      | Medium      |      |
| `taak-view` (§8)                                                                                    | Verify `getAngularHandleFormBuilder` complete (see §8) | Medium-High |      |
| `process-task-do` (§9)                                                                              | **Product/tech decision required** (see §9)            | High        |      |

---

## Background

The project uses a legacy Angular form framework called the **ATOS Material Form Builder (MFB)**,
located at `src/main/app/src/app/shared/material-form-builder/`. This library was originally
developed by Atos and is being phased out.

MFB consists of:

- **`<mfb-form>`** — a full dynamic form renderer driven by `FormConfig` + `AbstractFormField[][]`
- **`<mfb-form-field>`** — a single dynamic field renderer driven by `AbstractFormField`
- **Field builder classes** — `InputFormFieldBuilder`, `DateFormFieldBuilder`, etc. — that produce
  `AbstractFormField` instances with embedded `FormControl`s
- **`MaterialFormBuilderService`** — service used by edit components to create field instances

### Migration principle: one component per PR

Each component in this plan is migrated in its own PR with its own tests. This keeps diffs
reviewable, failures bisectable, and rollbacks safe.

### Migration principle: do not touch MFB source code

The `shared/material-form-builder/` folder must not be modified during this migration. All work
happens in the **consumers** — components are migrated away from MFB one by one. Once every
consumer has been removed, the entire `shared/material-form-builder/` folder is deleted in a
single final commit. Touching MFB internals risks breaking other consumers mid-migration and
creates unnecessary noise in the diff.

### Migration principle: no dynamic form rendering

The goal is **not** to replace MFB with another dynamic form system. All components should be
migrated to **explicit Angular templates** with concrete, statically declared form fields. Each
form field should appear once in the template as a typed component, bound to an explicit
`FormControl` on a `FormGroup` defined in the component class.

**Use these atomic shared form components from `shared/form/`:**

| MFB builder                                             | Replacement selector                                                                  |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `InputFormFieldBuilder`                                 | `<zac-input>`                                                                         |
| `TextareaFormFieldBuilder`                              | `<zac-textarea>`                                                                      |
| `DateFormFieldBuilder`                                  | `<zac-date>`                                                                          |
| `SelectFormFieldBuilder`                                | `<zac-select>`                                                                        |
| `MedewerkerGroepFieldBuilder`                           | Two separate `<zac-auto-complete>` fields: one for group, one for medewerker (see §8) |
| `DocumentenLijstFieldBuilder`                           | `<zac-documents>`                                                                     |
| `ParagraphFormFieldBuilder` / `DividerFormFieldBuilder` | Plain `<p>` / `<mat-divider>` in template                                             |
| `HiddenFormFieldBuilder`                                | No component — just a `FormControl` in the group, not rendered                        |
| `MessageFormFieldBuilder`                               | `<p>` or Angular Material `<mat-hint>` / `<span>` in template                         |

Submit/cancel actions should use `shared/form/form-actions/` or be written directly in the
component template with `<button>` elements bound to component methods.

**Do not use `<zac-form>` (the `ZacForm` wrapper component).** That component itself is a
dynamic form renderer and its use is discouraged for the same reasons as MFB. Render fields
explicitly in each component's own template.

---

## Affected components (verified)

#### 1. `plan-items/human-task-do/human-task-do.component.ts` _(MFB cleanup)_

The taak formulieren migration is complete — `getFormulierBuilder` now throws
`"DEPRECATED, use Angular form"` for every known `formulierDefinitie`, so the MFB `catch` block
is entirely dead code and can be removed immediately.

**Exact changes:**

_`human-task-do.component.ts`:_

- Remove `private formulier?: AbstractTaakFormulier` field and its import (`AbstractTaakFormulier`)
- Remove `formItems: Array<AbstractFormField[]> = []` field
- Remove `formConfig = new FormConfigBuilder()...build()` field
- Remove imports: `AbstractFormField`, `FormConfigBuilder`
- In `ngOnInit` early-return branch: remove only the `this.formItems = [[]]` assignment; the `this.formFields = []` assignment must stay
- Delete the entire `catch` block (lines 167–178)
- In `onFormSubmit`: remove the `try/catch` wrapper — the Angular path is the only path; keep only
  the mutation call that was previously in the `catch` body

_`human-task-do.component.html`:_

- Remove the `<div class="form"> … <mfb-form> … </div>` block (lines 36–43)
- Simplify the spinner `*ngIf` from `!formFields.length && !formItems.length` to `!formFields.length`

---

#### 2. `shared/edit/edit.component.ts`

**What it does:** Abstract base class for all inline-edit components. Takes
`formField: AbstractFormField` as an `@Input` and wires it into a `FormGroup` for inline editing.
Has no selector and cannot be used directly.

**MFB imports:**

- `MaterialFormBuilderService` — injected in the constructor but **never called**. Dead injection,
  leftover from an earlier version. Can be removed with zero behaviour change.
- `AbstractFormField` — genuinely used: `edit()` accesses `.id` (as the `FormGroup` control key)
  and `.formControl` (the actual `AbstractControl`) directly.

**Note:** `EditComponent` is abstract — it has no template or selector and cannot compile or be
tested independently. Its migration is therefore coupled to `EditInputComponent` (§3) and must
ship in the same PR.

**Migration approach:**

1. Remove `MaterialFormBuilderService` from the constructor — it is never called, so this is a
   pure dead-code removal with no behaviour change.
2. Replace `@Input() abstract formField: AbstractFormField` with
   `@Input({ required: true }) formControl!: FormControl`. The two usages of `AbstractFormField`
   in `EditComponent` collapse cleanly:
   - `this.formField.id` (the `FormGroup` control key) → replace with a fixed key `'value'`,
     since there is always exactly one control per inline-edit instance.
   - `this.formField.formControl` → replace with `this.formControl` directly.
3. Remove all `AbstractFormField` and `MaterialFormBuilderService` imports.

**Caller inventory (only one direct consumer of `<zac-edit-input>`):**

- `admin/referentie-tabel/referentie-tabel.component` — passes `[formField]="codeFormField"`,
  `[formField]="naamFormField"`, `[formField]="waardeFormField[row.id]"` where each is an
  `InputFormField` created by `InputFormFieldBuilder`. After migration these should be plain
  `FormControl` instances on the component, passed as `[formControl]="codeControl"` etc.
  `referentie-tabel.component.ts` must be updated in the same PR to remove the three
  `InputFormFieldBuilder` usages and replace them with `FormControl` instances.

---

#### 3. `shared/edit/edit-input/edit-input.component.ts`

**What it does:** An inline-edit component rendered as `<zac-edit-input>`. Shows a static value;
clicking it activates an editable `<mfb-form-field>` (an `InputFormField`) directly in place.
Used for inline editing of individual fields across the app.

**MFB imports:**

- `MaterialFormBuilderModule`
- `InputFormField` (type)

**Migration approach:**

1. Remove the dependency on `InputFormField` (an MFB type). Let the callers drive the API decision.
2. Replace `<mfb-form-field>` in the template with a plain Angular Material field:
   ```html
   <mat-form-field>
     <input matInput [formControl]="formControl" />
   </mat-form-field>
   ```
   Do **not** use `<zac-input>` here — that component is designed to operate inside a full
   `FormGroup` context and adds indirection with no benefit for a single inline field.
3. Remove `MaterialFormBuilderModule` and `InputFormField` imports.
4. Update `referentie-tabel.component.ts` and any other direct consumers to align with the new API.

---

#### 4. `informatie-objecten/informatie-object-verzenden/informatie-object-verzenden.component.ts`

**What it does:** A side-nav panel for sending documents linked to a case. Renders a full
`<mfb-form>` with 4 rows: an explanatory paragraph, a multi-document selector, a date picker,
and a textarea for notes.

**MFB imports:**

- `MaterialFormBuilderModule`, `FormComponent`, `FormConfigBuilder`, `FormConfig`,
  `AbstractFormField`
- `ParagraphFormFieldBuilder`, `DateFormFieldBuilder`, `DocumentenLijstFieldBuilder`,
  `DocumentenLijstFormField`, `TextareaFormFieldBuilder`

**Special complexity:** Uses `DocumentenLijstFormField.updateDocumenten()` in `ngOnChanges` to
swap the document list observable when the active case changes. This embedded-observable pattern
is what MFB uses instead of a plain input binding.

**Migration approach:**

1. Define an explicit `FormGroup` in the component with three controls: `documenten`, `verzenddatum`,
   `toelichting`
2. Write an explicit template replacing `<mfb-form>` with:
   - A `<p>` for the explanation text (was `ParagraphFormFieldBuilder`)
   - `<zac-documents>` bound to `documenten`
   - `<zac-date>` bound to `verzenddatum`
   - `<zac-textarea>` bound to `toelichting`
   - Explicit submit/cancel `<button>` elements
3. Replace `ngOnChanges` document refresh: use `injectQuery` driven by the active zaak UUID; pass
   the query result directly as the `options` input on `<zac-documents>`. No `ngOnChanges` needed.
4. Remove `@ViewChild(FormComponent)`, all field builder imports,
   `MaterialFormBuilderModule`, `FormConfigBuilder`, `AbstractFormField`

---

#### 5. `shared/dialog/dialog.component.ts` + `dialog-data.ts`

**What it does:** A generic reusable dialog that can optionally render a list of MFB form fields
(e.g. a "reason" text input before a destructive action). The `DialogData` class carries an
`AbstractFormField[]` array; the template iterates it with `*ngFor` and renders each via
`<mfb-form-field [field]="formField">`.

**MFB imports:**

- `MaterialFormBuilderModule`
- `FieldType` (enum)
- `AbstractFormField` (in `dialog-data.ts`)

**Blast radius:** `DialogData` (with `formFields`) is instantiated in 10 places across `zaak-view`,
`besluit-view`, `zaak-documenten`, `informatie-object-view` — all passing MFB field builder
instances.

**Problems with the current design:**

1. **MFB dependency** — `formFields: AbstractFormField[]` drives the dynamic field rendering.
2. **`callback` anti-pattern** — `DialogComponent` owns the API call, subscribes to the callback
   observable internally, and closes on success or error. This is the wrong responsibility split:
   on error the dialog silently closes with `false` — no user feedback, no retry possible.
3. **Spinner is internal** — `loading` state is managed inside `DialogComponent` while the API
   call runs. After removing the callback, a new mechanism is needed to preserve the spinner.

**Chosen solution — `PromptDialogComponent`:**

Replace `DialogComponent` with a new `PromptDialogComponent` that is self-contained: it owns the
mutation, shows a spinner via TanStack's `isPending()`, and closes on success. The name
`PromptDialogComponent` signals that this component does more than layout — it actively executes
an action. The caller only opens it and reacts to success (reload data, show snackbar).

**`PromptDialogData` interface:**

```typescript
interface PromptDialogData {
  title: string;                           // toolbar title (i18n key)
  icon?: string;                           // toolbar icon (Material icon name)
  message?: string;                        // optional explanation paragraph
  fieldConfig?: PromptFieldConfig;         // absent = pure confirmation, no input field
  mutationFn: (value: string) => Promise<unknown>;  // owned by the dialog
}

type PromptFieldConfig =
  | { type: 'input' | 'textarea'; label: string; maxlength?: number }
  | { type: 'select'; label: string; options$: Observable<{ id: string; naam: string }[]> };
```

`fieldConfig` is optional — when absent the dialog is a pure confirmation (no input field). This
covers `informatie-object-view` which conditionally passes an empty `formFields` array today.

**`PromptDialogComponent` internals:**

- `injectMutation(() => ({ mutationFn: this.data.mutationFn }))` — TanStack owns lifecycle
- Confirm button: `[disabled]="mutation.isPending()"` + spinner icon while `isPending()`
- Template uses `@switch (data.fieldConfig?.type)` — no `<zac-form>`, no dynamic renderer:

```html
@if (data.fieldConfig) {
  @switch (data.fieldConfig.type) {
    @case ('input')    { <zac-input    [formControl]="control" ... /> }
    @case ('textarea') { <zac-textarea [formControl]="control" ... /> }
    @case ('select')   { <zac-select   [formControl]="control" [options$]="data.fieldConfig.options$" ... /> }
  }
}
```

- `onSuccess`: `this.dialogRef.close(true)` — caller reacts via `afterClosed()`
- `onError`: `this.foutAfhandelingService.foutAfhandelen(error)` — dialog stays open, button
  re-enables, user can retry

**Note on service methods:** Most callbacks currently use `zacHttpClient` methods that return
plain Observables (not `zacQueryClient` mutations). Wrap them in `lastValueFrom()` in the
caller's `mutationFn` — no service changes needed:

```typescript
mutationFn: (reden: string) =>
  lastValueFrom(this.zakenService.afbreken(this.zaak.uuid, { zaakbeeindigRedenId: reden }))
```

**Audit of all 10 call sites:**

| #  | Call site                                       | Field                                     | `title` (i18n key)             | `icon`           | `mutationFn` |
| -- | ----------------------------------------------- | ----------------------------------------- | ------------------------------ | ---------------- | ------------ |
| a1 | `zaak-view` — afbreken                          | select: `RestZaakbeeindigReden`           | `actie.zaak.afbreken`          | `thumb_down_alt` | `zakenService.afbreken(uuid, { zaakbeeindigRedenId })` |
| a2 | `zaak-view` — heropenen                         | input                                     | `actie.zaak.heropenen`         | `restart_alt`    | `zakenService.heropenen(uuid, { reden })` |
| a3 | `zaak-view` — hervatten                         | input                                     | `actie.zaak.hervatten`         | `play_circle`    | `zakenService.resumeZaak(uuid, { reason })` |
| a4 | `zaak-view` — initiator wijzigen                | textarea                                  | `actie.initiator.wijzigen`     | `link`           | `zakenService.updateInitiator(...)` |
| a5 | `zaak-view` — document ontkoppelen              | textarea                                  | `actie.document.ontkoppelen`   | `link_off`       | `zakenService.ontkoppelen(...)` |
| b1 | `zaak-view` — betrokkene ontkoppelen            | textarea                                  | `actie.betrokkene.ontkoppelen` | `link_off`       | `zakenService.deleteBetrokkene(rolid, reden)` |
| b2 | `zaak-view` — BAG-object ontkoppelen            | input                                     | `actie.bagObject.ontkoppelen`  | `link_off`       | `bagService.delete(...)` |
| b3 | `zaak-documenten` — document ontkoppelen        | textarea                                  | `actie.document.ontkoppelen`   | `link_off`       | `zakenService.ontkoppelInformatieObject(...)` |
| b4 | `informatie-object-view` — document verwijderen | input _(absent when no zaak — see below)_ | `actie.document.verwijderen`   | `delete`         | `deleteEnkelvoudigInformatieObject$(reden)` |
| b5 | `besluit-view` — intrekken                      | select: `VervalReden`                     | `actie.besluit.intrekken`      | `stop_circle`    | `saveIntrekking(reden)` |

**b4 special case:** `informatie-object-view` currently passes `formFields: this.zaak ? [...] : []`.
When `this.zaak` is absent, pass `fieldConfig: undefined` — `PromptDialogComponent` renders as a
pure confirmation dialog with no input field.

**Migration approach:**

1. **Create `PromptDialogComponent`** in `shared/prompt-dialog/` (standalone). Implement the
   `PromptDialogData` interface, `@switch` template, and TanStack mutation as described above.
   Write unit tests. `DialogComponent` is not touched yet.
2. **Migrate call sites in two batches** (batch a: a1–a5, batch b: b1–b5). For each call site:
   replace `this.dialog.open(DialogComponent, { data: new DialogData(...) })` with
   `this.dialog.open(PromptDialogComponent, { data: { ... } as PromptDialogData })`. The caller
   subscribes to `afterClosed().pipe(filter(Boolean))` for post-success UI updates only (reload,
   snackbar). No service call in the subscriber.
   - **Batch a** (5 call sites — all in `zaak-view`): a1 afbreken, a2 heropenen, a3 hervatten,
     a4 initiator wijzigen, a5 document ontkoppelen.
   - **Batch b** (5 call sites — cross-component, ends with `besluit-view`): b1 betrokkene
     ontkoppelen, b2 BAG-object ontkoppelen, b3 zaak-documenten document ontkoppelen, b4
     informatie-object-view document verwijderen, b5 besluit-view intrekken.
     `besluit-view` (§7) is unblocked once b5 is done.
3. **Clean up `DialogComponent`** after batch b: remove `formFields` from `DialogData`, remove
   `MaterialFormBuilderModule`, `FieldType`, `AbstractFormField` from `dialog.component.ts`, and
   remove the `*ngFor` + `<mfb-form-field>` block from the template.

---

#### 6. `zaken/besluit-edit/besluit-edit.component.ts`

**What it does:** Side-nav panel for editing an existing decision (`besluit`). Currently builds a
dynamic `<mfb-form>` with many fields: dividers, paragraphs, dates, document lists, text inputs,
and textareas.

**MFB imports:**

- `FormConfigBuilder`, `AbstractFormField`
- `DateFormField`, `DateFormFieldBuilder`
- `DividerFormFieldBuilder`, `ParagraphFormFieldBuilder`
- `DocumentenLijstFieldBuilder`
- `InputFormFieldBuilder`, `TextareaFormFieldBuilder`

**Special complexity:** Two reactive field dependencies currently encoded inside MFB's
observable-per-field model:

1. `ingangsdatum` value changes → update `vervaldatum` minDate
2. `besluittype` value changes → swap the document list observable (calls
   `listInformatieObjecten(besluittype.id)`)

Additionally, a conditional publication section (divider + paragraph + `publicationDate` +
`lastResponseDate`) is only shown when `besluittype.publication.enabled` is true.

**Migration approach:**

1. Define an explicit `FormGroup` with all required controls:
   - `besluittype` (disabled input — read-only display of the besluittype name)
   - `ingangsdatum` (date, required)
   - `vervaldatum` (date, minDate driven by `ingangsdatum`)
   - `toelichting` (textarea, maxlength 1000)
   - `documenten` (documents list)
   - `reden` (input, required, maxlength 80 — wijziging reden)
   - `publicationDate` (date, optional — only rendered when `besluittype.publication.enabled`)
   - `lastResponseDate` (date, optional — only rendered when `besluittype.publication.enabled`)
2. Write an explicit template in `besluit-edit.component.html`:
   - `<zac-input>` for `besluittype` (disabled)
   - `<zac-date>` for `ingangsdatum`, `vervaldatum`
   - `<zac-textarea>` for `toelichting`
   - `<zac-documents>` for `documenten`
   - `<zac-input>` for `reden`
   - `@if (showPublicationSection)` block containing `<mat-divider>`, `<p>`, `<zac-date>` for
     `publicationDate` and `lastResponseDate`
   - Explicit submit/cancel buttons
3. Implement the reactive dependencies in `ngOnInit`:
   - Subscribe to `ingangsdatum` value changes → update a `protected minVervaldatum` property
     used as `[minDate]` on the `<zac-date>` for `vervaldatum`
   - Subscribe to `besluittype` value changes → update the signal/query source driving the
     `<zac-documents>` options; update `protected showPublicationSection` boolean for the `@if`
4. Remove all MFB imports and `fields: Array<AbstractFormField[]>`

---

#### 7. `zaken/besluit-view/besluit-view.component.ts`

**What it does:** Read-only view of a decision. Renders individual `<mfb-form-field>` instances
for each besluit field. Also opens a `DialogComponent` carrying MFB field builders to collect an
"intrekkingsreden" before revoking a decision.

**MFB imports:**

- `DateFormFieldBuilder`, `HiddenFormFieldBuilder`, `InputFormFieldBuilder`
- `DocumentenLijstFieldBuilder`, `DocumentenLijstFormField`
- `MessageFormFieldBuilder`, `MessageLevel`
- `SelectFormFieldBuilder`

**Special complexity:**

- Uses `DocumentenLijstFormField` instances stored in `besluitInformatieobjecten` record
  (keyed by besluit UUID) to drive per-besluit document list rendering
- Passes MFB field builders into `DialogData` for the intrekking confirmation — blocked by §5 dialog batch b

**Migration approach:**

1. Replace `besluitInformatieobjecten: Record<string, DocumentenLijstFormField>` with a plain
   `Record<string, RestEnkelvoudigInformatieobject[]>` populated via the existing service call
2. Replace each `<mfb-form-field>` in the template with the appropriate direct display: use
   `<zac-documents [readonly]="true">` for document lists, and static bindings (`{{ value }}`,
   `<zac-static-text>`, or equivalent) for scalar fields
3. Replace the `DialogData` call site with the dedicated "intrekking bevestigen" dialog component
   created in §5 batch b
4. Remove all MFB field builder imports

---

#### 8. `taken/taak-view/taak-view.component.ts`

**What it does:** The full task detail view. Has a dual rendering path: Form.io tasks use
`<formio>`; legacy MFB tasks use `<mfb-form>`. The MFB form renders task assignment fields
(group + employee) built with `MedewerkerGroepFieldBuilder` plus task-specific fields from
`AbstractTaakFormulier`.

**MFB imports:**

- `InputFormFieldBuilder`, `MedewerkerGroepFieldBuilder`, `TextareaFormFieldBuilder`
- `FormConfig`, `FormConfigBuilder`

**Migration approach:**

1. Verify that `TaakFormulierenService.getAngularHandleFormBuilder` covers all known
   `formulierDefinitieId` values (the same check that unblocked §1 — confirm by reading
   `taak-formulieren.service.ts`). If every case is handled, this component is unblocked.
   Note: this is the **handle/behandel** form path in `taak-view`, distinct from the
   **request/start** form path already confirmed complete for §1 (`human-task-do`).
2. The group/employee assignment fields are already handled explicitly in `human-task-do` using
   two `<zac-auto-complete>` components — replicate that same explicit template pattern here
3. Once confirmed unblocked, delete the `<mfb-form>` branch from the template
4. Remove `MedewerkerGroepFieldBuilder`, `InputFormFieldBuilder`, `TextareaFormFieldBuilder`,
   `FormConfig`, `FormConfigBuilder`

---

`shared/dialog` (§5) starts with creating `PromptDialogComponent`, then migrates call sites in two
batches of 5 (batch a: all in `zaak-view`; batch b: cross-component, ending with `besluit-view`
intrekken). `DialogComponent` is not touched until the final step of batch b, which cleans up its
MFB imports and removes the `*ngFor` + `<mfb-form-field>` block.

#### 9. `plan-items/process-task-do/process-task-do.component.ts`

> ⚠️ **Product/tech decision required before this ticket can be scoped.** This is not a
> straightforward MFB migration — the feature is currently broken end-to-end and the backend
> API for it has never been called from the frontend.

**Current state — broken in production:**

The component exists but is entirely non-functional:

1. **`ngSwitchCase` collision** — `zaak-view.component.html` uses the same expression
   (`actiefPlanItem?.naam ?? 'none'`) for both `*ngSwitchCase` blocks (human-task-do and
   process-task-do). Angular evaluates the first match and never reaches the second. Process
   tasks always open the human task panel.

2. **Wrong read endpoint called** — `createPlanItemMenuItem` in `zaak-view.component.ts` calls
   `readHumanTaskPlanItem` for every plan item, including process tasks.
   `planItemsService.readProcessTaskPlanItem()` exists but is never called anywhere.

3. **`ProcessFormulierenBuilder` never assigns `_formulier`** —
   `build()` always returns `undefined`. There are no concrete
   `AbstractProcessFormulier` implementations anywhere in the codebase.

4. **`AbstractProcessFormulier.getData()` returns `null`** — cast to
   `GeneratedType<"RESTProcessTaskData">`. Any code that reaches it would produce a null
   payload.

5. **`<mfb-form>` never renders** — `formItems` is declared as `Array<AbstractFormField[]>` but
   is never populated, so `<mfb-form>` receives an empty array.

6. **`doProcessTaskPlanItem` backend endpoint unreachable** — The REST endpoint and
   `CMMNService.startProcessTaskPlanItem()` have never been called from the frontend.

**Dead backend code to flag for cleanup:**

- `PlanItemsRestService.kt` — `processTaskPlanItem/{id}` (GET) and `doProcessTaskPlanItem` (POST) endpoints
- `CMMNService.startProcessTaskPlanItem()` — called only from the dead POST endpoint
- `RESTProcessTaskData` — model used only by the dead endpoint

**MFB imports in component:**

- `AbstractFormField`, `FormConfig`, `FormConfigBuilder`

**Migration approach:**

This ticket is a combination of three things, and the scope cannot be determined until a
product/tech decision is made:

1. **Bug fix** — Fix `zaak-view.component.html` `ngSwitchCase` collision and fix
   `createPlanItemMenuItem` to call `readProcessTaskPlanItem` for process tasks.

2. **Feature implementation** — Define what data a process task actually needs (group, user,
   deadline?), implement the complete flow: read → display form → `doProcessTaskPlanItem` POST.
   This requires aligning with product on expected UX and with backend on what fields the
   endpoint should accept.

3. **MFB migration** — Once the feature is defined and implemented, replace `<mfb-form>` with an
   explicit Angular template and remove all MFB imports. Delete `AbstractProcessFormulier`,
   `ProcessFormulierenBuilder`, `ProcessFormulierenService` (all are effectively dead).

**Decision required:** Fix the full process task flow, or consciously remove the feature
end-to-end (frontend + dead backend endpoints). Do not start implementation without this
decision.

---
