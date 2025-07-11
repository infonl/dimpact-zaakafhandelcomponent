/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.enkelvoudiginformatieobject

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@AllOpen
@NoArgConstructor
class EnkelvoudigInformatieObjectLockService @Inject constructor(
    private val entityManager: EntityManager,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService
) {
    @Transactional(REQUIRED)
    fun createLock(informationObjectUUID: UUID, userID: String): EnkelvoudigInformatieObjectLock =
        EnkelvoudigInformatieObjectLock().apply {
            enkelvoudiginformatieobjectUUID = informationObjectUUID
            userId = userID
            lock = drcClientService.lockEnkelvoudigInformatieobject(informationObjectUUID)
            entityManager.persist(this)
        }

    fun findLock(informationObjectUUID: UUID): EnkelvoudigInformatieObjectLock? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(EnkelvoudigInformatieObjectLock::class.java)
        val root = query.from(EnkelvoudigInformatieObjectLock::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>("enkelvoudiginformatieobjectUUID"), informationObjectUUID))
        val resultList = entityManager.createQuery(query).resultList
        return if (resultList.isEmpty()) null else resultList.first()
    }

    fun readLock(informationObjectUUID: UUID): EnkelvoudigInformatieObjectLock =
        findLock(informationObjectUUID).takeIf { it != null }
            ?: throw EnkelvoudigInformatieObjectLockNotFoundException(
                "Lock for EnkelvoudigInformatieObject with uuid '$informationObjectUUID' not found"
            )

    @Transactional(REQUIRED)
    fun deleteLock(informationObjectUUID: UUID) =
        findLock(informationObjectUUID)?.let { lock ->
            drcClientService.unlockEnkelvoudigInformatieobject(informationObjectUUID, lock.lock)
            entityManager.remove(lock)
        }

    fun hasLockedInformatieobjecten(zaak: Zaak): Boolean {
        val informationObjectUUIDs = zrcClientService.listZaakinformatieobjecten(zaak)
            .map { it.informatieobject.extractUuid() }
        if (informationObjectUUIDs.isEmpty()) {
            return false
        }
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(EnkelvoudigInformatieObjectLock::class.java)
        val root = query.from(EnkelvoudigInformatieObjectLock::class.java)
        query.select(root).where(root.get<Any>("enkelvoudiginformatieobjectUUID").`in`(informationObjectUUIDs))
        return entityManager.createQuery(query).resultList.isNotEmpty()
    }
}
