## Why

ZAC stores the configuration of a zaaktype (the "zaakafhandelparameters") in one inherited data model, but
writes, reads, and validates it through two independently written stacks: one for CMMN and one for BPMN. The
two stacks diverged. PZ-12241 is one result: a new version of a BPMN zaaktype kept
resultaattype references that pointed at the previous version. The same split causes more defects today. A
BPMN zaak is never cleaned up when it is deleted. A BPMN zaak ignores its BRP doelbindingen. A BPMN zaak never
gets a deadline warning. A BPMN zaaktype cannot be re-versioned with its own productaanvraagtype.

PZ-12637 implements the backend part of the RFC "unify the BPMN and CMMN zaaktype configuration" that is
attached to the ticket. The REST contract and the frontend stay unchanged. PZ-12754 covers those.

## What Changes

- Repair the schema defects of the configuration tables: a missing primary key, missing foreign keys from the
  subclass tables to the base, a nullable discriminator, and missing unique constraints on one-to-one children.
- Move the settings that describe the zaak and not the engine to the engine-agnostic configuration:
  - the two deadline warning windows
  - the confirmation email parameters
  - the zaakafzenders
  - the mailtemplate koppelingen
- Replace the CMMN/BPMN inheritance with three tables:
  - one engine-agnostic zaaktype configuration
  - a process binding that names the engine and the definition key
  - a CMMN extension for humantask parameters, user event listeners, and the intake/afronden mail options
- Replace the six engine-specific configuration services with one read service and one beheer service. Both
  REST resources use these services. One function answers "does this zaaktype have a configuration?".
- Put zaak start, zaak termination, and zaak cleanup behind one process binding interface with one CDI adapter
  per engine. The callers no longer branch on the engine.
- Store a resultaattype reference by its omschrijving next to its UUID (the expand step), and backfill the
  omschrijving for existing rows. Dropping the UUID columns is a later release.
- Make the versioning of a configuration a pure function that returns the new configuration. Rename
  `ZaaktypeHelperService` to `ZaaktypeConfigurationVersioning`.
- Behaviour changes that the unification brings about:
  - one productaanvraagtype check for both engines, which excludes by zaaktype omschrijving
  - BPMN process instances are deleted when their zaak is deleted
  - BPMN zaken use their BRP doelbindingen and deadline warnings
  - BPMN configurations get the same bean validation as CMMN configurations
- **No REST contract change.** Both `/zaakafhandelparameters` and `/zaaktype-bpmn-configuration` keep their
  paths, verbs, keys, and payloads.

Delivery: one stacked GitHub pull request per RFC chunk (A1, A2, A3, B1, B2, B3). Each pull request is based
on the branch of the previous chunk.

## Capabilities

### New Capabilities

- `zaaktype-configuration`: storage, validation, versioning, and the engine-agnostic behaviour of the
  configuration of a zaaktype, for both CMMN and BPMN, behind an unchanged REST contract.
- `zaaktype-process-binding`: how ZAC starts, terminates, and cleans up the process of a zaak for the engine
  that the zaaktype is bound to, and how productaanvraag intake selects the engine.

### Modified Capabilities

None. `productaanvraag-notification-idempotency` keeps its requirements; only the engine selection below it
changes, and that is specified in `zaaktype-process-binding`.

## Impact

- **Database**: Flyway migrations V100 to V103 in `src/main/resources/schemas/`. V102 drops
  `zaaktype_cmmn_configuration`, `zaaktype_bpmn_configuration`, and the `configuration_type` enum. V103 adds
  the resultaattype omschrijving columns.
- **Backend code**:
  - `nl.info.zac.admin` and its `model` package, plus the converters and both REST resources in
    `nl.info.zac.app.admin`
  - `ZaakRestService`, `PlanItemsRestService`, `RestPlanItemConverter`, `RestTaskConverter`,
    `RestZaaktypeConverter`, `MailtemplateRESTService`, `MailtemplateKoppelingRestService`
  - `ProductaanvraagService`, `ProductaanvraagEmailService`, `NotificationReceiver`, `BrpClientService`,
    `HealthCheckService`, `ZaakTaskDueDateEmailNotificationService`, `SmartDocumentsTemplatesService`,
    `CMMNService`, `BpmnService`
  - About 30 call sites are typed to `ZaaktypeCmmnConfiguration` today.
- **REST API**: none. The generated OpenAPI spec must not change. Each PR compares the output of
  `./gradlew generateOpenApiSpec` with a baseline taken on `main`.
- **Frontend**: none.
- **Tests**: unit tests for every touched service, parameterised by engine. Integration tests for the version
  chain, the productaanvraagtype check, and zaak deletion for both engines.
- **Out of scope**:
  - the REST and UI unification (PZ-12754)
  - the task form configuration question (RFC section 7)
  - zaaktype identity by omschrijving (RFC section 10)
  - dropping the resultaattype UUID columns
  - the automatic confirmation of receipt (automatische ontvangstbevestiging) for BPMN zaken. It stays CMMN only;
    a BPMN process sends a confirmation email itself when it needs one.
  - using zaakafzenders and mailtemplate koppelingen for BPMN zaken. The engine-agnostic configuration stores them
    for both engines, so a later change can add this.
