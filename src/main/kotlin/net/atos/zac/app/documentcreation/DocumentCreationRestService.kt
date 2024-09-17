/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.documentcreation

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import net.atos.zac.app.documentcreation.model.RestDocumentCreationAttendedResponse
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.documentcreation.model.DocumentCreationDataAttended
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("document-creation")
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class DocumentCreationRestService @Inject constructor(
    private val policyService: PolicyService,
    private val documentCreationService: DocumentCreationService,
    private val zrcClientService: ZrcClientService,
    private val configuratieService: ConfiguratieService
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/create-document-attended")
    fun createDocumentAttended(
        @Valid restDocumentCreationAttendedData: RestDocumentCreationAttendedData
    ): RestDocumentCreationAttendedResponse =
        zrcClientService.readZaak(restDocumentCreationAttendedData.zaakUuid).also {
            assertPolicy(policyService.readZaakRechten(it).creeerenDocument)
        }.let {
            DocumentCreationDataAttended(
                zaak = it,
                templateId = restDocumentCreationAttendedData.smartDocumentsTemplateId,
                templateGroupId = restDocumentCreationAttendedData.smartDocumentsTemplateGroupId,
                taskId = restDocumentCreationAttendedData.taskId,
            )
                .let(documentCreationService::createDocumentAttended)
                .let { response -> RestDocumentCreationAttendedResponse(response.redirectUrl, response.message) }
        }

    /**
     * SmartDocuments callback
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a zaak:
     * zaak ID, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}")
    @Produces(MediaType.TEXT_HTML)
    fun createDocumentForZaakCallback(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") fileId: String,
    ): String =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            documentCreationService.storeDocument(
                fileId,
                templateGroupId,
                templateId,
                userName,
                zaak
            ).let { zaakInformatieobject ->
                val fileName = zaakInformatieobject.titel
                val zaakUri = configuratieService.zaakTonenUrl(zaak.identificatie)
                """<!DOCTYPE html><html>
                   <head><title>Document $fileName</title></head>
                   <body>
                       <p>Document $fileName gemaakt voor zaak <a href="$zaakUri">${zaak.identificatie}</a> !</p>
                       <p>Sluit dit tabblad!</p>
                       <hr/>
                       <p>Document $fileName created for zaak <a href="$zaakUri">${zaak.identificatie}</a> !</p>
                       <p>Please, close this tab!</p>
                   <body></html>
                """.trimIndent()
            }
        }

    /**
     * SmartDocuments callback
     *
     * Called when SmartDocument Wizard "Finish" button is clicked. The URL provided as "redirectUrl" to
     * SmartDocuments contains all the parameters needed to store the document for a task:
     * zaak and task IDs, template and template group IDs, username and created document ID
     */
    @POST
    @Path("/smartdocuments/callback/zaak/{zaakUuid}/task/{taskId}")
    @Produces(MediaType.TEXT_HTML)
    @Suppress("LongParameterList")
    fun createDocumentForTaskCallback(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @PathParam("taskId") taskId: String,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") fileId: String,
    ): String =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            documentCreationService.storeDocument(
                fileId,
                templateGroupId,
                templateId,
                userName,
                zaak,
                taskId
            ).let { zaakInformatieobject ->
                val fileName = zaakInformatieobject.titel
                val zaakUri = configuratieService.zaakTonenUrl(zaak.identificatie)
                val taskUri = configuratieService.taakTonenUrl(taskId)
                """<!DOCTYPE html><html>
                   <head><title>Document $fileName</title></head>
                   <body>
                       <p>Document $fileName gemaakt voor zaak <a href="$zaakUri">${zaak.identificatie}</a>, taak <a href="$taskUri">$taskId</a> !</p>
                       <p>Sluit dit tabblad!</p>
                       <hr/>
                       <p>Document $fileName created for zaak <a href="$zaakUri">${zaak.identificatie}</a>, taak <a href="$taskUri">$taskId</a> !</p>
                       <p>Please, close this tab!</p>
                   <body></html>
                """.trimIndent()
            }
        }
}
