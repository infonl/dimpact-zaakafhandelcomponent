/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

/**
 * This test creates a zaak with BPMN type.
 */
class ZaakRestServiceBpmnTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A BPMN type zaak has been created") {
        lateinit var zaakUUID: UUID
        val takenCreateResponse: String

        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/taken/zaak/$zaakUUID"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            takenCreateResponse = responseBody
        }

        When("the user data form is submitted") {
            val takenPatchResponse: String
            val zaakIdentificatie = JSONArray(takenCreateResponse).getJSONObject(0).get("zaakIdentificatie")

            val patchedTakenData = takenCreateResponse.replace(
                """"taakdata":{}""",
                """
                "taakdata":{
                    "zaakIdentificatie":"$zaakIdentificatie",
                    "initiator":null,
                    "zaaktypeOmschrijving":"BPMN test zaaktype",
                    "firstName":"Name",
                    "AM_TeamBehandelaar_Groep": "test-group-co",
                    "AM_TeamBehandelaar_Medewerker": "coordinator1",
                    "AM_SmartDocuments_Template": "Advies oud"
                }
                """.trimIndent()
            )
            logger.info { "Patched request: $patchedTakenData" }

            itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = patchedTakenData
            ).run {
                val responseBody = body!!.string()
                logger.info { "Response: $responseBody" }
                code shouldBe HTTP_OK
                takenPatchResponse = responseBody
            }

            Then("process task should be completed") {
                JSONObject(takenPatchResponse).run {
                    getString("status") shouldBe "AFGEROND"
                }
            }

            And("the zaak should still be open and without result") {
                zacClient.retrieveZaak(zaakUUID).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", true)
                        shouldNotContainJsonKey("resultaat")
                    }
                }
            }
        }
    }
})
