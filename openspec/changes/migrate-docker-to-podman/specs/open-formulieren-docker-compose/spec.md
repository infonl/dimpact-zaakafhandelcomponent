## MODIFIED Requirements

### Requirement: Open Formulieren runs under dedicated Docker Compose profile
The docker-compose.yaml SHALL include all Open Formulieren services (database, web, celery worker, celery beat, nginx, init) exclusively under the `openformulieren` Docker Compose profile. No Open Formulieren service SHALL appear in the `itest` profile.

#### Scenario: Start with openformulieren profile
- **WHEN** a developer runs `podman compose --profile openformulieren up`
- **THEN** all Open Formulieren services start, including the database, web, celery worker, celery beat, nginx and init container

#### Scenario: itest profile does not start Open Formulieren
- **WHEN** a developer runs `podman compose --profile itest up`
- **THEN** no Open Formulieren service is started

### Requirement: Open Formulieren has its own Postgres database
The docker-compose.yaml SHALL include an `openformulieren-database` service using the `postgres:17` image with a healthcheck. Open Formulieren web and init containers SHALL depend on it being healthy.

#### Scenario: Database ready before web starts
- **WHEN** `podman compose --profile openformulieren up` is run from a clean state
- **THEN** the web container starts only after the database healthcheck passes

### Requirement: Open Formulieren Celery worker runs for async tasks
The docker-compose.yaml SHALL include an `openformulieren-celery` service using the same image as the web service, running the `/celery_worker.sh` entrypoint. It SHALL share the same environment as the web service.

#### Scenario: Celery worker starts after web
- **WHEN** `podman compose --profile openformulieren up` is run
- **THEN** the celery worker container starts and its healthcheck passes

### Requirement: Open Formulieren Celery beat runs for scheduled tasks
The docker-compose.yaml SHALL include an `openformulieren-celery-beat` service using the same image as the web service, running the `/celery_beat.sh` entrypoint.

#### Scenario: Celery beat starts with the stack
- **WHEN** `podman compose --profile openformulieren up` is run
- **THEN** the celery beat container starts

### Requirement: Open Formulieren uses shared Redis service
Open Formulieren services SHALL use the existing `redis` service (already in the default profile) for Celery broker and cache. Open Formulieren SHALL use different Redis database indices than the ones already used by Open Zaak and Objecten API to avoid conflicts (indices 2 and 3 are available).

#### Scenario: No second Redis container
- **WHEN** `podman compose --profile openformulieren up` is run
- **THEN** no new Redis container is created; the existing `redis` container serves Open Formulieren

### Requirement: Init container pre-configures service integrations
The docker-compose.yaml SHALL include an `openformulieren-init` service that runs `/setup_configuration.sh` to register API credentials, notification channels, and service configurations in Open Formulieren on first start. The init container SHALL exit after completion (one-shot) and the web service SHALL depend on it completing successfully.

#### Scenario: Init runs once and exits
- **WHEN** `podman compose --profile openformulieren up` is run
- **THEN** the openformulieren-init container runs, configures integrations, and exits with code 0

#### Scenario: Web starts after init completes
- **WHEN** `podman compose --profile openformulieren up` is run
- **THEN** openformulieren-web starts only after openformulieren-init exits successfully
