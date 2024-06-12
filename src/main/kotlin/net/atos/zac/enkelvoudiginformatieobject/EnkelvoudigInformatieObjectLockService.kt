/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.enkelvoudiginformatieobject

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import net.atos.zac.util.UriUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.Optional
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@AllOpen
@NoArgConstructor
class EnkelvoudigInformatieObjectLockService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZRCClientService
) {
    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private lateinit var entityManager: EntityManager

    @Transactional(REQUIRED)
    fun createLock(enkelvoudiginformatieobjectUUID: UUID, idUser: String): EnkelvoudigInformatieObjectLock =
        EnkelvoudigInformatieObjectLock().apply {
            this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID
            userId = idUser
            lock = drcClientService.lockEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID)
            entityManager.persist(this)
        }

    fun findLock(enkelvoudiginformatieobjectUUID: UUID): Optional<EnkelvoudigInformatieObjectLock> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            EnkelvoudigInformatieObjectLock::class.java
        )
        val root = query.from(
            EnkelvoudigInformatieObjectLock::class.java
        )
        query.select(root)
            .where(builder.equal(root.get<Any>("enkelvoudiginformatieobjectUUID"), enkelvoudiginformatieobjectUUID))
        val resultList = entityManager.createQuery(query).resultList
        return if (resultList.isEmpty()) Optional.empty() else Optional.of(resultList.first())
    }

    fun readLock(enkelvoudiginformatieobjectUUID: UUID): EnkelvoudigInformatieObjectLock =
        findLock(enkelvoudiginformatieobjectUUID).orElseThrow {
            RuntimeException(
                "Lock for EnkelvoudigInformatieObject with uuid '$enkelvoudiginformatieobjectUUID' not found"
            )
        }

    @Transactional(REQUIRED)
    fun deleteLock(enkelvoudiginformatieObjectUUID: UUID) =
        findLock(enkelvoudiginformatieObjectUUID).ifPresent { lock ->
            drcClientService.unlockEnkelvoudigInformatieobject(enkelvoudiginformatieObjectUUID, lock.lock)
            entityManager.remove(lock)
        }

    fun hasLockedInformatieobjecten(zaak: Zaak): Boolean {
        val informatieobjectUUIDs = zrcClientService.listZaakinformatieobjecten(zaak)
            .map { UriUtil.uuidFromURI(it.informatieobject) }
            .toList()
        if (informatieobjectUUIDs.isEmpty()) {
            return false
        }
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            EnkelvoudigInformatieObjectLock::class.java
        )
        val root = query.from(
            EnkelvoudigInformatieObjectLock::class.java
        )
        query.select(root).where(root.get<Any>("enkelvoudiginformatieobjectUUID").`in`(informatieobjectUUIDs))
        return entityManager.createQuery(query).resultList.isNotEmpty()
    }
}
