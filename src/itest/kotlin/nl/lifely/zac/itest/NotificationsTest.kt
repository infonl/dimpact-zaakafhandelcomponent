/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.ProjectConfig
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTS_API_HOSTNAME_URL
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG
import nl.lifely.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import org.testcontainers.containers.wait.strategy.Wait
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

lateinit var zaak1UUID: UUID

/**
 * This test creates a zaak which we use in other tests, and therefore we run this test first.
 */
@Order(0)
class NotificationsTest : BehaviorSpec({
    given("ZAC and all related Docker containers are running") {
        When("the notificaties endpoint is called with dummy payload without authentication header") {
            then("the response should be forbidden") {
                khttp.post(
                    url = "${ZAC_API_URI}/notificaties",
                    headers = mapOf("Content-Type" to "application/json"),
                    data = JSONObject(
                        mapOf(
                            "dummy" to "dummy"
                        )
                    )
                ).apply {
                    statusCode shouldBe HttpStatus.SC_FORBIDDEN
                }
            }
        }
    }
    given(
        "ZAC and all related Docker containers are running, productaanvraag object exists in Objecten API " +
            "and productaanvraag PDF exists in Open Zaak"
    ) {
        When("the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header") {
            then(
                "the response should be 'no content', a zaak should be created in OpenZaak " +
                    "and a zaak productaanvraag proces of type 'Productaanvraag-Denhaag' should be started in ZAC"
            ) {
                khttp.post(
                    url = "${ZAC_API_URI}/notificaties",
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        // this test simulates that Open Notificaties sends the request to ZAC
                        // using the secret API key that is configured in ZAC
                        "Authorization" to OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    data = JSONObject(
                        mapOf(
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_API_HOSTNAME_URL/$OBJECT_PRODUCTAANVRAAG_UUID",
                            "actie" to "create",
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_API_HOSTNAME_URL/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG"
                            ),
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    )
                ).apply {
                    // Note that the 'notificaties' endpoint always returns 'no content' even if things go wrong
                    // since it is a fire-and-forget kind of endpoint.
                    statusCode shouldBe HttpStatus.SC_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    khttp.get(
                        url = "${ZAC_API_URI}/zaken/zaak/id/$ZAAK_1_IDENTIFICATION",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        ),
                    ).apply {
                        logger.info { "Response: $text" }

                        statusCode shouldBe HttpStatus.SC_OK
                        val zaak = JSONObject(text)
                        zaak.getString("identificatie") shouldBe ZAAK_1_IDENTIFICATION
                        zaak.getJSONObject("zaaktype")
                            .getString("uuid") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID.toString()
                        zaak.getJSONObject("status").getString("naam") shouldBe "Intake"
                        zaak.getJSONObject("groep").getString("id") shouldBe "test-group-a"
                        // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                        // since we have defined zaakafhandelparameters for this zaaktype a CMMN proces should be started
                        zaak.getBoolean("isProcesGestuurd") shouldBe false
                        zaak.getJSONObject("communicatiekanaal")
                            .getString("naam") shouldBe "E-formulier"
                        zaak1UUID = UUID.fromString(zaak.getString("uuid"))
                    }
                }
            }
        }
    }
    given(
        "ZAC and all related Docker containers are running"
    ) {
        When(
            "the notificaties endpoint is called with a 'create zaaktype' payload with a " +
                "dummy resourceUrl that does not start with the 'ZGW_API_CLIENT_MP_REST_URL' environment variable"
        ) {
            then(
                "a corresponding error message should be logged in ZAC"
            ) {
                khttp.post(
                    url = "${ZAC_API_URI}/notificaties",
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        // this test simulates that Open Notificaties sends the request to ZAC
                        // using the secret API key that is configured in ZAC
                        "Authorization" to OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    data = JSONObject(
                        mapOf(
                            "resource" to "zaaktype",
                            "resourceUrl" to "http://example.com/dummyResourceUrl",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    )
                ).apply {
                    // Note that the 'notificaties' endpoint always returns 'no content' even if things go wrong
                    // since it is a fire-and-forget kind of endpoint.
                    statusCode shouldBe HttpStatus.SC_NO_CONTENT

                    // we expect ZAC to log an error message indicating that the resourceURL is invalid
                    ProjectConfig.dockerComposeContainer.waitingFor(
                        "zac",
                        Wait.forLogMessage(
                            ".* Er is iets fout gegaan in de Zaaktype-handler bij het afhandelen van notificatie: " +
                                "null ZAAKTYPE CREATE .*: java.lang.RuntimeException: URI 'http://example.com/dummyResourceUrl' does not " +
                                "start with value for environment variable 'ZGW_API_CLIENT_MP_REST_URL': 'http://openzaak.local:8000/' .*",
                            1
                        ).withStartupTimeout(ProjectConfig.THIRTY_SECONDS)
                    )
                }
            }
        }
    }
})
