/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_RESUME_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_SUSPEND_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for the suspend/resume BPMN process.
 * Creates a zaak, runs through the suspend-resume-extend process,
 * and verifies that the zaak suspension state changes correctly after
 * the SuspendZaakDelegate and ResumeZaakDelegate service tasks execute.
 */
class BpmnSuspendResumeExtendRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

    Given("A BPMN suspend-resume zaak exists") {
        val (zaakUuid, zaakIdentificatie) = zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_4_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).getJSONObject("zaakdata").run {
                UUID.fromString(getString("zaakUUID")) to getString("zaakIdentificatie")
            }
        }

        When("the suspend form is submitted with 5 suspend days") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = zaakUuid,
                taakData = """{ "suspendDays": 5 }""",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the suspend task is completed") {
                JSONObject(takenPatchResponse).getString("status") shouldBe "AFGEROND"
            }

            And("the suspend task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_SUSPEND_RESUME_SUSPEND_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }

            And("the resume task becomes available after the SuspendZaakDelegate executes") {
                eventually(afterThirtySeconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_SUSPEND_RESUME_RESUME_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                }
            }

            And("the zaak is suspended") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                ).run {
                    val responseBody = bodyAsString
                    logger.info { "Response: $responseBody" }
                    code shouldBe HTTP_OK
                    responseBody.shouldContainJsonKeyValue("isOpgeschort", true)
                }
            }

            When("the resume form is submitted") {
                val resumeTaskPatchResponse = zacClient.submitFormData(
                    bpmnZaakUuid = zaakUuid,
                    taakData = """{ "resumeDate": "2026-05-01T10:00:00+02:00" }""",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )

                Then("the resume task is completed") {
                    JSONObject(resumeTaskPatchResponse).getString("status") shouldBe "AFGEROND"
                }

                And("the extend task becomes available after the ResumeZaakDelegate executes") {
                    eventually(afterThirtySeconds) {
                        val searchResponseBody = zacClient.searchForTasks(
                            zaakIdentificatie = zaakIdentificatie,
                            taskName = BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME,
                            testUser = BEHANDELAAR_DOMAIN_TEST_1
                        )
                        JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                    }
                }

                And("the zaak is resumed") {
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    ).run {
                        val responseBody = bodyAsString
                        logger.info { "Response: $responseBody" }
                        code shouldBe HTTP_OK
                        responseBody.shouldContainJsonKeyValue("isOpgeschort", false)
                    }
                }

                When("the extend form is submitted with 5 extend days") {
                    val extendTaskPatchResponse = zacClient.submitFormData(
                        bpmnZaakUuid = zaakUuid,
                        taakData = """{ "extendDays": 5, "extendTasks": false }""",
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )

                    Then("the extend task is completed") {
                        JSONObject(extendTaskPatchResponse).getString("status") shouldBe "AFGEROND"
                    }

                    And("the extend task is removed from the task list") {
                        eventually(afterThirtySeconds) {
                            val searchResponseBody = zacClient.searchForTasks(
                                zaakIdentificatie = zaakIdentificatie,
                                taskName = BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME,
                                testUser = BEHANDELAAR_DOMAIN_TEST_1
                            )
                            JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                        }
                    }
                }
            }
        }
    }
})
