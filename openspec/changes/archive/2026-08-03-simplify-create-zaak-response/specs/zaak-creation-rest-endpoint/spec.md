## ADDED Requirements

### Requirement: Zaak creation endpoint returns only the zaak identification
The `POST /rest/zaken/zaak` endpoint SHALL, upon successfully creating a zaak, respond with a `CreateZaakResponse` object containing only the zaak identification (`identificatie`) in the response body. It SHALL NOT include any other zaak fields (such as `uuid`, `groep`, `behandelaar`, `zaaktype`, `besluiten`, rights, or status) in the create response.

#### Scenario: Successful zaak creation returns the identification
- **WHEN** a client submits a valid `RestZaakAanmaakGegevens` payload to `POST /rest/zaken/zaak` and the zaak is created successfully
- **THEN** the response body is a JSON object with only an `identificatie` field containing the identification of the newly created zaak, and no other zaak field is present in the response body

#### Scenario: Retrieving full zaak details requires a follow-up read
- **WHEN** a client needs any zaak field other than the identification after creating a zaak (e.g. its UUID, group, or rights)
- **THEN** the client SHALL fetch that information via a separate call, such as `GET /rest/zaken/zaak/id/{identificatie}`, rather than expecting it in the create response
