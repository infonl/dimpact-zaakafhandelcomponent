/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import jakarta.ws.rs.NotFoundException
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.document.inboxdocument.repository.InboxDocumentRepository
import nl.info.zac.document.inboxdocument.repository.model.InboxDocument
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class InboxDocumentService @Inject constructor(
    private val inboxDocumentRepository: InboxDocumentRepository,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService
) {
    @Transactional(REQUIRED)
    fun create(enkelvoudiginformatieobjectUUID: UUID): InboxDocument {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID)
        return InboxDocument().apply {
            enkelvoudiginformatieobjectID = informatieobject.identificatie
            this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID
            creatiedatum = informatieobject.creatiedatum
            titel = informatieobject.titel
            bestandsnaam = informatieobject.bestandsnaam
        }.also(inboxDocumentRepository::save)
    }

    fun find(id: Long): InboxDocument? = inboxDocumentRepository.find(id)

    fun find(enkelvoudiginformatieobjectUUID: UUID): InboxDocument? =
        inboxDocumentRepository.find(enkelvoudiginformatieobjectUUID)

    fun read(enkelvoudiginformatieobjectUUID: UUID): InboxDocument =
        inboxDocumentRepository.find(enkelvoudiginformatieobjectUUID)
            ?: throw NotFoundException("InboxDocument with uuid '$enkelvoudiginformatieobjectUUID' not found.")

    fun count(listParameters: InboxDocumentListParameters): Long = inboxDocumentRepository.count(listParameters)

    @Transactional(REQUIRED)
    fun deleteIfExists(id: Long) {
        inboxDocumentRepository.find(id)?.run(inboxDocumentRepository::delete)
    }

    @Transactional(REQUIRED)
    fun deleteIfExists(uuid: UUID) {
        inboxDocumentRepository.find(uuid)?.run(inboxDocumentRepository::delete)
    }

    @Transactional(REQUIRED)
    fun deleteForZaakinformatieobject(zaakinformatieobjectUUID: UUID) {
        val zaakInformatieobject = zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID)
        inboxDocumentRepository.find(zaakInformatieobject.informatieobject.extractUuid())
            ?.let { inboxDocumentRepository.delete(it) }
    }

    fun list(listParameters: InboxDocumentListParameters): List<InboxDocument> =
        inboxDocumentRepository.list(listParameters)
}
