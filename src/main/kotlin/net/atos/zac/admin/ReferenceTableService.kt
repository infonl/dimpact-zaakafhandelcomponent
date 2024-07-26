/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.admin.exception.ReferenceTableNotFoundException
import net.atos.zac.admin.model.ReferenceTable
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Transactional(Transactional.TxType.SUPPORTS)
class ReferenceTableService @Inject constructor(
    val entityManager: EntityManager
) {
    fun findReferenceTable(code: String): ReferenceTable? {
        val resultList = entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ReferenceTable::class.java).let { query ->
                query.from(ReferenceTable::class.java).let {
                    query.select(it).where(criteriaBuilder.equal(it.get<Any>("code"), code))
                }
                entityManager.createQuery(query).resultList
            }
        }
        return resultList.firstOrNull()
    }

    fun listReferenceTableWaardenSorted(referenceTable: ReferenceTable) =
        referenceTable.values
            .sortedBy { it.sortOrder }
            .toList()

    fun listReferenceTables(): List<ReferenceTable> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ReferenceTable::class.java).let { query ->
                query.from(ReferenceTable::class.java).let {
                    query.orderBy(criteriaBuilder.asc(it.get<Any>("name")))
                    query.select(it)
                }
                entityManager.createQuery(query).resultList
            }
        }

    fun readReferenceTable(id: Long): ReferenceTable =
        entityManager.find(ReferenceTable::class.java, id) ?: run {
            throw ReferenceTableNotFoundException("Reference table with id '$id' not found")
        }

    fun readReferenceTable(code: String): ReferenceTable =
        findReferenceTable(code) ?: run {
            throw ReferenceTableNotFoundException("Reference table with code '$code' not found")
        }
}
