# Externe API's

Rows (fixed order): KVK Zoeken API, KVK Basisprofiel API, KVK Vestigingsprofiel API, Haal-Centraal BRP Personen Bevragen.

Columns: Latest | Our Version on TEST.

## 1. Latest

Public release notes. Strip the HTML tags and ignore the `__NEXT_DATA__` JSON; the newest version is the first `X.Y.Z` in the page text.

- KVK Zoeken: https://developers.kvk.nl/documentation/release-notes/zoeken-api
- KVK Basisprofiel: https://developers.kvk.nl/documentation/release-notes/basisprofiel-api
- KVK Vestigingsprofiel: https://developers.kvk.nl/documentation/release-notes/vestigingsprofiel-api. The page contains a 2022 fix labelled "1.4.1", but this is not an API version; report 1.4.0 as latest unless a newer real API version appears, and mention the misleading label in a remark.
- BRP: https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes, the highest "versie X.Y.Z".

## 2. Our Version on TEST

The `info.version` of the OpenAPI specs in the ZAC repo on `origin/main` (TEST runs ZAC main), read with `git show origin/main:<file>`:

- KVK Zoeken: `src/main/resources/api-specs/kvk/zoeken-openapi.yaml`
- KVK Basisprofiel: `src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml`
- KVK Vestigingsprofiel: `src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml`
- BRP: `src/main/resources/api-specs/brp/brp-openapi.yaml`
