/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class DocumentHelper(
    val zacClient: ZacClient,
) {
    val logger = KotlinLogging.logger {}
    val itestHttpClient = zacClient.itestHttpClient

    fun uploadDocumentToZaakAndIndexDocument(
        documentTitle: String,
        fileName: String,
        mediaType: String = PDF_MIME_TYPE,
        vertrouwelijkheidsaanduiding: String = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK,
        zaakUUID: UUID
    ) {
        val response = zacClient.createEnkelvoudigInformatieobjectForZaak(
            zaakUUID = zaakUUID,
            fileName = fileName,
            title = documentTitle,
            fileMediaType = mediaType,
            vertrouwelijkheidaanduiding = vertrouwelijkheidsaanduiding
        )
        val responseBody = response.bodyAsString
        logger.info { "response: $responseBody" }
        response.code shouldBe HTTP_OK
        JSONObject(responseBody).getString("bestandsnaam") shouldBe fileName

        // TODO: send 'create document' notification request to ZAC
        // and perform search to verify reindexing has completed
    }
}
