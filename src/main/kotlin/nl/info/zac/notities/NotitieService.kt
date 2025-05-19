/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notities

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.util.ValidationUtil
import nl.info.zac.notities.model.Notitie
import nl.info.zac.notities.model.Notitie.Companion.ZAAK_UUID_FIELD
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class NotitieService @Inject constructor(
    private val entityManager: EntityManager
) {
    @Transactional(REQUIRED)
    fun createNotitie(notitie: Notitie): Notitie {
        ValidationUtil.valideerObject(notitie)
        entityManager.persist(notitie)
        return notitie
    }

    fun listNotitiesForZaak(zaakUUID: UUID): List<Notitie> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Notitie::class.java)
        val root = query.from(Notitie::class.java)
        query.select(root).where(builder.equal(root.get<Any>(ZAAK_UUID_FIELD), zaakUUID))
        return entityManager.createQuery(query).getResultList()
    }

    @Transactional(REQUIRED)
    fun updateNotitie(notitie: Notitie): Notitie {
        ValidationUtil.valideerObject(notitie)
        return entityManager.merge(notitie)
    }

    @Transactional(REQUIRED)
    fun deleteNotitie(notitieId: Long) {
        val notitie = entityManager.find(Notitie::class.java, notitieId)
        entityManager.remove(notitie)
    }
}
