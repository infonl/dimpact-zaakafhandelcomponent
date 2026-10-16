## Context

See proposal.md for the motivation. The sources are the RFC attached to PZ-12637
(`rfc-unify-bpmn-cmmn-zaaktype-configuration.md`, chunks C1 and D1–D5) and the architecture note
"Two Engines, One Table" (option B). PZ-12754 has a second attachment, `PZ-12754-rfc-unify-zaaktype-configuration.md`,
that was not available when this change was written. Check it against this design before layer 5 starts.

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

- No frontend code outside the engine step and the container decides anything by engine.
- Every layer of the stack compiles and passes `./gradlew build`, `npm test`, and `./gradlew itest`, and merges
  on its own, in stack order.
- The two edit components (1780 ts lines today) are deleted. The container and its step components together are
  smaller.
- Every step that both engines show has one spec, parameterised by engine with `it.each`.

**Non-Goals:**

- Any database change. PZ-12637 delivered the storage.
- The fallback from the BPMN process to the stored confirmation email parameters (RFC section 4.4).
- Showing the humantask or user event listener steps for BPMN. They configure CMMN plan items.
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

### D2. Engine blocks as nullable nested objects, validated by the backend

`RestZaaktypeConfiguration` gets `processEngine: ProcessEngine?`, `cmmn: RestCmmnConfiguration?`, and
`bpmn: RestBpmnConfiguration?`. The engine-agnostic fields stay at the top level and keep their names, so the
step components that layers 1–4 extract bind to the same field paths before and after layer 5.

- `RestCmmnConfiguration`: `caseDefinition`, `humanTaskParameters`, `userEventListenerParameters`,
  `intakeMail`, `afrondenMail`, `automaticEmailConfirmation`.
- `RestBpmnConfiguration`: `processDefinitionKey`.
- The backend rejects a write whose block does not match `processEngine`, and a write with both blocks.
- The frontend narrows with one type guard per engine. These guards are the only places that read `processEngine`.

The confirmation email goes in the `cmmn` block, although the entity stores it on the base. The
`zaaktype-process-binding` spec makes the automatic confirmation of receipt CMMN only, so the contract follows
the runtime and not the storage. For a BPMN configuration the stored value is kept on update (see the
`zaaktype-configuration` delta).

Alternatives considered:

- A polymorphic `oneOf` type with a discriminator (a sealed class in Kotlin). This gives the cleanest TypeScript
  union. It was rejected for now: the generated types contain no `allOf` or `oneOf` anywhere, and JSON-B
  polymorphism plus the SmallRye schema output is new ground in this codebase. A later change can switch the
  backend and keep the same JSON on the wire.
- Flat nullable CMMN fields next to `processEngine`. This is the smallest diff, but it was rejected because it is
  not the end state.

### D3. Paths, verbs, and rights

| Operation | Endpoint |
|---|---|
| list | `GET /zaakafhandelparameters` (unchanged) |
| read | `GET /zaakafhandelparameters/{zaaktypeUUID}` (unchanged path, new payload) |
| write | `PUT /zaakafhandelparameters/{zaaktypeUUID}` (new; replaces `PUT` without a key and `POST /zaaktype-bpmn-configuration`) |

- The path key must match `zaaktype.uuid` in the payload.
- Every operation requires `beheren`. The `startenZaak` alternative on the BPMN reads goes away: no zaak creation
  flow calls them, and zaak creation reads the configuration through `listZaaktypesForZaakCreation`.
- The sub-resources under `/zaakafhandelparameters/` and `/bpmn-process-definitions` do not change.
- The productaanvraagtype check and the groep/behandelaar check move from the CMMN `PUT` into the one `PUT`. The
  BPMN path loses its separate copy.

### D4. Container and step components

```
parameters-edit                  container: route data, one typed FormGroup, submit, engine guards
 ├─ parameters-step-proces-model   engine; replaces 3 copies
 ├─ parameters-step-algemeen       groep/behandelaar cascade, warning windows, productaanvraagtype,
 │                                 definition slot (case definition | process definition)
 ├─ parameters-step-humantasks     @if CMMN
 ├─ parameters-step-acties         @if CMMN (user event listeners)
 ├─ parameters-step-mail           koppelingen + afzenders; intake/afronden + confirmation @if CMMN,
 │                                 collapsed explanation @if BPMN
 ├─ parameters-step-beeindiging
 ├─ parameters-step-koppelingen    betrokkene + BRP
 └─ smart-documents-form           already shared
```

- **Form ownership.** The container owns one typed `FormGroup` composed of one `FormGroup` per step. Each step
  component gets its group through a signal `input.required<…>()`, and gets its option lists (groepen,
  resultaattypen, mail templates) through its own TanStack queries. Steps have no outputs. The container reads
  the whole form on submit.
- **Mapping.** Two pure functions, `toForm(configuration)` and `toRestConfiguration(form, engine)`, live next to
  the container. They are unit-tested with `it.each` over both engines. This replaces the per-component
  `patchValue` and submit code. It also fixes the SmartDocuments asymmetry (RFC section 5.3), because the mapping
  covers every step whether the step was opened or not.
- Alternatives considered:
  - Steps that own their form and implement `ControlValueAccessor`. This was rejected, because it is more code
    per step and validation state is harder to read for the stepper's `stepControl`.
  - The container reading child forms through `viewChild`. This was rejected, because it depends on render order,
    and that dependency is the root of the current `ngAfterViewInit` SmartDocuments save.

### D5. Engine choice and the stepper

`mat-stepper` collects its steps when it renders, so the CMMN-only steps cannot appear or disappear under a
rendered stepper.

- **Saved configuration.** The engine is known from the `GET`. The container renders the stepper once, with the
  steps for that engine. The process model step shows the engine as read-only and only offers definitions of that
  engine.
- **Unsaved configuration.** The container first renders only the process model step. When the beheerder chooses
  the engine and the definition, the container renders the full stepper for that engine, with the process model
  step as step 1 and `selectedIndex` 1. Today the shell does the same thing with `selectedIndexStart`, so the user
  sees no difference.

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
- Layer 6 is the only layer that changes what a beheerder sees. Keeping it out of layer 5 lets the reviewer read
  layer 5 as "the same screen on a new contract".
- TanStack `queryOptions()` and `mutationOptions()` move in with the layer that first touches each endpoint, not
  in a separate layer.

## Risks / Trade-offs

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
- [Removing `startenZaak` from the BPMN reads breaks an unknown caller] → `git grep` shows no caller outside the
  admin service and the itests. The itests for that right are deleted together with the resource.
- [The PZ-12754 RFC attachment disagrees with this design] → Check it before layer 5. Layers 1–4 do not depend
  on the contract.

## Migration Plan

- No database migration.
- ZAC ships the backend and the frontend in one image, so the contract change has no version skew. Layer 5
  deploys as one unit.
- Rollback is reverting the release. No stored data changes shape.
