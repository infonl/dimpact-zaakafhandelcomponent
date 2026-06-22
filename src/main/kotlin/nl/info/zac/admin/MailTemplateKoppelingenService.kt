/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.util.ValidationUtil.validateObject
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@Transactional(SUPPORTS)
@AllOpen
@NoArgConstructor
class MailTemplateKoppelingenService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun find(id: Long): ZaaktypeCmmnMailtemplateParameters? =
        entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id)

    @Transactional(REQUIRED)
    fun delete(id: Long) {
        find(id)?.let { entityManager.remove(it) }
    }

    @Transactional(REQUIRED)
    fun storeMailtemplateKoppeling(
        zaaktypeCmmnMailtemplateParameters: ZaaktypeCmmnMailtemplateParameters
    ): ZaaktypeCmmnMailtemplateParameters {
        validateObject(zaaktypeCmmnMailtemplateParameters)
        val existingId = zaaktypeCmmnMailtemplateParameters.id
        return if (existingId != null && find(existingId) != null) {
            entityManager.merge(zaaktypeCmmnMailtemplateParameters)
        } else {
            entityManager.persist(zaaktypeCmmnMailtemplateParameters)
            zaaktypeCmmnMailtemplateParameters
        }
    }

    fun readMailtemplateKoppeling(id: Long): ZaaktypeCmmnMailtemplateParameters =
        find(id) ?: throw NoSuchElementException(
            "${ZaaktypeCmmnMailtemplateParameters::class.java.simpleName} with id=$id not found"
        )

    fun listMailtemplateKoppelingen(): List<ZaaktypeCmmnMailtemplateParameters> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeCmmnMailtemplateParameters::class.java)
        val root = query.from(ZaaktypeCmmnMailtemplateParameters::class.java)
        query.select(root)
        return entityManager.createQuery(query).resultList
    }
}
