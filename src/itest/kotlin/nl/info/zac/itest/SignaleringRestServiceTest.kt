/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.info.zac.itest.config.ItestConfiguration.START_DATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SignaleringRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }
    lateinit var zaakUUID: UUID
    lateinit var zaakIdentificatie: String
    lateinit var zaakPath: String

    Given("A logged-in behandelaar") {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        When("dashboard signaleringen are turned on for all signalering types") {
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
                    requestBodyAsString = it
                )

                Then("the response should be 'ok'") {
                    response.code shouldBe HTTP_OK
                }
            }
        }
    }

    Given("A logged-in behandelaar and a newly created zaak assigned to this user") {
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            behandelaarId = BEHANDELAAR_DOMAIN_TEST_1.username,
            startDate = DATE_TIME_2024_01_31
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }

        zaakPath = "zaken/api/v1/zaken/$zaakUUID"
        // retrieve the roles of this zaak from Open Zaak
        val zaakRollenResponse = openZaakClient.getRolesForZaak(zaakUUID)
        val responseBody = zaakRollenResponse.bodyAsString
        logger.info { "Response: $responseBody" }
        zaakRollenResponse.code shouldBe HTTP_OK
        val zaakRollenUrl = JSONObject(responseBody)
            .getJSONArray("results")
            .getJSONObject(0)
            .getString("url")
            .replace(OPEN_ZAAK_EXTERNAL_URI, OPEN_ZAAK_BASE_URI)
        val now = ZonedDateTime.now(ZoneId.of("UTC"))

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
                        "aanmaakdatum" to now.plusSeconds(1).toString()
                    )
                ).toString()
            )

            Then("the response should be 'no content'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
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
                        "aanmaakdatum" to now.plusSeconds(2).toString()
                    )
                ).toString()
            )

            Then("the response should be 'no content'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
            }
        }

        When("the latest signaleringen are requested") {
            val latestSignaleringenDateUrl = "$ZAC_API_URI/signaleringen/latest"

            Then("it returns a date between the start of the tests and current moment") {
                // The backend event processing is asynchronous. Wait a bit until the events are processed
                eventually(afterThirtySeconds) {
                    val response = itestHttpClient.performGetRequest(latestSignaleringenDateUrl)
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK

                    // application/json should be changed to text/plain in the endpoint to get rid of the quotes
                    val dateString = responseBody.replace("\"", "")

                    val date = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
                        .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    date.shouldBeBetween(START_DATE, LocalDateTime.now())
                }
            }
        }

        When("the list of zaken signaleringen for ZAAK_OP_NAAM is requested") {
            val response = itestHttpClient.performPutRequest(
                "$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM",
                requestBodyAsString = """{
                    "page": 0,
                    "rows": 5
                }
                """.trimIndent()
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }
            response.code shouldBe HTTP_OK

            Then("the returned list should contain one result, being the newly created zaak") {
                with(responseBody) {
                    shouldContainJsonKeyValue("totaal", "1.0")
                    with(JSONObject(responseBody).getJSONArray("resultaten").getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("identificatie", zaakIdentificatie)
                        shouldContainJsonKeyValue("startdatum", DATE_2024_01_31.toString())
                        shouldContainJsonKeyValue("omschrijving", ZAAK_OMSCHRIJVING)
                        shouldContainJsonKeyValue(
                            "zaaktype",
                            ZAAKTYPE_TEST_2_DESCRIPTION
                        )
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
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            Then("400 should be returned") {
                response.code shouldBe HTTP_BAD_REQUEST
                responseBody.shouldContainJsonKeyValue("message", "Requested page 2 must be <= 1")
            }
        }

        When(
            """
            the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a TXT file
            """
        ) {
            val response = zacClient.createEnkelvoudigInformatieobjectForZaak(
                zaakUUID = zaakUUID,
                fileName = TEST_TXT_FILE_NAME,
                fileMediaType = TEXT_MIME_TYPE,
                vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
            )

            Then("the response should be OK and contain information for the created document") {
                val responseBody = response.bodyAsString
                logger.info { "response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
        }

        When("a notification is sent to ZAC that a document was added to the zaak") {
            // obtain the zaakinformatieobject URL from OpenZaak for the document that was just added to the zaak
            val zaakInformatieObjectenResponse = itestHttpClient.performGetRequest(
                url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaakinformatieobjecten?zaak=$OPEN_ZAAK_EXTERNAL_URI/$zaakPath"
            )
            var responseBody = zaakInformatieObjectenResponse.bodyAsString
            logger.info { "Response: $responseBody" }
            val now = ZonedDateTime.now(ZoneId.of("UTC"))
            zaakInformatieObjectenResponse.code shouldBe HTTP_OK
            val zaakInformatieObjectenUrl = JSONArray(responseBody)
                .getJSONObject(0)
                .getString("url")
            logger.info { "Zaak informatie objecten URL: $zaakInformatieObjectenUrl" }
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
                        "aanmaakdatum" to now.toString()
                    )
                ).toString()
            )
            Then("the response should be 'no content'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
            }
            When("the list of zaken signaleringen for ZAAK_DOCUMENT_TOEGEVOEGD is requested") {
                // it may take a while before the notification is processed and the signalering is created
                lateinit var responseBody: String
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        "$ZAC_API_URI/signaleringen/zaken/ZAAK_DOCUMENT_TOEGEVOEGD",
                        requestBodyAsString = """
                        {
                            "page": 0,
                            "rows": 5
                        }
                        """.trimIndent()
                    )
                    responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    JSONObject(responseBody).getDouble("totaal") shouldBe 1.0
                }

                Then("a response of 1 is returned for the zaak to which a document was added") {
                    with(responseBody) {
                        shouldContainJsonKeyValue("totaal", "1.0")
                        with(JSONObject(responseBody).getJSONArray("resultaten").getJSONObject(0).toString()) {
                            shouldContainJsonKeyValue("identificatie", zaakIdentificatie)
                            shouldContainJsonKeyValue("startdatum", DATE_2024_01_31.toString())
                            shouldContainJsonKeyValue("omschrijving", ZAAK_OMSCHRIJVING)
                            shouldContainJsonKeyValue(
                                "zaaktype",
                                ZAAKTYPE_TEST_2_DESCRIPTION
                            )
                        }
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
                url = "$ZAC_API_URI/internal/signaleringen/delete-old",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders(),
                addAuthorizationHeader = false
            )
            val responseBody = response.bodyAsString
            logger.info { "Response: $responseBody" }

            Then("at least the two existing signaleringen should be deleted") {
                response.code shouldBe HTTP_OK

                with(JSONObject(responseBody)) {
                    // there may be more than two signaleringen deleted if other tests have created signaleringen as well
                    getInt("deletedSignaleringenCount") shouldBeGreaterThan 1
                }
            }
        }
    }
})
