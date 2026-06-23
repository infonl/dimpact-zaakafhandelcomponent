## ADDED Requirements

### Requirement: AuditEnkelvoudigInformatieobjectConverter detects field changes between two versions
When converting an audit wijziging that contains both an old and new `EnkelvoudigInformatieObject`, the converter SHALL return `HistoryLine` entries only for fields that differ between the two versions.

#### Scenario: No fields changed
- **WHEN** `convert` is called with two identical `EnkelvoudigInformatieObject` instances
- **THEN** an empty list is returned

#### Scenario: titel changed
- **WHEN** `convert` is called with old and new objects where only `titel` differs
- **THEN** one `HistoryLine` with label `"titel"` is returned

#### Scenario: informatieobjecttype URI changed
- **WHEN** `convert` is called with old and new objects where `informatieobjecttype` URI differs
- **THEN** `ZtcClientService.readInformatieobjecttype` is called for both URIs and one `HistoryLine` with label `"documentType"` is returned

#### Scenario: Multiple fields changed
- **WHEN** `convert` is called with objects where `titel`, `auteur`, and `locked` all differ
- **THEN** three `HistoryLine` entries are returned, one for each changed field

### Requirement: AuditEnkelvoudigInformatieobjectConverter handles null old or new version
When either the old or new `EnkelvoudigInformatieObject` in the wijziging is null, the converter SHALL return a single `HistoryLine` with label `"informatieobject"`.

#### Scenario: Old version is null
- **WHEN** `convert` is called with `oud = null` and a non-null `nieuw`
- **THEN** exactly one `HistoryLine` with label `"informatieobject"` is returned

#### Scenario: New version is null
- **WHEN** `convert` is called with a non-null `oud` and `nieuw = null`
- **THEN** exactly one `HistoryLine` with label `"informatieobject"` is returned
