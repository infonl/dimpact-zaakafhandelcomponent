## ADDED Requirements

### Requirement: RESTHumanTaskParametersConverter converts a collection of human task definitions
When `convertHumanTaskParametersCollection` is called, the converter SHALL return one `RESTHumanTaskParameters` per plan item definition, populated from the matching `ZaaktypeCmmnHumantaskParameters` when a match exists, or with default values when no match exists.

#### Scenario: Plan item definition matches existing parameters
- **WHEN** `convertHumanTaskParametersCollection` is called and a `ZaaktypeCmmnHumantaskParameters` exists whose `planItemDefinitionID` matches the plan item definition's `id`
- **THEN** the returned `RESTHumanTaskParameters` has `id`, `actief`, `defaultGroepId`, `formulierDefinitieId`, `doorlooptijd`, and `referentieTabellen` populated from the matching parameters

#### Scenario: Plan item definition has no existing parameters
- **WHEN** `convertHumanTaskParametersCollection` is called and no `ZaaktypeCmmnHumantaskParameters` matches the plan item definition
- **THEN** the returned `RESTHumanTaskParameters` has `actief = false` and `formulierDefinitieId` derived from the plan item definition id

### Requirement: RESTHumanTaskParametersConverter converts a list of REST parameters back to domain objects
When `convertRESTHumanTaskParameters` is called with a list of `RESTHumanTaskParameters`, each entry SHALL be converted to a `ZaaktypeCmmnHumantaskParameters`.

#### Scenario: Single REST parameter converted to domain object
- **WHEN** `convertRESTHumanTaskParameters` is called with a list containing one `RESTHumanTaskParameters`
- **THEN** the result contains one `ZaaktypeCmmnHumantaskParameters` with all fields mapped correctly

#### Scenario: Multiple REST parameters converted
- **WHEN** `convertRESTHumanTaskParameters` is called with a list of multiple `RESTHumanTaskParameters`
- **THEN** the result list has the same size and each element is correctly mapped

