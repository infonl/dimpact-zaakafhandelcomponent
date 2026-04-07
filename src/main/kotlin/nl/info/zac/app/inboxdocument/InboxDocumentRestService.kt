/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.inboxdocument

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.shared.RESTResultaat
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.inboxdocument.converter.RestInboxDocumentListParametersConverter
import nl.info.zac.app.inboxdocument.model.RestInboxDocument
import nl.info.zac.app.inboxdocument.model.RestInboxDocumentListParameters
import nl.info.zac.app.inboxdocument.model.toRestInboxDocuments
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.document.inboxdocument.model.InboxDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@Path("inboxdocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class InboxDocumentRestService @Inject constructor(
    private val inboxDocumentService: InboxDocumentService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val listParametersConverter: RestInboxDocumentListParametersConverter,
    private val policyService: PolicyService
) {
    companion object {
        private val LOG = Logger.getLogger(InboxDocumentRestService::class.java.name)
    }

    @PUT
    fun listInboxDocuments(restListParameters: RestInboxDocumentListParameters?): RESTResultaat<RestInboxDocument> {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val listParameters = listParametersConverter.convert(restListParameters)
        val inboxDocuments = inboxDocumentService.list(listParameters)
        // the list of informatie object type UUIDs has the same length as the inbox documents, and can contain null\
        // values if the enkelvoudiginformatieobject for a specific inbox document could not be found
        val informatieobjectTypeUUIDs = inboxDocuments.map(::getInformatieobjectTypeUUID).toList()
        val restInboxDocuments = inboxDocuments.toRestInboxDocuments(informatieobjectTypeUUIDs)
        return RESTResultaat<RestInboxDocument>(
            restInboxDocuments,
            inboxDocumentService.count(listParameters).toLong()
        )
    }

    @DELETE
    @Path("{id}")
    fun deleteInboxDocument(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val inboxDocument = inboxDocumentService.find(id) ?: return
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
            inboxDocument.enkelvoudiginformatieobjectUUID
        )
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
            enkelvoudigInformatieobject
        )
        if (zaakInformatieobjecten.isNotEmpty()) {
            val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
            LOG.log(Level.WARNING) {
                "Deleted InboxDocument but not the informatieobject. " +
                    "Reason: informatieobject '${enkelvoudigInformatieobject.identificatie}' is linked " +
                    "to zaak '$zaakUuid'."
            }
        } else {
            drcClientService.deleteEnkelvoudigInformatieobject(
                inboxDocument.enkelvoudiginformatieobjectUUID
            )
        }
        inboxDocumentService.delete(id)
    }

    private fun getInformatieobjectTypeUUID(inboxDocument: InboxDocument): UUID? {
        try {
            val informatieobject = drcClientService.readEnkelvoudigInformatieobject(
                inboxDocument.enkelvoudiginformatieobjectUUID
            )
            return informatieobject.getInformatieobjecttype().extractUuid()
        } catch (notFoundException: NotFoundException) {
            LOG.log(Level.WARNING, notFoundException) {
                "Error reading EnkelvoudigInformatieobject for InboxDocument with id '${inboxDocument.id}' " +
                    "and enkelvoudiginformatieobjectUUID '${inboxDocument.enkelvoudiginformatieobjectUUID}' " +
                    "Error: ${notFoundException.message}"
            }
        }
        return null
    }
}
