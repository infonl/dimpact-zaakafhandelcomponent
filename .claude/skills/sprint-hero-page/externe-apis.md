# Externe API's

Rows (fixed order): KVK Zoeken API, KVK Basisprofiel API, KVK Vestigingsprofiel API, Haal-Centraal BRP Personen Bevragen.

Columns: Latest | Our Version on TEST.

## 1. Latest

Run `python3 scripts/python/dependencies/versions.py` from the root of the ZAC checkout. Use only these lines and ignore the other components it prints:

- `KvK Zoeken API latest version: <version> - <url>`
- `KvK Basisprofiel API latest version: <version> - <url>`
- `KvK Vestigingsprofiel API latest version: <version> - <url>`
- `Haal-Centraal-BRP latest version: <version> - <url>`

The script prints these four lines first. If it stops with an error further on, the lines already printed still count.

If the script cannot fill a row (it printed `?` with an error on that line, or `None`), show `?` in that cell and name the row and the error below the table. Do not look the version up elsewhere: a failing script is to be fixed, not worked around. Leave that column out of the Confluence values file, so the page keeps its value.

### The KVK rule

The script takes the highest version number on the release notes page. KVK sometimes labels a bug fix with a version number without releasing a new API version. So for every KVK API, read the text of the newest release note (strip the HTML tags and ignore the `__NEXT_DATA__` JSON):

- KVK Zoeken: https://developers.kvk.nl/documentation/release-notes/zoeken-api
- KVK Basisprofiel: https://developers.kvk.nl/documentation/release-notes/basisprofiel-api
- KVK Vestigingsprofiel: https://developers.kvk.nl/documentation/release-notes/vestigingsprofiel-api

If it only describes a fix and no new or changed specification, report the previous real version as Latest and mention the label in a remark. When Latest is higher than our version, say a human should confirm it is a real API version, so a changed page or label is noticed instead of silently giving a wrong number.

## 2. Our Version on TEST

The `info.version` of the OpenAPI specs in the ZAC repo on `origin/main` (TEST runs ZAC main), read with `git show origin/main:<file>`:

- KVK Zoeken: `src/main/resources/api-specs/kvk/zoeken-openapi.yaml`
- KVK Basisprofiel: `src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml`
- KVK Vestigingsprofiel: `src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml`
- BRP: `src/main/resources/api-specs/brp/brp-openapi.yaml`
