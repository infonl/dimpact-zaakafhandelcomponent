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
) {
    @Transactional(REQUIRED)
    fun createLock(informationObjectUUID: UUID, userID: String): EnkelvoudigInformatieObjectLock {
        val enkelvoudigInformatieObjectLock = EnkelvoudigInformatieObjectLock().apply {
            enkelvoudiginformatieobjectUUID = informationObjectUUID
            userId = userID
            lock = drcClientService.lockEnkelvoudigInformatieobject(informationObjectUUID)
        }
        entityManager.persist(enkelvoudigInformatieObjectLock)
        entityManager.flush()
        return enkelvoudigInformatieObjectLock
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
            entityManager.flush()
        }
}
