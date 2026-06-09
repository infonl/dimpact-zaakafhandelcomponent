# ZAC file-upload validation — Bruno collection

A hand-authored Bruno collection for QA / testers to verify the backend's file-upload
allowlist (`AllowedFileType`, enforced by `ValidRestEnkelvoudigInformatieFileUploadForm`).

It lives outside `zaakafhandelcomponent_backend_api/` on purpose: the main collection is
regenerated from OpenAPI by `update-bruno-collection.sh` and the regenerator wipes any
hand-authored requests inside that folder.

## What it tests

The endpoint under test is the case-document creation route:

```
POST /rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}?taakObject=false
```

| # | Request | Fixture | Expected |
|---|---------|---------|----------|
| 01 | List informatieobjecttypes for the case | — | 200, captures the first type UUID into the environment |
| 02 | Upload `sample.txt` (`text/plain`) | `fixtures/sample.txt` | 200 — happy path |
| 03 | Upload `sample.exe` (`application/x-msdownload`) | `fixtures/sample.exe` | 400 — disallowed extension |
| 04 | Upload `sample.zip` (`application/zip`) | `fixtures/sample.zip` | 400 — disallowed extension |
| 05 | Upload `sneaky.pdf` with a `application/pdf` MIME but arbitrary content | `fixtures/sneaky.pdf` | 200 — **documents an existing bypass** (see below) |
| 06 | Upload `sample.rtf` with the Windows MIME `application/msword` | `fixtures/sample.rtf` | 200 — variant MIME accepted |
| 07 | Upload `sample.avi` with the Windows MIME `video/avi` | `fixtures/sample.avi` | 200 — variant MIME accepted |
| 08 | Upload `sample.mkv` with the variant MIME `video/mkv` | `fixtures/sample.mkv` | 200 — variant MIME accepted |
| 09 | Upload `sample.vsd` with the Windows MIME `application/x-visio` | `fixtures/sample.vsd` | 200 — variant MIME accepted |

The fixtures are plain text — the validator inspects only the filename extension and the
declared MIME (`formaat`), never the bytes, so byte-accurate fixtures aren't needed.

## OS/browser MIME variants (requests 06–09)

The media type a browser puts on an upload is derived from the client OS and is not reliable.
Windows in particular reports `.rtf` as `application/msword`, `.avi` as `video/avi`, etc. —
not the canonical media type. The allowlist originally required an exact match, so these valid
files were rejected with 400, and the *add document* screen swallowed the error (no message at
all). `AllowedFileType` now registers the known media-type variants per extension, and the
frontend surfaces rejections. Requests 06–09 are regression guards for those four extensions.

If a tester still hits a silent-looking rejection for one of these (or another) type, the new
error dialog now shows the exact `formaat` value the browser sent — add that media type to the
matching `AllowedFileType` entry's variant set.

## Setup

1. Open Bruno → **Open collection** → select this folder
   (`scripts/bruno/collections/zaakafhandelcomponent_file_upload_validation`).
2. Pick an environment:
   - **ZAC Localhost** — for `http://localhost:8080`. The Keycloak secret is pre-filled for the
     standard local devstack.
   - **ZAC INFO TEST** — for `https://zaakafhandelcomponent-zac-dev.dimpact.lifely.nl`. Set
     `keycloakClientSecret` to the value from Keycloak on the INFO TEST environment.
3. Set the two test-data environment variables for the chosen environment:
   - `zaakUuid` — UUID of any case the test user has access to. Easiest source: open a case
     in ZAC and copy the UUID from the URL or from a `GET /rest/zaken/zaak/id/{identificatie}`
     call.
   - `documentTypeUuid` — leave empty. Request **01** captures it automatically.
4. Open the collection's **Auth** tab and click **Get Access Token**. Log in via Keycloak when
   redirected.

## Running

Run the requests in order (01 → 05), or run the whole folder with Bruno's runner. Each
request has a `tests {}` block, so the runner reports pass/fail per test.

The happy-path upload (02) and the bypass probe (05) both create a real document on the
referenced case. Clean up afterwards via ZAC or the existing
`deleteEnkelvoudigInformatieObject` request in the main collection.

## When the validator is hardened

Request **05** is written to *expect* the current behaviour (status 200) — that keeps the
suite green for now while still being a tripwire. When content/magic-byte sniffing is added
to the validator and the bypass is closed, flip the assertion in `05-uploadExeAsPdfBypass.bru`
from `expect(res.getStatus()).to.equal(200)` to `400`. From that point onwards the test
guards against regressions of the fix.

## Related code

- Validator: `src/main/kotlin/nl/info/zac/app/informatieobjecten/model/validation/ValidRestEnkelvoudigInformatieFileUploadForm*.kt`
- Allowlist enum: `src/main/kotlin/nl/info/zac/configuration/AllowedFileType.kt`
- Endpoint: `EnkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile`
