/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

private const val ROWS_DEFAULT = 10
private const val PAGE_DEFAULT = 0

class ZaakKoppelenRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaakHelper = ZaakHelper(ZacClient(itestHttpClient))
    val logger = KotlinLogging.logger {}
    val now = System.currentTimeMillis()

    Given("Two zaken have been created and the behandelaar for domain test 1 is logged in") {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
        val zaakDescription = "${ZaakKoppelenRestServiceTest::class.simpleName}-listingsearchresults1-$now"
        val toBeLinkedZaakDescription = "${ZaakKoppelenRestServiceTest::class.simpleName}-listingsearchresults2-$now"
        val (_, zaakUuid) = zaakHelper.createAndIndexZaak(
            zaakDescription = zaakDescription,
            zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
            group = BEHANDELAARS_DOMAIN_TEST_1,
            startDate = DATE_TIME_2000_01_01
        )
        val (teKoppelenZaakIdentification, teKoppelenZaakUuid) = zaakHelper.createAndIndexZaak(
            zaakDescription = toBeLinkedZaakDescription,
            zaaktypeUuid = ZAAKTYPE_TEST_3_UUID,
            group = BEHANDELAARS_DOMAIN_TEST_1,
            startDate = DATE_TIME_2000_01_01
        )

        When(
            """
            searching for a DEELZAAK linkable zaken on the first created zaak for the
            'to be linked' zaak identifier
            """.trimIndent()
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakUuid/zoek-koppelbare-zaken" +
                    "?zoekZaakIdentifier=$teKoppelenZaakIdentification" +
                    "&relationType=DEELZAAK" +
                    "&rows=$ROWS_DEFAULT" +
                    "&page=$PAGE_DEFAULT"
            )

            Then("it returns the 'to be linked' zaak as linkable zaak") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringOrder """
                    {
                      "foutmelding" : "",
                      "resultaten" : [ {
                        "id" : "$teKoppelenZaakUuid",
                        "identificatie" : "$teKoppelenZaakIdentification",
                        "isKoppelbaar" : true,
                        "omschrijving" : "$toBeLinkedZaakDescription",
                        "statustypeOmschrijving" : "Intake",
                        "type" : "ZAAK",
                        "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_3_DESCRIPTION"
                      } ],
                      "totaal" : 1,
                      "filters" : { }
                    }
                """.trimIndent()
            }
        }

        When("link the first created zaak as hoofdzaak to the 'to be linked' created zaak") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/koppel",
                requestBodyAsString = """
                {
                     "zaakUuid": "$teKoppelenZaakUuid",
                     "teKoppelenZaakUuid": "$zaakUuid",
                     "relatieType": "HOOFDZAAK"
                }
                """.trimIndent()
            )

            Then("successfully links the zaken") {
                response.code shouldBe HTTP_NO_CONTENT
            }
        }
    }
})
