## 1. Backend – service method

- [ ] 1.1 Add `listActiveGroupsForBehandelaarRoleAndZaaktypes(zaaktypeDescriptions: List<String>): List<Group>` to `IdentityService` — fetch groups per description and return the intersection
- [ ] 1.2 Write unit tests for the new `IdentityService` method (single description, intersection with common group, no common group)

## 2. Backend – REST endpoint

- [ ] 2.1 Add a request body data class (e.g. `RestBehandelaarGroupsRequest`) with a `zaaktypeDescriptions: List<String>` field
- [ ] 2.2 Add `POST /rest/identity/behandelaar-groups` to `IdentityRestService`; return HTTP 400 when the list is empty
- [ ] 2.3 Run `./gradlew generateOpenApiSpec` and verify the new endpoint appears in the spec

## 3. Integration tests

- [ ] 3.1 Add an integration test for the happy path: POST with descriptions that have a common authorised group → HTTP 200 with that group
- [ ] 3.2 Add an integration test for no common authorised group → HTTP 200 empty list
- [ ] 3.3 Add an integration test for empty descriptions list → HTTP 400
