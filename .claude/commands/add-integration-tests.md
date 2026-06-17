# Add Integration Tests (Kotlin / Kotest)

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

## One `Given` vs multiple `Given` blocks

| Situation | Structure |
|---|---|
| Scenarios **depend on shared state** or must run in a fixed sequence (e.g. create → suspend → resume → check) | One `Given`, deeply nested `When`/`And` blocks. Variables declared with `val (a, b) = ...` or `var` outside the `When`. |
| Scenarios are **independent** — each needs its own fresh zaak or different preconditions | Separate `Given` blocks, each with its own setup. |

**Nested `And` inside `When` = the previous step already happened.** Use this to chain sequential actions.
**Sibling `When` blocks inside the same `Given` = parallel scenarios sharing the same precondition but not each other's state.**

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

// DELETE
itestHttpClient.performDeleteRequest(url = "...", testUser = BEHANDELAAR_1)
```
All methods return `ResponseContent` with `.code` (Int) and `.bodyAsString` (String).

### Assertions
```kotlin
response.code shouldBe HTTP_OK                              // status code
response.bodyAsString.shouldContainJsonKeyValue("key", 42)  // exact JSON value
response.bodyAsString.shouldContainJsonKey("key")           // key present
response.bodyAsString.shouldNotContainJsonKey("key")        // key absent
JSONObject(response.bodyAsString).getString("id")           // parse manually
JSONArray(response.bodyAsString).length() shouldBe 3
```

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
zacClient.createZaak(zaakTypeUUID = ..., groupId = ..., groupName = ..., startDate = ..., testUser = ...)
zacClient.retrieveZaak(zaakUUID, testUser)
zacClient.submitFormData(bpmnZaakUuid = ..., taakData = """{ ... }""", testUser = ...)   // returns String (body)
zacClient.submitFormDataRaw(bpmnZaakUuid = ..., taakData = """{ ... }""", testUser = ...) // returns ResponseContent (code + body)
zacClient.searchForTasks(zaakIdentificatie = ..., taskName = ..., testUser = ...)
zacClient.getTemporaryPersonId(bsn, testUser)   // needed before adding an initiator
zacClient.createEnkelvoudigInformatieobjectForZaak(
    zaakUUID = ..., fileName = ..., fileMediaType = ...,
    vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR, testUser = ...
)  // returns ResponseContent; parse UUID with JSONObject(bodyAsString).getString("uuid")
zacClient.getHumanTaskPlanItemsForZaak(zaakUUID, testUser)  // GET /planitems/zaak/{uuid}/humanTaskPlanItems
zacClient.startHumanTaskPlanItem(planItemId = ..., fatalDate = ..., group = ..., testUser = ...)
```

### Higher-level helper classes (prefer over raw ZacClient when they fit)
These wrap ZacClient and add search-index polling so the test doesn't need its own `eventually` for indexing.
```kotlin
// ZaakHelper — creates zaak and waits until it is findable in the search index
val zaakHelper = ZaakHelper(zacClient)
val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(zaaktypeUuid = ..., testUser = ...)

// DocumentHelper — uploads a document and waits until it is indexed
val documentHelper = DocumentHelper(zacClient)
val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
    zaakUuid = ..., documentTitle = ..., authorName = ...,
    mediaType = ..., fileName = ..., testUser = ...
)

// TaskHelper — starts a human task and optionally waits for it to be indexed
val taskHelper = TaskHelper(zacClient)
val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
    zaakUuid = ..., zaakIdentificatie = ..., fatalDate = ...,
    group = ..., waitForTaskToBeIndexed = true, testUser = ...
)
```
Use raw `ZacClient` calls when you do not need indexing (e.g. BPMN form submission).

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
| Val | Username | Role |
|---|---|---|
| `BEHANDELAAR_1` | behandelaar1newiam | Behandelaar domein test 1 |
| `BEHANDELAAR_2` | behandelaar2newiam | Behandelaar domein test 2 |
| `COORDINATOR_1` | coordinator1newiam | Coordinator domein test 1 |
| `COORDINATOR_2` | coordinator2newiam | Coordinator domein test 2 |
| `BEHEERDER_1` | beheerder1newiam | Beheerder all domeinen |
| `RAADPLEGER_1` | raadpleger1newiam | Raadpleger domein test 1 |
| `RECORDMANAGER_1` | recordmanager1newiam | Recordmanager domein test 1 |

### All available test groups (`src/itest/.../config/TestGroups.kt`)
| Val | name | description |
|---|---|---|
| `GROUP_BEHANDELAARS_TEST_1` | behandelaars-test-1 | Test group behandelaars domein test 1 |
| `GROUP_BEHANDELAARS_TEST_2` | behandelaars-test-2 | Test group behandelaars domein test 2 |
| `GROUP_COORDINATORS_TEST_1` | coordinators-test-1 | Test group coordinators domein test 1 |
| `GROUP_COORDINATORS_TEST_2` | coordinators-test-2 | Test group coordinators domein test 2 |
| `GROUP_BEHEERDERS_ELK_DOMEIN` | beheerders-elk-domein | Test group beheerders elk domein |

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

## BPMN test persons / BPMN test group
```kotlin
// BPMN test group defined in bpmn process files + TestGroups.kt:
BPMN_TEST_GROUP_1     // name: "test-group-1"
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
| `ZAAKTYPE_CMMN_TEST_1_UUID` | `8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a` | **Do NOT use for `createZaak` — returns 403** |
| `ZAAKTYPE_CMMN_TEST_1_DESCRIPTION` | `"Test zaaktype 1"` | |
| `ZAAKTYPE_CMMN_TEST_2_UUID` | `fd2bf643-c98a-4b00-b2b3-9ae0c41ed425` | **Default for most tests** |
| `ZAAKTYPE_CMMN_TEST_2_DESCRIPTION` | `"Test zaaktype 2"` | |
| `ZAAKTYPE_CMMN_TEST_3_UUID` | `448356ff-dcfb-4504-9501-7fe929077c4f` | Use when test 2 is already in use |
| `ZAAKTYPE_CMMN_TEST_3_DESCRIPTION` | `"Test zaaktype 3"` | |

Most CMMN tests use `ZAAKTYPE_CMMN_TEST_2_UUID` with `GROUP_BEHANDELAARS_TEST_1`. **Never use `ZAAKTYPE_CMMN_TEST_1_UUID` in `createZaak`** — it is not authorized for test users and will return 403.

## Creating a CMMN zaak
The response body is a flat `RestZaak` JSON object (root-level `"uuid"`) — **not** wrapped in `zaakdata` like BPMN.
```kotlin
lateinit var zaakUuid: String
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
    }
}
```

## Adding an initiator (BSN) — same pattern as BPMN
```kotlin
val temporaryPersonId = zacClient.getTemporaryPersonId(TEST_PERSON_HENDRIKA_JANSE_BSN, BEHANDELAAR_1)
// then PATCH /zaken/initiator (same JSON as BPMN section)
```

## Starting a human task ("Aanvullende informatie")
```kotlin
val taskHelper = TaskHelper(zacClient)
val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
    zaakUuid = zaakUUID,
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
    url = "$ZAC_API_URI/zaken/zaak/$zaakUUID",
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
| `ZAAKTYPE_CMMN_TEST_1_UUID` causes 403 on `createZaak` | Only `_2_` and `_3_` are authorized for test users. Never use `_1_` in `createZaak`. | All CMMN tests |
| CMMN zaak UUID is at root of response, not in `zaakdata` | `JSONObject(bodyAsString).getString("uuid")` — the `zaakdata` wrapper is BPMN-only | `ZaakRestServiceExtensionTest` |
| `redenOpschorting` in `RestZaak` suspend response | After `PATCH .../suspend`, the body contains `"redenOpschorting": "<reason>"` and `"isOpgeschort": true`. Use `shouldContainJsonKeyValue("redenOpschorting", reason)` to verify. | `ZaakSuspendRestServiceTest` |
| Absent fields after resume | After `PATCH .../resume`, `redenOpschorting` and `vanafDatumTijd` are removed from the response. Assert with `shouldNotContainJsonKey("redenOpschorting")` and `shouldNotContainJsonKey("vanafDatumTijd")`. | `ZaakSuspendRestServiceTest` |
| `@Suppress("MagicNumber")` on test class | Add when the test contains numeric literals (days, counts, status codes as raw ints). | Multiple test classes |
| Use productaanvraag notification when delegate runs at process start | If a BPMN service task (delegate) is the **first step** in the process (before any user task), `createZaak()` is too late to add an initiator — the delegate already ran synchronously. Instead send a `POST /notificaties` payload that triggers zaak creation with the initiator already embedded via the productaanvraag object. Use `createZaak()` only when the delegate runs **after** a user task. | `BpmnSendConfirmationEmailRestServiceTest` |
| Productaanvraag notification body | `POST $ZAC_API_URI/notificaties` with `Authorization: OPEN_NOTIFICATIONS_API_SECRET_KEY` header. Body: `kanaal=objecten`, `resource=object`, `actie=create`, `resourceUrl` and `hoofdObject` pointing to the productaanvraag object URL, `kenmerken.objectType` pointing to the objecttype URL. | `BpmnSendConfirmationEmailRestServiceTest` |
| GreenMail requests need no `testUser` | `itestHttpClient.performGetRequest(url = "$GREENMAIL_API_URI/user/$email/messages/")` — omit `testUser`. | `MailRestServiceTest`, `BpmnSendConfirmationEmailRestServiceTest` |
| Use `forAtLeastOne` for email assertions | Multiple test runs share GreenMail state. Use `(0 until mails.length()).map { mails.getJSONObject(it) }.forAtLeastOne { ... }` instead of checking a specific index. | `BpmnSendConfirmationEmailRestServiceTest` |
| Look up zaak by identification string | `GET $ZAC_API_URI/zaken/zaak/id/$IDENTIFICATION` — use when the UUID is unknown but the zaak identification (e.g. `ZAAK-2001-0000000001`) is known from the productaanvraag object. | `BpmnSendConfirmationEmailRestServiceTest` |
