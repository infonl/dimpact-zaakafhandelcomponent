## Why

PZ-12637 unified the storage and the services of the zaaktype configuration (the "zaakafhandelparameters"). The
REST contract and the admin screen are still split by engine. A beheerder edits one stored configuration through
two REST resources, with two payloads, two keys, and two verbs, in two Angular screens that are mostly copies of
each other. The resolver of the screen fetches both resources and merges them by hand. Because the BPMN payload
carries fewer fields than the stored configuration, a beheerder cannot set the deadline warning windows, the
mailtemplate koppelingen, or the zaakafzenders of a BPMN zaaktype, although ZAC applies them to BPMN zaken since
PZ-12637.

PZ-12754 implements chunks C and D of the RFC that is attached to PZ-12637
(`rfc-unify-bpmn-cmmn-zaaktype-configuration.md`), with one deviation: one REST resource in place of two
composed payloads (see design.md, D1).

## What Changes

- **BREAKING (internal API)** One REST resource for the configuration of a zaaktype:
  `GET` and `PUT /zaakafhandelparameters/{zaaktypeUUID}`, and `GET /zaakafhandelparameters` for the list.
  - `RestZaaktypeConfiguration` holds the engine-agnostic settings, a `processEngine` field, and one
    engine-specific block: `cmmn` or `bpmn`.
  - The `cmmn` block holds the case definition, the humantask parameters, the user event listener parameters,
    the intake and afronden mail options, and the automatic confirmation email.
  - The `bpmn` block holds the process definition key.
  - `PUT /zaakafhandelparameters` (without a path key) and the `/zaaktype-bpmn-configuration` resource are
    removed, together with `RestZaaktypeBpmnConfiguration` and its `groepNaam` field.
  - Only this repository calls these endpoints, so no external client breaks.
- One admin screen, `parameters-edit`, replaces `parameters-edit-cmmn`, `parameters-edit-bpmn`, and the runtime
  switch in `parameters-edit-shell`. It is a container that composes one child component per step. Each step
  component serves both engines.
- The resolver of the admin screen does one `GET`. It no longer derives the engine from the BPMN list, and it
  no longer merges two payloads.
- The three copies of the process model step become one step component. This includes
  `parameters-select-process-model-method`.
- New for a BPMN zaaktype, in the admin screen:
  - the einddatum-gepland and uiterlijke-einddatum-afdoening warning windows
  - the mailtemplate koppelingen
  - the zaakafzenders and their reply-to addresses
  - a collapsed confirmation email section that states that the BPMN process sends the confirmation of receipt
- The humantask, user event listener, and intake/afronden mail settings stay CMMN only. The screen does not
  show them for a BPMN zaaktype.
- A beheerder chooses the engine once, for a zaaktype configuration that was never saved. After the first save
  the screen does not offer the choice again, and the backend keeps rejecting a change of engine.
- The admin service uses TanStack `queryOptions()` and `mutationOptions()` for every endpoint that the step
  components call.

Delivery: a GitHub stack of six pull requests (design.md, D6). Layers 1 to 4 are frontend refactors without a
behaviour change. Layer 5 changes the REST contract and the frontend in one pull request. Layer 6 adds the BPMN
settings to the screen.

## Capabilities

### New Capabilities

- `zaaktype-configuration-admin-screen`: the admin screen in which a beheerder views and edits the
  configuration of a zaaktype, for both engines: which steps it shows per engine, how the engine is chosen, and
  what it shows for the BPMN confirmation email.

### Modified Capabilities

- `zaaktype-configuration`:
  - The requirement "The configuration REST contract stays unchanged" is replaced by one REST resource with
    engine blocks.
  - The requirement "A BPMN update preserves settings that its payload does not carry" now covers only the
    confirmation email parameters. These are the only engine-agnostic setting that the BPMN payload does not
    carry.
  - "Engine-agnostic settings apply to BPMN zaken" adds the mailtemplate koppelingen and the zaakafzenders.
- `zaaktype-process-binding`: "A zaaktype configuration is bound to at most one engine" refers to the one REST
  resource and its engine blocks, in place of the two resources.

## Impact

- **REST API**:
  - `ZaaktypeConfigurationRestService` serves both engines.
  - `ZaaktypeBpmnConfigurationRestService` and `RestZaaktypeBpmnConfiguration` are deleted.
  - `RestZaaktypeConfiguration` and `RestZaaktypeConfigurationConverter` get the engine blocks.
  - The generated OpenAPI spec changes. The generated frontend types change with it.
  - `BpmnProcessDefinitionRestService` does not change.
- **Backend tests**: the unit tests of both REST services, and the itests that call
  `/zaaktype-bpmn-configuration`: `ZaaktypeBpmnConfigurationRestServiceTest`,
  `NotificationZaaktypeCompletionParametersTest`, and `ZacClient`.
- **Frontend**:
  - `src/main/app/src/app/admin/`: `parameters-edit-cmmn`, `parameters-edit-bpmn`, `parameters-edit-shell`,
    `parameters-select-process-model-method`, `zaakafhandel-parameters-resolver.service.ts`,
    `zaakafhandel-parameters.service.ts`, and the admin routes.
  - New step components under `parameters-edit/`.
  - The 1780 lines of the two edit components today (1176 CMMN, 604 BPMN) must fall.
- **Database**: none. PZ-12637 already stores every setting engine-agnostically.
- **Out of scope**:
  - Using the stored confirmation email parameters as a fallback for a BPMN process. This is RFC section 4.4,
    and a separate ticket.
  - A shared task form configuration (RFC section 7).
  - Clean-up of the quarantine tables (PZ-12947).
