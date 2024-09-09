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
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.documentcreation.model.DocumentCreationDataAttended
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("document-creation")
@NoArgConstructor
@AllOpen
class DocumentCreationRestService @Inject constructor(
    private val policyService: PolicyService,
    private val documentCreationService: DocumentCreationService,
    private val zrcClientService: ZrcClientService,
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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

    @POST
    @Path("/redirect/zaak/{zaakUuid}")
    fun createDocumentRedirect(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") smartDocumentId: String,
    ) =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            documentCreationService.storeDocument(
                smartDocumentId,
                templateGroupId,
                templateId,
                userName,
                zaak
            )
        }.let { zaakInformatieobject ->
            "File ${zaakInformatieobject.titel} stored for zaak $zaakUuid"
        }

    @POST
    @Path("/redirect/zaak/{zaakUuid}/taak/{taakId}")
    @Suppress("LongParameterList")
    fun createDocumentRedirect(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @PathParam("taakId") taskId: String,
        @QueryParam("templateGroupId") templateGroupId: String,
        @QueryParam("templateId") templateId: String,
        @QueryParam("userName") userName: String,
        @FormParam("sdDocument") smartDocumentId: String,
    ) =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            documentCreationService.storeDocument(
                smartDocumentId,
                templateGroupId,
                templateId,
                userName,
                zaak,
                taskId
            )
        }.let { zaakInformatieobject ->
            "File ${zaakInformatieobject.titel} stored for zaak $zaakUuid and task $taskId"
        }
}
