/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.config.RAADPLEGER_1
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.UUID

/**
 * Tests that BPMN process delegates return HTTP 403 Forbidden when called without the required permissions.
 */
class BpmnDelegateAuthorisationTest : BehaviorSpec({
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("Users attempt BPMN process actions without the required permissions") {
        var bpmnZaakUuid: UUID? = null
        var zaakIdentificatie: String? = null

        When("zaak is created for sendEmailAfterClosingZaak") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_BPMN_TEST_5_UUID,
                groupId = GROUP_BEHANDELAARS_TEST_1.name,
                groupName = GROUP_BEHANDELAARS_TEST_1.description,
                behandelaarId = BEHANDELAAR_1.username,
                behandelaarName = BEHANDELAAR_1.displayName,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_1
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

        When("the 'chooseTestProcess' form is submitted for sendEmailAfterClosingZaak") {
            val takenResponse = zacClient.submitFormDataRaw(
                bpmnZaakUuid = bpmnZaakUuid!!,
                taakData = """
                   {
                     "testProcess":"sendEmailAfterClosingZaak"
                   }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            )

            Then("process task returns forbidden") {
                takenResponse.code shouldBe HttpURLConnection.HTTP_FORBIDDEN
                logger.info { "Cannot complete task for zaak $zaakIdentificatie because it is forbidden" }
            }
        }

        When("zaak is created for resumeZaakWhichIsNotSuspended") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_BPMN_TEST_5_UUID,
                groupId = GROUP_BEHANDELAARS_TEST_1.name,
                groupName = GROUP_BEHANDELAARS_TEST_1.description,
                behandelaarId = BEHANDELAAR_1.username,
                behandelaarName = BEHANDELAAR_1.displayName,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_1
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

        When("the 'chooseTestProcess' form is submitted for resumeZaakWhichIsNotSuspended") {
            val takenResponse = zacClient.submitFormDataRaw(
                bpmnZaakUuid = bpmnZaakUuid!!,
                taakData = """
                   {
                     "testProcess":"resumeZaakWhichIsNotSuspended"
                   }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            )

            Then("process task returns forbidden") {
                takenResponse.code shouldBe HttpURLConnection.HTTP_FORBIDDEN
                logger.info { "Cannot complete task for zaak $zaakIdentificatie because it is forbidden" }
            }
        }

        When("zaak is created for assignZaak") {
            val response = zacClient.createZaak(
                zaakTypeUUID = ZAAKTYPE_BPMN_TEST_5_UUID,
                groupId = GROUP_BEHANDELAARS_TEST_1.name,
                groupName = GROUP_BEHANDELAARS_TEST_1.description,
                behandelaarId = BEHANDELAAR_1.username,
                behandelaarName = BEHANDELAAR_1.displayName,
                startDate = DATE_TIME_2000_01_01,
                testUser = BEHANDELAAR_1
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

        When("the 'chooseTestProcess' form is submitted for assignZaak as a raadpleger") {
            val takenResponse = zacClient.submitFormDataRaw(
                bpmnZaakUuid = bpmnZaakUuid!!,
                taakData = """
                   {
                     "testProcess":"assignZaak"
                   }
                """.trimIndent(),
                testUser = RAADPLEGER_1
            )

            Then("process task returns forbidden") {
                takenResponse.code shouldBe HttpURLConnection.HTTP_FORBIDDEN
                logger.info { "Cannot complete task for zaak $zaakIdentificatie because it is forbidden" }
            }
        }
    }
})
