/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.exception.BadRequestException
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.client.smartdocuments.model.wizard.Selection
import net.atos.client.smartdocuments.model.wizard.SmartDocument
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.DocumentCreationData
import net.atos.zac.documentcreation.model.DocumentCreationResponse
import net.atos.zac.documentcreation.model.Registratie
import net.atos.zac.documentcreation.model.WizardRequest
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.LocalDate
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

    private val documentCreationDataConverter: DocumentCreationDataConverter,

    private val loggedInUserInstance: Instance<LoggedInUser>,

    private val ztcClientService: ZtcClientService,

    private val zrcClientService: ZrcClientService
) {
    companion object {
        private const val AUDIT_TOELICHTING = "Door SmartDocuments"
        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    /**
     * Create a document using the SmartDocuments wizard.
     *
     * @param documentCreationData data used to create the document
     * @return the redirect URI to the SmartDocuments wizard
     */
    fun createDocumentAttended(documentCreationData: DocumentCreationData): DocumentCreationResponse {
        val wizardRequest = WizardRequest(
            data = documentCreationDataConverter.createData(documentCreationData, loggedInUserInstance.get()),
            registratie = createRegistratie(documentCreationData),
            smartDocument = createSmartDocument(documentCreationData)
        )
        val userName = fixedUserName.orElse(loggedInUserInstance.get().id)
        try {
            LOG.fine("Starting Smart Documents wizard for user: '$userName'")
            val wizardResponse = smartDocumentsClient.wizardDeposit(
                authenticationToken= "Basic $authenticationToken",
                userName = userName,
                wizardRequest = wizardRequest
            )
            return DocumentCreationResponse(
                UriBuilder.fromUri(smartDocumentsURL)
                    .path("smartdocuments/wizard")
                    .queryParam("ticket", wizardResponse.ticket)
                    .build()
            )
        } catch (badRequestException: BadRequestException) {
            return DocumentCreationResponse(
                message = "Aanmaken van een document is helaas niet mogelijk. " +
                    "Ben je als user geregistreerd in SmartDocuments? " +
                    "Details: '$badRequestException.message'"
            )
        }
    }

    /**
     * Lists all Smart Document templates groups and templates available for the current user.
     *
     * @return A structure describing templates and groups
     */
    fun listTemplates(): SmartDocumentsTemplatesResponse =
        smartDocumentsClient.listTemplates(
            "Basic $authenticationToken",
            fixedUserName.orElse(loggedInUserInstance.get().id)
        )

    private fun createSmartDocument(documentCreationData: DocumentCreationData) =
        ztcClientService.readZaaktype(documentCreationData.zaak.zaaktype).let {
            SmartDocument().apply {
                selection = Selection().apply {
                    templateGroup = it.omschrijving
                }
            }
        }

    private fun createRegistratie(documentCreationData: DocumentCreationData) =
        Registratie().apply {
            bronOrganisatie = ConfiguratieService.BRON_ORGANISATIE
            zaak = zrcClientService.createUrlExternToZaak(documentCreationData.zaak.uuid)
            informatieObjectStatus = documentCreationData.informatieobjectStatus
            informatieObjectType = documentCreationData.informatieobjecttype.url
            creatieDatum = LocalDate.now()
            auditToelichting = AUDIT_TOELICHTING
        }
}
