package net.atos.zac.enkelvoudiginformatieobject

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import net.atos.zac.util.UriUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.Optional
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
class EnkelvoudigInformatieObjectLockService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZRCClientService
) {
    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private lateinit var entityManager: EntityManager

    @OptIn(FlowPreview::class)
    fun waitForLockState(enkelvoudiginformatieobjectUUID: UUID, isLocked: Boolean) = flow<Void> {
        throw EnkelvoudigInformatieObjectLockRetryException(
            "Waiting for lock state $isLocked on info object $enkelvoudiginformatieobjectUUID"
        )
    }.retry {
        drcClientService.readEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID).locked != isLocked
    }.timeout(2.seconds).onEach { delay(200.milliseconds) }

    fun createLock(enkelvoudiginformatieobjectUUID: UUID, idUser: String): EnkelvoudigInformatieObjectLock =
        EnkelvoudigInformatieObjectLock().apply {
            this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID
            userId = idUser
            lock = drcClientService.lockEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID)
            entityManager.persist(this)
            runBlocking { waitForLockState(enkelvoudiginformatieobjectUUID, true).collect{} }
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

    fun deleteLock(enkelvoudiginformatieObjectUUID: UUID) =
        findLock(enkelvoudiginformatieObjectUUID).ifPresent { lock ->
            drcClientService.unlockEnkelvoudigInformatieobject(enkelvoudiginformatieObjectUUID, lock.lock)
            entityManager.remove(lock)
            runBlocking { waitForLockState(enkelvoudiginformatieObjectUUID, false).collect{} }
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
