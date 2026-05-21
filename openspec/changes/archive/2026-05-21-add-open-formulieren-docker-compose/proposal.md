## Why

Developers need to test the complete productaanvraag flow locally — from form submission in Open Formulieren through Objecten, Open Zaak, Open Klant and Notifications into ZAC — but no local setup for Open Formulieren exists in the project. Adding it to docker-compose enables end-to-end local testing without any external dependencies.

## What Changes

- Add Open Formulieren services to `docker-compose.yaml` under a dedicated `openformulieren` profile (excluded from `itest` profile)
- Add Open Formulieren database (postgres)
- Add Open Formulieren web service (Django app)
- Add Open Formulieren Celery worker for async task processing
- Add Open Formulieren Celery beat for scheduled tasks
- Add nginx reverse proxy in front of Open Formulieren web
- Add ClamAV virus scanner (required by Open Formulieren for file uploads)
- Configure Open Formulieren to integrate with existing services: Objecten API, Open Zaak, Open Klant, Open Notificaties
- Add setup/init container to configure Open Formulieren integrations on first start
- Update existing services (objecten-api, openzaak, openklant, opennotificaties) with any additional env vars needed to accept connections from Open Formulieren

## Capabilities

### New Capabilities

- `open-formulieren-docker-compose`: All Docker Compose services, volumes, configuration and integration setup for running Open Formulieren locally alongside the existing ZAC stack

### Modified Capabilities

<!-- none -->

## Impact

- `docker-compose.yaml`: new services, volumes, env vars on existing services
- No code changes to ZAC application itself
- No impact on `itest` profile or CI integration tests
- Developers need to pull additional Docker images (~1 GB)
- Open Notificaties profile (`opennotificaties`) is a prerequisite when using Open Formulieren, since form submissions trigger notifications into ZAC
