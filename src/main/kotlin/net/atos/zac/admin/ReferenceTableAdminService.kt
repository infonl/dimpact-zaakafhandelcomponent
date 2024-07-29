/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.admin.model.HumanTaskReferentieTabel
import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.shared.exception.FoutmeldingException
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class ReferenceTableAdminService @Inject constructor(
    private val entityManager: EntityManager,
    private val referenceTableService: ReferenceTableService
) {
    @Transactional(Transactional.TxType.REQUIRED)
    fun createReferenceTable(referenceTable: ReferenceTable) =
        updateReferenceTable(referenceTable)

    @Transactional(Transactional.TxType.REQUIRED)
    fun updateReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        referenceTableService.findReferenceTable(referenceTable.code)?.let {
            if (it.id != referenceTable.id) {
                throw FoutmeldingException("Er bestaat al een referentietabel met de code '${referenceTable.code}'")
            }
        }
        return entityManager.merge(referenceTable)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun deleteReferenceTable(id: Long) {
        val referenceTable = entityManager.find(ReferenceTable::class.java, id)
        require(!referenceTable.isSystemReferenceTable) {
            "Deze referentietabel is een systeemtabel en kan niet verwijderd worden."
        }
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            val query = criteriaBuilder.createQuery(HumanTaskReferentieTabel::class.java)
            query.from(HumanTaskReferentieTabel::class.java).let {
                query.select(it).where(criteriaBuilder.equal(it.get<Any>("tabel").get<Any>("id"), referenceTable.id))
            }
            entityManager.createQuery(query).resultList.run {
                if (this.isNotEmpty()) {
                    throw FoutmeldingException(
                        "Deze referentietabel wordt gebruikt (voor: ${this
                            .map { it.veld }
                            .distinct()
                            .joinToString()
                        }) en kan niet verwijderd worden."
                    )
                }
            }
            entityManager.remove(referenceTable)
        }
    }
}
