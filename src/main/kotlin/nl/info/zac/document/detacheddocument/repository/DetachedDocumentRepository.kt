/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository

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
<<<<<<<< HEAD:src/main/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentService.kt
import nl.info.zac.document.detacheddocument.repository.DetachedDocumentRepository
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
========
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.REDEN_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.TITEL_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.ZAAK_ID_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/main/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepository.kt
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
<<<<<<<< HEAD:src/main/kotlin/nl/info/zac/document/detacheddocument/DetachedDocumentService.kt
class DetachedDocumentService @Inject constructor(
    private val detachedDocumentRepository: DetachedDocumentRepository,
========
@Suppress("TooManyFunctions")
class DetachedDocumentRepository @Inject constructor(
    private val entityManager: EntityManager,
>>>>>>>> a58f746e6 (refactor: move DetachedDocumentService and model classes to repository package):src/main/kotlin/nl/info/zac/document/detacheddocument/repository/DetachedDocumentRepository.kt
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @Transactional(REQUIRED)
    fun create(
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        zaak: Zaak,
        reason: String
    ) {
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
        detachedDocumentRepository.save(detachedDocument)
    }

    fun getDetachedDocumentResult(listParameters: DetachedDocumentListParameters) = DetachedDocumentResult(
        items = detachedDocumentRepository.list(listParameters),
        count = detachedDocumentRepository.count(listParameters).toLong(),
        detachedByFilter = detachedDocumentRepository.getOntkoppeldDoor(listParameters)
    )

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
