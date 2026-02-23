/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BPMN_TEST_GROUP_1
import nl.info.zac.itest.config.BPMN_TEST_GROUP_2
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_COPY_FUNCTIONS_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_HARDCODED_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGERS_DOMAIN_TEST_1
import nl.info.zac.itest.config.RECORDMANAGER_DOMAIN_TEST_1
import nl.info.zac.itest.config.TestUser
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates a zaak with a BPMN type.
 */
class BpmnUserGroupAssignTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}
    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

    suspend fun getTaskData(
        zaakIdentificatie: String,
        bpmnZaakUuid: UUID,
        taskName: String,
        testUser: TestUser,
    ): String {
        eventually(afterThirtySeconds) {
            val searchResponseBody = zacClient.searchForTasks(zaakIdentificatie, taskName, testUser)
            JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
        }
        val response = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/taken/zaak/$bpmnZaakUuid",
            testUser = testUser,
        )
        val responseBody = response.bodyAsString
        logger.info { "Response: $responseBody" }
        response.code shouldBe HttpURLConnection.HTTP_OK

        val tasksArray = JSONArray(responseBody)
        return tasksArray.firstOrNull { (it as JSONObject).getString("naam") == taskName }.toString()
    }

    Given("A behandelaar is logged in") {
        var bpmnZaakUuid: UUID? = null
        var zaakIdentificatie: String? = null

        When("zaak is created") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_BPMN_TEST_2_UUID,
                groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
                groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
                behandelaarId = BEHANDELAAR_DOMAIN_TEST_1.username,
                behandelaarName = BEHANDELAAR_DOMAIN_TEST_1.displayName,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("response is ok") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_OK
                JSONObject(responseBody).run {
                    getJSONObject("zaakdata").run {
                        bpmnZaakUuid = getString("zaakUUID").run(UUID::fromString)
                        zaakIdentificatie = getString("zaakIdentificatie")
                    }
                }
            }
        }

        When("the first user task is created") {
            val taskData = getTaskData(
                zaakIdentificatie = zaakIdentificatie!!,
                bpmnZaakUuid = bpmnZaakUuid!!,
                taskName = BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("task user and group are the zaak defaults") {
                // currently BPMN sets the behandelaar display name and group description fields in the task data
                // and not the respective id fields
                taskData shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "groep" : {
                    "id" : "${BEHANDELAARS_DOMAIN_TEST_1.description}",
                    "naam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                  },
                  "behandelaar" : {
                    "id" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                    "naam" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
                  }
                }
                """.trimIndent()
            }
        }

        When("the 'zaak defaults' form is submitted") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}", BEHANDELAAR_DOMAIN_TEST_1)

            Then("the next task is assigned a hard-coded user and group") {
                // currently BPMN sets the behandelaar display name and group description fields in the task data
                // and not the respective id fields
                getTaskData(
                    zaakIdentificatie = zaakIdentificatie!!,
                    bpmnZaakUuid = bpmnZaakUuid,
                    taskName = BPMN_USER_MANAGEMENT_HARDCODED_TASK_NAME,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "${BPMN_TEST_GROUP_1.description}",
                        "naam" : "${BPMN_TEST_GROUP_1.description}"
                      },
                      "behandelaar" : {
                        "id" : "${BPMN_TEST_GROUP_2.description}",
                        "naam" : "${BPMN_TEST_GROUP_2.description}"
                      }
                    }                    
                """.trimIndent()
            }
        }

        When("the 'hard-coded' and 'select user&group' forms are submitted") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}", BEHANDELAAR_DOMAIN_TEST_1)
            zacClient.submitFormData(
                bpmnZaakUuid = bpmnZaakUuid,
                taakData = """
                    {
                        "selectedGroup": "${RECORDMANAGERS_DOMAIN_TEST_1.name}",
                        "selectedUser": "${RECORDMANAGER_DOMAIN_TEST_1.username}",
                        "copyTaskUsesZaakDefaults": false
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the next task has the selected user and group assigned") {
                getTaskData(
                    zaakIdentificatie = zaakIdentificatie!!,
                    bpmnZaakUuid = bpmnZaakUuid,
                    taskName = BPMN_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_TASK_NAME,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "${RECORDMANAGERS_DOMAIN_TEST_1.description}",
                        "naam" : "${RECORDMANAGERS_DOMAIN_TEST_1.description}"
                      },
                      "behandelaar" : {
                        "id" : "${RECORDMANAGER_DOMAIN_TEST_1.displayName}",
                        "naam" : "${RECORDMANAGER_DOMAIN_TEST_1.displayName}"
                      }
                    }                    
                """.trimIndent()
            }
        }

        When("the copy user & group functions are used ") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}", BEHANDELAAR_DOMAIN_TEST_1)

            Then("the next task has the copied user and group assigned") {
                getTaskData(
                    zaakIdentificatie = zaakIdentificatie!!,
                    bpmnZaakUuid = bpmnZaakUuid,
                    taskName = BPMN_USER_MANAGEMENT_COPY_FUNCTIONS_TASK_NAME,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "${BPMN_TEST_GROUP_1.description}",
                        "naam" : "${BPMN_TEST_GROUP_1.description}"
                      },
                      "behandelaar" : {
                        "id": "${BEHANDELAAR_DOMAIN_TEST_1.username}",
                        "naam": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
                      }
                    }                    
                """.trimIndent()
            }
        }
    }
})
