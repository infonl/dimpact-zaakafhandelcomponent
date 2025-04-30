/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2020_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2024_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.util.UUID

private const val ROWS_DEFAULT = 10
private const val PAGE_DEFAULT = 0
private const val ZOEK_ZAAK_IDENTIFIER = "ZAAK-"

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}
    lateinit var zaakUUID: UUID

    Given("ZAC Docker container is running and the zaakafhandelparameters have been created") {
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/zaak/id/$ZAAK_MANUAL_2024_01_IDENTIFICATION"
        ).use { getZaakResponse ->
            val responseBody = getZaakResponse.body!!.string()
            logger.info { "Response: $responseBody" }
            with(JSONObject(responseBody)) {
                zaakUUID = getString("uuid").let(UUID::fromString)
            }
        }

        When("searching for a HOOFDZAAK linkable zaken for indienen-aansprakelijkstelling-behandelen zaak") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakUUID/zoek-koppelbare-zaken" +
                    "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                    "&relationType=HOOFDZAAK" +
                    "&rows=$ROWS_DEFAULT" +
                    "&page=$PAGE_DEFAULT"
            )

            Then("the response should be a 200 HTTP response") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "isKoppelbaar": true,
                      "omschrijving": "changedDescription",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000006",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000005",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000004",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "In behandeling",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000003",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000002",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000001",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-1999-0000000001",
                      "isKoppelbaar": true,
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeZaakOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": 9,
                  "filters": {}
                }
                """.trimIndent()
            }
        }

        When("searching for a DEELZAAK linkable zaken for melding-evenement-organiseren-behandelen zaak") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakProductaanvraag1Uuid/zoek-koppelbare-zaken" +
                    "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                    "&relationType=DEELZAAK" +
                    "&rows=$ROWS_DEFAULT" +
                    "&page=$PAGE_DEFAULT"
            )

            Then("the response should be a 200 HTTP response") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "id": "e89e21e7-0374-4262-adb5-d69d948bc623",
                      "identificatie": "$ZAAK_MANUAL_2024_01_IDENTIFICATION",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeZaakDescription1",
                      "statustypeOmschrijving": "Wacht op aanvullende informatie",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "$ZAAK_MANUAL_2020_01_IDENTIFICATION",
                      "isKoppelbaar": false,
                      "omschrijving": "changedDescription",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000006",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000005",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000004",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "In behandeling",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000003",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000002",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000001",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-1999-0000000001",
                      "isKoppelbaar": false,
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": 9,
                  "filters": {}
                }
                """.trimIndent()
            }
        }
    }
})
