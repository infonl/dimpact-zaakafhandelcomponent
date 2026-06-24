## ADDED Requirements

### Requirement: Cascade validation of reference table values
`RestReferenceTable.waarden` and `RestReferenceTableUpdate.waarden` SHALL cascade Bean Validation to each element of type `RestReferenceTableValue` by annotating the type argument rather than the container.

#### Scenario: Invalid element in waarden list is rejected
- **WHEN** a REST request body contains a `waarden` list with an element that fails `RestReferenceTableValue` constraints
- **THEN** the server SHALL return a 400 Bad Request response with validation error details
