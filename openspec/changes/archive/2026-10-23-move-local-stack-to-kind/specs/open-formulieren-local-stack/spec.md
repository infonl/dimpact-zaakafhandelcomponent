## ADDED Requirements

### Requirement: Open Formulieren starts with an option of the kind stack
The local kind stack SHALL start Open Formulieren, with its Celery worker and beat, only when it is started with the
`-f` option. That option SHALL also start Open Object and Open Notificaties, which the productaanvraag flow needs.

#### Scenario: Start with the Open Formulieren option
- **WHEN** a developer runs `./start-kind.sh -f`
- **THEN** Open Formulieren, Open Object and Open Notificaties start
- **AND** the Open Formulieren admin UI answers on `http://localhost:8007/admin/`

#### Scenario: Start without the Open Formulieren option
- **WHEN** a developer runs `./start-kind.sh` without `-f`
- **THEN** no Open Formulieren service runs

### Requirement: Open Formulieren is preconfigured for the productaanvraag flow
When Open Formulieren starts in the kind stack, the stack SHALL register the Open Zaak, Open Object, Open Klant and
BRP services and an Objects API group in Open Formulieren, and SHALL import the productaanvraag-Dimpact test form,
so that no manual configuration is needed before a developer submits a productaanvraag.

#### Scenario: Submit a productaanvraag
- **WHEN** the kind stack runs with `-f` and a developer submits the productaanvraag-Dimpact test form
- **THEN** Open Formulieren registers the productaanvraag as an object in Open Object
