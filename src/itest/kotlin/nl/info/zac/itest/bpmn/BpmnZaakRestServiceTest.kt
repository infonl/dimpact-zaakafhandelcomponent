/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.COORDINATORS_DOMAIN_TEST_1
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates a zaak with a BPMN type.
 */
class BpmnZaakRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

    val temporaryPersonId: UUID = zacClient.getTemporaryPersonId(
        TEST_PERSON_HENDRIKA_JANSE_BSN,
        BEHANDELAAR_DOMAIN_TEST_1
    )

    Given("A behandelaar is logged in and a BPMN type zaak has been created") {
        var bpmnZaakUuid: UUID
        var zaakIdentificatie: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HttpURLConnection.HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    bpmnZaakUuid = getString("zaakUUID").run(UUID::fromString)
                    zaakIdentificatie = getString("zaakIdentificatie")
                }
            }
        }

        When("initiator is added") {
            val response = itestHttpClient.performPatchRequest(
                url = "${ZAC_API_URI}/zaken/initiator",
                requestBodyAsString = """
                    {
                        "betrokkeneIdentificatie": {
                            "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                            "temporaryPersonId": "$temporaryPersonId",
                            "type": "${BETROKKENE_IDENTIFICATION_TYPE_BSN}"
                        },
                        "zaakUUID": "$bpmnZaakUuid"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            Then("the response should be a 200 HTTP response and the initiator should be added") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_OK
                with(JSONObject(responseBody).getJSONObject("initiatorIdentificatie").toString()) {
                    shouldContainJsonKeyValue("type", BETROKKENE_IDENTIFICATION_TYPE_BSN)
                    shouldContainJsonKeyValue("temporaryPersonId", temporaryPersonId.toString())
                }
            }
        }

        When("the user data form is submitted") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = bpmnZaakUuid,
                taakData = """
                   {
                     "zaakIdentificatie":"$zaakIdentificatie",
                     "initiator":null,
                     "zaaktypeOmschrijving":"${ZAAKTYPE_BPMN_TEST_1_DESCRIPTION}",
                     "firstName":"Name",
                     "AM_TeamBehandelaar_Groep": "${COORDINATORS_DOMAIN_TEST_1.name}",
                     "AM_TeamBehandelaar_Medewerker": "${COORDINATOR_DOMAIN_TEST_1.username}",
                     "SD_SmartDocuments_Template": "OpenZaakTest",
                     "SD_SmartDocuments_Create": false,
                     "RT_ReferenceTable_Values": "Post",
                     "ZK_Result": "Verleend",
                     "ZK_Status": "Afgerond"
                   }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("process task is completed") {
                JSONObject(takenPatchResponse).run {
                    getString("status") shouldBe "AFGEROND"
                }
            }

            And("the zaak is still open and without result") {
                zacClient.retrieveZaak(bpmnZaakUuid, BEHANDELAAR_DOMAIN_TEST_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpURLConnection.HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", true)
                        shouldNotContainJsonKey("resultaat")
                    }
                }
            }

            And("the task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = ItestConfiguration.BPMN_TEST_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }

            And("summary form task becomes available") {
                eventually(afterThirtySeconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = ItestConfiguration.BPMN_SUMMARY_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                }
            }
        }

        When("the summary form is completed") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = bpmnZaakUuid,
                taakData = """
                    {
                        "zaakIdentificatie":"$zaakIdentificatie",
                        "initiator":null,
                        "zaaktypeOmschrijving":"$ZAAKTYPE_BPMN_TEST_1_DESCRIPTION",
                        "firstName":"Name",
                        "AM_TeamBehandelaar_Groep": "${COORDINATORS_DOMAIN_TEST_1.name}",
                        "AM_TeamBehandelaar_Medewerker": "${COORDINATOR_DOMAIN_TEST_1.username}",
                        "SD_SmartDocuments_Template": "OpenZaakTest",
                        "SD_SmartDocuments_Create": false,
                        "RT_ReferenceTable_Values": "Post",
                        "ZK_Result": "Verleend",
                        "ZK_Status": "Afgerond",
                        "TF_EMAIL_TO": "shared-team-dimpact@info.nl"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("process task should be completed") {
                JSONObject(takenPatchResponse).run {
                    getString("status") shouldBe "AFGEROND"
                }
            }

            And("the zaak is closed and with result") {
                zacClient.retrieveZaak(bpmnZaakUuid, BEHANDELAAR_DOMAIN_TEST_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpURLConnection.HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKeyValue("$.resultaat.resultaattype.naam", "Verleend")
                    }
                }
            }

            And("the send email service task sent an email") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.GREENMAIL_API_URI}/user/shared-team-dimpact@info.nl/messages/",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                receivedMailsResponse.code shouldBe HttpURLConnection.HTTP_OK

                val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                with(receivedMails) {
                    length() shouldBe 1
                    with(getJSONObject(0)) {
                        getString("subject") shouldContain "Informatie over zaak ZAAK-"
                    }
                }
            }

            And("the task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = ItestConfiguration.BPMN_SUMMARY_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }
        }
    }

    Given("A behandelaar is logged in and a BPMN type zaak has been created for a case that will be aborted") {
        var bpmnZaakUuid: UUID
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HttpURLConnection.HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    bpmnZaakUuid = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }

        When("the case is aborted") {
            val response = itestHttpClient.performPatchRequest(
                url = "${ZAC_API_URI}/zaken/zaak/$bpmnZaakUuid/afbreken",
                requestBodyAsString = """
                    {
                        "zaakbeeindigRedenId": "ZAAK_NIET_ONTVANKELIJK"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            Then("the response should be a 204 HTTP response") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_NO_CONTENT
            }
        }
    }
})
