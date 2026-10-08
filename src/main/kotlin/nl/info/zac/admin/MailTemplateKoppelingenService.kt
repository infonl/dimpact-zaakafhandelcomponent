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
import nl.info.zac.admin.model.ZaaktypeMailtemplateParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.validateObject

@ApplicationScoped
@Transactional(SUPPORTS)
@AllOpen
@NoArgConstructor
class MailTemplateKoppelingenService @Inject constructor(
    private val entityManager: EntityManager,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService
) {
    fun find(id: Long): ZaaktypeMailtemplateParameters? =
        entityManager.find(ZaaktypeMailtemplateParameters::class.java, id)

    @Transactional(REQUIRED)
    fun delete(id: Long) {
        find(id)?.let {
            entityManager.remove(it)
            zaaktypeConfigurationService.evict(it.zaaktypeConfiguration.zaaktypeUuid)
        }
    }

    @Transactional(REQUIRED)
    fun storeMailtemplateKoppeling(
        zaaktypeCmmnMailtemplateParameters: ZaaktypeMailtemplateParameters
    ): ZaaktypeMailtemplateParameters {
        validateObject(zaaktypeCmmnMailtemplateParameters)
        val existingId = zaaktypeCmmnMailtemplateParameters.id
        return if (existingId != null && find(existingId) != null) {
            entityManager.merge(zaaktypeCmmnMailtemplateParameters)
        } else {
            entityManager.persist(zaaktypeCmmnMailtemplateParameters)
            zaaktypeCmmnMailtemplateParameters
        }.also {
            zaaktypeConfigurationService.evict(it.zaaktypeConfiguration.zaaktypeUuid)
        }
    }

    fun readMailtemplateKoppeling(id: Long): ZaaktypeMailtemplateParameters =
        find(id) ?: throw NoSuchElementException(
            "${ZaaktypeMailtemplateParameters::class.java.simpleName} with id=$id not found"
        )

    fun listMailtemplateKoppelingen(): List<ZaaktypeMailtemplateParameters> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeMailtemplateParameters::class.java)
        val root = query.from(ZaaktypeMailtemplateParameters::class.java)
        query.select(root)
        return entityManager.createQuery(query).resultList
    }
}
