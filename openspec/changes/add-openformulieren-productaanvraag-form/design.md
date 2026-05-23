## 1. Enable ENABLE_DEMO_PLUGINS Feature Flag

The `digid-mock` authentication backend (`openforms.authentication.contrib.digid_mock`) is guarded by the `ENABLE_DEMO_PLUGINS` Django feature flag (from `django-feature-flags`). The default in `openforms.conf.docker` is an empty conditions list (= disabled). Override in `docker_local.py`:

```python
from openforms.conf.docker import *  # noqa

FLAGS = {
    **FLAGS,
    "ENABLE_DEMO_PLUGINS": [{"condition": "boolean", "value": True}],
}
```

`FLAGS` is in scope via the wildcard import. Rebinding it here creates a new dict that Django settings loading picks up for this process only.

## 2. Objects API Group in data.yaml

Open Formulieren's Objects API registration backend (version 1, "verouderd/sjabloon") requires an `objects_api` group config with at minimum an `objects_service_identifier`. Add to `data.yaml`:

```yaml
objects_api_config_enable: True
objects_api:
  groups:
  - name: Local Objects API
    identifier: local-objects-api
    objects_service_identifier: objecten-api
    objecttypes_service_identifier: objecten-api
    organisatie_rsin: "002564440"
```

`objecttypes_service_identifier` is set to the same `objecten-api` service. The `maykinmedia/objects-api` image does not serve a real Objecttypes API, but this value is only used by Open Formulieren's admin UI for objecttype selection dropdowns — it is not required for programmatic form import or submission. `catalogue_domain` / `catalogue_rsin` and document-type fields are omitted because the version-1 registration backend does not upload submission PDFs/CSVs in this basic form setup.

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

Extend `openformulieren-init.command` in `docker-compose.yaml`. The `manage.py import` command accepts a ZIP file. Wrap in a check so it is idempotent (skips import if form UUID already exists in the database):

```sh
set -e
/setup_configuration.sh

# Create admin user (existing block)
OTEL_SDK_DISABLED=True src/manage.py shell -c "
from django.contrib.auth import get_user_model
User = get_user_model()
if not User.objects.filter(username='admin').exists():
    User.objects.create_superuser('admin', 'admin@example.com', 'admin')
    print('Admin user created')
else:
    print('Admin user already exists')
"

# Import productaanvraag-Dimpact test form (idempotent)
OTEL_SDK_DISABLED=True src/manage.py shell -c "
from openforms.forms.models import Form
if not Form.objects.filter(uuid='b056224a-bdb5-43c2-804a-5fd3f17214b2').exists():
    import subprocess, sys
    import os
    os.chdir('/tmp')
    subprocess.run(['zip', '-j', 'forms.zip'] + __import__('glob').glob('/forms/*.json'), check=True)
    result = subprocess.run(
        [sys.executable, '/app/src/manage.py', 'import', '--import-file', '/tmp/forms.zip'],
        capture_output=True, text=True
    )
    print(result.stdout)
    if result.returncode != 0:
        print(result.stderr)
        sys.exit(result.returncode)
    print('Form imported successfully')
else:
    print('Form already exists, skipping import')
"
```

Alternatively, verify if `manage.py import` supports a `--skip-existing` or `--overwrite` flag and use that directly. Check `manage.py import --help` in 3.4.2.

Add volume mount to `openformulieren-init`:
```yaml
volumes:
  - ./scripts/docker-compose/imports/openformulieren/forms:/forms:ro
```

This mount goes alongside the existing `openformulieren-volumes` anchor mounts. Since `openformulieren-init` uses `*openformulieren-volumes`, add it explicitly in the init service (cannot append to an anchor).

## 5. BRP API Configuration for Profile Plugin

The `customerProfile` component (Profile plugin) fetches BRP person data using the Haal Centraal BRP Personen API. Open Forms must be pointed at the local `brp-personen-wiremock` service. This is not configurable via `data.yaml` setup_configuration in 3.4.2 — configure programmatically in the init container using the Django shell:

```python
from openforms.contrib.haalcentraal.models import HaalCentraalConfig
from openforms.contrib.haalcentraal.api_models.brp import BRPVersion

config = HaalCentraalConfig.get_solo()
config.brp_personen_service_identifier = 'brp-personen'  # or set via ZGW service
config.save()
```

First, register the BRP service in `data.yaml` as a ZGW consumer:
```yaml
- identifier: brp-personen
  label: BRP Personen Wiremock
  api_root: http://brp-personen-wiremock:8080/haalcentraal/api/brp/
  api_type: orc
  auth_type: no_auth
```

Then use the service identifier to configure the BRP plugin. Check the exact model/field names in `openforms/contrib/haalcentraal/` for 3.4.2.

## 6. Klantinteracties API Group (communication_preferences prefill)

The `profilePrefill` form variable uses `prefill_plugin: communication_preferences` with `customer_interactions_api_group: "klantinteracties-api-group"`. This references an Open Klant klantinteracties group configured in Open Forms. The `openklant-klantinteracties` service is already registered in `data.yaml`.

Check whether Open Forms 3.4.2 has a `setup_configuration` step for klantinteracties groups (look for `KlantInteractiesConfigStep` or similar in `openforms/contrib/klantinteracties/`). If it does, add a group config to `data.yaml`. If not, configure via Django shell in the init container or document as a manual step.

If the communication_preferences prefill fails (404 on klantinteracties group), the form still submits correctly — only the pre-filled communication preferences field will be empty.

## Key Constraints

- The `initiator_bsn` variable in `forms.json` has `"defaultValue": "{{ auth_bsn }}"` — this is populated by the DigiD mock authentication. The `content_json` template uses `variables.initiator_bsn` to populate the `betrokkenen` block in the submitted object.
- The form UUID is `b056224a-bdb5-43c2-804a-5fd3f17214b2` — keep this stable so the idempotency check works.
- The `objecttype_version` in the registration backend is `9` — this must match the version in the local Objects API. Check if the local Productaanvraag-Dimpact objecttype has version 9; if not, update to `1`.
