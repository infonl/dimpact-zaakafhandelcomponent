## ADDED Requirements

### Requirement: Open Formulieren runs under dedicated Docker Compose profile
The docker-compose.yaml SHALL include all Open Formulieren services (database, web, celery worker, celery beat, nginx, init) exclusively under the `openformulieren` Docker Compose profile. No Open Formulieren service SHALL appear in the `itest` profile.

#### Scenario: Start with openformulieren profile
- **WHEN** a developer runs `docker compose --profile openformulieren up`
- **THEN** all Open Formulieren services start, including the database, web, celery worker, celery beat, nginx and init container

#### Scenario: itest profile does not start Open Formulieren
- **WHEN** a developer runs `docker compose --profile itest up`
- **THEN** no Open Formulieren service is started

### Requirement: Open Formulieren has its own Postgres database
The docker-compose.yaml SHALL include an `openformulieren-database` service using the `postgres:17` image with a healthcheck. Open Formulieren web and init containers SHALL depend on it being healthy.

#### Scenario: Database ready before web starts
- **WHEN** `docker compose --profile openformulieren up` is run from a clean state
- **THEN** the web container starts only after the database healthcheck passes

### Requirement: Open Formulieren web service is accessible
The docker-compose.yaml SHALL include an `openformulieren-web` service running the `openformulieren/open-forms` image. It SHALL be exposed via an `openformulieren-nginx` reverse proxy on a host port (8007) for browser access to the admin UI.

#### Scenario: Admin UI reachable
- **WHEN** Open Formulieren stack is running
- **THEN** `http://localhost:8007/admin/` returns an HTTP 200 or 302 response

### Requirement: Open Formulieren Celery worker runs for async tasks
The docker-compose.yaml SHALL include an `openformulieren-celery` service using the same image as the web service, running the `/celery_worker.sh` entrypoint. It SHALL share the same environment as the web service.

#### Scenario: Celery worker starts after web
- **WHEN** `docker compose --profile openformulieren up` is run
- **THEN** the celery worker container starts and its healthcheck passes

### Requirement: Open Formulieren Celery beat runs for scheduled tasks
The docker-compose.yaml SHALL include an `openformulieren-celery-beat` service using the same image as the web service, running the `/celery_beat.sh` entrypoint.

#### Scenario: Celery beat starts with the stack
- **WHEN** `docker compose --profile openformulieren up` is run
- **THEN** the celery beat container starts

### Requirement: Open Formulieren uses shared Redis service
Open Formulieren services SHALL use the existing `redis` service (already in the default profile) for Celery broker and cache. Open Formulieren SHALL use different Redis database indices than the ones already used by Open Zaak and Objecten API to avoid conflicts (indices 2 and 3 are available).

#### Scenario: No second Redis container
- **WHEN** `docker compose --profile openformulieren up` is run
- **THEN** no new Redis container is created; the existing `redis` container serves Open Formulieren

### Requirement: Open Formulieren integrates with Open Object
Open Formulieren's environment SHALL be configured with the Open Object URL (`http://open-object.local:8000`) and an API token so the Objects registration plugin can write productaanvraag objects. The `open-object.local` service (from the `objecten` profile) SHALL be referenced as a dependency in the init container.

#### Scenario: Objects registration plugin can reach Open Object
- **WHEN** both `openformulieren` and `objecten` profiles are running
- **THEN** Open Formulieren can write objects to Open Object using the configured token

### Requirement: Open Formulieren integrates with Open Zaak
Open Formulieren's environment SHALL be configured with the Open Zaak API URL (via `http://openzaak-nginx:8000`) and ZGW API credentials so the ZGW registration plugin can create zaken.

#### Scenario: ZGW registration plugin can reach Open Zaak
- **WHEN** `openformulieren` profile is running alongside the default services
- **THEN** Open Formulieren can authenticate to Open Zaak and register a zaak

### Requirement: Open Formulieren integrates with Open Klant
Open Formulieren's environment SHALL be configured with the Open Klant API URL (`http://openklant.local:8000`) and token so klantinteracties can be registered.

#### Scenario: Open Klant integration configured
- **WHEN** `openformulieren` profile is running
- **THEN** Open Formulieren can reach Open Klant via the configured URL

### Requirement: Open Formulieren integrates with Open Notificaties
Open Formulieren's environment SHALL be configured with the Open Notificaties API URL and API secret key so submission notifications are sent to Open Notificaties and forwarded to ZAC.

#### Scenario: Notification sent on form submission
- **WHEN** a form is submitted in Open Formulieren and both `openformulieren` and `opennotificaties` profiles are running
- **THEN** Open Formulieren sends a notification to Open Notificaties, which forwards it to ZAC

### Requirement: Init container pre-configures service integrations
The docker-compose.yaml SHALL include an `openformulieren-init` service that runs `/setup_configuration.sh` to register API credentials, notification channels, and service configurations in Open Formulieren on first start. The init container SHALL exit after completion (one-shot) and the web service SHALL depend on it completing successfully.

#### Scenario: Init runs once and exits
- **WHEN** `docker compose --profile openformulieren up` is run
- **THEN** the openformulieren-init container runs, configures integrations, and exits with code 0

#### Scenario: Web starts after init completes
- **WHEN** `docker compose --profile openformulieren up` is run
- **THEN** openformulieren-web starts only after openformulieren-init exits successfully

### Requirement: Setup configuration files are mounted from the project
YAML/JSON setup configuration files for Open Formulieren SHALL be stored under `scripts/docker-compose/imports/openformulieren/setup-configuration/` and mounted into the init and web containers, following the same pattern as the opennotificaties setup configuration.

#### Scenario: Configuration files are version-controlled
- **WHEN** a developer clones the repository and starts the openformulieren profile
- **THEN** all integration configuration is applied automatically without manual steps

### Requirement: Open Formulieren data is persisted in a named volume
Open Formulieren's media and private media files SHALL be stored in named Docker volumes under `scripts/docker-compose/volume-data/openformulieren-*` or Docker-managed named volumes, so data survives container restarts.

#### Scenario: Data survives restart
- **WHEN** the openformulieren-web container is stopped and restarted
- **THEN** previously uploaded files and form definitions are still accessible
