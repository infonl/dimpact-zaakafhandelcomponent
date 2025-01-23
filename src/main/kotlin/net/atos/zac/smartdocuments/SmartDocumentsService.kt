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
import kotlin.jvm.optionals.getOrDefault

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class SmartDocumentsService @Inject constructor(
    // RestEasy declarative clients use configuration properties
    // To make the client optional we use Instance, which is alternative to @Autowire(required=false) in Spring
    @RestClient
    private val smartDocumentsClient: Instance<SmartDocumentsClient>,

    // With nullable Kotlin types ConfigProperty and Weld error with:
    //     io.smallrye.config.inject.ConfigException: SRCFG02000: Failed to Inject @ConfigProperty for key
    // Therefore we use Optional to support non-mandatory properties.
    // Weld inject Optional.empty() if property is not available and overrides default value

    @ConfigProperty(name = "SMARTDOCUMENTS_ENABLED")
    private val enabled: Optional<Boolean> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_CLIENT_MP_REST_URL")
    private val smartDocumentsURL: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_AUTHENTICATION")
    private val authenticationToken: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "SMARTDOCUMENTS_FIXED_USER_NAME")
    private val fixedUserName: Optional<String> = Optional.empty(),

    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    init {
        if (isEnabled()) {
            require(smartDocumentsURL.isPresent) { "SMARTDOCUMENTS_CLIENT_MP_REST_URL environment variable required" }
            require(authenticationToken.isPresent) { "SMARTDOCUMENTS_AUTHENTICATION environment variable required" }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    fun isEnabled() = enabled.getOrDefault(false)

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
        return smartDocumentsClient.get().attendedDeposit(
            authenticationToken = "Basic ${authenticationToken.get()}",
            userName = userName,
            deposit = deposit
        ).also {
            LOG.fine("SmartDocuments attended document creation response: $it")
        }.let {
            DocumentCreationAttendedResponse(
                redirectUrl = UriBuilder.fromUri(smartDocumentsURL.get())
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
        smartDocumentsClient.get().listTemplates(
            authenticationToken = "Basic ${authenticationToken.get()}",
            userName = fixedUserName.orElse(loggedInUserInstance.get().id)
        )

    /**
     * Download generated document
     */
    fun downloadDocument(fileId: String): File =
        smartDocumentsClient.get().downloadFile(
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
