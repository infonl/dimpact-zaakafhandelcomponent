/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * This test assumes previous tests completed successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class SignaleringenRestServiceTest : BehaviorSpec() {
    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()

    init {
        Given("A test user") {
            When("a dashboard notification is turned on") {
                val notificationBodies = arrayOf(
                    """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_DOCUMENT_TOEGEVOEGD"}""",
                    """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_OP_NAAM"}""",
                    """{"dashboard":true,"mail":false,"subjecttype":"ZAAK","type":"ZAAK_VERLOPEND"}""",
                    """{"dashboard":true,"mail":false,"subjecttype":"TAAK","type":"TAAK_OP_NAAM"}"""
                )
                notificationBodies.forEach() {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/signaleringen/instellingen",
                        headers = Headers.headersOf(
                            "Content-Type",
                            "application/json",
                            "Authorization",
                            ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
                        ),
                        requestBodyAsString = it,
                        addAuthorizationHeader = false
                    )

                    Then("the responses should be 'ok'") {
                        response.code shouldBe HttpStatusCode.OK_200.code()
                    }
                }
            }
        }

        Given("A zaak with informatie objecten") {
            val zaakInformatieObjectenResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobjecten/zaak/$zaak1UUID",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                )
            )
            var responseBody = zaakInformatieObjectenResponse.body!!.string()
            logger.info { "Response: $responseBody" }
            zaakInformatieObjectenResponse.code shouldBe HttpStatusCode.OK_200.code()

            val zaakInformatieObjectenUrl = JSONArray(responseBody).getJSONObject(0).getString("url")
            logger.info { "Zaak informatie objecten UUID: $zaakInformatieObjectenUrl" }

            When("a notification is sent to ZAC that a zaak is updated") {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/notificaties",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json",
                        "Authorization",
                        ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    requestBodyAsString = JSONObject(mapOf(
                        "actie" to "create",
                        "kanaal" to "zaken",
                        "resource" to "zaakinformatieobject",
                        "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaak1UUID",
                        "resourceUrl" to zaakInformatieObjectenUrl,
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )).toString(),
                    addAuthorizationHeader = false
                )

                Then("the response should be 'no content'") {
                    responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                }
            }
        }

        Given("A task has been assigned") {

            When("the latest signaleringen date is requested") {
                val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/latest")

                Then("""it returns a date between the start of the tests and current moment""") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true

                    val date = ZonedDateTime.parse(responseBody).toLocalDateTime()
                    date.shouldBeBetween(START_DATE, LocalDateTime.now())
                }
            }

            When("the list of zaken signaleringen for ZAAK_OP_NAAM is requested") {
                val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM")

                Then("""it returns the correct signaleringen""") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                }
            }

            When("the list of zaken signaleringen for TAAK_OP_NAAM is requested") {
                val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/zaken/TAAK_OP_NAAM")

                Then("""it returns the correct signaleringen""") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                }
            }

            When("the list of taken signaleringen for TAAK_OP_NAAM is requested") {
                val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/taken/TAAK_OP_NAAM")

                Then("""it returns the correct signaleringen""") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                }
            }

            When("the list of taken signaleringen for TAAK_OP_NAAM is requested") {
                val response = itestHttpClient.performGetRequest("$ZAC_API_URI/signaleringen/typen/dashboard")

                Then("""it returns the correct dashboard types""") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                }
            }
        }
    }
}
