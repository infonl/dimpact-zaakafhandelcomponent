/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_BSN
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_KVK
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATION_TYPE_VESTIGING
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_3_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_4_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_5_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_5_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_COMBO_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_COMBO_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_VESTIGINGS_ONLY_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_FORMULIER_BRON_NAAM
import nl.info.zac.itest.config.ItestConfiguration.OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE
import nl.info.zac.itest.config.ItestConfiguration.TEST_GEMEENTE_EMAIL_ADDRESS
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_3_ALTERNATIVE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_3_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_3_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_3_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_4_ALTERNATIVE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_4_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_4_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_4_TOELICHTING
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_COMBO_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_INVALID_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * This test tests the productaanvraag flow in ZAC for CMMN zaaktypes.
 * The productaanvraag flow starts with a received productaanvraag notification.
 */
@Suppress("LargeClass")
class NotificationProductaanvraagCmmnTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    lateinit var zaakProductaanvraag1Uuid: UUID
    lateinit var zaakProductaanvraagComboUuid: UUID

    Context("Productaanvraag with an initiator of type person") {
        Given(
            """
            A productaanvraag object exists in Objecten with an initiator with a BSN number and a productaanvraag type, 
            a zaaktype CMMN configuration is defined in ZAC with the same productaanvraag type
            and with 'automatic acknowledgement of receipt' (ontvangstbevestiging) enabled,
            and the related productaanvraag PDF exists in Open Zaak
            """.trimIndent()
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
                            "kanaal" to "objecten",
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_1_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_1_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )
                Then(
                    """the response should be 'no content', a zaak should be created in OpenZaak
                        using zaaktype 'melding klein evenement' and a zaak CMMN proces should be started in ZAC"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                            // since we have defined zaaktypeCmmnConfiguration for this zaaktype a CMMN proces should be started
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("omschrijving") shouldBe ZAAK_PRODUCTAANVRAAG_1_OMSCHRIJVING
                            getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OBJECT_PRODUCTAANVRAAG_1_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_1_TOELICHTING"
                            getString("uiterlijkeEinddatumAfdoening") shouldBe ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING
                            with(getJSONObject("zaakgeometrie").getJSONObject("point")) {
                                getBigDecimal("latitude") shouldBe PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LATITUDE.toBigDecimal()
                                getBigDecimal("longitude") shouldBe PRODUCTAANVRAAG_ZAAKGEGEVENS_GEOMETRY_LONGITUDE.toBigDecimal()
                            }
                            with(getJSONObject("initiatorIdentificatie")) {
                                getString("type") shouldBe BETROKKENE_IDENTIFICATION_TYPE_BSN
                                UUID.fromString(getString("temporaryPersonId"))
                            }
                            zaakProductaanvraag1Uuid = getString("uuid").let(UUID::fromString)
                        }
                    }
                }

                And("an automated acknowledgement of receipt email is sent") {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/$TEST_PERSON_HENDRIKA_JANSE_EMAIL/messages/"
                    )
                    logger.info { "Response: ${receivedMailsResponse.bodyAsString}" }
                    receivedMailsResponse.code shouldBe HTTP_OK

                    val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    receivedMails.length() shouldBeGreaterThan 0
                    (0 until receivedMails.length()).map { receivedMails.getJSONObject(it) }
                        .forAtLeastOne { mail ->
                            mail.getString("subject") shouldContain
                                "Ontvangstbevestiging van zaak $ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION"
                            mail.getString("contentType") shouldStartWith "multipart/mixed"
                            with(mail.getString("mimeMessage")) {
                                shouldContain("From: $CONFIG_GEMEENTE_NAAM <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Wij hebben uw verzoek ontvangen en deze op")
                            }
                        }
                }
            }

            When("the get betrokkene endpoint is called for the zaak created from the productaanvraag") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/betrokkene",
                    testUser = RAADPLEGER_1
                )

                Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
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
                    UUID.fromString(JSONArray(responseBody).getJSONObject(0).getString("temporaryPersonId"))
                    UUID.fromString(JSONArray(responseBody).getJSONObject(1).getString("temporaryPersonId"))
                    UUID.fromString(JSONArray(responseBody).getJSONObject(2).getString("temporaryPersonId"))
                    JSONArray(responseBody).getJSONObject(0).getString("rolid").let(UUID::fromString) shouldNotBe null
                }
            }
        }

        Given(
            """
            A productaanvraag object exists with an initiator of type person that changes their standard email address, 
            betrokkene in Objecten with a productaanvraag type exists,
            zaaktype CMMN configuration is defined in ZAC with the same productaanvraag type and with
            'automatic acknowledgement of receipt' (ontvangstbevestiging) enabled
            """.trimIndent()
        ) {
            When(
                """
                the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header, 
                changed email address and initiator of type 'BSN'
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
                            "kanaal" to "objecten",
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_5_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_5_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )
                Then(
                    """the response should be 'no content', a zaak should be created in OpenZaak
                        using zaaktype 'melding klein evenement' and a zaak CMMN proces should be started in ZAC
                        with the correct zaak identification and no zaak specific contact details."""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_5_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                            // since we have defined zaaktypeCmmnConfiguration for this zaaktype a CMMN proces should be started
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("toelichting") shouldContain "kenmerk '$OBJECT_PRODUCTAANVRAAG_5_BRON_KENMERK'"
                            has("zaakSpecificContactDetails") shouldBe false
                        }
                    }
                }
            }
        }
    }

    Context("Productaanvraag with an initiator of type KVK rechtspersoon") {
        Given(
            """A productaanvraag object exists in Objecten with an initiator with a KVK nummer,
             and a CMMN zaaktype configuration exists in ZAC for the same productaanvraag type"""
        ) {
            When(
                """
                the notificaties endpoint is called with a second 'create productaanvraag' payload with authentication header
                 and without zaakgegevens and with an initiator of type 'KVK nummer'
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
                            "kanaal" to "objecten",
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_2_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_2_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )

                Then(
                    "the response should be 'no content', a zaak should be created and started in ZAC"
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                            // since we have defined zaaktypeCmmnConfiguration for this zaaktype a CMMN proces should be started
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OPEN_FORMULIEREN_PRODUCTAANVRAAG_FORMULIER_2_BRON_KENMERK'."
                            with(getJSONObject("initiatorIdentificatie")) {
                                getString("kvkNummer") shouldBe TEST_KVK_NUMMER_1
                                getString("type") shouldBe BETROKKENE_IDENTIFICATION_TYPE_KVK
                            }
                        }
                    }
                }

                And("an automated email is sent") {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/$TEST_KVK_EMAIL/messages/",
                        testUser = RAADPLEGER_1
                    )
                    receivedMailsResponse.code shouldBe HTTP_OK

                    val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    with(receivedMails) {
                        length() shouldBe 1
                        with(getJSONObject(0)) {
                            getString("subject") shouldContain
                                "Ontvangstbevestiging van zaak $ZAAK_PRODUCTAANVRAAG_2_IDENTIFICATION"
                            getString("contentType") shouldStartWith "multipart/mixed"
                            with(getString("mimeMessage")) {
                                shouldContain("From: $CONFIG_GEMEENTE_NAAM <$TEST_GEMEENTE_EMAIL_ADDRESS")
                                shouldContain("Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Wij hebben uw verzoek ontvangen en deze op")
                            }
                        }
                    }
                }
            }
        }
    }

    Context("Productaanvraag with an initiator of type KVK vestiging") {
        Given(
            """A productaanvraag object exists in Objecten with an initiator with both a KVK nummer and a vestigingsnummer,
            and a CMMN zaaktype configuration exists in ZAC for the same productaanvraag type"""
        ) {
            When(
                """
                the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header
                 and with an initiator of type 'kvkNummer' and 'vestigingsNummer'
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
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_COMBO_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_COMBO_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )

                Then(
                    """the response should be 'no content', a zaak should be created in OpenZaak
                        and a zaak productaanvraag proces should be started in ZAC with both kvkNummer and vestigingsNummer"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_COMBO_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_COMBO_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OBJECT_PRODUCTAANVRAAG_COMBO_BRON_KENMERK'."
                            with(getJSONObject("initiatorIdentificatie")) {
                                getString("kvkNummer") shouldBe TEST_KVK_NUMMER_1
                                getString("vestigingsnummer") shouldBe TEST_KVK_VESTIGINGSNUMMER_1
                                getString("type") shouldBe BETROKKENE_IDENTIFICATION_TYPE_VESTIGING
                            }
                            zaakProductaanvraagComboUuid = getString("uuid").let(UUID::fromString)
                        }
                    }
                }
            }

            When("the get betrokkene endpoint is called for the combo zaak created from the productaanvraag") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraagComboUuid/betrokkene",
                    testUser = RAADPLEGER_1
                )
                Then("the response should be a 200 HTTP response with a list consisting of the betrokkenen") {
                    response.code shouldBe HTTP_OK
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [ {
                      "identificatie" : "999991838",
                      "roltoelichting" : "Overgenomen vanuit de product aanvraag",
                      "roltype" : "Medeaanvrager",
                      "type" : "NATUURLIJK_PERSOON"
                    } ]
                    """.trimIndent()
                    UUID.fromString(JSONArray(responseBody).getJSONObject(0).getString("temporaryPersonId"))
                }
            }
        }

        Given(
            "A productaanvraag object exists in Objecten with an initiator with only a vestigingsnummer (invalid scenario)"

        ) {
            When(
                """
                the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header
                 and with an initiator of type 'vestigingsNummer' only (should be invalid)
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
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_VESTIGINGS_ONLY_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_VESTIGINGS_ONLY_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )

                val getZaakResponse = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_INVALID_IDENTIFICATION",
                    testUser = RAADPLEGER_1
                )

                Then(
                    """the zaak should still be created in OpenZaak
                        even though only vestigingsNummer without KVK nummer is invalid"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT
                    getZaakResponse.code shouldBe HTTP_OK
                }

                And("No initiator should be set") {
                    val responseBody = getZaakResponse.bodyAsString
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_INVALID_IDENTIFICATION
                        has("initiatorIdentificatie") shouldBe false
                    }
                }
            }
        }
    }

    Context("Productaanvraag without an initiator") {
        Given(
            """
            A productaanvraag object exists without an initiator and with a productaanvraag-specific email address in Objecten, 
            a zaaktype CMMN configuration is defined in ZAC configured for the same productaanvraag type and with 'automatic acknowledgement of receipt'
            (ontvangstbevestiging) enabled, and the related productaanvraag PDF exists in Open Zaak
                """
        ) {
            When(
                """
                the notification endpoint is called with a 'create productaanvraag' payload with authentication header, 
                alternative email address without initiator'
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
                            "kanaal" to "objecten",
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_3_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_3_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )
                Then(
                    """the response should be 'no content', a zaak should be created in OpenZaak
                        using zaaktype 'melding klein evenement' and a zaak CMMN proces should be started in ZAC
                        with the correct identification"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_3_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_3_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                            // since we have defined zaaktypeCmmnConfiguration for this zaaktype a CMMN proces should be started
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("omschrijving") shouldBe ZAAK_PRODUCTAANVRAAG_3_OMSCHRIJVING
                            getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OBJECT_PRODUCTAANVRAAG_3_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_3_TOELICHTING"
                            getJSONObject("zaakSpecificContactDetails").getString("emailAddress") shouldBe
                                ZAAK_PRODUCTAANVRAAG_3_ALTERNATIVE_EMAIL
                        }
                    }
                }

                And("an automated acknowledgement of receipt email is sent to alternative email address") {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/$ZAAK_PRODUCTAANVRAAG_3_ALTERNATIVE_EMAIL/messages/"
                    )
                    logger.info { "Response: ${receivedMailsResponse.bodyAsString}" }
                    receivedMailsResponse.code shouldBe HTTP_OK

                    val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    with(receivedMails) {
                        length() shouldBe 1
                        with(getJSONObject(0)) {
                            getString("subject") shouldContain
                                "Ontvangstbevestiging van zaak $ZAAK_PRODUCTAANVRAAG_3_IDENTIFICATION"
                            getString("contentType") shouldStartWith "multipart/mixed"
                            with(getString("mimeMessage")) {
                                shouldContain("From: $CONFIG_GEMEENTE_NAAM <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Wij hebben uw verzoek ontvangen en deze op")
                            }
                        }
                    }
                }
            }
        }

        Given(
            """
            A productaanvraag object exists without an initiator and with a productaanvraag-specific email address and betrokkene in Objecten, 
            a zaaktype CMMN configuration is defined in ZAC with the same productaanvraag type and with 'automatic acknowledgement of
            receipt' (ontvangstbevestiging) enabled, and the related productaanvraag PDF exists in Open Zaak
                """
        ) {
            When(
                """
                the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header, 
                alternative email address and initiator of type 'BSN'
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
                            "kanaal" to "objecten",
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_4_UUID",
                            "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_4_UUID",
                            "actie" to "create",
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                            )
                        )
                    ).toString()
                )
                Then(
                    """the response should be 'no content', a zaak should be created in OpenZaak
                        using zaaktype 'melding klein evenement' and a zaak CMMN proces should be started in ZAC
                        with the correct identification for the zaak"""
                ) {
                    response.code shouldBe HTTP_NO_CONTENT

                    // retrieve the newly created zaak and check the contents
                    itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/zaken/zaak/id/$ZAAK_PRODUCTAANVRAAG_4_IDENTIFICATION",
                        testUser = RAADPLEGER_1
                    ).let { getZaakResponse ->
                        val responseBody = getZaakResponse.bodyAsString
                        logger.info { "Response: $responseBody" }
                        with(JSONObject(responseBody)) {
                            getString("identificatie") shouldBe ZAAK_PRODUCTAANVRAAG_4_IDENTIFICATION
                            getJSONObject("zaaktype").getString("uuid") shouldBe ZAAKTYPE_CMMN_TEST_3_UUID.toString()
                            getJSONObject("status").getString("naam") shouldBe "Intake"
                            getJSONObject("groep").getString("id") shouldBe GROUP_BEHANDELAARS_TEST_1.name
                            // 'proces gestuurd' is true when a BPMN rather than a CMMN proces has been started
                            // since we have defined zaaktypeCmmnConfiguration for this zaaktype a CMMN proces should be started
                            getBoolean("isProcesGestuurd") shouldBe false
                            getString("communicatiekanaal") shouldBe "E-formulier"
                            getString("omschrijving") shouldBe ZAAK_PRODUCTAANVRAAG_4_OMSCHRIJVING
                            getString("toelichting") shouldBe "Aangemaakt vanuit $OPEN_FORMULIEREN_FORMULIER_BRON_NAAM " +
                                "met kenmerk '$OBJECT_PRODUCTAANVRAAG_4_BRON_KENMERK'. $ZAAK_PRODUCTAANVRAAG_4_TOELICHTING"
                            getJSONObject("zaakSpecificContactDetails").getString("emailAddress") shouldBe
                                ZAAK_PRODUCTAANVRAAG_4_ALTERNATIVE_EMAIL
                        }
                    }
                }

                And("an automated acknowledgement of receipt email is sent to alternative email address 2") {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/$ZAAK_PRODUCTAANVRAAG_4_ALTERNATIVE_EMAIL/messages/"
                    )
                    logger.info { "Response: ${receivedMailsResponse.bodyAsString}" }
                    receivedMailsResponse.code shouldBe HTTP_OK

                    val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    with(receivedMails) {
                        length() shouldBe 1
                        with(getJSONObject(0)) {
                            getString("subject") shouldContain
                                "Ontvangstbevestiging van zaak $ZAAK_PRODUCTAANVRAAG_4_IDENTIFICATION"
                            getString("contentType") shouldStartWith "multipart/mixed"
                            with(getString("mimeMessage")) {
                                shouldContain("From: $CONFIG_GEMEENTE_NAAM <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>")
                                shouldContain("Wij hebben uw verzoek ontvangen en deze op")
                            }
                        }
                    }
                }
            }
        }
    }
})
