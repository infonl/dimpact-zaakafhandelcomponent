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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import net.atos.zac.event.EventingService
import net.atos.zac.util.UriUtil
import net.atos.zac.websocket.event.ScreenEventType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@AllOpen
@NoArgConstructor
class EnkelvoudigInformatieObjectLockService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZRCClientService,
    private val eventingService: EventingService
) {
    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private lateinit var entityManager: EntityManager

    companion object {
        private const val CHANGE_EVENT_RETRIES = 5
        private const val CHANGE_EVENT_WAIT_MILLISECONDS: Long = 200

        private val LOG = Logger.getLogger(EnkelvoudigInformatieObjectLockService::class.java.name)
    }

    @Transactional(REQUIRED)
    fun createLock(informationObjectUUID: UUID, idUser: String): EnkelvoudigInformatieObjectLock =
        EnkelvoudigInformatieObjectLock().apply {
            enkelvoudiginformatieobjectUUID = informationObjectUUID
            userId = idUser
            lock = drcClientService.lockEnkelvoudigInformatieobject(informationObjectUUID)
            entityManager.persist(this)
            CoroutineScope(Dispatchers.IO).launch { sendChangeEvent(informationObjectUUID, true) }
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
            CoroutineScope(Dispatchers.IO).launch { sendChangeEvent(informationObjectUUID, false) }
        }

    fun hasLockedInformatieobjecten(zaak: Zaak): Boolean {
        val informationObjectUUIDs = zrcClientService.listZaakinformatieobjecten(zaak)
            .map { UriUtil.uuidFromURI(it.informatieobject) }
            .toList()
        if (informationObjectUUIDs.isEmpty()) {
            return false
        }
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(EnkelvoudigInformatieObjectLock::class.java)
        val root = query.from(EnkelvoudigInformatieObjectLock::class.java)
        query.select(root).where(root.get<Any>("enkelvoudiginformatieobjectUUID").`in`(informationObjectUUIDs))
        return entityManager.createQuery(query).resultList.isNotEmpty()
    }

    private suspend fun sendChangeEvent(informationObjectUUID: UUID, isLocked: Boolean) {
        repeat(CHANGE_EVENT_RETRIES) {
            if (drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID).locked != isLocked) {
                // No notification by OpenZaak for lock/unlock, so we send this on our own
                eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID))
                LOG.fine("Change event for for information object with UUID $informationObjectUUID sent")
                return
            }
            delay(CHANGE_EVENT_WAIT_MILLISECONDS)
        }
        LOG.warning(
            "Change event was not sent for information object with UUID $informationObjectUUID as " +
                "desired state ${if (isLocked) "" else "UN"}LOCKED was not reached after " +
                "${CHANGE_EVENT_RETRIES * CHANGE_EVENT_WAIT_MILLISECONDS} ms"
        )
    }
}
