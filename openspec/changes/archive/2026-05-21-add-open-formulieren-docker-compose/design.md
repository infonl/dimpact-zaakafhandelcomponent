## Context

The ZAC docker-compose stack already includes Open Zaak, Open Klant, Objecten API, Open Notificaties, Keycloak, Solr, and various wiremocks. The productaanvraag flow requires a form submission in Open Formulieren to produce an object in Objecten API (via the Objects registration backend), trigger Open Zaak to create a zaak, and deliver a notification to ZAC via Open Notificaties. Currently developers cannot test this full flow locally because Open Formulieren is absent from the stack.

Open Formulieren is a Django/Celery application with the same architectural shape as the other Maykin/OpenZaak services already in the compose file. It needs its own Postgres database, a Redis connection (shared with the existing `redis` service), a Celery worker, a Celery beat scheduler, and an nginx reverse proxy to serve the static files and act as ingress.

## Goals / Non-Goals

**Goals:**
- Add all Open Formulieren services to `docker-compose.yaml` under a new `openformulieren` profile
- Wire Open Formulieren to the existing Objecten API, Open Zaak (via nginx), Open Klant, and Open Notificaties services
- Provide an `openformulieren-init` container that runs Django setup_configuration so integrations are pre-configured on first start
- Keep the `itest` profile unchanged — Open Formulieren services MUST NOT be included in it
- Re-use the existing shared `redis` service (already in the default profile)

**Non-Goals:**
- Changing any ZAC application code
- Keycloak SSO integration for Open Formulieren end-users (can be configured manually)
- Full production-grade TLS / secrets handling
- Open Formulieren SDK frontend hosting (developers can use the Django admin UI directly)

## Decisions

### 1. Docker Compose profile: `openformulieren`

All new services get `profiles: ["openformulieren"]`. This keeps them out of the default startup and explicitly separate from the `itest` profile. Developers start the integration with:

```
docker compose --profile openformulieren --profile opennotificaties up
```

The `opennotificaties` profile is a prerequisite because notifications are the channel through which Open Formulieren triggers ZAC.

**Alternative considered**: Fold everything into `itest`. Rejected — would make CI slower and add unneeded complexity to integration tests that mock these integrations with WireMock already.

### 2. Shared Redis vs dedicated Redis

Open Formulieren can share the existing `redis` service using a different database index (e.g. `/2`) for Celery and `/3` for cache, avoiding db-index collision with Open Zaak and Objecten API which already use `/0` and `/1`. This avoids spinning up a second Redis.

### 3. ClamAV omitted

ClamAV is not required for local development testing. Open Formulieren's virus scanning is optional and can be left unconfigured. We omit ClamAV entirely to avoid the ~6-minute startup delay from virus definition downloads and the extra container overhead.

### 4. Open Formulieren image tag

Use `openformulieren/open-forms:3.4.2` pinned to its SHA256 digest, following the same pattern as all other services in the compose file. The digest must be retrieved during implementation via `docker pull openformulieren/open-forms:3.4.2`.

### 5. Objecten API integration

Open Formulieren's Objects registration plugin writes productaanvraag objects to Objecten API. The three objecten services (`objecten-api-database`, `objecten-api.local`, `objecten-api-import`) currently live under `profiles: ["objecten", "itest"]`. We add `"openformulieren"` to their profiles list so they start automatically when `--profile openformulieren` is used. Docker Compose supports multiple profiles per service, so no extra tooling is required.

### 6. Setup configuration

Open Formulieren supports Django setup_configuration via a `setup_configuration.sh` entrypoint. We provide an `openformulieren-init` container and mount configuration YAML/JSON files from `scripts/docker-compose/imports/openformulieren/setup-configuration/`. This mirrors the approach used by `opennotificaties-init`.

## Risks / Trade-offs

- **Port conflicts**: Open Formulieren nginx on 8007, Open Formulieren web on 8009. Check against existing allocations (8001=openzaak-nginx, 8002=openklant, 8003=opennotificaties... wait, 8003 is office-converter, 8004=openarchiefbeheer-web, 8005=openarchiefbeheer-ui, 8006=pabc-api, 8007 is free). Mitigation: allocate 8007 for OF nginx and 8009 for OF web admin.
- **objecten-api.local dependency**: Resolved by adding `"openformulieren"` to the objecten services' profiles — they start automatically with `--profile openformulieren`.
- **Notifications circular setup**: Open Notificaties needs Open Zaak to register its notification channel on startup; Open Formulieren needs Open Notificaties to push notifications. Start order: Open Zaak → Open Notificaties → Open Formulieren.

## Migration Plan

No data migration. This is additive: new services and volumes. Existing docker-compose volume-data directories are unaffected. New volumes are created under `scripts/docker-compose/volume-data/`.

Rollback: simply stop and remove the `openformulieren`-profile containers. No changes to existing services' data.

## Open Questions

- Open Formulieren version: `3.4.2` (decided).
- Should `opennotificaties` profile also be added to openformulieren profile in the same way? Proposed: yes, for the same reason — add `"openformulieren"` to the opennotificaties services' profiles list so the full flow works with a single `--profile openformulieren`.
