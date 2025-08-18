/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_COORDINATOR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_COORDINATORS_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates a zaak with a BPMN type.
 */
class ZaakRestServiceBpmnTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    fun submitFormData(bpmnZaakUuid: UUID, taakData: String): String {
        val takenCreateResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/taken/zaak/$bpmnZaakUuid"
        ).let {
            val responseBody = it.body.string()
            logger.info { "Response: $responseBody" }
            it.code shouldBe HTTP_OK
            responseBody
        }
        val zaakIdentificatie = JSONArray(takenCreateResponse).getJSONObject(0).get("zaakIdentificatie")

        val patchedTakenData = takenCreateResponse.replace(""""taakdata":{}""", taakData)
        logger.info { "Patched request: $patchedTakenData" }

        return itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/taken/complete",
            requestBodyAsString = patchedTakenData
        ).run {
            val responseBody = body.string()
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            responseBody
        }
    }

    Given("A BPMN type zaak has been created") {
        var bpmnZaakUuid: UUID
        var zaakIdentificatie: String

        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body.string()
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    bpmnZaakUuid = getString("zaakUUID").run(UUID::fromString)
                    zaakIdentificatie = getString("zaakIdentificatie")
                }
            }
        }

        When("the user data form is submitted") {
            val takenPatchResponse = submitFormData(
                bpmnZaakUuid = bpmnZaakUuid,
                taakData = """
                   "taakdata":{
                     "zaakIdentificatie":"$zaakIdentificatie",
                     "initiator":null,
                     "zaaktypeOmschrijving":"$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                     "firstName":"Name",
                     "AM_TeamBehandelaar_Groep": "$TEST_GROUP_COORDINATORS_ID",
                     "AM_TeamBehandelaar_Medewerker": "$TEST_COORDINATOR_1_USERNAME",
                     "SD_SmartDocuments_Template": "OpenZaakTest",
                     "SD_SmartDocuments_Create": false,
                     "RT_ReferenceTable_Values": "Post",
                     "ZK_Result": "Verleend",
                     "ZK_Status": "In behandeling"
                   }
                """.trimIndent()
            )

            Then("process task is completed") {
                JSONObject(takenPatchResponse).run {
                    getString("status") shouldBe "AFGEROND"
                }
            }

            And("the zaak is still open and without result") {
                zacClient.retrieveZaak(bpmnZaakUuid).use { response ->
                    val responseBody = response.body.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", true)
                        shouldNotContainJsonKey("resultaat")
                    }
                }
            }

            And("the task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                            {
                              "rows": 10,
                              "page": 0,
                              "alleenMijnZaken": false,
                              "alleenOpenstaandeZaken": false,
                              "alleenAfgeslotenZaken": false,
                              "alleenMijnTaken": false,
                              "datums": {},
                              "zoeken": {
                                "TAAK_ZAAK_ID": "$zaakIdentificatie"
                              },
                              "filters": {
                                "TAAK_NAAM": {
                                  "values": [ "Test form" ],
                                  "inverse": "false"
                                }
                              },
                              "sorteerRichting": "",
                              "type": "TAAK"
                            }
                        """.trimIndent()
                    ).body.string()
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }
        }

        When("the summary form is completed") {
            val takenPatchResponse = submitFormData(
                bpmnZaakUuid = bpmnZaakUuid,
                taakData = """
                  "taakdata":{
                    "zaakIdentificatie":"$zaakIdentificatie",
                    "initiator":null,
                    "zaaktypeOmschrijving":"$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "firstName":"Name",
                    "AM_TeamBehandelaar_Groep": "$TEST_GROUP_COORDINATORS_ID",
                    "AM_TeamBehandelaar_Medewerker": "$TEST_COORDINATOR_1_USERNAME",
                    "SD_SmartDocuments_Template": "OpenZaakTest",
                    "SD_SmartDocuments_Create": false,
                    "RT_ReferenceTable_Values": "Post",
                    "ZK_Result": "Verleend",
                    "ZK_Status": "In behandeling"
                    "TF_EMAIL_TO": "shared-team-dimpact@info.nl"
                  }
                """.trimIndent()
            )

            Then("process task should be completed") {
                JSONObject(takenPatchResponse).run {
                    getString("status") shouldBe "AFGEROND"
                }
            }

            And("the zaak is closed and with result") {
                zacClient.retrieveZaak(bpmnZaakUuid).use { response ->
                    val responseBody = response.body.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKeyValue("$.resultaat.resultaattype.naam", "Verleend")
                    }
                }
            }

            And("the send email service task sent an email") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "$GREENMAIL_API_URI/user/shared-team-dimpact@info.nl/messages/"
                )
                receivedMailsResponse.code shouldBe HTTP_OK

                val receivedMails = JSONArray(receivedMailsResponse.body.string())
                with(receivedMails) {
                    length() shouldBe 1
                    with(getJSONObject(0)) {
                        getString("subject") shouldContain "Informatie over zaak ZAAK-"
                    }
                }
            }

            And("the task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                            {
                              "rows": 10,
                              "page": 0,
                              "alleenMijnZaken": false,
                              "alleenOpenstaandeZaken": false,
                              "alleenAfgeslotenZaken": false,
                              "alleenMijnTaken": false,
                              "datums": {},
                              "zoeken": {
                                "TAAK_ZAAK_ID": "$zaakIdentificatie"
                              },
                              "filters": {
                                "TAAK_NAAM": {
                                  "values": [ "Summary" ],
                                  "inverse": "false"
                                }
                              },
                              "sorteerRichting": "",
                              "type": "TAAK"
                            }
                        """.trimIndent()
                    ).body.string()
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }
        }
    }
})
