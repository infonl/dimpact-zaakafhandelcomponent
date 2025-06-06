/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.admin.exception.ReferenceTableNotFoundException
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Transactional(Transactional.TxType.SUPPORTS)
class ReferenceTableService @Inject constructor(
    val entityManager: EntityManager
) {
    /**
     * Finds a reference table by its code, where code is treated as uppercase only.
     *
     * @return the reference table or null if none could be found
     */
    @Suppress("NestedBlockDepth")
    fun findReferenceTable(code: String): ReferenceTable? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ReferenceTable::class.java).let { query ->
                query.from(ReferenceTable::class.java).let { root ->
                    criteriaBuilder.equal(root.get<Any>("code"), code.uppercase()).let { predicate ->
                        query.select(root).where(predicate)
                    }
                }
                entityManager.createQuery(query).resultList
            }
        }.firstOrNull()

    fun listReferenceTableValuesSorted(referenceTable: ReferenceTable) =
        referenceTable.values.sortedBy { it.sortOrder }

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
            throw ReferenceTableNotFoundException(id)
        }

    fun readReferenceTable(code: String): ReferenceTable =
        findReferenceTable(code) ?: run {
            throw ReferenceTableNotFoundException("No reference table found with code '$code'")
        }
}
