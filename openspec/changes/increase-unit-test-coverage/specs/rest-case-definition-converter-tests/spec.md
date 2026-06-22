## ADDED Requirements

### Requirement: RESTCaseDefinitionConverter converts a case definition without relations
When `inclusiefRelaties` is false, the converter SHALL return a `RESTCaseDefinition` with name and key set, and empty human task and user event listener lists.

#### Scenario: Convert without relations
- **WHEN** `convertToRESTCaseDefinition(CaseDefinition, false)` is called
- **THEN** the returned `RESTCaseDefinition` has the correct name and key
- **THEN** `humanTaskDefinitions` and `userEventListenerDefinitions` are null or empty

### Requirement: RESTCaseDefinitionConverter converts a case definition with relations
When `inclusiefRelaties` is true, the converter SHALL query human tasks and user event listeners from `CMMNService` and include them in the result.

#### Scenario: Convert with relations
- **WHEN** `convertToRESTCaseDefinition(CaseDefinition, true)` is called
- **THEN** `CMMNService.listHumanTasks` is called with the case definition ID
- **THEN** `CMMNService.listUserEventListeners` is called with the case definition ID
- **THEN** the returned `RESTCaseDefinition` contains `RESTPlanItemDefinition` entries with type `HUMAN_TASK` and `USER_EVENT_LISTENER` respectively

### Requirement: RESTCaseDefinitionConverter looks up a case definition by key
`convertToRESTCaseDefinition(String, Boolean)` SHALL delegate to `CMMNService.readCaseDefinition` before converting.

#### Scenario: Convert by key
- **WHEN** `convertToRESTCaseDefinition(key, inclusiefRelaties)` is called
- **THEN** `CMMNService.readCaseDefinition` is called with the key
- **THEN** the result is the converted case definition
