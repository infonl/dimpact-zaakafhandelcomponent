/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_RESUME_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_SUSPEND_RESUME_SUSPEND_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_DOORLOOPTIJD_DAYS
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_SERVICENORM_DAYS
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.time.ZonedDateTime
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
    val suspendDays = 5L
    val extendDays = 5L
    val dueDate = DATE_2000_01_01.plusDays(ZAAKTYPE_BPMN_TEST_4_SERVICENORM_DAYS)
    val fatalDate = DATE_2000_01_01.plusDays(ZAAKTYPE_BPMN_TEST_4_DOORLOOPTIJD_DAYS)

    fun retrieveZaak(zaakUuid: UUID): JSONObject =
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody)
        }

    fun JSONObject.shouldHaveDueDateAndFatalDate(expectedDueDate: LocalDate, expectedFatalDate: LocalDate) {
        getString("einddatumGepland") shouldBe expectedDueDate.toString()
        getString("uiterlijkeEinddatumAfdoening") shouldBe expectedFatalDate.toString()
    }

    given("A BPMN suspend-resume zaak exists") {
        val (zaakUuid, zaakIdentificatie) = zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_4_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).getJSONObject("zaakdata").run {
                UUID.fromString(getString("zaakUUID")) to getString("zaakIdentificatie")
            }
        }

        `when`("the zaak is retrieved") {
            val zaak = retrieveZaak(zaakUuid)

            then("the zaaktype has a servicenorm and the due date is the start date plus the servicenorm") {
                zaak.getJSONObject("zaaktype").getBoolean("hasServicenorm") shouldBe true
                zaak.shouldHaveDueDateAndFatalDate(dueDate, fatalDate)
            }
        }

        `when`("the suspend form is submitted with $suspendDays suspend days") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = zaakUuid,
                taakData = """{ "suspendDays": $suspendDays }""",
                testUser = BEHANDELAAR_1
            )

            then("the suspend task is completed") {
                JSONObject(takenPatchResponse).getString("status") shouldBe "AFGEROND"
            }

            and("the suspend task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_SUSPEND_RESUME_SUSPEND_TASK_NAME,
                        testUser = BEHANDELAAR_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }

            and("the resume task becomes available after the SuspendZaakDelegate executes") {
                eventually(afterThirtySeconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_SUSPEND_RESUME_RESUME_TASK_NAME,
                        testUser = BEHANDELAAR_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                }
            }

            and("the zaak is suspended and its due date and fatal date are postponed by the suspend days") {
                with(retrieveZaak(zaakUuid)) {
                    getBoolean("isOpgeschort") shouldBe true
                    shouldHaveDueDateAndFatalDate(dueDate.plusDays(suspendDays), fatalDate.plusDays(suspendDays))
                }
            }

            and("the resume form is submitted with a resume date set to now") {
                val resumeDate = ZonedDateTime.now()
                val resumeTaskPatchResponse = zacClient.submitFormData(
                    bpmnZaakUuid = zaakUuid,
                    taakData = """{ "resumeDate": "$resumeDate" }""",
                    testUser = BEHANDELAAR_1
                )

                then("the resume task is completed") {
                    JSONObject(resumeTaskPatchResponse).getString("status") shouldBe "AFGEROND"
                }

                and("the extend task becomes available after the ResumeZaakDelegate executes") {
                    eventually(afterThirtySeconds) {
                        val searchResponseBody = zacClient.searchForTasks(
                            zaakIdentificatie = zaakIdentificatie,
                            taskName = BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME,
                            testUser = BEHANDELAAR_1
                        )
                        JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                    }
                }

                and(
                    """
                    the zaak is resumed and, because it was resumed on the day it was suspended,
                    its due date and fatal date are back at their original values
                    """
                ) {
                    with(retrieveZaak(zaakUuid)) {
                        getBoolean("isOpgeschort") shouldBe false
                        shouldHaveDueDateAndFatalDate(dueDate, fatalDate)
                    }
                }

                and("the extend form is submitted with $extendDays extend days") {
                    val extendTaskPatchResponse = zacClient.submitFormData(
                        bpmnZaakUuid = zaakUuid,
                        taakData = """{ "extendDays": $extendDays, "extendTasks": false }""",
                        testUser = BEHANDELAAR_1
                    )

                    then("the extend task is completed") {
                        JSONObject(extendTaskPatchResponse).getString("status") shouldBe "AFGEROND"
                    }

                    and("the extend task is removed from the task list") {
                        eventually(afterThirtySeconds) {
                            val searchResponseBody = zacClient.searchForTasks(
                                zaakIdentificatie = zaakIdentificatie,
                                taskName = BPMN_SUSPEND_RESUME_EXTEND_TASK_NAME,
                                testUser = BEHANDELAAR_1
                            )
                            JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                        }
                    }

                    and("the due date and fatal date of the zaak are extended by the extend days") {
                        eventually(afterThirtySeconds) {
                            retrieveZaak(zaakUuid).shouldHaveDueDateAndFatalDate(
                                dueDate.plusDays(extendDays),
                                fatalDate.plusDays(extendDays)
                            )
                        }
                    }
                }
            }
        }
    }
})
