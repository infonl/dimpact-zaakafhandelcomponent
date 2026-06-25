## 1. Backend – service method

- [x] 1.1 Add `listActiveGroupsForBehandelaarRoleAndZaaktypes(zaaktypeDescriptions: List<String>): List<Group>` to `IdentityService` — fetch groups per description and return the intersection
- [x] 1.2 Write unit tests for the new `IdentityService` method (single description, intersection with common group, no common group)

## 2. Backend – REST endpoint

- [x] 2.1 Add a request body data class (e.g. `RestBehandelaarGroupsRequest`) with a `zaaktypeDescriptions: List<String>` field
- [x] 2.2 Add `POST /rest/identity/behandelaar-groups` to `IdentityRestService`; return HTTP 400 when the list is empty
- [x] 2.3 Run `./gradlew generateOpenApiSpec` and verify the new endpoint appears in the spec
- [x] 2.4 Write unit tests for `IdentityRestService.listBehandelaarGroupsForZaaktypes` (common group returned with HTTP 200, empty list returns HTTP 400)

## 3. Integration tests

- [x] 3.1 Add an integration test for the happy path: POST with descriptions that have a common authorised group → HTTP 200 with that group
- [x] 3.2 Add an integration test for no common authorised group → HTTP 200 empty list
- [x] 3.3 Add an integration test for empty descriptions list → HTTP 400
