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
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_MANUAL_2024_01_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.util.UUID

private const val ROWS_DEFAULT = 10
private const val PAGE_DEFAULT = 0
private const val ZOEK_ZAAK_IDENTIFIER = "ZAAK-2000"

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}
    lateinit var zaakUUID: UUID
    lateinit var teKoppelenZaakUuid: UUID

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
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/zaak/id/ZAAK-2000-0000000003"
        ).use { getZaakResponse ->
            val responseBody = getZaakResponse.body!!.string()
            logger.info { "Response: $responseBody" }
            with(JSONObject(responseBody)) {
                teKoppelenZaakUuid = getString("uuid").let(UUID::fromString)
            }
        }

        When(
            """
            searching for a DEELZAAK linkable zaken with 'ZAAK-2000' zaak identifier and then link the linkable
            zaak
            """.trimIndent()
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakUUID/zoek-koppelbare-zaken" +
                    "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                    "&relationType=DEELZAAK" +
                    "&rows=$ROWS_DEFAULT" +
                    "&page=$PAGE_DEFAULT"
            )

            Then("returns list of zaken each with a linkable flag") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "identificatie": "ZAAK-2000-0000000006",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000005",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000004",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "In behandeling",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000003",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000002",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000001",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": 6,
                  "filters": {}
                }
                """.trimIndent()
            }
        }

        When("link zaak-ZAAK-2000-0000000003 as hoofdzaak to $ZAAK_MANUAL_2024_01_IDENTIFICATION") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/koppel",
                requestBodyAsString = """
                {
                     "zaakUuid": "$teKoppelenZaakUuid",
                     "teKoppelenZaakUuid": "$zaakUUID",
                     "relatieType": "HOOFDZAAK"
                }
                """.trimIndent()
            )

            Then("successfully links the zaak") {
                response.code shouldBe HTTP_STATUS_NO_CONTENT
            }
        }

        When(
            """
                searching for a HOOFDZAAK linkable zaken with 'ZAAK-2000' zaak identifier but without the 'rows' and 
                'page' request parameters
            """.trimIndent()
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakProductaanvraag1Uuid/zoek-koppelbare-zaken" +
                    "?zoekZaakIdentifier=$ZOEK_ZAAK_IDENTIFIER" +
                    "&relationType=HOOFDZAAK"
            )

            Then("returns list of zaken each with a linkable flag") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                {
                  "foutmelding": "",
                  "resultaten": [
                    {
                      "identificatie": "ZAAK-2000-0000000006",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    { 
                      "identificatie": "ZAAK-2000-0000000005",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Afgerond",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000004",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "In behandeling",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000003",
                      "isKoppelbaar": false,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000002",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    },
                    {
                      "identificatie": "ZAAK-2000-0000000001",
                      "isKoppelbaar": true,
                      "omschrijving": "fakeOmschrijving",
                      "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                      "statustypeOmschrijving": "Intake",
                      "type": "ZAAK"
                    }
                  ],
                  "totaal": 6,
                  "filters": {}
                }
                """.trimIndent()
            }
        }
    }
})
