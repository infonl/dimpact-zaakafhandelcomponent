/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@NoArgConstructor
@ApplicationScoped
@AllOpen
@Suppress("LongParameterList")
class DocumentCreationService @Inject constructor(
    private val smartDocumentsService: SmartDocumentsService,
    private val documentCreationDataConverter: DocumentCreationDataConverter,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val configuratieService: ConfiguratieService,
) {
    companion object {
        private const val SMART_DOCUMENTS_WIZARD_FINISH_PAGE = "static/smart-documents-result.html"
    }

    /**
     * Download a generated SmartDocuments file and store it as an Informatieobject
     */
    fun storeDocument(
        zaak: Zaak,
        taskId: String? = null,
        fileId: String,
        title: String,
        description: String?,
        informatieobjecttypeUuid: UUID,
        creationDate: ZonedDateTime,
        userName: String
    ): ZaakInformatieobject =
        smartDocumentsService.downloadDocument(fileId).let { file ->
            documentCreationDataConverter.toEnkelvoudigInformatieObjectCreateLockRequest(
                file = file,
                format = MediaTypes.Application.MS_WORD_OPEN_XML.mediaType,
                informatieobjecttypeUuid = informatieobjecttypeUuid,
                title = title,
                description = description,
                creationDate = creationDate,
                userName = userName,
            ).let {
                enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                    zaak = zaak,
                    enkelvoudigInformatieObjectCreateLockRequest = it,
                    taskId = taskId,
                    // We open SmartDocuments in a new tab. This means that authorization token we have from Keycloak
                    // will expire in some time (60-90 seconds usually). After this time no policy checks can be done,
                    // as we no longer have a valid token. All policy checks need to be performed on document creation
                    // request time.
                    skipPolicyCheck = true
                )
            }
        }

    fun createDocumentCreationUriBulder(
        title: String,
        description: String?,
        creationDate: ZonedDateTime,
        userName: String
    ): UriBuilder =
        UriBuilder
            .fromUri(configuratieService.readContextUrl())
            .queryParam("title", title)
            .queryParam("userName", userName)
            .queryParam("creationDate", creationDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .apply {
                if (description != null) {
                    queryParam("description", description)
                }
            }

    fun documentCreationFinishPageUrl(
        zaakId: String,
        taskId: String? = null,
        documentName: String? = null,
        result: String
    ): URI =
        UriBuilder
            .fromUri(configuratieService.readContextUrl())
            .path(SMART_DOCUMENTS_WIZARD_FINISH_PAGE)
            .queryParam("zaak", zaakId)
            .apply {
                if (taskId != null) {
                    queryParam("taak", taskId)
                }
                if (documentName != null) {
                    queryParam("doc", documentName)
                }
            }
            .queryParam("result", result)
            .build()
}
