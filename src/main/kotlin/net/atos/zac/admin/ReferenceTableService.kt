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
import net.atos.zac.admin.model.ReferenceTableValue
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.Optional

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Transactional(Transactional.TxType.SUPPORTS)
class ReferenceTableService @Inject constructor(
    val entityManager: EntityManager
) {
    fun findReferenceTable(code: String): Optional<ReferenceTable> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ReferenceTable::class.java)
        val root = query.from(ReferenceTable::class.java)
        query.select(root).where(builder.equal(root.get<Any>("code"), code))
        val resultList = entityManager.createQuery(query).resultList
        return if (resultList.isEmpty()) Optional.empty() else Optional.of(resultList.first())
    }

    fun listReferenceTableWaardenSorted(referenceTable: ReferenceTable) =
        referenceTable.waarden
            .sortedBy { it.volgorde }
            .toList()

    fun listReferenceTables(): List<ReferenceTable> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            ReferenceTable::class.java
        )
        val root = query.from(
            ReferenceTable::class.java
        )
        query.orderBy(builder.asc(root.get<Any>("naam")))
        query.select(root)
        return entityManager.createQuery(query).resultList
    }

    fun readReferenceTable(id: Long): ReferenceTable =
        entityManager.find(ReferenceTable::class.java, id)?.let {
            return it
        } ?: throw ReferenceTableNotFoundException("Reference table with id '$id' not found")

    fun readReferenceTable(code: String): ReferenceTable =
        findReferenceTable(code).orElseThrow {
            ReferenceTableNotFoundException("Reference table with code '$code' not found")
        }
}
