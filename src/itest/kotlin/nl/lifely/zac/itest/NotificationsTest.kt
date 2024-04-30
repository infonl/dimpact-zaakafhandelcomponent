/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_KENMERK
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_INITIAL
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_UITERLIJKE_EINDDATUM_AFDOENING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import nl.lifely.zac.itest.config.dockerComposeContainer
import nl.lifely.zac.itest.util.WebSocketTestListener
import okhttp3.Headers
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
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
                response.code shouldBe HttpStatus.SC_FORBIDDEN
            }
        }
    }
    Given(
        """"ZAC and all related Docker containers are running, productaanvraag object exists in Objecten API
                    and productaanvraag PDF exists in Open Zaak"""
    ) {
        When(
            """the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header"""
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
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_UUID",
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
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()

                // retrieve the newly created zaak and check the contents
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/zaak/id/$ZAAK_1_IDENTIFICATION"
                ).use { getZaakResponse ->
                    val responseBody = getZaakResponse.body!!.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_1_IDENTIFICATION
                        getJSONObject("zaaktype")
                            .getString("uuid") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID.toString()
                        getJSONObject("status").getString("naam") shouldBe "Intake"
                        getJSONObject("groep").getString("id") shouldBe "test-group-a"
                        // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                        // since we have defined zaakafhandelparameters for this zaaktype a CMMN proces should be started
                        getBoolean("isProcesGestuurd") shouldBe false
                        getJSONObject("communicatiekanaal")
                            .getString("naam") shouldBe "E-formulier"
                        getString("omschrijving") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                            "met kenmerk '$OPEN_FORMULIEREN_FORMULIER_BRON_KENMERK'"
                        getString("uiterlijkeEinddatumAfdoening") shouldBe ZAAK_1_UITERLIJKE_EINDDATUM_AFDOENING
                        zaak1UUID = getString("uuid").let(UUID::fromString)
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
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()

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
                "    \"resource\":\"$zaak1UUID\"" +
                "  }," +
                "\"_key\":\"ANY;ZAAK;$zaak1UUID\"" +
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
            eventually(10.seconds) {
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
                            "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaak1UUID",
                            "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaak1UUID",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    ).toString(),
                    addAuthorizationHeader = false
                )
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                websocketListener.messagesReceived.size shouldBe 1
            }
        }
        Then(
            """the response should be 'no content' and an event that the zaak has been updated should be sent to the websocket"""
        ) {
            with(JSONObject(websocketListener.messagesReceived[0])) {
                getString("opcode") shouldBe "UPDATED"
                getString("objectType") shouldBe "ZAAK"
                getJSONObject("objectId").getString("resource") shouldBe zaak1UUID.toString()
            }
        }
    }
})
