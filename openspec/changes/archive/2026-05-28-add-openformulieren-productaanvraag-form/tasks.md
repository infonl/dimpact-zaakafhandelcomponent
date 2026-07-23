## 1. Enable ENABLE_DEMO_PLUGINS Feature Flag

- [x] 1.1 Update `scripts/docker-compose/imports/openformulieren/settings/docker_local.py` — append after the existing `from openforms.conf.docker import *` line:
  ```python
  FLAGS = {
      **FLAGS,
      "ENABLE_DEMO_PLUGINS": [{"condition": "boolean", "value": True}],
  }

  STATICFILES_STORAGE = "django.contrib.staticfiles.storage.StaticFilesStorage"
  ```
  `ENABLE_DEMO_PLUGINS` enables the `digid-mock` authentication backend. `STATICFILES_STORAGE` works around a missing UMD `.js` SDK bundle in the ARM64 image manifest that otherwise crashes every page render.

## 2. Add Objects API Group to Setup Configuration

- [x] 2.1 In `scripts/docker-compose/imports/openformulieren/setup-configuration/data.yaml`, add to `zgw_consumers.services` and then after the block:
  ```yaml
  # in zgw_consumers.services:
  - identifier: objecttypes-api
    label: Objecttypes API
    api_root: http://objecttypes-api:8000/api/v2/
    api_type: orc
    auth_type: api_key
    header_key: Authorization
    header_value: Token fakeOpenFormulierenObjecttypesToken

  # after the zgw_consumers block:
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
  `objecttypes_service_identifier` points to the dedicated `objecttypes-api` service (not `objecten-api`). `documenten_service_identifier` and `catalogi_service_identifier` are required by Open Formulieren's admin UI for IOT validation; the registration backend does not upload PDFs/CSVs (all IOT fields in `forms.json` are `""`).

## 3. Register BRP Service in data.yaml

- [x] 3.1 Add a BRP Personen service entry to the existing `zgw_consumers.services` list in `data.yaml`:
  ```yaml
  - identifier: brp-personen
    label: BRP Personen (Wiremock)
    api_root: http://brp-personen-wiremock:8080/haalcentraal/api/brp/
    api_type: orc
    auth_type: no_auth
  ```

## 4. Create Adapted Form JSON Files

- [x] 4.1 Create directory `scripts/docker-compose/imports/openformulieren/forms/`

- [x] 4.2 Copy `tmp/productaanvraag-dimpact-test-formulier-compleet/_meta.json` to `scripts/docker-compose/imports/openformulieren/forms/_meta.json` — no changes needed.

- [x] 4.3 Copy `tmp/productaanvraag-dimpact-test-formulier-compleet/formDefinitions.json` to `scripts/docker-compose/imports/openformulieren/forms/formDefinitions.json` — no changes needed.

- [x] 4.4 Copy `tmp/productaanvraag-dimpact-test-formulier-compleet/formSteps.json` to `scripts/docker-compose/imports/openformulieren/forms/formSteps.json` — no changes needed.

- [x] 4.5 Copy `tmp/productaanvraag-dimpact-test-formulier-compleet/formLogic.json` to `scripts/docker-compose/imports/openformulieren/forms/formLogic.json` — no changes needed (file is `[]`).

- [x] 4.6 Copy `tmp/productaanvraag-dimpact-test-formulier-compleet/formVariables.json` to `scripts/docker-compose/imports/openformulieren/forms/formVariables.json` — no changes needed (form URL reference is ignored by the import command).

- [x] 4.7 Create `scripts/docker-compose/imports/openformulieren/forms/forms.json` based on `tmp/productaanvraag-dimpact-test-formulier-compleet/forms.json` with these changes:
  - Replace `"objecttype": "f437384b-3f0a-4559-9f6d-e42085e64227"` → `"objecttype": "021f685e-9482-4620-b157-34cd4003da6b"` (local Productaanvraag-Dimpact UUID from `scripts/docker-compose/imports/objects-api/fixtures/demodata.json`)
  - Replace `"objects_api_group": "objects-apis"` → `"objects_api_group": "local-objects-api"`
  - Set `"objecttype_version": 1` (the local objecttype has version 1; the test environment had version 9)
  - Set `"informatieobjecttype_attachment": ""`
  - Set `"informatieobjecttype_submission_csv": ""`
  - Set `"informatieobjecttype_submission_report": ""`
  - Set `"iot_attachment": ""`
  - Set `"iot_submission_csv": ""`
  - Set `"iot_submission_report": ""`
  - Set top-level `"url": ""`
  - Set each `steps[*].url` to `""`
  - Keep all other fields unchanged, including:
    - `"new_renderer_enabled": true`
    - `"auth_backends": [{"backend": "digid-mock", "options": {}}]`
    - `"auto_login_authentication_backend": "digid-mock"`
    - The full `content_json` template (includes betrokkenen/initiator block)
    - `"productaanvraag_type": "productaanvraag-test-compleet"`
    - `"uuid": "b056224a-bdb5-43c2-804a-5fd3f17214b2"`

## 5. Update docker-compose.yaml — openformulieren-init

- [x] 5.1 Add volume mount for forms to the `openformulieren-init` service. Since the service uses `*openformulieren-volumes`, add the forms mount explicitly in the `volumes` list after the anchor expansion:
  ```yaml
  - ./scripts/docker-compose/imports/openformulieren/forms:/forms:ro
  ```

- [x] 5.2 Extend the `openformulieren-init` command to configure BRP and import the form after the existing admin user creation block. Append to the inline shell script:
  ```sh
  # Configure BRP Personen API for the Profile (customerProfile) plugin
  OTEL_SDK_DISABLED=True src/manage.py shell -c "
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
  "

  # Import productaanvraag-Dimpact test form (idempotent by slug check)
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
  Note: module path is `openforms.contrib.haal_centraal` (underscore). Idempotency uses slug (not UUID). ZIP built with Python `zipfile`, no `zip` binary needed.

## 6. Check Klantinteracties API Group Setup Configuration Support

- [x] 6.1 Confirmed: Open Forms 3.4.2 does **not** support a `setup_configuration` step for klantinteracties groups.
- [x] 6.2 Automated via Django shell in `openformulieren-init`. Append to the init script:
  ```sh
  OTEL_SDK_DISABLED=True src/manage.py shell -c "
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
  "
  ```

## 7. Verification

- [x] 7.1 Start the stack: `docker compose --profile openformulieren up` — verify all containers reach healthy state and the init container exits 0
- [x] 7.2 Open `http://localhost:8007/` — the form "Productaanvraag-Dimpact test formulier - met DigiD en communicatievoorkeuren" should appear
- [ ] 7.3 Click the form — it should auto-redirect to the DigiD mock login; select a BSN and proceed
- [ ] 7.4 Verify the Initiator step shows the Profile (`customerProfile`) component with person data from BRP wiremock
- [ ] 7.5 Complete all steps and submit — verify a new object appears in the local Objects API at `http://localhost:8010/api/v2/objects/` with the Productaanvraag-Dimpact objecttype and the `betrokkenen` block containing the BSN
- [ ] 7.6 Verify the form renders using the new engine (modern multi-step layout without the legacy FormIO toolbar)
- [x] 7.7 Run `docker compose --profile openformulieren down && docker compose --profile openformulieren up` — verify the init container is idempotent (does not fail on second run when form already exists)
