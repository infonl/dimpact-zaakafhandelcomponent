## Why

Developers need to validate the complete productaanvraag flow — DigiD-authenticated form submission in Open Formulieren → Objects API → ZAC — locally without external test environments. The Open Formulieren docker-compose stack exists but no form is configured. A pre-configured test form that mirrors the production setup enables this flow out of the box.

## What Changes

- Enable the `ENABLE_DEMO_PLUGINS` Django feature flag in the Open Formulieren local settings so the `digid-mock` authentication backend becomes available
- Add an Objects API group to the Open Formulieren setup configuration (`data.yaml`) pointing to the local `objecten-api` service
- Adapt the exported productaanvraag-Dimpact test form (from `tmp/productaanvraag-dimpact-test-formulier-compleet/`) for local use: replace the external objecttype UUID with the local `Productaanvraag-Dimpact` UUID, update the Objects API group identifier, and remove external Open Zaak informatieobjecttype URLs
- Add the adapted form JSON files to `scripts/docker-compose/imports/openformulieren/forms/`
- Extend the `openformulieren-init` container to import the form automatically on first start
- Configure the BRP API endpoint in Open Formulieren (programmatically in the init container) so the Profile (`customerProfile`) component can fetch BRP person data
- Optionally configure a Klantinteracties API group for the `communication_preferences` prefill plugin used by the Profile component

## Capabilities

### New Capabilities

- `openformulieren-productaanvraag-form`: A pre-configured productaanvraag-Dimpact test form in Open Formulieren featuring: DigiD mock (simulatie) auto-login, the new rendering engine, Objects API v1 (verouderd/sjabloon) registration backend, the Profile plugin with BRP prefill on the Initiator step, a free-text aanvraaggegevens step, and a file-upload step

### Modified Capabilities

- `open-formulieren-docker-compose`: Extended with form auto-import, ENABLE_DEMO_PLUGINS flag, and BRP API configuration in the init container

## Impact

- `docker-compose.yaml`: extend `openformulieren-init` command; add volume mount for forms directory
- `scripts/docker-compose/imports/openformulieren/settings/docker_local.py`: enable `ENABLE_DEMO_PLUGINS`
- `scripts/docker-compose/imports/openformulieren/setup-configuration/data.yaml`: add Objects API group config
- New directory `scripts/docker-compose/imports/openformulieren/forms/` with 6 JSON files adapted from the test environment export
- No ZAC application code changes; no impact on the `itest` CI profile
