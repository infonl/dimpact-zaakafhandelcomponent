/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate

class EnkelvoudigInformatieObjectRestServiceHistorieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val now = System.currentTimeMillis()

    Context("Listing informatieobject history") {
        Given(
            """
                A zaak exists with a zaaktype in domain test 1, a document has been uploaded to this zaak,
                and a raadpleger for domain test 1 is logged in
                """
        ) {
            authenticate(BEHANDELAAR_DOMAIN_TEST_1)
            val zaakDescription =
                "${EnkelvoudigInformatieObjectRestServiceHistorieTest::class.simpleName}-listing-$now"
            val documentTitle = "${EnkelvoudigInformatieObjectRestServiceHistorieTest::class.simpleName}-documenttitle-$now"
            val documentAuthorName = "fakeAuthorName"
            val (_, zaakUuid) = zaakHelper.createZaak(
                zaakDescription = zaakDescription,
                zaaktypeUuid = ZAAKTYPE_TEST_2_UUID
            )
            val (enkelvoudiginformatieobjectUuid, enkelvoudiginformatieobjectIdentification) =
                documentHelper.uploadDocumentToZaak(
                    zaakUuid = zaakUuid,
                    documentTitle = documentTitle,
                    authorName = documentAuthorName,
                    fileName = TEST_PDF_FILE_NAME
                )
            authenticate(RAADPLEGER_DOMAIN_TEST_1)

            When("informatieobjecten history is requested") {
                val today = LocalDate.now()
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudiginformatieobjectUuid/historie"
                )

                Then("the response should be ok and the expected history records are returned") {
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody shouldEqualJsonIgnoringExtraneousFields """
                        [ 
                            {
                              "actie" : "GEKOPPELD",
                              "applicatie" : "ZAC",
                              "attribuutLabel" : "indicatieGebruiksrecht",
                              "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                              "nieuweWaarde" : "geen",
                              "toelichting" : ""
                            }, 
                            {
                              "actie" : "GEKOPPELD",
                              "applicatie" : "ZAC",
                              "attribuutLabel" : "informatieobject",
                              "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                              "nieuweWaarde" : "$enkelvoudiginformatieobjectIdentification",
                              "toelichting" : ""
                            } 
                        ]
                    """.trimIndent()
                    JSONArray(responseBody).forEach { item ->
                        (item as JSONObject).getString("datumTijd") shouldStartWith "$today"
                    }
                }
            }
        }
    }
})
