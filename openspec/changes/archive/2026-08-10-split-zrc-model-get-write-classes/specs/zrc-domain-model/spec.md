## MODIFIED Requirements

### Requirement: ZaakInformatieobject zaak UUID extraction
The system SHALL derive a `nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject`'s zaak UUID from the trailing UUID segment of its `zaak` URI, via a Kotlin extension property rather than a class member, since `ZaakInformatieObject` is a generated Java class.

#### Scenario: Extract UUID from the zaak URI
- **WHEN** the `zaakUUID` extension property is read on a `ZaakInformatieObject` whose `zaak` URI ends in a valid UUID
- **THEN** it returns that UUID
