## Context

See proposal.md for the motivation. The sources are the RFC attached to PZ-12637
(`rfc-unify-bpmn-cmmn-zaaktype-configuration.md`, chunks C1 and D1–D5), the architecture note
"Two Engines, One Table" (option B), and RFC-001 attached to PZ-12754
(`PZ-12754-rfc-unify-zaaktype-configuration.md`, 2026-09-22). RFC-001 proposes one resource, one payload with
`processEngine` and an engine block, and one `parameters-edit` built from shared step components. This design
takes the resource and the screen, but not the engine blocks. RFC-001 was written before PZ-12637 landed, and
differs from this design in these points:

| RFC-001 | This design | Why |
|---|---|---|
| Warning windows, mailtemplate koppelingen, and zaakafzenders in the `cmmn` block (§3.1) | At the top level, for both engines | PZ-12637 moved them to the base, and the runtime applies them to BPMN zaken |
| A `processEngine` field and one block per engine, `cmmn` or `bpmn` (§3.1) | One flat payload with one `processDefinition` that names its engine (D2) | For ZAC the engine is only the process that a zaak runs with. Only the definition reference needs to know it |
| `processEngine` is never null (§3.1) | `processDefinition` is null for a configuration that was never saved | The screen must offer the choice of a process definition for an unsaved configuration (D5) |
| Keep the two JPA entities; option C later (§3.3, §5.2) | No entity work | PZ-12637 delivered one entity with a process binding and an optional CMMN extension |
| Step 0: decide and enforce `productaanvraagtype` uniqueness (§7.1) | Not in this change | PZ-12637 settled it: one validator per zaaktype omschrijving, spec "A productaanvraagtype belongs to at most one zaaktype". No DB constraint was added; that stays out of scope |
| Mail step CMMN only (§3.2) | Mail step for both engines, confirmation email CMMN only | Decision on PZ-12754: BPMN gets koppelingen and afzenders |
| Steps under `parameters-components/` (§3.2) | Steps under `parameters-edit/steps/` | Next to the container that composes them |
| The shell keeps the "nothing saved" branch (§3.2) | The container handles it, the shell is deleted (D5) | One component decides the engine, not two |
| `zac-inputs` migration and replacing the resolver by a query come after the collapse (§6) | TanStack queries move in per step, the resolver does one `GET` | The steps already fetch their own option lists |

RFC-001 points that this design adopts:

- Admin reads and writes require `beheren`; zaak creation does not use the admin resource (§7.2), see D3.
- `isZaakspecifiekAutoriseerbaar` stays a read-only, derived field at the top level of the payload (§7.4). It comes
  from the zaaktype, not from the stored configuration, so it applies to both engines.

RFC-001 points that this design no longer adopts: the engine block invariant with `@AssertTrue` on the DTO
(§3.1). Without engine blocks there is nothing to check (D2).

RFC-001 guardrails §8.2.1 (reflection test over the base) and §8.2.3 (Konsist rules) are not part of this change.

Current state, on `main` after PZ-12637:

- **Storage.** One `ZaaktypeConfiguration` entity with a `ZaaktypeProcessBinding` (engine + definition key) and
  an optional `ZaaktypeCmmnExtension` (humantask parameters, user event listeners, intake/afronden mail). The
  deadline warning windows, confirmation email parameters, zaakafzenders, and mailtemplate koppelingen are on the
  base. `bindTo()` already rejects a change of engine.
- **REST.** Two resources over that one entity:
  - `ZaaktypeConfigurationRestService` (`/zaakafhandelparameters`): `GET` list, `GET /{zaaktypeUUID}`, `PUT`
    without a path key, plus sub-resources (zaakbeeindigredenen, resultaattypes, case-definitions,
    formulierdefinities, replyTo, smartdocuments).
  - `ZaaktypeBpmnConfigurationRestService` (`/zaaktype-bpmn-configuration`): `GET` list, `GET /{processDefinitionKey}`,
    `POST`. Its reads accept `startenZaak || beheren`. The frontend calls only the list and the `POST`.
- **Frontend.** `/admin/parameters/:uuid` resolves through `ZaakafhandelParametersResolver`, which runs three
  `GET`s in parallel and merges them. It derives `isBpmn` from "this zaaktype UUID is in the BPMN list".
  `ParametersEditShellComponent` then renders `parameters-select-process-model-method`, `parameters-edit-cmmn`
  (1176 ts + 944 html lines), or `parameters-edit-bpmn` (604 ts + 420 html lines).
  - The CMMN component builds one `FormGroup` per step in a single class.
  - The BPMN component copies four of those steps.
  - Only `smart-documents-form` is a shared child component.
- **Constraint.** The shell waits for the resolver because "mat-stepper cannot load its steps dynamically".

## Goals / Non-Goals

**Goals:**

- The REST contract and the frontend are engine-agnostic. Only the process definition reference names its engine.
  The frontend decides by engine in two places only: which process definitions a saved configuration may switch
  to, and the confirmation email, which a BPMN process sends itself. Every other step shows what the chosen process
  definition has use for.
- Every layer of the stack compiles and passes `./gradlew build`, `npm test`, and `./gradlew itest`, and merges
  on its own, in stack order.
- The two edit components (1780 ts lines today) are deleted. The container and its step components together are
  smaller.
- Every step has one spec. A step that does not know about engines is not parameterised by engine.

**Non-Goals:**

- Any database change. PZ-12637 delivered the storage.
- The fallback from the BPMN process to the stored confirmation email parameters (RFC section 4.4).
- Plan item settings for the user tasks of a BPMN process. A BPMN process names its task forms itself; a shared
  task form configuration is RFC section 7.
- Redesigning the screen. The steps keep their current fields, order, and labels, except for what the specs add
  for BPMN.

## Decisions

### D1. One resource, not two composed payloads

The RFC (section 5.1) keeps one resource per engine, because one payload would be half empty for BPMN. Since
PZ-12637 that argument no longer holds. Only the CMMN extension, the case definition, and the confirmation email
are CMMN only. Everything else is on the base and applies to both engines. The ticket asks for one form and one
endpoint, and the resolver merge exists only because there are two resources.

Alternative considered: two resources whose payloads compose a shared nested object (the RFC). This was rejected,
because the client would still have to find out which resource to call, and that is the job the resolver does by
hand today.

### D2. One process definition reference, no engine blocks

`RestZaaktypeConfiguration` is one flat payload, as on `main`. Its `caseDefinition` becomes `processDefinition`:
a `RestProcessDefinition` with `processEngine`, `key`, `naam`, and the plan item definitions of that process
definition (`humanTaskDefinitions`, `userEventListenerDefinitions`). The settings of those plan items stay at
the top level, as on `main`: `humanTaskParameters`, `userEventListenerParameters`, `intakeMail`, `afrondenMail`,
and `automaticEmailConfirmation`.

- `processDefinition` is the only field that names an engine. A configuration that was never saved has none, and
  the `PUT` rejects a payload without one.
- A process definition without plan items, such as every BPMN process definition, has no plan item settings. The
  backend stores the plan item settings only in the CMMN extension of a CMMN-bound configuration, and a binding to
  CMMN creates that extension. A read of a BPMN configuration has empty plan item lists and no intake or afronden
  mail option.
- The confirmation email parameters are stored as the payload sets them, for both engines. Only the CMMN
  productaanvraag handling sends the confirmation of receipt (`zaaktype-process-binding`), and the admin screen
  sends the stored value back for BPMN.

Alternatives considered:

- A `processEngine` field and one nested block per engine, `cmmn` or `bpmn`, checked with an `@AssertTrue` on the
  DTO. This was the first version of this design. It was rejected before the stack merged: it made every reader of
  the configuration, in the backend, the admin screen and the zaak dialogs, branch on the engine. For ZAC the
  engine is only the process that a zaak runs with.
- A polymorphic `oneOf` type with a discriminator. Rejected for the same reason, and because the generated types
  contain no `allOf` or `oneOf` anywhere.

### D3. Paths, verbs, and rights

| Operation | Endpoint |
|---|---|
| list | `GET /zaakafhandelparameters` (unchanged) |
| read | `GET /zaakafhandelparameters/{zaaktypeUUID}` (unchanged path, new payload) |
| write | `PUT /zaakafhandelparameters/{zaaktypeUUID}` (new; replaces `PUT` without a key and `POST /zaaktype-bpmn-configuration`) |

- The path key must match `zaaktype.uuid` in the payload.
- Every operation requires `beheren`. The `startenZaak` alternative on the BPMN reads goes away. The zaak creation
  screen did call the BPMN list, for the default groep and behandelaar of a BPMN zaaktype, but
  `listZaaktypesForZaakCreation` already returns the configuration of every zaaktype, whatever its engine. The
  screen now reads both defaults from there, for both engines.
- `GET /zaakafhandelparameters/process-definitions` lists the process definitions of both engines, each with
  its plan item definitions. It replaces `GET /zaakafhandelparameters/case-definitions` and
  `GET /zaakafhandelparameters/case-definitions/{key}`, and the admin screen no longer calls
  `/bpmn-process-definitions`. The other sub-resources under `/zaakafhandelparameters/` and
  `/bpmn-process-definitions` do not change.
- The productaanvraagtype check and the groep/behandelaar check move from the CMMN `PUT` into the one `PUT`. The
  BPMN path loses its separate copy.

### D4. Container and step components

```
parameters-edit                  container: route data, one typed FormGroup, submit
 ├─ parameters-step-proces-model   one list of the process definitions of every engine; replaces 3 copies
 ├─ parameters-step-algemeen       groep/behandelaar cascade, warning windows, productaanvraagtype
 ├─ parameters-step-humantasks     @if the process definition has human tasks
 ├─ parameters-step-acties         @if the process definition has user event listeners
 ├─ parameters-step-mail           koppelingen + afzenders; intake/afronden mail @if the process definition has
 │                                 that user event listener; confirmation email @if CMMN, collapsed explanation
 │                                 otherwise
 ├─ parameters-step-beeindiging
 ├─ parameters-step-koppelingen    betrokkene + BRP
 └─ smart-documents-form           already shared
```

- **What applies.** One function next to the container, `applyProcessDefinition(form, processDefinition)`,
  enables the settings that the process definition has use for and disables the others. The template shows a
  step or a section when its controls are enabled, and a disabled control does not count for validity. This is
  the only frontend code that reads `processEngine` for a setting.
- **Form ownership.** The container owns one typed `FormGroup` composed of one `FormGroup` per step. Each step
  component gets its group through a signal `input.required<…>()`, and gets its option lists (groepen,
  resultaattypen, mail templates) through its own TanStack queries. Steps have no outputs. The container reads
  the whole form on submit.
- **Mapping.** Two pure functions, `toForm(configuration)` and `toRestConfiguration(form, configuration)`, live
  next to the container. They are unit-tested for a CMMN-bound and a BPMN-bound configuration. This replaces the per-component
  `patchValue` and submit code. It also fixes the SmartDocuments asymmetry (RFC section 5.3), because the mapping
  covers every step whether the step was opened or not.
- Alternatives considered:
  - Steps that own their form and implement `ControlValueAccessor`. This was rejected, because it is more code
    per step and validation state is harder to read for the stepper's `stepControl`.
  - The container reading child forms through `viewChild`. This was rejected, because it depends on render order,
    and that dependency is the root of the current `ngAfterViewInit` SmartDocuments save.

### D5. Process definition choice and the stepper

`mat-stepper` collects its steps when it renders, so steps cannot appear or disappear under a rendered stepper.

- **Saved configuration.** The process definition is known from the `GET`. The container renders the stepper
  with the steps that the process definition has use for, at the algemeen step. The process model step offers
  only the process definitions of the engine that the configuration is bound to, because a configuration never
  changes engine.
- **Unsaved configuration.** The container first renders the stepper with only the process model step, which
  offers the process definitions of every engine in one list. When the beheerder chooses one, the container
  renders the stepper anew with the steps that it has use for. It renders the stepper inside a `@for` over the
  chosen process definition, tracked by that definition, so a change always creates a new stepper and never adds
  steps to a rendered one. The stepper keeps its selected step across the re-render.
- Choosing another process definition before the first save keeps every value, because the container owns the
  form. The human task and acties forms are rebuilt from the plan items of the new definition. The confirm dialog
  that the two screens showed before a switch of engine is gone, together with its i18n text.

### D6. The stack

| # | Branch | Layer | Behaviour change |
|---|---|---|---|
| 0 | `PZ-12754-zaakafhandelparameters-za-ps-frontend-backend-api-code-voor-bpmn-en-cmmn-vereenvoudigen` | This OpenSpec change | none |
| 1 | `feature/PZ-12754-step-beeindiging` | Shared beëindiging step, used by both edit components | none |
| 2 | `feature/PZ-12754-step-koppelingen` | Shared koppelingen step; restore the two `aria-label`s | none (a11y fix) |
| 3 | `feature/PZ-12754-step-proces-model` | One process model step in place of three | none |
| 4 | `feature/PZ-12754-step-algemeen` | Shared algemeen step with the definition slot | none |
| 5 | `feature/PZ-12754-one-resource` | Backend contract (D2, D3) + container (D4, D5) + resolver; delete the BPMN resource, both edit components, and the shell | contract |
| 6 | `feature/PZ-12754-bpmn-settings` | Mail step and warning windows for BPMN | yes, for beheerders |

- Layers 1–4 are possible before the contract change, because both edit components already have these steps.
  Each layer extracts one step, makes both components use it, and moves its spec from the CMMN spec files into
  one engine-parameterised spec.
- Layer 5 cannot be split. Changing the shape of the payload breaks the frontend in the same commit.
  Layers 1–4 keep it reviewable: by the time it lands, the container only composes existing steps.
- Layer 6 adds the BPMN settings. Keeping it out of layer 5 lets the reviewer read layer 5 as "the same screen on
  a new contract".
- TanStack `queryOptions()` and `mutationOptions()` move in with the layer that first touches each endpoint, not
  in a separate layer.

## Risks / Trade-offs

- [A CMMN and a BPMN process definition have the same name in the one list] → The list shows the name, or the key
  when there is none. Names are distinct in practice; if they are not, the next step is a suffix with the engine.
- [The overview list now shows the process name of a BPMN zaaktype in the model column] → Intended: the column
  showed nothing for BPMN before.
- [Layer 5 is large: backend contract, container, resolver, and three deletions] → Layers 1–4 move every step
  out first. Layer 5 compares the generated OpenAPI spec diff with D2/D3 in the PR description, and an itest
  round-trips one configuration per engine through the new `PUT`.
- [Rendering the stepper twice for an unsaved configuration loses form state] → The container owns the form
  (D4), not the stepper, so re-rendering the stepper keeps the values. A spec covers "choose BPMN, then go back to
  step 1".
- [The SmartDocuments save regresses again (see #7452)] → The "Unopened step keeps its stored values" scenario
  becomes a container spec and an itest.
- [A beheerder sets a mailtemplate koppeling for BPMN and expects the process to use it] → The koppeling is
  used by the mail dialog of the zaak, not by BPMN service tasks, which name their own template. The
  collapsed text in the mail step covers only the confirmation email. If beheerders get confused, the next step
  is help text per section, not a behaviour change.
- [A BPMN configuration without a default zaakafzender can no longer be saved] → The mail step now shows the
  zaakafzenders for BPMN, and demands a default one as it does for CMMN. A beheerder who opens such a
  configuration sees the error on the mail step and picks one. The special afzenders are always offered.
- [Removing `startenZaak` from the BPMN reads breaks an unknown caller] → `git grep` shows one caller outside the
  admin service and the itests: the zaak creation screen, which now reads the defaults from the zaaktype (D3). An
  itest checks that a user without `beheren` is refused.
- [The PZ-12754 RFC attachment disagrees with this design] → Check it before layer 5. Layers 1–4 do not depend
  on the contract.

## Migration Plan

- No database migration.
- ZAC ships the backend and the frontend in one image, so the contract change has no version skew. Layer 5
  deploys as one unit.
- Rollback is reverting the release. No stored data changes shape.
