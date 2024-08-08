/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.exception.SmartDocumentsBadRequestException
import net.atos.client.smartdocuments.model.document.Selection
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.document.WizardRequest
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.DocumentCreationData
import net.atos.zac.documentcreation.model.DocumentCreationResponse
import net.atos.zac.documentcreation.model.Registratie
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
        try {
            val userName = fixedUserName.orElse(loggedInUserInstance.get().id).also {
                LOG.fine("Starting Smart Documents wizard for user: '$it'")
            }
            val wizardResponse = smartDocumentsClient.wizardDeposit(
                authenticationToken = "Basic $authenticationToken",
                userName = userName,
                wizardRequest = wizardRequest
            )
            return DocumentCreationResponse(
                redirectUrl = UriBuilder.fromUri(smartDocumentsURL)
                    .path("smartdocuments/wizard")
                    .queryParam("ticket", wizardResponse.ticket)
                    .build()
            )
        } catch (smartDocumentsBadRequestException: SmartDocumentsBadRequestException) {
            return DocumentCreationResponse(
                message = "Aanmaken van een document is helaas niet mogelijk. " +
                    "Ben je als user geregistreerd in SmartDocuments? " +
                    "Details: '$smartDocumentsBadRequestException.message'"
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
        SmartDocument(
            selection = Selection(
                templateGroup = ztcClientService.readZaaktype(documentCreationData.zaak.zaaktype).omschrijving
            )
        )

    private fun createRegistratie(documentCreationData: DocumentCreationData) =
        Registratie(
            bronOrganisatie = ConfiguratieService.BRON_ORGANISATIE,
            zaak = zrcClientService.createUrlExternToZaak(documentCreationData.zaak.uuid),
            informatieObjectStatus = StatusEnum.TER_VASTSTELLING,
            informatieObjectType = documentCreationData.informatieobjecttype.url,
            creatieDatum = LocalDate.now(),
            auditToelichting = AUDIT_TOELICHTING
        )
}
