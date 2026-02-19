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
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentListParametersConverter
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocumentListParameters
import net.atos.zac.app.inboxdocumenten.model.convertToRESTInboxDocuments
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
class InboxDocumentenRESTService @Inject constructor(
    private var inboxDocumentenService: InboxDocumentenService,
    private var drcClientService: DrcClientService,
    private var zrcClientService: ZrcClientService,
    private var listParametersConverter: RESTInboxDocumentListParametersConverter,
    private var policyService: PolicyService
) {
    companion object {
        private val LOG = Logger.getLogger(InboxDocumentenRESTService::class.java.name)
    }

    @PUT
    @Path("")
    fun listInboxDocuments(restListParameters: RESTInboxDocumentListParameters?): RESTResultaat<RESTInboxDocument> {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val listParameters = listParametersConverter.convert(restListParameters)
        val inboxDocuments = inboxDocumentenService.list(listParameters)
        val informationObjectTypeUUIDs = inboxDocuments.stream()
            .map<UUID?> { inboxDocument: InboxDocument? -> this.getInformatieobjectTypeUUID(inboxDocument!!) }.toList()
        return RESTResultaat<RESTInboxDocument>(
            inboxDocuments.convertToRESTInboxDocuments(informationObjectTypeUUIDs),
            inboxDocumentenService.count(listParameters).toLong()
        )
    }

    private fun getInformatieobjectTypeUUID(inboxDocument: InboxDocument): UUID? {
        try {
            val informatieobject = drcClientService.readEnkelvoudigInformatieobject(
                inboxDocument.getEnkelvoudiginformatieobjectUUID()
            )
            return informatieobject.getInformatieobjecttype().extractUuid()
        } catch (notFoundException: NotFoundException) {
            LOG.log(Level.WARNING, notFoundException) {
                "Error reading EnkelvoudigInformatieobject for inbox-document with id '${inboxDocument.id}'. " +
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
        val zaakInformatieobjecten: List<ZaakInformatieobject> = zrcClientService.listZaakinformatieobjecten(
            enkelvoudigInformatieobject
        )
        if (!zaakInformatieobjecten.isEmpty()) {
            val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
            LOG.log(Level.WARNING) {
                "Het inbox-document is verwijderd maar het informatieobject is niet verwijderd. " +
                    "Reden: informatieobject '${enkelvoudigInformatieobject.identificatie}' is gekoppeld aan zaak '$zaakUuid'."
            }
        } else {
            drcClientService.deleteEnkelvoudigInformatieobject(
                inboxDocument.get().getEnkelvoudiginformatieobjectUUID()
            )
        }
        inboxDocumentenService.delete(id)
    }
}
