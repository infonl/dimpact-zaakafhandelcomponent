/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class DocumentHelper(
    val zacClient: ZacClient,
) {
    val logger = KotlinLogging.logger {}
    val itestHttpClient = zacClient.itestHttpClient

    /**
     * Uploads a document to the specified zaak and triggers indexing of the document.
     * Waits until the document is indexed by searching for it by its title.
     * For this reason we assume that the provided document title is unique within the integration test suite scope.
     *
     * Returns the UUID and identification of the created document.
     */
    @Suppress("LongParameterList")
    suspend fun uploadDocumentToZaakAndIndexDocument(
        zaakUuid: UUID,
        fileName: String,
        documentTitle: String,
        authorName: String,
        mediaType: String = PDF_MIME_TYPE,
        vertrouwelijkheidsaanduiding: String = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK,
    ): Pair<UUID, String> {
        val response = zacClient.createEnkelvoudigInformatieobjectForZaak(
            zaakUUID = zaakUuid,
            fileName = fileName,
            title = documentTitle,
            authorName = authorName,
            fileMediaType = mediaType,
            vertrouwelijkheidaanduiding = vertrouwelijkheidsaanduiding
        )
        val responseBody = response.bodyAsString
        logger.info { "response: $responseBody" }
        response.code shouldBe HTTP_OK
        val responseBodyAsJsonObject = JSONObject(responseBody)
        responseBodyAsJsonObject.getString("bestandsnaam") shouldBe fileName
        val informatieobjectUuid = responseBodyAsJsonObject.getString("uuid").run(UUID::fromString)
        val informatieobjectIdentification = responseBodyAsJsonObject.getString("identificatie")
        // trigger the notification service to index the document
        sendEnkelvoudiginformatieobjectCreateNotification(informatieobjectUuid)
        // wait for the indexing to complete by searching for the newly created document until we get the expected result
        // note that this assumes that the document title is unique
        eventually(10.seconds) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "DOCUMENT_TITEL": "$documentTitle" },
                    "filters": {},
                    "datums": {},
                    "rows": 1,
                    "page": 0,
                    "type": "DOCUMENT"
                }
                """.trimIndent()
            )
            JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
        }
        return Pair(informatieobjectUuid, informatieobjectIdentification)
    }

    private fun sendEnkelvoudiginformatieobjectCreateNotification(informatieobjectUuid: UUID) {
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "documenten",
                    "resource" to "enkelvoudiginformatieobject",
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/documenten/api/v1/enkelvoudiginformatieobjecten/$informatieobjectUuid",
                    "resourceUrl" to "$OPEN_ZAAK_BASE_URI/documenten/api/v1/enkelvoudiginformatieobjecten/$informatieobjectUuid",
                    "actie" to "create",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString(),
            addAuthorizationHeader = false
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
    }
}
