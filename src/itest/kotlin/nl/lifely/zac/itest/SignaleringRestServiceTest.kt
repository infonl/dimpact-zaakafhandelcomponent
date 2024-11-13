/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_BAD_REQUEST
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.lifely.zac.itest.config.ItestConfiguration.START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_START_DATE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
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
class SignaleringRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

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

                Then("the response should be 'ok'") {
                    response.code shouldBe HTTP_STATUS_OK
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
                    "zaakUUID":"${ItestConfiguration.zaakProductaanvraag1Uuid}",
                    "behandelaarGebruikersnaam":"$TEST_USER_1_USERNAME",
                    "groepId":"$TEST_GROUP_A_ID",
                    "reden":null
                }
                """.trimIndent()
            )

            Then("the response should be 'ok'") {
                response.code shouldBe HTTP_STATUS_OK
            }
        }
    }

    Given("A zaak with informatie objecten") {
        val zaakPath = "zaken/api/v1/zaken/${ItestConfiguration.zaakProductaanvraag1Uuid}"

        val zaakInformatieObjectenResponse = itestHttpClient.performGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaakinformatieobjecten?zaak=$OPEN_ZAAK_EXTERNAL_URI/$zaakPath"
        )
        var responseBody = zaakInformatieObjectenResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        zaakInformatieObjectenResponse.code shouldBe HTTP_STATUS_OK
        val zaakInformatieObjectenUrl = JSONArray(responseBody)
            .getJSONObject(0)
            .getString("url")
        logger.info { "Zaak informatie objecten URL: $zaakInformatieObjectenUrl" }

        val zaakRollenResponse = itestHttpClient.performGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/rollen?zaak=$OPEN_ZAAK_EXTERNAL_URI/$zaakPath"
        )
        responseBody = zaakRollenResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        zaakRollenResponse.code shouldBe HTTP_STATUS_OK
        val zaakRollenUrl = JSONObject(responseBody)
            .getJSONArray("results")
            .getJSONObject(0)
            .getString("url")
            .replace(OPEN_ZAAK_EXTERNAL_URI, OPEN_ZAAK_BASE_URI)
        logger.info { "Zaak rollen URL: $zaakInformatieObjectenUrl" }

        val notificationDate = ZonedDateTime.now(ZoneId.of("UTC"))

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
                        "aanmaakdatum" to notificationDate.toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'no content'") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
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
                        "aanmaakdatum" to notificationDate.plusSeconds(1).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'no content'") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
            }
        }

        When("a second notification is sent to ZAC that a rol is updated for the same zaak") {
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
                        "aanmaakdatum" to notificationDate.plusSeconds(2).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'no content'") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
            }
        }
    }

    Given("A zaak has been assigned") {
        When("the latest signaleringen date is requested") {
            val latestSignaleringenDateUrl = "$ZAC_API_URI/signaleringen/latest"

            Then("it returns a date between the start of the tests and current moment") {
                // The backend event processing is asynchronous. Wait a bit until the events are processed
                eventually(afterThirtySeconds) {
                    val response = itestHttpClient.performGetRequest(latestSignaleringenDateUrl)
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
    }

    Given("An assigned zaak with information object") {
        When("the list of zaken signaleringen for ZAAK_OP_NAAM is requested") {
            val response = itestHttpClient.performPutRequest(
                "$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM",
                requestBodyAsString = """{
                    "page": 0,
                    "rows": 5
                }
                """.trimIndent()
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }
            response.isSuccessful shouldBe true

            Then("list content is correct") {
                with(responseBody) {
                    shouldContainJsonKeyValue("totaal", "1.0")
                    with(JSONObject(responseBody).getJSONArray("resultaten").getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("identificatie", ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION)
                        shouldContainJsonKeyValue("startdatum", ZAAK_PRODUCTAANVRAAG_1_START_DATE)
                        shouldContainJsonKeyValue("omschrijving", ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING)
                        shouldContainJsonKeyValue("zaaktype", ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION)
                    }
                }
            }
        }

        When("the list of zaken signaleringen is requested with wrong page") {
            val response = itestHttpClient.performPutRequest(
                "$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM",
                requestBodyAsString = """{
                    "page": 2,
                    "rows": 5
                }
                """.trimIndent()
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }

            Then("400 should be returned") {
                response.code shouldBe HTTP_STATUS_BAD_REQUEST
                responseBody.shouldContainJsonKeyValue("message", "Requested page 2 must be <= 1")
            }
        }

        When("the list of zaken signaleringen for ZAAK_DOCUMENT_TOEGEVOEGD is requested") {
            val response = itestHttpClient.performPutRequest(
                "$ZAC_API_URI/signaleringen/zaken/ZAAK_DOCUMENT_TOEGEVOEGD",
                requestBodyAsString = """{
                    "page": 0,
                    "rows": 5
                }
                """.trimIndent()
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }
            response.isSuccessful shouldBe true

            Then("list content is correct") {
                with(responseBody) {
                    shouldContainJsonKeyValue("totaal", "1.0")
                    with(JSONObject(responseBody).getJSONArray("resultaten").getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("identificatie", ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION)
                        shouldContainJsonKeyValue("startdatum", ZAAK_PRODUCTAANVRAAG_1_START_DATE)
                        shouldContainJsonKeyValue("omschrijving", ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING)
                        shouldContainJsonKeyValue("zaaktype", ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION)
                    }
                }
            }
        }
    }

    Given(
        """
        Two existing signaleringen and the ZAC environment variable 'SIGNALERINGEN_DELETE_OLDER_THAN_DAYS' set to 0 days
        """
    ) {
        When("signaleringen older than 0 days are deleted") {
            val response = itestHttpClient.performDeleteRequest(
                "$ZAC_API_URI/admin/signaleringen/delete-old"
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }

            Then("all existing signaleringen should have been deleted") {
                response.isSuccessful shouldBe true

                with(JSONObject(responseBody)) {
                    getInt("deletedSignaleringenCount") shouldBe 2
                }
            }
        }
    }
})
