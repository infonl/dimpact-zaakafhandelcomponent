## ADDED Requirements

### Requirement: RestHumanTaskReferenceTableConverter converts FormulierVeldDefinitie to a default RestHumanTaskReferenceTable
`convertDefault` SHALL look up the reference table for the default tabel of the veldDefinitie and return a `RestHumanTaskReferenceTable` with the veld and tabel populated.

#### Scenario: Convert default for a veldDefinitie
- **WHEN** `convertDefault` is called with a `FormulierVeldDefinitie`
- **THEN** `ReferenceTableService.readReferenceTable` is called with the veldDefinitie's default tabel name
- **THEN** a `RestHumanTaskReferenceTable` is returned with the `veld` and `tabel` set

### Requirement: RestHumanTaskReferenceTableConverter converts a collection of HumanTaskReferentieTabel to REST
`convert(Collection)` SHALL convert each `HumanTaskReferentieTabel` to a `RestHumanTaskReferenceTable` with `id`, `veld`, and `tabel` populated.

#### Scenario: Convert collection with multiple entries
- **WHEN** `convert` is called with a list of two `HumanTaskReferentieTabel` objects
- **THEN** a list of two `RestHumanTaskReferenceTable` objects is returned with all fields mapped

### Requirement: RestHumanTaskReferenceTableConverter converts a list of REST objects back to domain objects
`convert(List<RestHumanTaskReferenceTable>)` SHALL convert each REST entry to a `HumanTaskReferentieTabel`, fetching the referenced `ReferenceTable` from the service.

#### Scenario: Convert REST list to domain list
- **WHEN** `convert` is called with a list of `RestHumanTaskReferenceTable` objects
- **THEN** `ReferenceTableService.readReferenceTable` is called for each entry's tabel ID
- **THEN** the returned list contains `HumanTaskReferentieTabel` objects with `id`, `veld`, and `tabel` set
