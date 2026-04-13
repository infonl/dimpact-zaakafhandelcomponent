/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.shared.exception.ZgwErrorException
import net.atos.zac.app.shared.RESTResultaat
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentListParametersConverter
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.jetty.http.HttpStatus
import java.util.logging.Logger

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList")
class DetachedDocumentRestService @Inject constructor(
    private val detachedDocumentService: DetachedDocumentService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val restDetachedDocumentConverter: RestDetachedDocumentConverter,
    private val listParametersConverter: RestDetachedDocumentListParametersConverter,
    private val userConverter: RestUserConverter,
    private val policyService: PolicyService
) {
    companion object {
        private val LOG = Logger.getLogger(DetachedDocumentRestService::class.java.name)
    }

    @PUT
    @Path("")
    fun listDetachedDocuments(
        restListParameters: RestDetachedDocumentListParameters
    ): RESTResultaat<RestDetachedDocument> {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val listParameters = listParametersConverter.convert(restListParameters)
        val resultaat = detachedDocumentService.getDetachedDocumentResult(listParameters)
        val ontkoppeldeDocumenten = resultaat.items
        val informationObjectTypeUUIDs = ontkoppeldeDocumenten.map { ontkoppeldeDocument ->
            drcClientService
                .readEnkelvoudigInformatieobject(ontkoppeldeDocument.documentUUID)
                .informatieobjecttype
                .extractUuid()
        }
        val restDetachedDocumentResult = RestDetachedDocumentResult(
            resultaten = restDetachedDocumentConverter.convert(ontkoppeldeDocumenten, informationObjectTypeUUIDs),
            aantalTotaal = resultaat.count
        )
        val ontkoppeldDoor = resultaat.detachedByFilter
        if (ontkoppeldDoor.isEmpty()) {
            restListParameters.ontkoppeldDoor?.let { user ->
                restDetachedDocumentResult.filterOntkoppeldDoor = listOf(user)
            }
        } else {
            restDetachedDocumentResult.filterOntkoppeldDoor = with(userConverter) { ontkoppeldDoor.convertUserIds() }
        }
        return restDetachedDocumentResult
    }

    @DELETE
    @Path("{id}")
    fun deleteDetachedDocument(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().ontkoppeldeDocumentenVerwijderen)
        val detachedDocument = detachedDocumentService.find(id) ?: return
        var enkelvoudigInformatieobject: EnkelvoudigInformatieObject? = null
        val documentUUID = detachedDocument.documentUUID
        try {
            enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(documentUUID)
        } catch (zgwErrorException: ZgwErrorException) {
            if (zgwErrorException.zgwError.status != HttpStatus.NOT_FOUND_404) {
                throw zgwErrorException
            }
            LOG.info("Document met UUID '$documentUUID' wel gevonden in de database, maar niet in OpenZaak")
        }
        enkelvoudigInformatieobject?.let { informatieobject ->
            val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(informatieobject)
            if (zaakInformatieobjecten.isNotEmpty()) {
                val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
                error("Informatieobject is gekoppeld aan zaak '$zaakUuid'")
            }
            drcClientService.deleteEnkelvoudigInformatieobject(documentUUID)
        }
        detachedDocumentService.deleteIfExists(detachedDocument.id!!)
    }
}
