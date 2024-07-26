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
import net.atos.zac.util.ValidationUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.stream.Collectors

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class ReferenceTableAdminService @Inject constructor(
    private val entityManager: EntityManager,
    private val referenceTableService: ReferenceTableService
) {
    companion object {
        private const val UNIQUE_CONSTRAINT = "Er bestaat al een referentietabel met de code \"%s\"."
        private const val FOREIGN_KEY_CONSTRAINT =
            "Deze referentietabel wordt gebruikt (voor: %s) en kan niet verwijderd worden."
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun newReferenceTable(): ReferenceTable {
        val nieuw = ReferenceTable()
        nieuw.code = getUniqueCodeForReferenceTable(1, referenceTableService.listReferenceTables())
        nieuw.naam = "Nieuwe referentietabel"
        return nieuw
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun createReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        return updateReferenceTable(referenceTable)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun updateReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        ValidationUtil.valideerObject(referenceTable)
        referenceTableService.findReferenceTable(referenceTable.code)
            .ifPresent {
                if (it.id != referenceTable.id) {
                    throw FoutmeldingException(String.format(UNIQUE_CONSTRAINT, referenceTable.code))
                }
            }
        return entityManager.merge(referenceTable)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun deleteReferenceTable(id: Long) {
        val tabel = entityManager.find(ReferenceTable::class.java, id)
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            HumanTaskReferentieTabel::class.java
        )
        val root = query.from(
            HumanTaskReferentieTabel::class.java
        )
        query.select(root).where(builder.equal(root.get<Any>("tabel").get<Any>("id"), tabel.id))
        val resultList = entityManager.createQuery(query).resultList
        if (resultList.isNotEmpty()) {
            throw FoutmeldingException(
                String.format(
                    FOREIGN_KEY_CONSTRAINT,
                    resultList.stream()
                        .map { obj: HumanTaskReferentieTabel -> obj.veld }
                        .distinct()
                        .collect(Collectors.joining(", "))
                )
            )
        }
        entityManager.remove(tabel)
    }

    private fun getUniqueCodeForReferenceTable(i: Int, list: List<ReferenceTable>): String {
        val code = "TABEL" + (if (1 < i) i else "")
        if (list.stream()
                .anyMatch { referentieTabel: ReferenceTable -> code == referentieTabel.code }
        ) {
            return getUniqueCodeForReferenceTable(i + 1, list)
        }
        return code
    }
}
