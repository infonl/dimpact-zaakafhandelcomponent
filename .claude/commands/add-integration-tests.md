# Add Integration Tests (Kotlin / Kotest)

> If you discover an improvement to this spec file while writing a test, ask the user: "I noticed a pattern / gotcha that could be added to the spec file — want me to add it?"

---

## Step 0 — Ask before anything else

Ask both questions before doing anything else:

1. > "What type of integration test do you want to add? **CMMN** or **BPMN**?"
2. > "Which source file should the integration test be based on? (provide the filename or full path)"

Then read the matching configuration section at the bottom of this file before writing a single line of Kotlin.

---

## Workflow

| # | Step |
|---|---|
| 1 | Understand the feature — read the REST endpoint(s) under test, identify input/output, happy path, edge cases |
| 2 | Decide file location — BPMN: `src/itest/kotlin/nl/info/zac/itest/bpmn/`; CMMN and general: `src/itest/kotlin/nl/info/zac/itest/` — name the test class `<SourceClassName>Test` (e.g. `ZaakSuspendRestService` → `ZaakSuspendRestServiceTest`) |
| 3 | Write the test — see framework patterns below |
| 4 | Tell the user the run command (generated from the class name) and ask them to run it |

No PR step. The test is done when it compiles and the user has run it.

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
zacClient.submitFormData(bpmnZaakUuid = ..., taakData = """{ ... }""", testUser = ...)
zacClient.searchForTasks(zaakIdentificatie = ..., taskName = ..., testUser = ...)
zacClient.getTemporaryPersonId(bsn, testUser)   // needed before adding an initiator
```

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

## Zaaktype constants (from `ItestConfiguration`)
| Constant | Value |
|---|---|
| `ZAAKTYPE_BPMN_TEST_1_UUID` | `26076928-ce07-4d5d-8638-c2d276f6caca` |
| `ZAAKTYPE_BPMN_TEST_1_DESCRIPTION` | `"BPMN test zaaktype 1"` |
| `ZAAKTYPE_BPMN_TEST_2_UUID` | `7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e` |
| `ZAAKTYPE_BPMN_TEST_2_DESCRIPTION` | `"BPMN test zaaktype 2"` |
| `ZAAKTYPE_BPMN_TEST_3_UUID` | `e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c` |
| `ZAAKTYPE_BPMN_TEST_4_UUID` | `f5a7b8c9-d0e1-2345-f012-345678901bcd` |

Most BPMN tests use `ZAAKTYPE_BPMN_TEST_1_UUID` with `GROUP_BEHANDELAARS_TEST_1`.

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
```kotlin
var bpmnZaakUuid: UUID
var zaakIdentificatie: String
zacClient.createZaak(
    zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
    groupId = GROUP_BEHANDELAARS_TEST_1.name,
    groupName = GROUP_BEHANDELAARS_TEST_1.description,
    startDate = DATE_TIME_2000_01_01,    // use a fixed date for reproducibility
    testUser = BEHANDELAAR_1
).run {
    code shouldBe HTTP_OK
    JSONObject(bodyAsString).getJSONObject("zaakdata").run {
        bpmnZaakUuid = getString("zaakUUID").let(UUID::fromString)
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
| Constant | Value |
|---|---|
| `ZAAKTYPE_CMMN_TEST_1_UUID` | `8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a` |
| `ZAAKTYPE_CMMN_TEST_1_DESCRIPTION` | `"Test zaaktype 1"` |
| `ZAAKTYPE_CMMN_TEST_2_UUID` | `fd2bf643-c98a-4b00-b2b3-9ae0c41ed425` |
| `ZAAKTYPE_CMMN_TEST_2_DESCRIPTION` | `"Test zaaktype 2"` |
| `ZAAKTYPE_CMMN_TEST_3_UUID` | `448356ff-dcfb-4504-9501-7fe929077c4f` |
| `ZAAKTYPE_CMMN_TEST_3_DESCRIPTION` | `"Test zaaktype 3"` |

## Creating a CMMN zaak
```kotlin
var zaakUUID: UUID
var zaakIdentificatie: String
zacClient.createZaak(
    zaakTypeUUID = ZAAKTYPE_CMMN_TEST_1_UUID,
    groupId = GROUP_BEHANDELAARS_TEST_1.name,
    groupName = GROUP_BEHANDELAARS_TEST_1.description,
    startDate = DATE_TIME_2000_01_01,
    testUser = BEHANDELAAR_1
).run {
    code shouldBe HTTP_OK
    JSONObject(bodyAsString).getJSONObject("zaakdata").run {
        zaakUUID = getString("zaakUUID").let(UUID::fromString)
        zaakIdentificatie = getString("zaakIdentificatie")
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

_Add patterns here as they are found during test writing._

| Pattern | Detail | Found in |
|---|---|---|
| (none yet) | | |
