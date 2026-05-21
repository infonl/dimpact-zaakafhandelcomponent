## 1. Update Existing Services

- [x] 1.1 Add `"openformulieren"` to the `profiles` list of `objecten-api-database`, `objecten-api.local`, and `objecten-api-import` in docker-compose.yaml (so they start automatically with `--profile openformulieren`)

## 2. Research & Preparation

- [x] 2.1 Pull `openformulieren/open-forms:3.4.2` and record its SHA256 digest to pin in docker-compose.yaml
- [x] 2.2 Check existing port allocations in docker-compose.yaml and confirm 8007 (nginx) and 8009 (web) are free; reserve them for Open Formulieren
- [x] 2.3 Check existing Redis db-index usage in docker-compose.yaml (Open Zaak uses /0 and /1) and confirm db indices 2 and 3 are available for Open Formulieren

## 3. Open Formulieren Database Service

- [x] 3.1 Add `openformulieren-database` service to docker-compose.yaml using `postgres:17.10` with same SHA digest as other postgres services in the file
- [x] 3.2 Configure environment vars: `POSTGRES_USER=openformulieren`, `POSTGRES_PASSWORD=openformulieren`, `POSTGRES_DB=openformulieren`
- [x] 3.3 Add healthcheck (`pg_isready`) with interval 5s, timeout 10s, retries 20
- [x] 3.4 Add port `54330:5432` (check no collision) and named volume `./scripts/docker-compose/volume-data/openformulieren-database-data:/var/lib/postgresql/data`
- [x] 3.5 Set `profiles: ["openformulieren"]`

## 4. Open Formulieren Web Service

- [x] 4.1 Define `&openformulieren-env` YAML anchor with all required env vars:
  - `DJANGO_SETTINGS_MODULE=openforms.conf.docker`
  - `SECRET_KEY=openFormulierenSecretKey`
  - `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST=openformulieren-database`
  - `CACHE_DEFAULT=redis:6379/2`, `CACHE_AXES=redis:6379/2`, `CACHE_PORTALOCKER=redis:6379/2`
  - `CELERY_BROKER_URL=redis://redis:6379/2`, `CELERY_RESULT_BACKEND=redis://redis:6379/2`
  - `ALLOWED_HOSTS=*`, `CORS_ALLOW_ALL_ORIGINS=true`
  - `CSRF_TRUSTED_ORIGINS=http://localhost:8007,http://localhost:8009`
  - `USE_X_FORWARDED_HOST=true`
  - `EMAIL_HOST=localhost`
  - `TZ=Europe/Amsterdam`
  - `DISABLE_2FA=true`
  - `OTEL_SDK_DISABLED=true`
  - Objecten API: `OBJECTS_API_URL=http://objecten-api.local:8000`, `OBJECTS_API_TOKEN=fakeZacObjectsToken`
  - Open Zaak (via nginx): `ZGW_API_CLIENT_MP_REST_URL=http://openzaak-nginx:8000/`
  - Open Klant: `KLANTINTERACTIES_API_CLIENT_MP_REST_URL=http://openklant.local:8000`
  - Open Notificaties: `NOTIFICATIONS_API_ROOT=http://opennotificaties:8000/api/v1/`, `OPENFORMS_NOTIFICATION_SECRET=openNotificatiesApiSecretKey`
- [x] 4.2 Add `openformulieren-web` service using the pinned image with `*openformulieren-env`
- [x] 4.3 Add volumes: named static volume, named media volume, named private_media volume, setup_configuration mount from `scripts/docker-compose/imports/openformulieren/setup-configuration`
- [x] 4.4 Add healthcheck (`maykin-common health-check` or HTTP check on `/admin/`) interval 10s, start_period 60s
- [x] 4.5 Set `depends_on`: `openformulieren-database` healthy, `redis` started, `openformulieren-init` completed successfully
- [x] 4.6 Set `profiles: ["openformulieren"]`

## 5. Open Formulieren Init Container

- [x] 5.1 Add `openformulieren-init` service using same image, command `/setup_configuration.sh`
- [x] 5.2 Mount setup_configuration directory (same as web)
- [x] 5.3 Set `depends_on`: `openformulieren-database` healthy, `redis` started
- [x] 5.4 Set `profiles: ["openformulieren"]`

## 6. Open Formulieren Celery Worker

- [x] 6.1 Add `openformulieren-celery` service with command `/celery_worker.sh` and `*openformulieren-env`
- [x] 6.2 Add healthcheck (maykin-common worker-health-check) with appropriate worker name
- [x] 6.3 Set `depends_on`: `openformulieren-database` healthy, `redis` started
- [x] 6.4 Set `profiles: ["openformulieren"]`

## 7. Open Formulieren Celery Beat

- [x] 7.1 Add `openformulieren-celery-beat` service with command `/celery_beat.sh` and `*openformulieren-env`
- [x] 7.2 Set `depends_on`: `openformulieren-database` healthy, `redis` started
- [x] 7.3 Set `profiles: ["openformulieren"]`

## 8. Open Formulieren Nginx Reverse Proxy

- [x] 8.1 Add `openformulieren-nginx` service using `nginxinc/nginx-unprivileged:1.31.0` (same image/digest as other nginx in the file)
- [x] 8.2 Create nginx config file at `scripts/docker-compose/imports/openformulieren/nginx/nginx.conf` that proxies to `openformulieren-web:8000` and serves static files from the named static volume
- [x] 8.3 Mount the nginx config and static volume into the nginx container
- [x] 8.4 Expose port `8007:8080`
- [x] 8.5 Set `depends_on`: `openformulieren-web` healthy
- [x] 8.6 Set `profiles: ["openformulieren"]`

## 9. Setup Configuration Files

- [x] 9.1 Create directory `scripts/docker-compose/imports/openformulieren/setup-configuration/`
- [x] 9.2 Create `setup_configuration.yaml` (or equivalent files expected by Open Formulieren's setup_configuration mechanism) to configure:
  - Objecten API service registration with token `fakeZacObjectsToken`
  - Open Zaak ZGW service registration with credentials `zac_client` / `openzaakZaakafhandelcomponentClientSecret`
  - Open Klant service registration
  - Open Notificaties subscription registration
- [x] 9.3 Verify the configuration format matches the Open Formulieren version being used (check open-forms repo `docker/setup_configuration/` for examples)

## 10. Named Volumes

- [x] 10.1 Add named volume declarations to the top-level `volumes:` section of docker-compose.yaml for: `openformulieren-static`, `openformulieren-media`, `openformulieren-private-media`, `openformulieren-log`

## 11. Documentation

- [x] 11.1 Update relevant docs (e.g. `docs/development/local-development.md` or similar) to describe how to start the Open Formulieren profile with a single `--profile openformulieren` (objecten and opennotificaties services start automatically)

## 12. Verification

- [x] 12.1 Start the full stack with just `--profile openformulieren` and verify all containers (including objecten and opennotificaties services) reach healthy state
- [ ] 12.2 Verify Open Formulieren admin UI is accessible at `http://localhost:8007/admin/`
- [x] 12.3 Verify `docker compose --profile itest up` does NOT start any openformulieren containers
- [ ] 12.4 Confirm that submitting a form triggers a notification visible in Open Notificaties and reaches ZAC (manual smoke test)
