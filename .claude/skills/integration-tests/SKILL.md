---
name: add-integration-tests
description: Add Integration Tests (Kotlin / Kotest)
---

> If you discover an improvement to this spec file while writing a test, ask the user: "I noticed a pattern / gotcha that could be added to the spec file — want me to add it?"

---

## Step 0 — Ask before anything else

Ask all questions before doing anything else:

1. > "What type of integration test do you want to add? **CMMN** or **BPMN**?"
2. > "Are you **creating a new test file** or **adding to / modifying an existing one**?"
3. > If new: "Which source file should the integration test be based on? (filename or full path)"
   > If existing: "Which existing test file? (filename or full path)"

Then read the matching configuration section at the bottom of this file before writing a single line of Kotlin.

---

## Workflow — new test file

| # | Step |
|---|---|
| 1 | Understand the feature — read the REST endpoint(s) or delegate under test, identify input/output, happy path, edge cases |
| 2 | Decide file location — BPMN: `src/itest/kotlin/nl/info/zac/itest/bpmn/`; CMMN and general: `src/itest/kotlin/nl/info/zac/itest/` — name the test class `<SourceClassName>Test` (e.g. `ZaakSuspendRestService` → `ZaakSuspendRestServiceTest`) |
| 3 | Write the test — see framework patterns below |
| 4 | Tell the user the run command (generated from the class name) and ask them to run it |
| 5 | Summarize the test file and ask whether more tests are needed — see below |

## Workflow — existing test file (add or modify)

| # | Step |
|---|---|
| 1 | **Read the existing test file in full** — understand what is already tested, which variables are in scope, and the current `Given`/`When` structure |
| 2 | Understand the feature change or new scenario — read the relevant source file(s) only if the endpoint/delegate behaviour is unclear from the test |
| 3 | **Decide where to add** — see "One `Given` vs multiple `Given` blocks" below |
| 4 | Write the addition — new `Given` block, or new `When`/`And` inside an existing one |
| 5 | Tell the user the run command and ask them to run it |
| 6 | Summarize what was added/changed and ask whether more tests are needed |

No PR step. The test is done when it compiles and the user has run it.

## Test isolation — the shared live system

All integration tests run against the **same** ZAC container in sequence. There is no cleanup between tests. Data created by an earlier test is still present when a later test runs. Implications:

- **Use unique identifiers.** When a test creates a zaak, its identification will be in the search index for all subsequent tests. Use `zaakHelper.createZaak(zaakDescription = "itestZaakDescription-${System.currentTimeMillis()}", ...)` or a unique constant to avoid collisions.
- **Do not assume a clean state.** Never assert `totaal == 0` on the first search; some earlier test may have created matching data.
- **`eventually()` is mandatory for any side effect.** Indexing, notifications, and BPMN engine steps are asynchronous.
- The `ZacItestProjectConfig` (project-level listener) runs all zaaktype configuration **once** before any test spec. You do not need to set up zaaktypes inside your test.

## Block hierarchy — `Context`, `Given`, `When`, `Then`, `And`

Use `Context` to group thematically related `Given` blocks within one file:
```kotlin
Context("Managing zaak suspension") {
    Given("a zaak exists") {
        When("the zaak is suspended") {
            Then("the zaak should be suspended") { ... }
            And("the zaak reason should be set") { ... }
        }
        And("the zaak is then resumed") {
            Then("the zaak should no longer be suspended") { ... }
        }
    }
}
```

## One `Given` vs multiple `Given` blocks

| Situation | Structure |
|---|---|
| Scenarios **depend on shared state** or must run in a fixed sequence (e.g. create → suspend → resume → check) | One `Given`, deeply nested `When`/`And` blocks. Variables declared with `val (a, b) = ...` or `var` outside the `When`. |
| Scenarios are **independent** — each needs its own fresh zaak or different preconditions | Separate `Given` blocks, each with its own setup. |

**Nested `And` inside `When` = the previous step already happened.** Use this to chain sequential actions.
**Sibling `When` blocks inside the same `Given` = parallel scenarios sharing the same precondition but not each other's state.**

## Class-body initialization (one-time setup per spec)

Variables initialized at the **class body level** (outside any `Given`) run once when the spec is constructed — before any `Given` block. Use this for setup that every test in the file needs:
```kotlin
class MyTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    // runs once, result available in all Given/When blocks below
    val temporaryPersonId: UUID = zacClient.getTemporaryPersonId(TEST_PERSON_HENDRIKA_JANSE_BSN, BEHANDELAAR_1)
    lateinit var zaakUuid: UUID

    Given(...) { ... }
})
```

## Step 5 — Summarize and offer to continue

After step 4, output a summary in this format:

> **Test file added:** `<relative path to the new test file>`
>
> **Tests written:**
> - `Given ... When ... Then ...` — one line per scenario, plain English
> - _(repeat for each Given/When block)_
>
> **Coverage:** happy path / error cases / async behaviour / permission checks — list which were covered
>
> Would you like to add more tests to this file? If so, describe what you want to test next (e.g. a specific error case, a different user role, a missing edge case) and I will add it.

Wait for the user's answer before doing anything else. If they describe more tests:
- The source file and file location are already known — **skip steps 1 and 2**, go straight to step 3.
- Exception: if the new tests cover an endpoint not yet read, do a targeted read of that endpoint before writing.
- After writing, repeat step 5 (summarize the additions and ask again).

---

## Run command

Run all itests:
```
./gradlew itest --info
```

Run a single test class (replace with actual class name):
```
./gradlew itest --tests "nl.info.zac.itest.bpmn.MyNewBpmnTest" --info
```
```
./gradlew itest --tests "nl.info.zac.itest.MyNewCmmnTest" --info
```

---

## Framework cheat sheet

### Skeleton
```kotlin
/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest          // or nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import kotlin.time.Duration.Companion.seconds

class MyNewTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val logger = KotlinLogging.logger {}

    Given("...") {
        When("...") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/my/endpoint",
                testUser = BEHANDELAAR_1
            )
            logger.info { "Response: ${response.bodyAsString}" }

            Then("the response should be 200") {
                response.code shouldBe HTTP_OK
            }
            And("the response body should contain the expected value") {
                response.bodyAsString.shouldContainJsonKeyValue("key", "value")
            }
        }
    }
})
```

### HTTP methods
```kotlin
// GET
itestHttpClient.performGetRequest(url = "...", testUser = BEHANDELAAR_1)

// POST (JSON body)
itestHttpClient.performJSONPostRequest(url = "...", requestBodyAsString = """{ "key": "value" }""", testUser = BEHANDELAAR_1)

// PUT (JSON body)
itestHttpClient.performPutRequest(url = "...", requestBodyAsString = """{ ... }""", testUser = BEHANDELAAR_1)

// PATCH (JSON body)
itestHttpClient.performPatchRequest(url = "...", requestBodyAsString = """{ ... }""", testUser = BEHANDELAAR_1)

// DELETE (no body)
itestHttpClient.performDeleteRequest(url = "...", testUser = BEHANDELAAR_1)

// DELETE (with JSON body)
itestHttpClient.performDeleteRequest(url = "...", requestBodyAsString = """{ ... }""", testUser = BEHANDELAAR_1)

// HEAD — returns Int (status code only, no body)
itestHttpClient.performHeadRequest(url = "...")

// WebSocket — for testing screen events
itestHttpClient.connectNewWebSocket(url = ZAC_WEBSOCKET_BASE_URI + "/...", webSocketListener = ..., testUser = BEHANDELAAR_1)

// ZGW API — direct calls to Open Zaak with JWT auth (no testUser, uses service-to-service JWT)
itestHttpClient.performZgwApiGetRequest(url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/...")
itestHttpClient.performZgwApiPostRequest(url = "...", requestBodyAsString = """{ ... }""")
```
All standard methods return `ResponseContent` with `.code` (Int) and `.bodyAsString` (String).
`testUser` is optional on all methods — omit it for unauthenticated or header-authenticated calls.

### Assertions
```kotlin
response.code shouldBe HTTP_OK                              // status code 200
response.code shouldBe HTTP_NO_CONTENT                      // status code 204 (task start, notifications, delete)
response.bodyAsString.shouldContainJsonKeyValue("key", 42)  // exact JSON value
response.bodyAsString.shouldContainJsonKey("key")           // key present
response.bodyAsString.shouldNotContainJsonKey("key")        // key absent
response.bodyAsString.shouldBeJsonArray()                   // assert response is a JSON array
response.bodyAsString.shouldBeJsonObject()                  // assert response is a JSON object
JSONObject(response.bodyAsString).getString("id")           // parse manually
JSONArray(response.bodyAsString).length() shouldBe 3

// String matchers (import from io.kotest.matchers.string):
response.bodyAsString shouldContain "someSubstring"
response.bodyAsString shouldStartWith "multipart/mixed"
```
Import HTTP constants from `java.net.HttpURLConnection`: `HTTP_OK`, `HTTP_NO_CONTENT`, `HTTP_FORBIDDEN` (403), `HTTP_BAD_REQUEST` (400), `HTTP_NOT_FOUND` (404).

### sleepForOpenZaakUniqueConstraint — mandatory between consecutive status changes

OpenZaak enforces a unique constraint on `(zaak, datum_status_gezet)` — two status changes within the same second return a 400. **Always call this between consecutive `doUserEventListenerPlanItem` calls or between any two calls that change zaak status.**

```kotlin
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint

sleepForOpenZaakUniqueConstraint(1)   // sleeps 1 second
```

Do NOT use `eventually` to work around this — the constraint is time-based, not eventual. A 1-second sleep is the correct fix.

### Async polling — use `eventually()` whenever a side-effect may be delayed
```kotlin
eventually(10.seconds) {
    val body = itestHttpClient.performGetRequest(url = "...", testUser = BEHANDELAAR_1).bodyAsString
    JSONObject(body).getInt("totaal") shouldBe 1
}

// Longer waits (process task generation, BPMN engine):
val afterThirtySeconds = eventuallyConfig {
    duration = 30.seconds
    interval = 500.milliseconds
}
eventually(afterThirtySeconds) { ... }
```

### Always log request + response
```kotlin
logger.info { "Response: ${response.bodyAsString}" }
```

### ZacClient helpers (use these instead of raw HTTP when they fit)
```kotlin
// Create a zaak — optional params: behandelaarId, behandelaarName, description, toelichting,
//   communicatiekanaal (default: COMMUNICATIEKANAAL_TEST_1), vertrouwelijkheidaanduiding (default: OPENBAAR)
// When assigning a behandelaar: id = user.username, name = user.displayName (not user.username for name!)
zacClient.createZaak(
    zaakTypeUUID = ..., groupId = ..., groupName = ..., startDate = ..., testUser = ...,
    behandelaarId = BEHANDELAAR_1.username, behandelaarName = BEHANDELAAR_1.displayName
)
zacClient.createZaak(zaakTypeUUID = ..., groupId = ..., groupName = ..., startDate = ..., testUser = ...)

zacClient.retrieveZaak(zaakUUID, testUser)   // by UUID
zacClient.retrieveZaak(id = "ZAAK-2001-0000000001", testUser)   // by identification string
zacClient.submitFormData(bpmnZaakUuid = ..., taakData = """{ ... }""", testUser = ...)   // returns String (body)
zacClient.submitFormDataRaw(bpmnZaakUuid = ..., taakData = """{ ... }""", testUser = ...) // returns ResponseContent (code + body)
zacClient.searchForTasks(zaakIdentificatie = ..., taskName = ..., testUser = ...)
zacClient.getTemporaryPersonId(bsn, testUser)   // needed before adding an initiator
zacClient.createEnkelvoudigInformatieobjectForZaak(
    zaakUUID = ..., fileName = ..., fileMediaType = ...,
    vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR, testUser = ...
)  // returns ResponseContent; parse UUID with JSONObject(bodyAsString).getString("uuid")
zacClient.getHumanTaskPlanItemsForZaak(zaakUUID, testUser)  // GET /planitems/zaak/{uuid}/humanTaskPlanItems
// ⚠ Note the exact parameter names — planItemInstanceId, groupId, groupName (not planItemId/group)
zacClient.startHumanTaskPlanItem(
    planItemInstanceId = ..., fatalDate = ..., groupId = ..., groupName = ..., testUser = ...
)  // returns ResponseContent with HTTP_NO_CONTENT (204) on success
```

### Higher-level helper classes (prefer over raw ZacClient when they fit)
```kotlin
// ZaakHelper — creates zaak; pass indexZaak = true to wait until findable in the search index
val zaakHelper = ZaakHelper(zacClient)
val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
    zaaktypeUuid = ..., testUser = ..., indexZaak = true  // indexZaak = false by default!
)
// Also adds betrokkenen (roles) to a zaak:
zaakHelper.addNatuurlijkPersoonBetrokkeneToZaak(
    zaakUuid = ..., roltypeUUID = ROLTYPE_UUID_BELANGHEBBENDE, bsn = TEST_PERSON_HENDRIKA_JANSE_BSN, testUser = ...
)
zaakHelper.addNietNatuurlijkPersoonBetrokkeneToZaak(
    zaakUuid = ..., roltypeUUID = ..., kvkNummer = ..., vestigingsnummer = null, testUser = ...
)

// DocumentHelper — uploads a document; pass indexDocument = true to wait until indexed
val documentHelper = DocumentHelper(zacClient)
val (documentUuid, documentIdentification) = documentHelper.uploadDocumentToZaak(
    zaakUuid = ..., documentTitle = ..., authorName = ...,
    mediaType = PDF_MIME_TYPE, fileName = TEST_PDF_FILE_NAME,
    indexDocument = true,  // indexDocument = false by default!
    testUser = ...
)

// TaskHelper — starts a human task and optionally waits for it to be indexed
val taskHelper = TaskHelper(zacClient)
val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
    zaakUuid = ..., zaakIdentificatie = ..., fatalDate = ...,
    group = ..., waitForTaskToBeIndexed = true, testUser = ...
)
```
Use raw `ZacClient` calls when you do not need indexing (e.g. BPMN form submission).

### WebSocket testing — screen event assertions
```kotlin
import nl.info.zac.itest.util.WebSocketTestListener
import nl.info.zac.itest.config.ItestConfiguration.ZAC_WEBSOCKET_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_ZAKEN_VERDELEN

// Connect before triggering the action:
val webSocketListener = WebSocketTestListener(textToBeSentOnOpen = null)
itestHttpClient.connectNewWebSocket(
    url = "$ZAC_WEBSOCKET_BASE_URI/updates/zaken",
    webSocketListener = webSocketListener,
    testUser = BEHANDELAAR_1
)

// Trigger the action, then poll for the expected message:
eventually(10.seconds) {
    webSocketListener.messagesReceived.any { it.contains(SCREEN_EVENT_TYPE_ZAKEN_VERDELEN) } shouldBe true
}
```
Available `SCREEN_EVENT_TYPE_*` constants: `TAKEN_VERDELEN`, `TAKEN_VRIJGEVEN`, `ZAKEN_VERDELEN`, `ZAKEN_VRIJGEVEN`, `ZAAK_ROLLEN`.

### Class body declaration order
**CMMN / general:** `itestHttpClient` → `zacClient` → `logger` → optional helpers
**BPMN:** `logger` → `itestHttpClient` → `zacClient` → optional `eventuallyConfig`

### ZacClient construction
**CMMN tests:** `val itestHttpClient = ItestHttpClient(); val zacClient = ZacClient(itestHttpClient)` — pass the same `ItestHttpClient` instance to both so raw HTTP calls and ZacClient calls share the same client.
**BPMN tests:** `val zacClient = ZacClient()` — no shared `ItestHttpClient` needed unless the test also makes direct HTTP calls.

### Notification endpoint — no testUser
The `/notificaties` endpoint authenticates via a header, not a test user. Omit `testUser` and pass the auth manually:
```kotlin
itestHttpClient.performJSONPostRequest(
    url = "$ZAC_API_URI/notificaties",
    headers = Headers.headersOf(
        "Content-Type", "application/json",
        "Authorization", OPEN_NOTIFICATIONS_API_SECRET_KEY
    ),
    requestBodyAsString = JSONObject(mapOf(...)).toString()
)
```

### Custom JSON assertions (from `nl.info.zac.itest.util`)
```kotlin
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields

responseBody shouldEqualJsonIgnoringExtraneousFields """{ "key": "value" }"""  // ignores extra fields
JSONArray(responseBody)[0].toString() shouldEqualJsonIgnoringExtraneousFields """{ ... }"""
```
Use when asserting a subset of a large JSON response rather than individual key/value pairs.

### All available test users (`src/itest/.../config/TestUsers.kt`)
`TestUser` has: `.username` (Keycloak login), `.displayName` (shown in UI/responses), `.email`.

| Val | Username | Role / Notes |
|---|---|---|
| `BEHANDELAAR_1` | behandelaar1newiam | Behandelaar domein test 1 |
| `BEHANDELAAR_2` | behandelaar2newiam | Behandelaar domein test 2 — **no brp_zoeken role** |
| `COORDINATOR_1` | coordinator1newiam | Coordinator domein test 1 |
| `COORDINATOR_2` | coordinator2newiam | Coordinator domein test 2 |
| `BEHEERDER_1` | beheerder1newiam | Beheerder all domeinen — use for admin/config calls |
| `RAADPLEGER_1` | raadpleger1newiam | Raadpleger domein test 1 |
| `RAADPLEGER_2` | raadpleger2newiam | Raadpleger domein test 2 |
| `RECORDMANAGER_1` | recordmanager1newiam | Recordmanager domein test 1 |
| `RECORDMANAGER_2` | recordmanager2newiam | Recordmanager domein test 2 |
| `RAADPLEGER_EN_BEHANDELAAR_1` | raadplegerenbehandelaar1newiam | Raadpleger in domein 1 + behandelaar in domein 2 |
| `USER_WITHOUT_ANY_ROLE` | userwithoutanyrole | No role — for testing 403 scenarios |
| `BEHANDELAAR_INACTIVE_GROUP_1` | behandelaar1inactivegroup | Behandelaar in an inactive group |

### All available test groups (`src/itest/.../config/TestGroups.kt`)
| Val | name | description |
|---|---|---|
| `GROUP_BEHANDELAARS_TEST_1` | behandelaars-test-1 | Test group behandelaars domein test 1 |
| `GROUP_BEHANDELAARS_TEST_2` | behandelaars-test-2 | Test group behandelaars domein test 2 |
| `GROUP_COORDINATORS_TEST_1` | coordinators-test-1 | Test group coordinators domein test 1 |
| `GROUP_COORDINATORS_TEST_2` | coordinators-test-2 | Test group coordinators domein test 2 |
| `GROUP_RAADPLEGERS_TEST_1` | raadplegers-test-1 | Test group raadplegers domein test 1 |
| `GROUP_RAADPLEGERS_TEST_2` | raadplegers-test-2 | Test group raadplegers domein test 2 |
| `GROUP_RECORDMANAGERS_TEST_1` | recordmanagers-test-1 | Test group recordmanagers domein test 1 |
| `GROUP_RECORDMANAGERS_TEST_2` | recordmanagers-test-2 | Test group recordmanagers domein test 2 |
| `GROUP_BEHEERDERS_ELK_DOMEIN` | beheerders-elk-domein | Test group beheerders elk domein |
| `GROUP_INACTIVE_TEST_1` | inactive-group-test-1 | Test group inactive |

### Commonly needed ItestConfiguration constants

**Dates** — use `LocalDate` (`DATE_*`) or `ZonedDateTime` (`DATE_TIME_*`):
```kotlin
DATE_2000_01_01       // LocalDate (also available: DATE_2020_01_01, DATE_2024_01_01, DATE_2025_01_01, DATE_2025_07_01)
DATE_TIME_2000_01_01  // ZonedDateTime — pass to createZaak(startDate = ...)
DATE_TIME_2024_01_01  // ZonedDateTime — default used by ZaakHelper.createZaak
```

**Test files** — actual resources in `src/itest/resources/`:
```kotlin
TEST_PDF_FILE_NAME    = "fäkeTestDocument.pdf"
TEST_TXT_FILE_NAME    = "tëstTextDocument.txt"
TEST_WORD_FILE_NAME   = "fakeWordDocument.docx"
PDF_MIME_TYPE         = "application/pdf"
TEXT_MIME_TYPE        = "text/plain"
```

**Document constants:**
```kotlin
DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR        = "OPENBAAR"
DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK   = "ZAAKVERTROUWELIJK"
DOCUMENT_STATUS_IN_BEWERKING  = "in_bewerking"
DOCUMENT_STATUS_DEFINITIEF    = "definitief"
INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID   = "b1933137-94d6-49bc-9e12-afe712512276"
INFORMATIE_OBJECT_TYPE_FACTUUR_UUID   = "eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5"
```

**Betrokkene type / identification constants** (used in betrokkene JSON payloads):
```kotlin
BETROKKENE_TYPE_NATUURLIJK_PERSOON         = "NATUURLIJK_PERSOON"
BETROKKENE_IDENTIFICATION_TYPE_BSN         = "BSN"
BETROKKENE_IDENTIFICATION_TYPE_VESTIGING   = "VN"
BETROKKENE_IDENTIFICATION_TYPE_KVK         = "RSIN"
BETROKKENE_ROL_TOEVOEGEN_REDEN             = "Toegekend door de medewerker tijdens het behandelen van de zaak"
```

**Roltype UUIDs** (for adding betrokkenen to a zaak with `ZAAKTYPE_CMMN_TEST_2_UUID`):
```kotlin
ROLTYPE_UUID_BELANGHEBBENDE  = "4c4cd850-8332-4bb9-adc4-dd046f0614ad"
ROLTYPE_UUID_MEDEAANVRAGER   = "b14cf056-0480-4060-a376-1dd522a50431"
ROLTYPE_NAME_BELANGHEBBENDE  = "Belanghebbende"
ROLTYPE_NAME_MEDEAANVRAGER   = "Medeaanvrager"
// Per-zaaktype betrokkene roltype UUIDs for ZAAKTYPE_CMMN_TEST_2_UUID:
ZAAKTYPE_CMMN_TEST_2_BETROKKENE_BELANGHEBBENDE  = "3bb6928b-76de-4716-ac5f-fa3d7d6eca36"
ZAAKTYPE_CMMN_TEST_2_BETROKKENE_MEDEAANVRAGER   = "e49a634b-731c-4460-93f4-e919686811aa"
ZAAKTYPE_CMMN_TEST_2_BETROKKENE_GEMACHTIGDE     = "4b473a85-5516-441f-8d7d-57512c6b6833"
ZAAKTYPE_CMMN_TEST_2_BETROKKENE_CONTACTPERSOON  = "ca31355e-abbf-4675-8700-9d167b194db1"
ZAAKTYPE_CMMN_TEST_2_BETROKKENE_PLAATSVERVANGER = "74799b20-0350-457d-8773-a0f1ab16b299"
```

**Resultaattype:**
```kotlin
RESULTAAT_TYPE_GEWEIGERD_UUID = "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6"
```

**Zaak beëindig reasons** (used when closing a zaak):
```kotlin
ZAAK_BEEINDIG_VERZOEK_IS_DOOR_INITIATOR_INGETROKKEN_ID   = "-1"
ZAAK_BEEINDIG_ZAAK_IS_EEN_DUPLICAAT_ID                   = "-2"
ZAAK_BEEINDIG_VERZOEK_IS_BIJ_VERKEERDE_ORGANISATIE_ID    = "-3"
```

**Uris / external services:**
```kotlin
ZAC_API_URI              = "http://localhost:8080/rest"
ZAC_WEBSOCKET_BASE_URI   = "ws://localhost:8080/websocket"
GREENMAIL_API_URI        = "http://localhost:18083/api"    // for email assertions
OPEN_ZAAK_BASE_URI       = "http://openzaak.local:8000"   // internal Docker network
OPEN_ZAAK_EXTERNAL_URI   = "http://localhost:8001"        // from test host
BRP_WIREMOCK_API         = "http://localhost:18084/__admin" // BRP stub admin
```

---

---

# BPMN Configuration

> Read when user answers "BPMN". Everything you need to write BPMN tests without reading source files.

## Package
`nl.info.zac.itest.bpmn`

## Zaaktype → process definition mapping

**Before writing any BPMN test, check this table to find the correct zaaktype for the process under test.** Each process definition is pre-configured in `ZacItestProjectConfig` for exactly one zaaktype — using the wrong one means the delegate will never trigger.

| Zaaktype constant | Process definition key | Process description |
|---|---|---|
| `ZAAKTYPE_BPMN_TEST_1_UUID` | `BPMN_TEST_PROCESS_DEFINITION_KEY` | General integration test process |
| `ZAAKTYPE_BPMN_TEST_2_UUID` | `BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY` | User management process |
| `ZAAKTYPE_BPMN_TEST_3_UUID` | `BPMN_DOCUMENT_SIGN_PROCESS_DEFINITION_KEY` | Send confirmation email & sign documents |
| `ZAAKTYPE_BPMN_TEST_4_UUID` | `BPMN_SUSPEND_RESUME_PROCESS_DEFINITION_KEY` | Suspend / resume / extend |
| `ZAAKTYPE_BPMN_TEST_5_UUID` | `BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY` | Permission check process |

## Zaaktype constants (from `ItestConfiguration`)
| Constant | Value |
|---|---|
| `ZAAKTYPE_BPMN_TEST_1_UUID` | `26076928-ce07-4d5d-8638-c2d276f6caca` |
| `ZAAKTYPE_BPMN_TEST_1_DESCRIPTION` | `"BPMN test zaaktype 1"` |
| `ZAAKTYPE_BPMN_TEST_2_UUID` | `7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e` |
| `ZAAKTYPE_BPMN_TEST_2_DESCRIPTION` | `"BPMN test zaaktype 2"` |
| `ZAAKTYPE_BPMN_TEST_3_UUID` | `e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c` |
| `ZAAKTYPE_BPMN_TEST_3_DESCRIPTION` | `"BPMN test zaaktype 3"` |
| `ZAAKTYPE_BPMN_TEST_4_UUID` | `f5a7b8c9-d0e1-2345-f012-345678901bcd` |
| `ZAAKTYPE_BPMN_TEST_4_DESCRIPTION` | `"BPMN test zaaktype 4"` |
| `ZAAKTYPE_BPMN_TEST_5_UUID` | `7941c3b9-e4a2-4444-b2cb-c211f035cecd` |
| `ZAAKTYPE_BPMN_TEST_5_DESCRIPTION` | `"BPMN test zaaktype 5"` |

Most BPMN tests use `GROUP_BEHANDELAARS_TEST_1` and `BEHANDELAAR_1`.

## BPMN process definitions (from `ItestConfiguration`)
| Constant | Value | Resource path |
|---|---|---|
| `BPMN_TEST_PROCESS_DEFINITION_KEY` | `"itProcessDefinition"` | `bpmn/itProcessDefinition.bpmn` |
| `BPMN_DOCUMENT_SIGN_PROCESS_DEFINITION_KEY` | `"sendConfirmationEmailAndSignDocumentsProcess"` | `bpmn/document-sign/...` |
| `BPMN_SUSPEND_RESUME_PROCESS_DEFINITION_KEY` | `"suspendResume"` | `bpmn/suspend-resume-extend/...` |
| `BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY` | `"userManagement"` | `bpmn/user-management/...` |

## Standard task names
```kotlin
BPMN_TEST_TASK_NAME           = "Test"
BPMN_SUMMARY_TASK_NAME        = "Summary"
BPMN_DOCUMENT_SIGN_SELECT_TASK_NAME   = "Select documents to sign"
BPMN_DOCUMENT_SIGN_SUMMARY_TASK_NAME  = "Summary of selected documents to sign"
BPMN_SUSPEND_RESUME_SUSPEND_TASK_NAME = "Suspend parameters"
BPMN_SUSPEND_RESUME_RESUME_TASK_NAME  = "Resume parameters"
BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME  = "Extend parameters"
BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME       = "Zaak defaults"
BPMN_USER_MANAGEMENT_HARDCODED_TASK_NAME     = "Hard coded"
BPMN_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_TASK_NAME = "New zaak defaults"
BPMN_USER_MANAGEMENT_COPY_FUNCTIONS_TASK_NAME    = "Copy user and group"
```

## Creating a BPMN zaak

The response body has the UUID and identification inside `zaakdata` — **not** at the root like CMMN.

**Preferred: destructuring** (when both values are available at the same point):
```kotlin
val (bpmnZaakUuid, zaakIdentificatie) = zacClient.createZaak(
    zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
    groupId = GROUP_BEHANDELAARS_TEST_1.name,
    groupName = GROUP_BEHANDELAARS_TEST_1.description,
    startDate = DATE_TIME_2000_01_01,
    testUser = BEHANDELAAR_1
).run {
    code shouldBe HTTP_OK
    JSONObject(bodyAsString).getJSONObject("zaakdata").run {
        UUID.fromString(getString("zaakUUID")) to getString("zaakIdentificatie")
    }
}
```

**Alternative: `var` + `lateinit`** (when variables must be declared outside the block for later nesting):
```kotlin
var bpmnZaakUuid: UUID = UUID.randomUUID()
var zaakIdentificatie = ""
zacClient.createZaak(
    zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
    groupId = GROUP_BEHANDELAARS_TEST_1.name,
    groupName = GROUP_BEHANDELAARS_TEST_1.description,
    startDate = DATE_TIME_2000_01_01,
    testUser = BEHANDELAAR_1
).run {
    code shouldBe HTTP_OK
    JSONObject(bodyAsString).getJSONObject("zaakdata").run {
        bpmnZaakUuid = UUID.fromString(getString("zaakUUID"))
        zaakIdentificatie = getString("zaakIdentificatie")
    }
}
```

## Adding an initiator (BSN)
```kotlin
val temporaryPersonId: UUID = zacClient.getTemporaryPersonId(TEST_PERSON_HENDRIKA_JANSE_BSN, BEHANDELAAR_1)

itestHttpClient.performPatchRequest(
    url = "$ZAC_API_URI/zaken/initiator",
    requestBodyAsString = """
        {
            "betrokkeneIdentificatie": {
                "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                "temporaryPersonId": "$temporaryPersonId",
                "type": "$BETROKKENE_IDENTIFICATION_TYPE_BSN"
            },
            "zaakUUID": "$bpmnZaakUuid"
        }
    """.trimIndent(),
    testUser = BEHANDELAAR_1
)
```

## Submitting form data (task completion)
```kotlin
val response = zacClient.submitFormData(
    bpmnZaakUuid = bpmnZaakUuid,
    taakData = """
        {
            "zaakIdentificatie": "$zaakIdentificatie",
            "initiator": null,
            "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
            "firstName": "Name",
            "AM_TeamBehandelaar_Groep": "${GROUP_COORDINATORS_TEST_1.name}",
            "AM_TeamBehandelaar_Medewerker": "${COORDINATOR_1.username}",
            "SD_SmartDocuments_Template": "OpenZaakTest",
            "SD_SmartDocuments_Create": false,
            "RT_ReferenceTable_Values": "Post",
            "ZK_Result": "Verleend",
            "ZK_Status": "Afgerond"
        }
    """.trimIndent(),
    testUser = BEHANDELAAR_1
)
JSONObject(response).getString("status") shouldBe "AFGEROND"
```

## Polling for next process task (always use `eventually`)
```kotlin
// Wait for task to disappear after completion:
eventually(10.seconds) {
    JSONObject(zacClient.searchForTasks(zaakIdentificatie, BPMN_TEST_TASK_NAME, BEHANDELAAR_1))
        .getInt("totaal") shouldBe 0
}

// Wait for next task to appear:
eventually(afterThirtySeconds) {
    JSONObject(zacClient.searchForTasks(zaakIdentificatie, BPMN_SUMMARY_TASK_NAME, BEHANDELAAR_1))
        .getInt("totaal") shouldBe 1
}
```

## BPMN form fields reference
| JSON key | Purpose | Example value |
|---|---|---|
| `AM_TeamBehandelaar_Groep` | Group to assign task to | `GROUP_COORDINATORS_TEST_1.name` |
| `AM_TeamBehandelaar_Medewerker` | User to assign task to | `COORDINATOR_1.username` |
| `SD_SmartDocuments_Template` | SmartDocuments template | `"OpenZaakTest"` |
| `SD_SmartDocuments_Create` | Whether to create a document | `false` |
| `RT_ReferenceTable_Values` | Reference table value | `"Post"` |
| `ZK_Result` | Zaak result to set | `"Verleend"` |
| `ZK_Status` | Zaak status to set | `"Afgerond"` |
| `TF_EMAIL_TO` | Email recipient (confirmation flow) | `"test@example.com"` |

## BPMN test assignee groups (TestGroup)
```kotlin
// Both are TestGroup values defined in TestGroups.kt and referenced in BPMN process files:
BPMN_TEST_GROUP_1        // name: "test-group-1"
BPMN_TEST_BEHANDELAAR_1  // name: "test-behandelaar-1"
```

---

---

# CMMN Configuration

> Read when user answers "CMMN". CMMN cases have no fixed process — the flow is event/case-driven, tasks are created on-demand.

## Package
`nl.info.zac.itest`

## Zaaktype constants (from `ItestConfiguration`)
| Constant | Value | Notes |
|---|---|---|
| `ZAAKTYPE_CMMN_TEST_1_UUID` | `8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a` | Only works with `BEHEERDER_1` — returns 403 for `BEHANDELAAR_*` and `RAADPLEGER_*` |
| `ZAAKTYPE_CMMN_TEST_1_DESCRIPTION` | `"Test zaaktype 1"` | |
| `ZAAKTYPE_CMMN_TEST_2_UUID` | `fd2bf643-c98a-4b00-b2b3-9ae0c41ed425` | **Default for most tests** |
| `ZAAKTYPE_CMMN_TEST_2_DESCRIPTION` | `"Test zaaktype 2"` | |
| `ZAAKTYPE_CMMN_TEST_3_UUID` | `448356ff-dcfb-4504-9501-7fe929077c4f` | Use when test 2 is already in use |
| `ZAAKTYPE_CMMN_TEST_3_DESCRIPTION` | `"Test zaaktype 3"` | |

Most CMMN tests use `ZAAKTYPE_CMMN_TEST_2_UUID` with `GROUP_BEHANDELAARS_TEST_1`. Avoid `ZAAKTYPE_CMMN_TEST_1_UUID` when using `BEHANDELAAR_*` or `RAADPLEGER_*` users — it will return 403. It works with `BEHEERDER_1`.

## Creating a CMMN zaak
The CMMN response has `"uuid"` and `"identificatie"` at the root **and** also inside a nested `"zaakdata"` map (as `"zaakUUID"`). Both approaches work — prefer root-level access:
```kotlin
lateinit var zaakUuid: String
lateinit var zaakIdentificatie: String
zacClient.createZaak(
    zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
    groupId = GROUP_BEHANDELAARS_TEST_1.name,
    groupName = GROUP_BEHANDELAARS_TEST_1.description,
    startDate = DATE_TIME_2000_01_01,
    testUser = BEHANDELAAR_1
).run {
    logger.info { "Response: $bodyAsString" }
    code shouldBe HTTP_OK
    JSONObject(bodyAsString).run {
        zaakUuid = getString("uuid")
        zaakIdentificatie = getString("identificatie")
    }
}
```

## CMMN zaak lifecycle — intake, complete, re-open

```kotlin
// Get user event listener plan items (for intake / afhandelen actions)
val planItems = itestHttpClient.performGetRequest(
    url = "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems",
    testUser = RECORDMANAGER_1
)
val planItemId = JSONArray(planItems.bodyAsString).getJSONObject(0).getString("id").toInt()

sleepForOpenZaakUniqueConstraint(1)  // required before each status change

// Finish intake phase (INTAKE_AFRONDEN) — marks zaak as admissible
itestHttpClient.performJSONPostRequest(
    url = "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
    requestBodyAsString = """
        {
            "zaakUuid": "$zaakUUID",
            "planItemInstanceId": "$planItemId",
            "actie": "$ACTIE_INTAKE_AFRONDEN",
            "zaakOntvankelijk": true
        }
    """.trimIndent(),
    testUser = RECORDMANAGER_1
).run { code shouldBe HTTP_NO_CONTENT }

sleepForOpenZaakUniqueConstraint(1)  // always between consecutive status changes

// Close zaak (ZAAK_AFHANDELEN) — first get a resultaattype UUID
val resultaatTypeUuid = JSONArray(
    itestHttpClient.performGetRequest(
        url = "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_CMMN_TEST_2_UUID",
        testUser = RECORDMANAGER_1
    ).bodyAsString
).getJSONObject(0).getString("id").let(UUID::fromString)

itestHttpClient.performJSONPostRequest(
    url = "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
    requestBodyAsString = """
        {
            "zaakUuid": "$zaakUUID",
            "planItemInstanceId": "$afhandelenId",
            "actie": "$ACTIE_ZAAK_AFHANDELEN",
            "resultaattypeUuid": "$resultaatTypeUuid",
            "resultaatToelichting": "fakeReason"
        }
    """.trimIndent(),
    testUser = RECORDMANAGER_1
).run { code shouldBe HTTP_NO_CONTENT }

// Re-open a closed zaak
itestHttpClient.performPatchRequest(
    url = "$ZAC_API_URI/zaken/zaak/$zaakUUID/heropenen",
    requestBodyAsString = """{ "reden": "fakeReason" }""",
    testUser = RECORDMANAGER_1
).run { code shouldBe HTTP_NO_CONTENT }

// Close a re-opened zaak (use /afsluiten, not doUserEventListenerPlanItem)
itestHttpClient.performPatchRequest(
    url = "$ZAC_API_URI/zaken/zaak/$zaakUUID/afsluiten",
    requestBodyAsString = """{ "reden": "fakeReason", "resultaattypeUuid": "$resultaatTypeUuid" }""",
    testUser = RECORDMANAGER_1
).run { code shouldBe HTTP_NO_CONTENT }
```
Note: `RECORDMANAGER_1` is the user with rights to complete and close zaken.

## Adding an initiator (BSN) — same pattern as BPMN
```kotlin
val temporaryPersonId = zacClient.getTemporaryPersonId(TEST_PERSON_HENDRIKA_JANSE_BSN, BEHANDELAAR_1)
// then PATCH /zaken/initiator (same JSON as BPMN section)
```

## Starting a human task ("Aanvullende informatie")
```kotlin
val taskHelper = TaskHelper(zacClient)
val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
    zaakUuid = UUID.fromString(zaakUuid),
    zaakIdentificatie = zaakIdentificatie,
    fatalDate = LocalDate.now().plusDays(14),
    group = GROUP_BEHANDELAARS_TEST_1,
    waitForTaskToBeIndexed = true,
    testUser = BEHANDELAAR_1
)
```

## Completing a task
```kotlin
itestHttpClient.performPatchRequest(
    url = "$ZAC_API_URI/taken/complete/$taskId",
    requestBodyAsString = "{}",
    testUser = BEHANDELAAR_1
)
```

## Zaak status progression
CMMN: no fixed process — statuses set explicitly or by task completion.
Common status types:
```kotlin
STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE = "Wacht op aanvullende informatie"
ACTIE_INTAKE_AFRONDEN  = "INTAKE_AFRONDEN"
ACTIE_ZAAK_AFHANDELEN  = "ZAAK_AFHANDELEN"
```

## Checking zaak assignment
```kotlin
itestHttpClient.performGetRequest(
    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
    testUser = BEHANDELAAR_1
).bodyAsString.run {
    shouldContainJsonKeyValue("isOpen", true)
}
```

## Test person constants
```kotlin
TEST_PERSON_HENDRIKA_JANSE_BSN       = "999993896"
TEST_PERSON_HENDRIKA_JANSE_FULLNAME  = "Héndrika Janse"
TEST_PERSON_HENDRIKA_JANSE_EMAIL     = "hendrika.janse@example.com"
TEST_PERSON_ANITA_VAN_BUREN_BSN      = "999992958"
```

---

## Discovered Patterns

| Pattern | Detail | Found in |
|---|---|---|
| `ZAAKTYPE_CMMN_TEST_1_UUID` causes 403 for non-admin users | Returns 403 when `BEHANDELAAR_*` or `RAADPLEGER_*` calls `createZaak`. Works fine with `BEHEERDER_1`. Prefer `_2_` or `_3_` for standard test flows. | `CsvRestServiceTest`, `ZaakSuspendRestServiceTest` |
| CMMN zaak UUID is at root of response, not in `zaakdata` | `JSONObject(bodyAsString).getString("uuid")` — the `zaakdata` wrapper is BPMN-only | `ZaakRestServiceExtensionTest` |
| `redenOpschorting` in `RestZaak` suspend response | After `PATCH .../suspend`, the body contains `"redenOpschorting": "<reason>"` and `"isOpgeschort": true`. Use `shouldContainJsonKeyValue("redenOpschorting", reason)` to verify. | `ZaakSuspendRestServiceTest` |
| Absent fields after resume | After `PATCH .../resume`, `redenOpschorting` and `vanafDatumTijd` are removed from the response. Assert with `shouldNotContainJsonKey("redenOpschorting")` and `shouldNotContainJsonKey("vanafDatumTijd")`. | `ZaakSuspendRestServiceTest` |
| `@Suppress("MagicNumber")` on test class | Add when the test contains numeric literals (days, counts, status codes as raw ints). | Multiple test classes |
| Use productaanvraag notification when delegate runs at process start | If a BPMN service task (delegate) is the **first step** in the process (before any user task), `createZaak()` is too late to add an initiator — the delegate already ran synchronously. Instead send a `POST /notificaties` payload that triggers zaak creation with the initiator already embedded via the productaanvraag object. Use `createZaak()` only when the delegate runs **after** a user task. | `BpmnSendConfirmationEmailRestServiceTest` |
| Productaanvraag notification body | `POST $ZAC_API_URI/notificaties` with `Authorization: OPEN_NOTIFICATIONS_API_SECRET_KEY` header. Body: `kanaal=objecten`, `resource=object`, `actie=create`, `resourceUrl` and `hoofdObject` pointing to the productaanvraag object URL, `kenmerken.objectType` pointing to the objecttype URL. | `BpmnSendConfirmationEmailRestServiceTest` |
| GreenMail requests need no `testUser` | `itestHttpClient.performGetRequest(url = "$GREENMAIL_API_URI/user/$email/messages/")` — omit `testUser`. | `MailRestServiceTest`, `BpmnSendConfirmationEmailRestServiceTest` |
| Use `forAtLeastOne` for email assertions | Multiple test runs share GreenMail state. Use `(0 until mails.length()).map { mails.getJSONObject(it) }.forAtLeastOne { ... }` instead of checking a specific index. | `BpmnSendConfirmationEmailRestServiceTest` |
| Look up zaak by identification string | `GET $ZAC_API_URI/zaken/zaak/id/$IDENTIFICATION` — use when the UUID is unknown but the zaak identification (e.g. `ZAAK-2001-0000000001`) is known from the productaanvraag object. | `BpmnSendConfirmationEmailRestServiceTest` |
