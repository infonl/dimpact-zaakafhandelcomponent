/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.converter.toDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.converter.toRestDetachedDocumentBulkResult
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkDeleteRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkResult
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.document.detacheddocument.DetachedDocumentBulkService
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.SearchService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class DetachedDocumentRestService @Inject constructor(
    private val detachedDocumentService: DetachedDocumentService,
    private val detachedDocumentBulkService: DetachedDocumentBulkService,
    private val restDetachedDocumentConverter: RestDetachedDocumentConverter,
    private val userConverter: RestUserConverter,
    private val policyService: PolicyService,
    private val searchService: SearchService
) {
    @PUT
    @Path("")
    fun listDetachedDocuments(
        restListParameters: RestDetachedDocumentListParameters
    ): RestDetachedDocumentResult {
        assertPolicy(policyService.readWerklijstRechten().canInbox)
        val listParameters = restListParameters.toDetachedDocumentListParameters()
        val resultaat = detachedDocumentService.getDetachedDocumentResult(listParameters)
        val ontkoppeldeDocumenten = resultaat.items
        val informationObjectTypeUUIDs = ontkoppeldeDocumenten.map { ontkoppeldeDocument ->
            detachedDocumentBulkService
                .findEnkelvoudigInformatieobject(ontkoppeldeDocument.documentUUID)
                ?.informatieobjecttype
                ?.extractUuid()
        }
        val restDetachedDocumentResult = RestDetachedDocumentResult(
            results = restDetachedDocumentConverter.convertToRestDetachedDocuments(
                documenten = ontkoppeldeDocumenten,
                informatieobjectTypeUUIDs = informationObjectTypeUUIDs,
                zaakspecifiekGeautoriseerdeZaakIDs = searchService.listZaakspecifiekGeautoriseerdeWaarden(
                    ontkoppeldeDocumenten.map { it.zaakID }
                )
            ),
            resultCount = resultaat.count
        )
        val ontkoppeldDoor = resultaat.detachedByFilter
        if (ontkoppeldDoor.isEmpty()) {
            restListParameters.ontkoppeldDoor?.let { user ->
                restDetachedDocumentResult.filterOntkoppeldDoor = listOf(user)
            }
        } else {
            restDetachedDocumentResult.filterOntkoppeldDoor = with(userConverter) {
                ontkoppeldDoor.convertUsernames(logContext = "the ontkoppeld door filter of the ontkoppelde documenten")
            }
        }
        return restDetachedDocumentResult
    }

    @DELETE
    @Path("{id}")
    fun deleteDetachedDocument(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().canOntkoppeldeDocumentenVerwijderen)
        detachedDocumentBulkService.delete(id)
    }

    @PUT
    @Path("lijst/verwijderen")
    fun deleteDetachedDocuments(
        @Valid request: RestDetachedDocumentBulkDeleteRequest
    ): RestDetachedDocumentBulkResult {
        assertPolicy(policyService.readWerklijstRechten().canOntkoppeldeDocumentenVerwijderen)
        return detachedDocumentBulkService.deleteInBulk(request.ids).toRestDetachedDocumentBulkResult()
    }

    @PUT
    @Path("lijst/koppelen")
    fun linkDetachedDocuments(@Valid request: RestDetachedDocumentBulkLinkRequest): RestDetachedDocumentBulkResult {
        assertPolicy(policyService.readWerklijstRechten().canInbox)
        return detachedDocumentBulkService.linkInBulk(request.ids, request.nieuweZaakID)
            .toRestDetachedDocumentBulkResult()
    }
}
