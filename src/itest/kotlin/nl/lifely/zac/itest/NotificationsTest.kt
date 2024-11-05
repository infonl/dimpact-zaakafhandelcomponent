/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_FORBIDDEN
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_2_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_1_BRON_KENMERK
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_INITIAL
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Betrokkene1Uuid
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag2Uuid
import nl.lifely.zac.itest.config.dockerComposeContainer
import nl.lifely.zac.itest.util.WebSocketTestListener
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import org.testcontainers.containers.wait.strategy.Wait
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * This test creates a zaak and a document (the form data PDF) which we use in other tests, and therefore we run this test first.
 */
@Order(TEST_SPEC_ORDER_INITIAL)
class NotificationsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("""ZAC and all related Docker containers are running""") {
        When(""""the notificaties endpoint is called with dummy payload without authentication header""") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf("Content-Type", "application/json"),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "dummy" to "dummy"
                    )
                ).toString()
            )
            Then("the response should be forbidden") {
                response.code shouldBe HTTP_STATUS_FORBIDDEN
            }
        }
    }
    Given(
        """"ZAC and all related Docker containers are running, productaanvraag object exists in Objecten API
                    and productaanvraag PDF exists in Open Zaak"""
    ) {
        When(
            """
                the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header 
                and initiator of type 'BSN'
            """.trimIndent()
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    // this test simulates that Open Notificaties sends the request to ZAC
                    // using the secret API key that is configured in ZAC
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "resource" to "object",
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_1_UUID",
                        "actie" to "create",
                        "kenmerken" to mapOf(
                            "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                        ),
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )
            Then(
                """the response should be 'no content', a zaak should be created in OpenZaak
                        and a zaak productaanvraag proces of type 'Productaanvraag-Dimpact' should be started in ZAC"""
            ) {
                response.code shouldBe HTTP_STATUS_NO_CONTENT

                // retrieve the newly created zaak and check the contents
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION"
                ).use { getZaakResponse ->
                    val responseBody = getZaakResponse.body!!.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
                        getJSONObject("zaaktype")
                            .getString("uuid") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID.toString()
                        getJSONObject("status").getString("naam") shouldBe "Intake"
                        getJSONObject("groep").getString("id") shouldBe "test-group-a"
                        // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                        // since we have defined zaakafhandelparameters for this zaaktype a CMMN proces should be started
                        getBoolean("isProcesGestuurd") shouldBe false
                        getString("communicatiekanaal") shouldBe "E-formulier"
                        getString("omschrijving") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_1_BRON_KENMERK'"
                        getString("uiterlijkeEinddatumAfdoening") shouldBe ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING
                        with(getJSONObject("zaakgeometrie").getJSONObject("point")) {
                            getBigDecimal("latitude") shouldBe PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE.toBigDecimal()
                            getBigDecimal("longitude") shouldBe PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE.toBigDecimal()
                        }
                        getString("initiatorIdentificatie") shouldBe TEST_PERSON_HENDRIKA_JANSE_BSN
                        getString("initiatorIdentificatieType") shouldBe BETROKKENE_IDENTIFICATION_TYPE_BSN
                        zaakProductaanvraag1Uuid = getString("uuid").let(UUID::fromString)
                    }
                }
            }
        }
        When("the get betrokkene endpoint is called for the zaak created from the productaanvraag") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/betrokkene",
            )
            Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringExtraneousFields  """
                    [ {
                      "identificatie" : "999992958",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Bewindvoerder",
                      "type" : "NATUURLIJK_PERSOON"
                    }, {
                      "identificatie" : "999991838",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Bewindvoerder",
                      "type" : "NATUURLIJK_PERSOON"
                    }, {
                      "identificatie" : "999992958",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Medeaanvrager",
                      "type" : "NATUURLIJK_PERSOON"
                    } ]
                """.trimIndent()
                zaakProductaanvraag1Betrokkene1Uuid = JSONArray(responseBody).getJSONObject(0).getString("rolid").let(UUID::fromString)
            }
        }
    }
    Given(
        """"ZAC and all related Docker containers are running, productaanvraag object exists in Objecten API
                    and productaanvraag PDF exists in Open Zaak"""
    ) {
        When(
            """
                the notificaties endpoint is called with a second 'create productaanvraag' payload with authentication header
                 and without zaakgegevens and with an initiator of type 'vestigingsnummer'
            """.trimIndent()
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    // this test simulates that Open Notificaties sends the request to ZAC
                    // using the secret API key that is configured in ZAC
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "resource" to "object",
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_2_UUID",
                        "actie" to "create",
                        "kenmerken" to mapOf(
                            "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                        ),
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )
            Then(
                """the response should be 'no content', a zaak should be created in OpenZaak
                        and a zaak productaanvraag proces of type 'Productaanvraag-Dimpact' should be started in ZAC"""
            ) {
                response.code shouldBe HTTP_STATUS_NO_CONTENT

                // retrieve the newly created zaak and check the contents
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION"
                ).use { getZaakResponse ->
                    val responseBody = getZaakResponse.body!!.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
                        getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID.toString()
                        getJSONObject("status").getString("naam") shouldBe "Intake"
                        getJSONObject("groep").getString("id") shouldBe "test-group-a"
                        // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                        // since we have defined zaakafhandelparameters for this zaaktype a CMMN proces should be started
                        getBoolean("isProcesGestuurd") shouldBe false
                        getString("communicatiekanaal") shouldBe "E-formulier"
                        getString("omschrijving") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                            "met kenmerk '$OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK'"
                        getString("initiatorIdentificatie") shouldBe TEST_KVK_VESTIGINGSNUMMER_1
                        getString("initiatorIdentificatieType") shouldBe BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
                        zaakProductaanvraag2Uuid = getString("uuid").let(UUID::fromString)
                    }
                }
            }
        }
    }
    Given(
        """"ZAC and all related Docker containers are running"""
    ) {
        When(
            """the notificaties endpoint is called with a 'create zaaktype' payload with a 
                    dummy resourceUrl that does not start with the 'ZGW_API_CLIENT_MP_REST_URL' environment variable"""
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "${ZAC_API_URI}/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    // this test simulates that Open Notificaties sends the request to ZAC
                    // using the secret API key that is configured in ZAC
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "resource" to "zaaktype",
                        "resourceUrl" to "http://example.com/dummyResourceUrl",
                        "actie" to "create",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )
            Then(
                """the response should be 'no content' and a corresponding error message should be logged in ZAC"""
            ) {
                response.code shouldBe HTTP_STATUS_NO_CONTENT

                // we expect ZAC to log an error message indicating that the resourceURL is invalid
                dockerComposeContainer.waitingFor(
                    "zac",
                    Wait.forLogMessage(
                        ".* Er is iets fout gegaan in de Zaaktype-handler bij het afhandelen van notificatie: " +
                            "null ZAAKTYPE CREATE .*: java.lang.RuntimeException: URI 'http://example.com/dummyResourceUrl' does not " +
                            "start with value for environment variable 'ZGW_API_CLIENT_MP_REST_URL': '$OPEN_ZAAK_BASE_URI/' .*",
                        1
                    ).withStartupTimeout(30.seconds.toJavaDuration())
                )
            }
        }
    }
    Given("""A websocket subscription is created to listen to all changes made to a specific zaak""") {
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = "{" +
                "\"subscriptionType\":\"CREATE\"," +
                "\"event\":{" +
                "  \"opcode\":\"ANY\"," +
                "  \"objectType\":\"ZAAK\"," +
                "  \"objectId\":{" +
                "    \"resource\":\"$zaakProductaanvraag1Uuid\"" +
                "  }," +
                "\"_key\":\"ANY;ZAAK;$zaakProductaanvraag1Uuid\"" +
                "}" +
                "}"
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When(""""a notification is sent to ZAC that the zaak in question has been updated""") {
            // we need eventually here because it takes some time before the new websocket has been
            // successfully created in ZAC
            eventually(30.seconds) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/notificaties",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json",
                        "Authorization",
                        OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "actie" to "partial_update",
                            "kanaal" to "zaken",
                            "resource" to "zaak",
                            "kenmerken" to mapOf(
                                "zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                                "bronorganisatie" to "123443210",
                                "vertrouwelijkheidaanduiding" to "openbaar"
                            ),
                            "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    ).toString(),
                    addAuthorizationHeader = false
                )
                response.code shouldBe HTTP_STATUS_NO_CONTENT
                // because of the retries using eventually, we can end up with duplicate messages. that's ok.
                websocketListener.messagesReceived.size shouldBeGreaterThan 0
            }
        }
        Then(
            """the response should be 'no content' and an event that the zaak has been updated should be sent to the websocket"""
        ) {
            websocketListener.messagesReceived.forAtLeastOne {
                with(JSONObject(it)) {
                    getString("opcode") shouldBe "UPDATED"
                    getString("objectType") shouldBe "ZAAK"
                    getJSONObject("objectId").getString("resource") shouldBe zaakProductaanvraag1Uuid.toString()
                }
            }
        }
    }
    Given("""A websocket subscription is created to listen to all changes made to zaak-rollen""") {
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = """
                {
                    "subscriptionType": "CREATE",
                    "event": {
                        "opcode": "ANY",
                        "objectType": "ZAAK_ROLLEN",
                         "objectId": {
                            "resource": "$zaakProductaanvraag1Uuid"
                         },
                        "_key": "ANY;ZAAK_ROLLEN;$zaakProductaanvraag1Uuid"
                    }
                }
            """.trimIndent()
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When(""""a notification is sent to ZAC that a zaak-rol has been created""") {
            // we need eventually here because it takes some time before the new websocket has been
            // successfully created in ZAC
            eventually(30.seconds) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/notificaties",
                    headers = Headers.headersOf(
                        "Content-Type",
                        "application/json",
                        "Authorization",
                        OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "actie" to "create",
                            "kanaal" to "zaken",
                            "resource" to "rol",
                            "kenmerken" to mapOf(
                                "zaaktype" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                                "bronorganisatie" to "123443210",
                                "vertrouwelijkheidaanduiding" to "openbaar"
                            ),
                            "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
                            "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/rollen/$zaakProductaanvraag1Betrokkene1Uuid",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    ).toString(),
                    addAuthorizationHeader = false
                )
                response.code shouldBe HTTP_STATUS_NO_CONTENT
                // because of the retries using eventually, we can end up with duplicate messages. that's ok.
                websocketListener.messagesReceived.size shouldBeGreaterThan 0
            }
        }
        Then(
            """the response should be 'no content' and an event that the zaak-rol has been updated should be sent to the websocket"""
        ) {
            websocketListener.messagesReceived.forAtLeastOne {
                with(JSONObject(it)) {
                    getString("opcode") shouldBe "UPDATED"
                    getString("objectType") shouldBe "ZAAK_ROLLEN"
                }
            }
        }
    }
})
