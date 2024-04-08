/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.lifely.zac.itest.config.ItestConfiguration.START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This test assumes previous tests completed successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class SignaleringenRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A test user") {
        When("a dashboard notification is turned on") {
            val notificationBodies = arrayOf(
                """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_DOCUMENT_TOEGEVOEGD"}""",
                """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_OP_NAAM"}""",
                """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_VERLOPEND"}""",
                """{"dashboard":true,"mail":false,"subjecttype":"TAAK","type":"TAAK_OP_NAAM"}"""
            )
            notificationBodies.forEach {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/signaleringen/instellingen",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json"
                    ),
                    requestBodyAsString = it,
                    addAuthorizationHeader = true
                )

                Then("the responses should be 'ok'") {
                    response.code shouldBe HttpStatusCode.OK_200.code()
                }
            }
        }
    }

    Given("A created zaak") {
        When("it is assigned to a user") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                requestBodyAsString = """{
                    "zaakUUID":"$zaak1UUID",
                    "behandelaarGebruikersnaam":"$TEST_USER_1_ID",
                    "groepId":"$TEST_GROUP_A_ID",
                    "reden":null
                }
                """.trimIndent()
            )

            Then("the responses should be 'ok'") {
                response.code shouldBe HttpStatusCode.OK_200.code()
            }
        }
    }

    Given("A zaak with informatie objecten") {
        val zaakPath = "zaken/api/v1/zaken/$zaak1UUID"

        val zaakInformatieObjectenResponse = itestHttpClient.performGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaakinformatieobjecten?zaak=$OPEN_ZAAK_EXTERNAL_URI/$zaakPath"
        )
        var responseBody = zaakInformatieObjectenResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        zaakInformatieObjectenResponse.code shouldBe HttpStatusCode.OK_200.code()
        val zaakInformatieObjectenUrl = JSONArray(responseBody)
            .getJSONObject(0)
            .getString("url")
        logger.info { "Zaak informatie objecten URL: $zaakInformatieObjectenUrl" }

        val zaakRollenResponse = itestHttpClient.performGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/rollen?zaak=$OPEN_ZAAK_EXTERNAL_URI/$zaakPath"
        )
        responseBody = zaakRollenResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        zaakRollenResponse.code shouldBe HttpStatusCode.OK_200.code()
        val zaakRollenUrl = JSONObject(responseBody)
            .getJSONArray("results")
            .getJSONObject(0)
            .getString("url")
            .replace(OPEN_ZAAK_EXTERNAL_URI, OPEN_ZAAK_BASE_URI)
        logger.info { "Zaak rollen URL: $zaakInformatieObjectenUrl" }

        When("a notification is sent to ZAC that a zaak is updated") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    "Authorization",
                    ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "actie" to "create",
                        "kanaal" to "zaken",
                        "resource" to "zaakinformatieobject",
                        "hoofdObject" to "$OPEN_ZAAK_BASE_URI/$zaakPath",
                        "resourceUrl" to zaakInformatieObjectenUrl,
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'no content'") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
            }
        }

        When("a notification is sent to ZAC that a rol is updated") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    "Authorization",
                    ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "actie" to "create",
                        "kanaal" to "zaken",
                        "resource" to "rol",
                        "hoofdObject" to "$OPEN_ZAAK_BASE_URI/$zaakPath",
                        "resourceUrl" to zaakRollenUrl,
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'no content'") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
            }
        }
    }

    Given("A zaak has been assigned") {
        When("the list of zaken signaleringen for ZAAK_DOCUMENT_TOEGEVOEGD is requested") {
            lateinit var responseBody: String

            eventually(
                eventuallyConfig {
                    duration = 5.seconds
                    interval = 500.milliseconds
                }
            ) {
                val response = itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/signaleringen/zaken/ZAAK_DOCUMENT_TOEGEVOEGD"
                )
                response.isSuccessful shouldBe true
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody.shouldBeJsonArray()
                JSONArray(responseBody) shouldHaveSize 1
            }

            Then("it returns the correct signaleringen") {
                with(JSONArray(responseBody).getJSONObject(0).toString()) {
                    shouldContainJsonKey("behandelaar")
                    shouldContainJsonKey("groep")
                    shouldContainJsonKeyValue("identificatie", "ZAAK-2023-0000000001")
                    shouldContainJsonKey("omschrijving")
                    shouldContainJsonKey("openstaandeTaken")
                    shouldContainJsonKey("rechten")
                    shouldContainJsonKeyValue("startdatum", "2023-10-25")
                    shouldContainJsonKeyValue("status", "Intake")
                    shouldContainJsonKeyValue("toelichting", "")
                    shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", "2023-11-08")
                    shouldContainJsonKey("uuid")
                    shouldContainJsonKeyValue("zaaktype", "Melding evenement organiseren behandelen")
                }
            }
        }

        When("the list of zaken signaleringen for ZAAK_OP_NAAM is requested") {
            lateinit var responseBody: String

            eventually(
                eventuallyConfig {
                    duration = 5.seconds
                    interval = 500.milliseconds
                }
            ) {
                val response = itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM"
                )
                response.isSuccessful shouldBe true
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody.shouldBeJsonArray()
                JSONArray(responseBody) shouldHaveSize 1
            }

            Then("it returns the correct signaleringen") {
                with(JSONArray(responseBody).getJSONObject(0).toString()) {
                    shouldContainJsonKey("behandelaar")
                    shouldContainJsonKey("groep")
                    shouldContainJsonKeyValue("identificatie", "ZAAK-2023-0000000001")
                    shouldContainJsonKey("omschrijving")
                    shouldContainJsonKey("openstaandeTaken")
                    shouldContainJsonKey("rechten")
                    shouldContainJsonKeyValue("startdatum", "2023-10-25")
                    shouldContainJsonKeyValue("status", "Intake")
                    shouldContainJsonKeyValue("toelichting", "")
                    shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", "2023-11-08")
                    shouldContainJsonKey("uuid")
                    shouldContainJsonKeyValue("zaaktype", "Melding evenement organiseren behandelen")
                }
            }
        }

        When("the latest signaleringen date is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/latest")

            Then("it returns a date between the start of the tests and current moment") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                // application/json should be changed to text/plain in the endpoint to get rid of the quotes
                val dateString = responseBody.replace("\"", "")

                val date = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
                    .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                date.shouldBeBetween(START_DATE, LocalDateTime.now())
            }
        }
    }
})
