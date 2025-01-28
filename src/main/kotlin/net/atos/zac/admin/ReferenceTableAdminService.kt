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
import net.atos.zac.app.exception.InputValidationFailedException
import nl.info.zac.exception.ErrorCode.ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS
import nl.info.zac.exception.ErrorCode.ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

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
        referenceTableService.findReferenceTable(referenceTable.code)?.run {
            throw InputValidationFailedException(ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS)
        }
        return entityManager.merge(referenceTable)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun updateReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        // check if there is already a different reference table, i.e. one with a different id, but with the same code
        referenceTableService.findReferenceTable(referenceTable.code)?.run {
            if (this.id != referenceTable.id) {
                throw InputValidationFailedException(ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS)
            }
        }
        return entityManager.merge(referenceTable)
    }

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
                            ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS
                        )
                    }
                }
            }
            entityManager.remove(referenceTable)
        }
    }
}
