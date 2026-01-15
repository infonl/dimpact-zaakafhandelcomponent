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
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_COPY_FUNCTIONS_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_HARDCODED_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_USER_GROUP_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
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

    suspend fun getTaskData(zaakIdentificatie: String, bpmnZaakUuid: UUID, taskName: String): String {
        eventually(afterThirtySeconds) {
            val searchResponseBody = zacClient.searchForTasks(zaakIdentificatie, taskName)
            JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
        }
        val response = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/taken/zaak/$bpmnZaakUuid"
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

        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        When("zaak is created") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_BPMN_TEST_2_UUID,
                groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
                groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
                behandelaarId = BEHANDELAAR_DOMAIN_TEST_1.username,
                startDate = DATE_TIME_2000_01_01
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
            val taskData = getTaskData(zaakIdentificatie!!, bpmnZaakUuid!!, BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME)

            Then("task user and group are the zaak defaults") {
                taskData shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "groep" : {
                    "id" : "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                    "naam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                  }               
                }
                """.trimIndent()
            }
        }

        When("the 'zaak defaults' form is submitted") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}")

            Then("the next task is assigned a hard-coded user and group") {
                getTaskData(
                    zaakIdentificatie!!,
                    bpmnZaakUuid,
                    BPMN_USER_MANAGEMENT_HARDCODED_TASK_NAME
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "Superheroes",
                        "naam" : "Superheroes"
                      },
                      "behandelaar" : {
                        "id" : "Invisible Man",
                        "naam" : "Invisible Man"
                      }
                    }                    
                """.trimIndent()
            }
        }

        When("the 'hard-coded' and 'select user&group' forms are submitted") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}")
            zacClient.submitFormData(
                bpmnZaakUuid,
                """
                {
                    "selectedGroup": "recordmanagers-test-1",
                    "selectedUser": "recordmanager1newiam"
                }
                """.trimIndent()
            )

            Then("the next task has the selected user and group assigned") {
                getTaskData(
                    zaakIdentificatie!!,
                    bpmnZaakUuid,
                    BPMN_USER_MANAGEMENT_USER_GROUP_TASK_NAME
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "recordmanagers-test-1",
                        "naam" : "recordmanagers-test-1"
                      },
                      "behandelaar" : {
                        "id" : "recordmanager1newiam",
                        "naam" : "Test Recordmanager 1 - new IAM"
                      }
                    }                    
                """.trimIndent()
            }
        }

        When("the new zaak defaults are set by service task") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}")

            Then("the next task has the selected user and group assigned") {
                getTaskData(
                    zaakIdentificatie!!,
                    bpmnZaakUuid,
                    BPMN_USER_MANAGEMENT_NEW_ZAAK_DEFAULTS_TASK_NAME
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "recordmanagers-test-1",
                        "naam" : "recordmanagers-test-1"
                      },
                      "behandelaar" : {
                        "id" : "Test Recordmanager 1 - new IAM",
                        "naam" : "Test Recordmanager 1 - new IAM"
                      }
                    }                    
                """.trimIndent()
            }
        }

        When("the copy user & group functions are used ") {
            zacClient.submitFormData(bpmnZaakUuid!!, "{}")

            Then("the next task has the copied user and group assigned") {
                val behandelaar = if (FEATURE_FLAG_PABC_INTEGRATION) {
                    """
                      "behandelaar" : {
                        "id" : "behandelaar1newiam",
                        "naam" : "Test Behandelaar 1 - new IAM"
                      }
                    """.trimIndent()
                } else {
                    """
                      "behandelaar" : {
                        "id" : "behandelaar1",
                        "naam" : "Test Behandelaar1"
                      }
                    """.trimIndent()
                }

                getTaskData(
                    zaakIdentificatie!!,
                    bpmnZaakUuid,
                    BPMN_USER_MANAGEMENT_COPY_FUNCTIONS_TASK_NAME
                ) shouldEqualJsonIgnoringOrderAndExtraneousFields """
                    {
                      "groep" : {
                        "id" : "Superheroes",
                        "naam" : "Superheroes"
                      },
                      $behandelaar
                    }                    
                """.trimIndent()
            }
        }
    }
})
