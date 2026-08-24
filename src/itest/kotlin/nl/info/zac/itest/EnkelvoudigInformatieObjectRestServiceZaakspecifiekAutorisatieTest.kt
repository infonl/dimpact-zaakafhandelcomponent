/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK

class EnkelvoudigInformatieObjectRestServiceZaakspecifiekAutorisatieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    given(
        """
        A document has been added to a CMMN zaak of a zaaktype that supports zaakspecifieke
        autorisatie, and the zaak is then marked as zaakspecifiek geautoriseerd
        """
    ) {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = "itestDocumentTitle-${System.currentTimeMillis()}",
            authorName = FAKE_AUTHOR_NAME,
            testUser = BEHANDELAAR_1
        )
        openZaakClient.createZaakeigenschap(
            zaakUUID = zaakUuid,
            zaaktypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            eigenschapNaam = "ZAAK_GEAUTORISEERD",
            waarde = "true"
        )

        `when`(
            "the document is read by a behandelaar authorized for the zaaktype but without the " +
                "zaakspecifiek_geautoriseerd application role"
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid",
                testUser = BEHANDELAAR_1
            )
            then("the response should be a 200 HTTP response with rechten.lezen set to false") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                JSONObject(responseBody).getJSONObject("rechten").getBoolean("lezen") shouldBe false
            }
        }
        `when`("the document is read by a user holding the zaakspecifiek_autorisatie_behandelaar role") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid",
                testUser = ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
            )
            then("the response should be a 200 HTTP response with rechten.lezen set to true") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                JSONObject(responseBody).getJSONObject("rechten").getBoolean("lezen") shouldBe true
            }
        }
    }
})
