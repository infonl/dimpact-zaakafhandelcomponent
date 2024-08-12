/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.model.document.Data
import net.atos.client.smartdocuments.model.document.Deposit
import net.atos.client.smartdocuments.model.document.Registratie
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.documentcreation.model.DocumentCreationAttendedResponse
import net.atos.zac.documentcreation.model.DocumentCreationUnattendedResponse
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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

    @ConfigProperty(name = "SD_CLIENT_MP_REST_URL")
    private val smartDocumentsURL: String,

    @ConfigProperty(name = "SD_AUTHENTICATION")
    private val authenticationToken: String,

    @ConfigProperty(name = "SD_FIXED_USER_NAME")
    private val fixedUserName: Optional<String>,

    private val loggedInUserInstance: Instance<LoggedInUser>,

) {
    companion object {
        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    /**
     * Sends a request to SmartDocuments to create a document using the Smart Documents wizard (= attended mode).
     */
    fun createDocumentAttended(
        data: Data,
        registratie: Registratie,
        smartDocument: SmartDocument
    ): DocumentCreationAttendedResponse {
        val deposit = Deposit(
            data = data,
            registratie = registratie,
            smartDocument = smartDocument
        )
        try {
            val userName = fixedUserName.orElse(loggedInUserInstance.get().id).also {
                LOG.fine("Starting Smart Documents wizard for user: '$it'")
            }
            val wizardResponse = smartDocumentsClient.attendedDeposit(
                authenticationToken = "Basic $authenticationToken",
                userName = userName,
                deposit = deposit
            ).also {
                LOG.fine("SmartDocuments attended document creation response: $it")
            }
            return DocumentCreationAttendedResponse(
                redirectUrl = UriBuilder.fromUri(smartDocumentsURL)
                    .path("smartdocuments/wizard")
                    .queryParam("ticket", wizardResponse.ticket)
                    .build()
            )
        } catch (badRequestException: BadRequestException) {
            return DocumentCreationAttendedResponse(
                message = "Aanmaken van een document is helaas niet mogelijk. " +
                    "Ben je als user geregistreerd in SmartDocuments? " +
                    "Details: '$badRequestException.message'"
            )
        }
    }

    /**
     * Sends a request to SmartDocuments to create a document using the Smart Documents unattended mode.
     *
     * @return the document creation response
     */
    fun createDocumentUnattended(
        data: Data,
        smartDocument: SmartDocument
    ): DocumentCreationUnattendedResponse {
        val deposit = Deposit(
            data = data,
            smartDocument = smartDocument
        )
        try {
            val userName = fixedUserName.orElse(loggedInUserInstance.get().id).also {
                LOG.fine("Starting SmartDocuments unattended document creation flow for user: '$it'")
            }
            return smartDocumentsClient.unattendedDeposit(
                authenticationToken = "Basic $authenticationToken",
                userName = userName,
                deposit = deposit
            ).also {
                // for now log the response at INFO log level for testing
                // later reduce this to FINE
                LOG.info("SmartDocuments unattended document creation response: '$it'")
            }.let {
                DocumentCreationUnattendedResponse(
                    message = "SmartDocuments document was created succesfully but the document is not stored yet in the zaakregister. " +
                        "SmartDocument reponse details: '$it'"
                )
            }
        } catch (badRequestException: BadRequestException) {
            return DocumentCreationUnattendedResponse(
                message = "Aanmaken van een document is helaas niet mogelijk. " +
                    "Ben je als user geregistreerd in SmartDocuments? " +
                    "Details: '$badRequestException.message'"
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
            "Basic $authenticationToken",
            fixedUserName.orElse(loggedInUserInstance.get().id)
        )
}
