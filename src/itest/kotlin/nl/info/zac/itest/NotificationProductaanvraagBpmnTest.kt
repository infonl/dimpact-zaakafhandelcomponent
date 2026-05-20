/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_BPMN_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_BPMN_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_BPMN_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_BPMN_UITERLIJKE_EINDDATUM_AFDOENING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * This test tests the productaanvraag flow in ZAC for BPMN zaaktypes.
 * The productaanvraag flow starts with a received productaanvraag notification.
 */
@Suppress("LargeClass")
class NotificationProductaanvraagBpmnTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    lateinit var zaakProductaanvraagUuid: UUID

    Given(
        """
        A productaanvraag object exists in Objecten with a productaanvraag type, 
        and BPMN zaaktype configuration exists in ZAC for the same productaanvraag type
            """
    ) {
        When(
            """
            the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header
            and initiator of type 'BSN'
                """
        ) {
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
                        "kanaal" to "objecten",
                        "resource" to "object",
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_BPMN_UUID",
                        "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_BPMN_UUID",
                        "actie" to "create",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                        "kenmerken" to mapOf(
                            "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                        )
                    )
                ).toString()
            )
            logger.info { "Requested product aanvraag: $OBJECT_PRODUCTAANVRAAG_BPMN_UUID" }

            Then(
                """the response should be 'no content', a zaak should be created in OpenZaak
                    using the BPMN-configured zaaktype and a BPMN process should be started in ZAC"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT

                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_BPMN_IDENTIFICATION",
                    testUser = RAADPLEGER_1
                ).let { getZaakResponse ->
                    val responseBody = getZaakResponse.bodyAsString
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_BPMN_TEST_1_UUID.toString()
                        getJSONObject("zaaktype").getString("omschrijving") shouldBe ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
                        getBoolean("isOpen") shouldBe true
                        getBoolean("isProcesGestuurd") shouldBe true
                        getString("communicatiekanaal") shouldBe "E-formulier"
                        getString("uiterlijkeEinddatumAfdoening") shouldBe ZAAK_PRODUCTAANVRAAG_BPMN_UITERLIJKE_EINDDATUM_AFDOENING
                        getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                            "met kenmerk '$OBJECT_PRODUCTAANVRAAG_BPMN_BRON_KENMERK'."
                        zaakProductaanvraagUuid = getString("uuid").let(UUID::fromString)
                    }
                }
            }

            When("the get betrokkene endpoint is called for the BPMN zaak created from the productaanvraag") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraagUuid/betrokkene",
                    testUser = RAADPLEGER_1
                )

                Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [ {
                      "bsn" : "999992958",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Plaatsvervanger",
                      "type" : "NATUURLIJK_PERSOON"
                    }, {
                      "bsn" : "999991838",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Bewindvoerder",
                      "type" : "NATUURLIJK_PERSOON"
                    }, {
                      "bsn" : "999991838",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Medeaanvrager",
                      "type" : "NATUURLIJK_PERSOON"
                    } ]
                    """.trimIndent()
                }
            }
        }
    }
})
