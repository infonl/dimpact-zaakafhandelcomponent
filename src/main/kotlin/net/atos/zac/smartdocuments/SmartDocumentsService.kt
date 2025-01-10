/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.model.document.Data
import net.atos.client.smartdocuments.model.document.Deposit
import net.atos.client.smartdocuments.model.document.Document
import net.atos.client.smartdocuments.model.document.File
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.documentcreation.model.DocumentCreationAttendedResponse
import net.atos.zac.util.MediaTypes
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Optional
import java.util.logging.Logger

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class SmartDocumentsService @Inject constructor(
    @RestClient
    private val smartDocumentsClient: SmartDocumentsClient,

    @ConfigProperty(name = "SMARTDOCUMENTS_ENABLED", defaultValue = "false")
    private val enabled: Boolean,

    @ConfigProperty(name = "SMARTDOCUMENTS_CLIENT_MP_REST_URL")
    private val smartDocumentsURL: String,

    @ConfigProperty(name = "SMARTDOCUMENTS_AUTHENTICATION")
    private val authenticationToken: String,

    @ConfigProperty(name = "SMARTDOCUMENTS_FIXED_USER_NAME")
    private val fixedUserName: Optional<String>,

    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    companion object {
        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    fun isEnabled() = enabled

    /**
     * Sends a request to SmartDocuments to create a document using the Smart Documents wizard (= attended mode).
     */
    fun createDocumentAttended(
        data: Data,
        smartDocument: SmartDocument
    ): DocumentCreationAttendedResponse {
        val deposit = Deposit(
            data = data,
            smartDocument = smartDocument
        )
        val userName = fixedUserName.orElse(loggedInUserInstance.get().id).also {
            LOG.fine("Starting Smart Documents wizard for user: '$it'")
        }
        return smartDocumentsClient.attendedDeposit(
            authenticationToken = "Basic $authenticationToken",
            userName = userName,
            deposit = deposit
        ).also {
            LOG.fine("SmartDocuments attended document creation response: $it")
        }.let {
            DocumentCreationAttendedResponse(
                redirectUrl = UriBuilder.fromUri(smartDocumentsURL)
                    .path("smartdocuments/wizard")
                    .queryParam("ticket", it.ticket)
                    .build()
            )
        }
    }

    /**
     * Lists all SmartDocuments templates groups and templates available for the current user.
     *
     * @return A structure describing templates and groups
     */
    fun listTemplates(): SmartDocumentsTemplatesResponse =
        smartDocumentsClient.listTemplates(
            authenticationToken = "Basic $authenticationToken",
            userName = fixedUserName.orElse(loggedInUserInstance.get().id)
        )

    /**
     * Download generated document
     */
    fun downloadDocument(fileId: String): File =
        smartDocumentsClient.downloadFile(
            smartDocumentsId = fileId,
            documentFormat = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType
        ).let { downloadedFile ->
            File(
                fileName = downloadedFile.contentDisposition()
                    .removePrefix("attachment; filename=\"")
                    .removeSuffix("\""),
                document = Document(data = downloadedFile.body().toBase64String()),
                outputFormat = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType
            )
        }
}
