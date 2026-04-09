/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.repository.DetachedDocumentRepository
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class DetachedDocumentService @Inject constructor(
    private val detachedDocumentRepository: DetachedDocumentRepository,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @Transactional(REQUIRED)
    fun create(
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        zaak: Zaak,
        reason: String
    ): DetachedDocument {
        val detachedDocument = DetachedDocument().apply {
            documentID = enkelvoudigInformatieObject.getIdentificatie()
            documentUUID = enkelvoudigInformatieObject.getUrl().extractUuid()
            creatiedatum = enkelvoudigInformatieObject.getCreatiedatum()
            titel = enkelvoudigInformatieObject.getTitel()
            bestandsnaam = enkelvoudigInformatieObject.getBestandsnaam()
            ontkoppeldOp = ZonedDateTime.now()
            ontkoppeldDoor = loggedInUserInstance.get().id
            zaakID = zaak.getIdentificatie()
            reden = reason
        }
        return detachedDocumentRepository.save(detachedDocument)
    }

    fun getDetachedDocumentResult(listParameters: DetachedDocumentListParameters): DetachedDocumentResult =
        DetachedDocumentResult(
            items = detachedDocumentRepository.list(listParameters),
            count = detachedDocumentRepository.count(listParameters).toLong(),
            detachedByFilter = detachedDocumentRepository.getOntkoppeldDoor(listParameters)
        )

    /**
     * Returns the detached document for the provided enkelvoudiginformatieobject UUID.
     *
     * @param enkelvoudiginformatieobjectUUID the enkelvoudiginformatieobject UUID
     * @return the detached document
     * @throws DetachedDocumentNotFoundException if the detached document could not be found
     */
    fun read(enkelvoudiginformatieobjectUUID: UUID): DetachedDocument =
        detachedDocumentRepository.find(enkelvoudiginformatieobjectUUID) ?: throw DetachedDocumentNotFoundException(
            "No detached document found for enkelvoudiginformatieobject UUID: '$enkelvoudiginformatieobjectUUID'"
        )

    fun find(id: Long): DetachedDocument? = detachedDocumentRepository.find(id)

    fun find(uuid: UUID): DetachedDocument? = detachedDocumentRepository.find(uuid)

    @Transactional(REQUIRED)
    fun delete(id: Long) = detachedDocumentRepository.delete(id)

    @Transactional(REQUIRED)
    fun delete(uuid: UUID) = detachedDocumentRepository.delete(uuid)
}
