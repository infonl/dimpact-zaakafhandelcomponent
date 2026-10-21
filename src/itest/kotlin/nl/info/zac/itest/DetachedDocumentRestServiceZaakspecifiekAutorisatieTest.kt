/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.PDF_MEDIA_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class DetachedDocumentRestServiceZaakspecifiekAutorisatieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    // ZAAKTYPE_CMMN_TEST_2 is shared with other itests, so afterSpec unflags the created zaak again.
    val flaggedZaken = mutableListOf<Pair<UUID, UUID>>()

    afterSpec {
        flaggedZaken.forEach { (zaakUuid, zaakeigenschapUuid) ->
            openZaakClient.deleteZaakeigenschap(zaakUuid, zaakeigenschapUuid)
            openZaakClient.sendZaakeigenschapDestroyNotification(zaakUuid, zaakeigenschapUuid)
        }
    }

    fun listDetachedDocument(documentUuid: UUID) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/ontkoppeldedocumenten",
            requestBodyAsString = """
                {
                    "page": 0,
                    "maxResults": 100,
                    "sort": "",
                    "order": ""
                }
            """.trimIndent(),
            testUser = COORDINATOR_1
        ).let { response ->
            response.code shouldBe HTTP_OK
            JSONObject(response.bodyAsString).getJSONArray("resultaten").let { resultaten ->
                (0 until resultaten.length())
                    .map { resultaten.getJSONObject(it) }
                    .firstOrNull { it.getString("documentUUID") == documentUuid.toString() }
            }
        }

    given(
        """
        A document has been detached from an indexed zaak, after which the zaak is marked as zaakspecifiek
        geautoriseerd
        """
    ) {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            indexZaak = true,
            testUser = BEHANDELAAR_1
        )
        val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = "detachedDocumentZaakspecifiekItestTitle-${System.currentTimeMillis()}",
            authorName = FAKE_AUTHOR_NAME,
            mediaType = PDF_MEDIA_TYPE,
            testUser = BEHANDELAAR_1
        )
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/zaakinformatieobjecten/ontkoppel",
            requestBodyAsString = JSONObject(
                mapOf(
                    "zaakUUID" to zaakUuid.toString(),
                    "documentUUID" to documentUuid.toString(),
                    "reden" to "fakeDetachReason"
                )
            ).toString(),
            testUser = BEHANDELAAR_1
        ).code shouldBe HTTP_NO_CONTENT
        listDetachedDocument(documentUuid).shouldNotBeNull().getBoolean("isZaakspecifiekGeautoriseerd") shouldBe false
        val zaakeigenschapUuid = openZaakClient.createZaakeigenschap(
            zaakUUID = zaakUuid,
            zaaktypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            eigenschapNaam = "ZAAK_GEAUTORISEERD",
            waarde = "true"
        )
        flaggedZaken += zaakUuid to zaakeigenschapUuid
        // createZaakeigenschap() bypasses ZAC, so simulate the notificatie that makes ZAC reindex the zaak.
        openZaakClient.sendZaakeigenschapCreateNotification(zaakUuid, zaakeigenschapUuid)

        `when`(
            """
            a coordinator without the zaakspecifiek_geautoriseerd role lists the ontkoppelde documenten once the
            zaak has been reindexed
            """
        ) {
            then("the detached document is marked as belonging to a zaakspecifiek geautoriseerde zaak") {
                eventually(30.seconds) {
                    val detachedDocument = listDetachedDocument(documentUuid).shouldNotBeNull()
                    logger.info { "Detached document: $detachedDocument" }
                    detachedDocument.getString("zaakID") shouldBe zaakIdentificatie
                    detachedDocument.getBoolean("isZaakspecifiekGeautoriseerd") shouldBe true
                }
            }
        }
    }
})
