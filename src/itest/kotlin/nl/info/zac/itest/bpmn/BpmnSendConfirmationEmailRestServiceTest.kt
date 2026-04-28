/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_UITERLIJKE_EINDDATUM_AFDOENING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for the automatische ontvangstbevestiging BPMN process.
 * Verifies that when a productaanvraag is received, a zaak is created and a confirmation email
 * is automatically sent to the initiator via the SendConfirmationEmailDelegate.
 */
class BpmnSendConfirmationEmailRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        """
            A productaanvraag object exists in Objecten with type 'bpmn-test-5-productaanvraagtype',
            a zaaktypeBpmnConfiguration is defined in ZAC for zaaktype 5 with the
            'automatischeOntvangstBevestiging' BPMN process, and the initiator BSN 999993896
            (Hendrika Janse) has a known email address
        """.trimIndent()
    ) {
        When("the notificaties endpoint is called with a 'create productaanvraag' payload") {
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
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_UUID",
                        "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_UUID",
                        "actie" to "create",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                        "kenmerken" to mapOf(
                            "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                        )
                    )
                ).toString()
            )

            Then("the response is 'no content' and a zaak is created with the BPMN zaaktype") {
                response.code shouldBe HTTP_NO_CONTENT

                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_IDENTIFICATION",
                    testUser = RAADPLEGER_DOMAIN_TEST_1
                ).let { getZaakResponse ->
                    val responseBody = getZaakResponse.bodyAsString
                    logger.info { "Response: $responseBody" }
                    getZaakResponse.code shouldBe HTTP_OK
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_IDENTIFICATION
                        getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_BPMN_TEST_5_UUID.toString()
                        getJSONObject("zaaktype").getString("omschrijving") shouldBe ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
                        getBoolean("isOpen") shouldBe true
                        getString("communicatiekanaal") shouldBe "E-formulier"
                        getString("uiterlijkeEinddatumAfdoening") shouldBe
                            ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_UITERLIJKE_EINDDATUM_AFDOENING
                        getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                            "met kenmerk '$OBJECT_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_BRON_KENMERK'."
                    }
                }
            }

            And("a confirmation email is sent to the initiator Hendrika Janse") {
                eventually(30.seconds) {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/$TEST_PERSON_HENDRIKA_JANSE_EMAIL/messages/"
                    )
                    logger.info { "Received mails response: ${receivedMailsResponse.bodyAsString}" }
                    receivedMailsResponse.code shouldBe HTTP_OK

                    val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    receivedMails.length() shouldBeGreaterThan 0
                    (0 until receivedMails.length()).map { receivedMails.getJSONObject(it) }
                        .forAtLeastOne { mail ->
                            mail.getString("subject") shouldContain
                                "Informatie over zaak $ZAAK_PRODUCTAANVRAAG_SEND_CONFIRMATION_EMAIL_IDENTIFICATION"
                        }
                }
            }
        }
    }
})
