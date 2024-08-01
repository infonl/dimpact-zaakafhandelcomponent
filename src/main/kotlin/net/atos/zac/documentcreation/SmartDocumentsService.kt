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
import net.atos.client.smartdocuments.exception.BadRequestException
import net.atos.client.smartdocuments.model.document.Deposit
import net.atos.client.smartdocuments.model.document.Registratie
import net.atos.client.smartdocuments.model.document.Selection
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.DocumentCreationAttendedResponse
import net.atos.zac.documentcreation.model.DocumentCreationData
import net.atos.zac.documentcreation.model.DocumentCreationUnattendedResponse
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
     * Sends a request to SmartDocuments to create a document using the Smart Documents wizard (= attended mode).
     *
     * @param documentCreationData data used to create the document
     * @return the redirect URI to the SmartDocuments wizard
     */
    fun createDocumentAttended(
        documentCreationData: DocumentCreationData
    ): DocumentCreationAttendedResponse {
        val deposit = Deposit(
            data = documentCreationDataConverter.createData(
                loggedInUser = loggedInUserInstance.get(),
                zaak = documentCreationData.zaak,
                taskId = documentCreationData.taskId
            ),
            registratie = createRegistratie(
                zaak = documentCreationData.zaak,
                informatieObjectType = documentCreationData.informatieobjecttype
            ),
            smartDocument = createSmartDocumentForAttendedFlow(documentCreationData)
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
     * @param documentCreationData data used to create the document
     * @return the document creation response
     */
    fun createDocumentUnattended(
        documentCreationData: DocumentCreationData
    ): DocumentCreationUnattendedResponse {
        val deposit = Deposit(
            data = documentCreationDataConverter.createData(
                loggedInUser = loggedInUserInstance.get(),
                zaak = documentCreationData.zaak,
                taskId = documentCreationData.taskId
            ),
            registratie = createRegistratie(
                zaak = documentCreationData.zaak
            ),
            smartDocument = createSmartDocumentForUnttendedFlow(documentCreationData)
        )
        try {
            val userName = fixedUserName.orElse(loggedInUserInstance.get().id).also {
                LOG.fine("Starting SmartDocuments unattended document creation flow for user: '$it'")
            }
            val depositResponse = smartDocumentsClient.unattendedDeposit(
                authenticationToken = "Basic $authenticationToken",
                userName = userName,
                deposit = deposit
            ).also {
                LOG.fine("SmartDocuments unattended document creation response: $it")
            }
            return DocumentCreationUnattendedResponse(
                // TODO
                message = "TODO"
            )
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

    /**
     * Creates a SmartDocument object for the attended flow.
     * In this flow the description of the zaaktype of the zaak in the provided document creation data is used
     * as the SmartDocuments template group.
     */
    private fun createSmartDocumentForAttendedFlow(documentCreationData: DocumentCreationData) =
        SmartDocument(
            selection = Selection(
                templateGroup = ztcClientService.readZaaktype(documentCreationData.zaak.zaaktype).omschrijving
            )
        )

    private fun createSmartDocumentForUnttendedFlow(documentCreationData: DocumentCreationData) =
        SmartDocument(
            selection = Selection(
                templateGroup = documentCreationData.templateGroup,
                template = documentCreationData.template
            )
        )

    private fun createRegistratie(zaak: Zaak, informatieObjectType: InformatieObjectType? = null) =
        Registratie(
            bronOrganisatie = ConfiguratieService.BRON_ORGANISATIE,
            zaak = zrcClientService.createUrlExternToZaak(zaak.uuid),
            informatieObjectStatus = StatusEnum.TER_VASTSTELLING,
            informatieObjectType = informatieObjectType?.url,
            creatieDatum = LocalDate.now(),
            auditToelichting = AUDIT_TOELICHTING
        )
}
