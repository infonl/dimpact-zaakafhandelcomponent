# Design: Unit tests for `listDetachedDocuments`

**Date:** 2026-04-06
**File:** `OntkoppeldeDocumentenRESTServiceTest.kt`
**Branch:** `feature/PZ-10110-enkelvoudig-informatieobject-service-unit-tests`

## Scope

Add a new `Context("Listing detached documents")` block to the existing test class, covering all branches of `OntkoppeldeDocumentenRESTService.listDetachedDocuments`.

## Function under test

```java
public RESTResultaat<RESTOntkoppeldDocument> listDetachedDocuments(
    final RESTOntkoppeldDocumentListParameters restListParameters
)
```

Logic branches:
1. `assertPolicy(policyService.readWerklijstRechten().getInbox())` — throws `PolicyException` if denied
2. Converts parameters and fetches result from `ontkoppeldeDocumentenService.getResultaat`
3. For each document, reads `informatieobjecttype` URI from DRC and extracts UUID
4. Converts documents via `ontkoppeldDocumentConverter.convert`
5. Sets `filterOntkoppeldDoor`:
   - If DB filter empty AND request has `ontkoppeldDoor` set → use request user
   - If DB filter non-empty → call `userConverter.convertUserIds`
   - Otherwise → leave empty

## Test cases

| # | Given | When | Then |
|---|-------|------|------|
| 1 | `inbox` policy is denied | `listDetachedDocuments` called | `PolicyException` thrown |
| 2 | Valid request; DB filter empty; request `ontkoppeldDoor` is `null` | `listDetachedDocuments` called | Returns result with empty `filterOntkoppeldDoor` |
| 3 | Valid request; DB filter empty; request `ontkoppeldDoor` is set | `listDetachedDocuments` called | `filterOntkoppeldDoor` contains the `RestUser` from the request |
| 4 | Valid request; DB filter non-empty | `listDetachedDocuments` called | `filterOntkoppeldDoor` is populated via `userConverter.convertUserIds` |

## Mocks and fixtures

- `policyService.readWerklijstRechten()` → `createWerklijstRechten(inbox = ...)`
- `listParametersConverter.convert(restListParameters)` → `OntkoppeldDocumentListParameters` mock
- `ontkoppeldeDocumentenService.getResultaat(listParameters)` → `OntkoppeldeDocumentenResultaat`
- `drcClientService.readEnkelvoudigInformatieobject(uuid)` → `createEnkelvoudigInformatieObject()`
- `ontkoppeldDocumentConverter.convert(documents, uuids)` → list of `RESTOntkoppeldDocument`
- `userConverter.convertUserIds(userIds)` → list of `RestUser` (test 4 only)

Existing fixtures used: `createOntkoppeldDocument`, `createEnkelvoudigInformatieObject`, `createRestUser`, `createWerklijstRechten`.
