## ADDED Requirements

### Requirement: Zaakobject read/write field requiredness
The system SHALL model the ZRC `zaakobject` resource as two distinct types: a read type (`Zaakobject` and its leaf subclasses) where `url` and `uuid` are always present, and a write type (`ZaakobjectRequest` and its leaf subclasses) that has no `url`/`uuid` fields at all. Both types SHALL require `zaak` and `objectType`.

#### Scenario: Read type guarantees url and uuid are present
- **WHEN** a `Zaakobject` is deserialized from a `GET /zaakobjecten` or `GET /zaakobjecten/{uuid}` response
- **THEN** its `url` and `uuid` properties are non-null, requiring no null-check or non-null assertion at call sites

#### Scenario: Write type has no url or uuid fields
- **WHEN** a `ZaakobjectRequest` is constructed to build a `POST /zaakobjecten` request body
- **THEN** it exposes no `url` or `uuid` property, since those are server-assigned and never sent by the client

#### Scenario: Deleting a zaakobject uses the read type's guaranteed uuid
- **WHEN** `ZrcClientService.deleteZaakobject` is called with a `Zaakobject` obtained from a prior read
- **THEN** it reads `zaakobject.uuid` directly, without a non-null assertion, to issue the delete request
