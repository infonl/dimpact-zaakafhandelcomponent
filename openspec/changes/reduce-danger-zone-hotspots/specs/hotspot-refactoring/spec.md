## ADDED Requirements

### Requirement: A hotspot refactoring preserves the REST contract
A pull request of a phase of the hotspot refactoring SHALL leave the REST contract of ZAC unchanged: the same paths,
HTTP methods, request and response payloads, status codes and error codes.

#### Scenario: The generated OpenAPI specification is identical
- **WHEN** `./gradlew generateOpenApiSpec` runs on the pull request and on the commit it is based on
- **THEN** the two generated `openapi.json` files are byte for byte identical

#### Scenario: The integration tests pass without changes
- **WHEN** the integration tests run on the pull request
- **THEN** they pass, and the pull request does not change, remove or skip any existing integration test

#### Scenario: The test-data scripts are untouched
- **WHEN** the pull request is reviewed
- **THEN** it contains no change to `scripts/test-data/`

### Requirement: A hotspot refactoring makes no functional change
A pull request of a phase of the hotspot refactoring SHALL NOT fix defects or change behaviour, also when it moves
code that has a known defect. An existing test that describes today's behaviour SHALL keep its assertions.

#### Scenario: A moved test keeps its assertions
- **WHEN** a unit test moves to the test class of the new service
- **THEN** its given, when and then descriptions and its assertions are the same as before the move

#### Scenario: A known defect in moved code stays for its own ticket
- **WHEN** moved code contains a defect that has a ticket of its own
- **THEN** the pull request leaves the defect in place and does not add a test that asserts the fixed behaviour

### Requirement: Moved endpoints are covered before they move
Before an endpoint moves to another class, it SHALL have a unit test and an integration test of its normal use. A
missing test SHALL be added against the current class, in the same pull request and before the move.

#### Scenario: An endpoint without an integration test
- **WHEN** an endpoint that is about to move has no integration test
- **THEN** the pull request first adds an integration test of its normal use, which passes against the current class
  and keeps passing after the move

### Requirement: Each phase is measured with the churn vs complexity report
Each phase SHALL record, in its pull request description, the numbers of the files it changes from the churn vs
complexity report before and after: commits, total and worst-function cyclomatic complexity, number of functions and
lines of code.

#### Scenario: The pull request of a phase shows its effect
- **WHEN** the pull request of a phase is opened
- **THEN** its description lists those numbers for each changed top-ten file, before and after the change
