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
import net.atos.zac.app.util.exception.InputValidationFailedException
import net.atos.zac.app.util.exception.RestExceptionMapper.Companion.ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS
import net.atos.zac.app.util.exception.RestExceptionMapper.Companion.ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED
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
    fun createReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        referenceTableService.findReferenceTable(referenceTable.code)?.let {
            throw InputValidationFailedException(ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS)
        }
        return entityManager.merge(referenceTable)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun updateReferenceTable(referenceTable: ReferenceTable): ReferenceTable = entityManager.merge(referenceTable)

    @Transactional(Transactional.TxType.REQUIRED)
    @Suppress("NestedBlockDepth")
    fun deleteReferenceTable(id: Long) {
        val referenceTable = referenceTableService.readReferenceTable(id).also {
            if (it.isSystemReferenceTable) {
                throw InputValidationFailedException(ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED)
            }
        }
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(HumanTaskReferentieTabel::class.java).let { query ->
                query.from(HumanTaskReferentieTabel::class.java).let {
                    query.select(it)
                        .where(criteriaBuilder.equal(it.get<Any>("tabel").get<Any>("id"), referenceTable.id))
                }
                entityManager.createQuery(query).resultList.run {
                    if (this.isNotEmpty()) {
                        throw InputValidationFailedException(
                            "This reference table is in use by one or more human task reference tables and cannot be deleted." +
                                "Human task reference table fields: '${
                                    this.map { it.veld }
                                        .distinct()
                                        .joinToString()
                                }'"
                        )
                    }
                }
            }
            entityManager.remove(referenceTable)
        }
    }
}
