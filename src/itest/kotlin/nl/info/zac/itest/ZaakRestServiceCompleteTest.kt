/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_ZAAK_AFHANDELEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, closes the zaak, then re-opens and again closes the zaak.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class ZaakRestServiceCompleteTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak has been created that has finished the intake phase with the status 'admissible'") {
        lateinit var zaakUUID: UUID
        lateinit var resultaatTypeUuid: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
            {
                "zaakUuid":"$zaakUUID",
                "planItemInstanceId":"$intakeId",
                "actie":"$ACTIE_INTAKE_AFRONDEN",
                "zaakOntvankelijk":true
            }
            """.trimIndent()
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                // we do not care about the specific result type, so we just take the first one
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }

        When("the zaak is completed") {
            val afhandelenId: Int
            itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
            ).run {
                val responseBody = body!!.string()
                logger.info { "Response: $responseBody" }
                JSONArray(responseBody).getJSONObject(0).run {
                    afhandelenId = getString("id").toInt()
                }
            }
            // wait for OpenZaak to accept this request
            sleepForOpenZaakUniqueConstraint(1)
            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = """{
                    "zaakUuid":"$zaakUUID",
                    "planItemInstanceId":"$afhandelenId",
                    "actie":"$ACTIE_ZAAK_AFHANDELEN",
                    "resultaattypeUuid": "$resultaatTypeUuid",
                    "resultaatToelichting":"afronden"
                }
                """.trimIndent()
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            Then("the zaak should be closed and have a result") {
                zacClient.retrieveZaak(zaakUUID).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                }
            }
        }

        When("the closed zaak is re-opened") {
            // wait for OpenZaak to accept this request
            sleepForOpenZaakUniqueConstraint(1)
            itestHttpClient.performPatchRequest(
                "$ZAC_API_URI/zaken/zaak/$zaakUUID/heropenen",
                requestBodyAsString = """
                    {"reden":"fakeReason"}
                """.trimIndent()
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            Then("the zaak should be open and should no longer have a result") {
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

        When("the re-opened zaak is completed again") {
            // wait for OpenZaak to accept this request
            sleepForOpenZaakUniqueConstraint(1)
            // Completing a re-opened zaak is done using the 'afsluiten' endpoint
            // instead of the 'doUserEventListenerPlanItem' endpoint.
            // Not sure why.
            itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUUID/afsluiten",
                requestBodyAsString = """
                    {
                    "reden":"fakeReason",
                    "resultaattypeUuid":"$resultaatTypeUuid"
                    }
                """.trimIndent()
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            Then("the zaak should be closed and have a result") {
                zacClient.retrieveZaak(zaakUUID).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                }
            }
        }
    }

    Given("A zaak has been created that has not finished the intake phase") {
        lateinit var zaakUUID: UUID
        lateinit var resultaatTypeUuid: UUID
        val afhandelenId: Int

        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                // we do not care about the specific result type, so we just take the first one
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                afhandelenId = getString("id").toInt()
            }
        }

        When("the zaak is completed as 'not-admissible'") {
            val receiverMail = "receiverZaakComplete@example.com"
            val senderMail = "sender@example.com"
            val mailBody = "<p><b>bold</b>paragraph<i>italic</i></p>"
            // wait for OpenZaak to accept this request
            sleepForOpenZaakUniqueConstraint(1)
            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = """{
                    "zaakUuid":"$zaakUUID",
                    "planItemInstanceId":"$afhandelenId",
                    "actie":"$ACTIE_ZAAK_AFHANDELEN",
                    "resultaattypeUuid": "$resultaatTypeUuid",
                    "resultaatToelichting":"afronden",
                    "zaakOntvankelijk": false,
                    "restMailGegevens": {
                        "verzender": "$senderMail",
                        "ontvanger": "$receiverMail",
                        "replyTo": "replyTo@example.com",
                        "onderwerp": "closed zaak subject",
                        "body": "$mailBody",
                        "createDocumentFromMail": false
                    }
                }
                """.trimIndent()
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            Then("email should be sent with correct details") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "$GREENMAIL_API_URI/user/$receiverMail/messages/"
                )
                receivedMailsResponse.code shouldBe HTTP_OK

                val responseBody = receivedMailsResponse.body!!.string()
                logger.info { "Response: $responseBody" }
                with(JSONArray(responseBody)) {
                    length() shouldBe 1
                    with(getJSONObject(0)) {
                        getString("subject") shouldBe "closed zaak subject"
                        getString("contentType") shouldStartWith "multipart/mixed"
                        with(getString("mimeMessage")) {
                            shouldContain(senderMail)
                            shouldContain(receiverMail)
                            shouldContain(mailBody)
                        }
                    }
                }
            }
        }
    }
})
