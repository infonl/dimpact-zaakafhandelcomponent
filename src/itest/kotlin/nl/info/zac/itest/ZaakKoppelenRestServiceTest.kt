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
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields

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

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("searching for a HOOFDZAAK linkable zaken") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakProductaanvraag1Uuid/zoek-koppelbare-zaken" +
                        "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                        "&linkType=HOOFDZAAK" +
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
                      "identificatie": "ZAAK-2024-0000000001",
                      "isKoppelbaar": true,
                      "omschrijving": "$ZAAK_DESCRIPTION_1",
                      "statustypeOmschrijving": "Wacht op aanvullende informatie",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "$ZAAK_MANUAL_1_IDENTIFICATION",
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
                      "isKoppelbaar": true,
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
                      "isKoppelbaar": true,
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

        When("searching for a DEELZAAK linkable zaken") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakProductaanvraag1Uuid/zoek-koppelbare-zaken" +
                        "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                        "&linkType=DEELZAAK" +
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
                      "identificatie": "ZAAK-2024-0000000001",
                      "isKoppelbaar": false,
                      "omschrijving": "$ZAAK_DESCRIPTION_1",
                      "statustypeOmschrijving": "Wacht op aanvullende informatie",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "$ZAAK_MANUAL_1_IDENTIFICATION",
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
                      "isKoppelbaar": false,
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
