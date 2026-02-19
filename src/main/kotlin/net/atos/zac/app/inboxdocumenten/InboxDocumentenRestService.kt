/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten

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
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.inboxdocumenten.converter.RestInboxDocumentListParametersConverter
import net.atos.zac.app.inboxdocumenten.model.RestInboxDocument
import net.atos.zac.app.inboxdocumenten.model.RestInboxDocumentListParameters
import net.atos.zac.app.inboxdocumenten.model.toRestInboxDocuments
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.model.InboxDocument
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
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
class InboxDocumentenRestService @Inject constructor(
    private var inboxDocumentenService: InboxDocumentenService,
    private var drcClientService: DrcClientService,
    private var zrcClientService: ZrcClientService,
    private var listParametersConverter: RestInboxDocumentListParametersConverter,
    private var policyService: PolicyService
) {
    companion object {
        private val LOG = Logger.getLogger(InboxDocumentenRestService::class.java.name)
    }

    @PUT
    @Path("")
    fun listInboxDocuments(restListParameters: RestInboxDocumentListParameters?): RESTResultaat<RestInboxDocument> {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val listParameters = listParametersConverter.convert(restListParameters)
        val inboxDocuments = inboxDocumentenService.list(listParameters)
        val informationObjectTypeUUIDs = inboxDocuments.stream()
            .map<UUID> { inboxDocument: InboxDocument -> this.getInformatieobjectTypeUUID(inboxDocument) }.toList()
        return RESTResultaat<RestInboxDocument>(
            inboxDocuments.toRestInboxDocuments(informationObjectTypeUUIDs),
            inboxDocumentenService.count(listParameters).toLong()
        )
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

    @DELETE
    @Path("{id}")
    fun deleteInboxDocument(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val inboxDocument = inboxDocumentenService.find(id)
        if (inboxDocument.isEmpty()) {
            return // reeds verwijderd
        }
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
            inboxDocument.get().getEnkelvoudiginformatieobjectUUID()
        )
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
            enkelvoudigInformatieobject
        )
        if (!zaakInformatieobjecten.isEmpty()) {
            val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
            LOG.log(Level.WARNING) {
                "Deleted InboxDocument but not the informatieobject. " +
                    "Reason: informatieobject '${enkelvoudigInformatieobject.identificatie}' is linked " +
                    "to zaak '$zaakUuid'."
            }
        } else {
            drcClientService.deleteEnkelvoudigInformatieobject(
                inboxDocument.get().getEnkelvoudiginformatieobjectUUID()
            )
        }
        inboxDocumentenService.delete(id)
    }
}
