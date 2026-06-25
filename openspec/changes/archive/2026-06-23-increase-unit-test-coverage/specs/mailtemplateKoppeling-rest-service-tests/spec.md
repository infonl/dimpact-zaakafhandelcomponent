## ADDED Requirements

### Requirement: MailtemplateKoppelingRestService enforces beheer policy on all endpoints
Every endpoint SHALL call `assertPolicy` with the `beheren` right from `PolicyService.readOverigeRechten` before delegating to the service layer.

#### Scenario: Policy denies beheren for readMailtemplateKoppeling
- **WHEN** `readMailtemplateKoppeling` is called and `readOverigeRechten.beheren` is false
- **THEN** `assertPolicy` throws and `MailTemplateKoppelingenService` is NOT called

### Requirement: MailtemplateKoppelingRestService reads a single koppeling by ID
`readMailtemplateKoppeling` SHALL delegate to `MailTemplateKoppelingenService.readMailtemplateKoppeling` and return the converted REST representation.

#### Scenario: Read existing koppeling
- **WHEN** `readMailtemplateKoppeling` is called with a valid ID and policy permits
- **THEN** `MailTemplateKoppelingenService.readMailtemplateKoppeling` is called with the ID
- **THEN** the converted `RESTMailtemplateKoppeling` is returned

### Requirement: MailtemplateKoppelingRestService deletes a koppeling by ID
`deleteMailtemplateKoppeling` SHALL delegate deletion to `MailTemplateKoppelingenService.delete`.

#### Scenario: Delete koppeling
- **WHEN** `deleteMailtemplateKoppeling` is called with a valid ID and policy permits
- **THEN** `MailTemplateKoppelingenService.delete` is called with the ID

### Requirement: MailtemplateKoppelingRestService stores a new or updated koppeling
`storeMailtemplateKoppeling` SHALL convert the REST input, delegate to `MailTemplateKoppelingenService.storeMailtemplateKoppeling`, and return the converted result.

#### Scenario: Store koppeling
- **WHEN** `storeMailtemplateKoppeling` is called with a `RESTMailtemplateKoppeling` and policy permits
- **THEN** `MailTemplateKoppelingenService.storeMailtemplateKoppeling` is called
- **THEN** the saved koppeling is converted and returned
