## 1. Enable ENABLE_DEMO_PLUGINS Feature Flag

The `digid-mock` authentication backend (`openforms.authentication.contrib.digid_mock`) is guarded by the `ENABLE_DEMO_PLUGINS` Django feature flag (from `django-feature-flags`). The default in `openforms.conf.docker` is an empty conditions list (= disabled). Override in `docker_local.py`:

```python
from openforms.conf.docker import *  # noqa

# Enable demo/simulatie plugins (digid-mock) for local development.
FLAGS = {
    **FLAGS,
    "ENABLE_DEMO_PLUGINS": [{"condition": "boolean", "value": True}],
}

# The ARM64 image (ghcr.io/infonl/open-formulieren) does not include the UMD (.js)
# SDK bundle in its staticfiles.json manifest — only .mjs (ESM) bundles are present.
# ManifestStaticFilesStorage raises ValueError for any missing manifest entry, crashing
# every page render. Non-manifest storage bypasses this check for local development.
STATICFILES_STORAGE = "django.contrib.staticfiles.storage.StaticFilesStorage"
```

`FLAGS` is in scope via the wildcard import. Rebinding it here creates a new dict that Django settings loading picks up for this process only.

`STATICFILES_STORAGE` is needed because the ARM64 image build omits the UMD `.js` SDK bundle from `staticfiles.json` — only ESM `.mjs` bundles are present. `ManifestStaticFilesStorage` (the default) raises `ValueError` for any missing manifest entry, which crashes page rendering. `StaticFilesStorage` bypasses manifest lookup entirely, safe for local dev.

## 2. Objects API Group in data.yaml

Open Formulieren's Objects API registration backend (version 1, "verouderd/sjabloon") requires an `objects_api` group config with at minimum an `objects_service_identifier`. Add to `data.yaml`:

First, add a dedicated Objecttypes API service to `zgw_consumers.services` (the local `objecttypes-api` container, not the objects-api):

```yaml
- identifier: objecttypes-api
  label: Objecttypes API
  api_root: http://objecttypes-api:8000/api/v2/
  api_type: orc
  auth_type: api_key
  header_key: Authorization
  header_value: Token fakeOpenFormulierenObjecttypesToken
```

Then add the Objects API group config:

```yaml
objects_api_config_enable: True
objects_api:
  groups:
    - name: Local Objects API
      identifier: local-objects-api
      objects_service_identifier: objecten-api
      objecttypes_service_identifier: objecttypes-api
      documenten_service_identifier: openzaak-documenten
      catalogi_service_identifier: openzaak-catalogi
      organisatie_rsin: "002564440"
```

`objecttypes_service_identifier` points to the separate `objecttypes-api` service. `documenten_service_identifier` and `catalogi_service_identifier` are set so Open Formulieren can validate IOT values in the admin; the form registration backend does not upload PDFs/CSVs (all IOT fields in `forms.json` are set to `""`).

## 3. Adapt Form JSON Files

Source: `tmp/productaanvraag-dimpact-test-formulier-compleet/` (6 JSON files exported from the test environment, Open Forms 3.4.2).

Target: `scripts/docker-compose/imports/openformulieren/forms/`

**`forms.json` — changes required:**

| Field | Old value | New value |
|-------|-----------|-----------|
| `registration_backends[0].options.objecttype` | `f437384b-3f0a-4559-9f6d-e42085e64227` | `021f685e-9482-4620-b157-34cd4003da6b` (local Productaanvraag-Dimpact UUID in demodata.json) |
| `registration_backends[0].options.objects_api_group` | `objects-apis` | `local-objects-api` |
| `registration_backends[0].options.informatieobjecttype_attachment` | external URL | `""` |
| `registration_backends[0].options.informatieobjecttype_submission_csv` | external URL | `""` |
| `registration_backends[0].options.informatieobjecttype_submission_report` | external URL | `""` |
| `registration_backends[0].options.iot_attachment` | (any value) | `""` |
| `registration_backends[0].options.iot_submission_csv` | (any value) | `""` |
| `registration_backends[0].options.iot_submission_report` | (any value) | `""` |
| `url` | external URL | `""` |
| each `steps[*].url` | external URL | `""` |

**Fields to keep as-is:**
- `"new_renderer_enabled": true` — preserved during import, enables the new rendering engine
- `"auth_backends": [{"backend": "digid-mock", "options": {}}]` — requires ENABLE_DEMO_PLUGINS (step 1)
- `"auto_login_authentication_backend": "digid-mock"` — auto-redirects to DigiD mock on form start
- `"productaanvraag_type": "productaanvraag-test-compleet"` — value embedded in submitted object's `type` field
- The full `content_json` template (includes betrokkenen/initiator block using `variables.initiator_bsn`)

**`formDefinitions.json`, `formSteps.json`, `formLogic.json`** — copy as-is. No local-environment references.

**`formVariables.json`** — copy as-is. The `"form"` URL field is ignored by the import command (form matching is done by UUID).

**`_meta.json`** — copy as-is. Version `3.4.2` matches our local Open Forms.

## 4. Form Import in openformulieren-init

Extend `openformulieren-init.command` in `docker-compose.yaml`. The `manage.py import` command accepts a ZIP file. Idempotency check is by slug (not UUID), and form activation is explicitly ensured after import:

```sh
OTEL_SDK_DISABLED=True src/manage.py shell -c "
from openforms.forms.models import Form
FORM_SLUG = 'productaanvraag-dimpact-test-formulier-compleet'
if not Form.objects.filter(slug=FORM_SLUG).exists():
    import subprocess, sys, os, glob, zipfile
    json_files = glob.glob('/forms/*.json')
    with zipfile.ZipFile('/tmp/forms.zip', 'w', zipfile.ZIP_DEFLATED) as zf:
        for f in json_files:
            zf.write(f, os.path.basename(f))
    result = subprocess.run(
        [sys.executable, '/app/src/manage.py', 'import', '--import-file', '/tmp/forms.zip'],
        capture_output=True, text=True
    )
    print(result.stdout)
    if result.returncode != 0:
        print(result.stderr)
        sys.exit(result.returncode)
    print('Productaanvraag-Dimpact form imported successfully')
form = Form.objects.get(slug=FORM_SLUG)
if not form.active:
    form.active = True
    form.save(update_fields=['active'])
    print('Productaanvraag-Dimpact form activated')
else:
    print('Productaanvraag-Dimpact form already active, skipping import')
"
```

ZIP is built with Python `zipfile` (no `zip` binary dependency). Slug used for idempotency because it is stable in the JSON export and simpler to verify than UUID.

Add volume mount to `openformulieren-init`:
```yaml
volumes:
  - ./scripts/docker-compose/imports/openformulieren/forms:/forms:ro
```

Also add `objecttypes-api: condition: service_started` to `depends_on` so the init container waits for the Objecttypes API to be available before running setup_configuration.

This mount goes alongside the existing `openformulieren-volumes` anchor mounts. Since `openformulieren-init` uses `*openformulieren-volumes`, add it explicitly in the init service (cannot append to an anchor).

## 5. BRP API Configuration for Profile Plugin

The `customerProfile` component (Profile plugin) fetches BRP person data using the Haal Centraal BRP Personen API. Open Forms must be pointed at the local `brp-personen-wiremock` service. This is not configurable via `data.yaml` setup_configuration in 3.4.2 — configure programmatically in the init container using the Django shell.

Register the BRP service in `data.yaml` as a ZGW consumer:
```yaml
- identifier: brp-personen
  label: BRP Personen (Wiremock)
  api_root: http://brp-personen-wiremock:8080/haalcentraal/api/brp/
  api_type: orc
  auth_type: no_auth
```

Then configure in the init container:

```python
try:
    from openforms.contrib.haal_centraal.models import HaalCentraalConfig
    from zgw_consumers.models import Service
    config = HaalCentraalConfig.get_solo()
    if not config.brp_personen_service_id:
        svc = Service.objects.get(slug='brp-personen')
        config.brp_personen_service = svc
        config.save()
        print('BRP service configured')
    else:
        print('BRP service already configured')
except Exception as e:
    print(f'BRP config skipped: {e}')
```

Module path is `openforms.contrib.haal_centraal` (with underscore), not `haalcentraal`. Wrapped in `try/except` so a missing module in future Open Forms versions does not block init.

## 6. Klantinteracties API Group (communication_preferences prefill)

The `profilePrefill` form variable uses `prefill_plugin: communication_preferences` with `customer_interactions_api_group: "klantinteracties-api-group"`. This references an Open Klant klantinteracties group configured in Open Forms. The `openklant-klantinteracties` service is registered in `data.yaml`.

Open Forms 3.4.2 does **not** have a `setup_configuration` step for klantinteracties groups. Configure via Django shell in the init container:

```python
try:
    from openforms.contrib.customer_interactions.models import CustomerInteractionsAPIGroupConfig
    from zgw_consumers.models import Service
    IDENTIFIER = 'klantinteracties-api-group'
    if not CustomerInteractionsAPIGroupConfig.objects.filter(identifier=IDENTIFIER).exists():
        svc = Service.objects.get(slug='openklant-klantinteracties')
        CustomerInteractionsAPIGroupConfig.objects.create(
            name='Local Klantinteracties API',
            identifier=IDENTIFIER,
            customer_interactions_service=svc,
        )
        print('Klantinteracties API group configured')
    else:
        print('Klantinteracties API group already exists')
except Exception as e:
    print(f'Klantinteracties config skipped: {e}')
```

Wrapped in `try/except` — if communication_preferences prefill fails the form still submits correctly, only the pre-filled communication preferences field will be empty.

## Key Constraints

- The `initiator_bsn` variable in `forms.json` has `"defaultValue": "{{ auth_bsn }}"` — this is populated by the DigiD mock authentication. The `content_json` template uses `variables.initiator_bsn` to populate the `betrokkenen` block in the submitted object.
- The form UUID is `b056224a-bdb5-43c2-804a-5fd3f17214b2` — keep this stable so the idempotency check works.
- The `objecttype_version` in the registration backend is `9` — this must match the version in the local Objects API. Check if the local Productaanvraag-Dimpact objecttype has version 9; if not, update to `1`.
