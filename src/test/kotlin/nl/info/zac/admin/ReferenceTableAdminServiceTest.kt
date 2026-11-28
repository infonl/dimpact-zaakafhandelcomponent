/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import net.atos.zac.admin.model.HumanTaskReferentieTabel
import nl.info.zac.admin.model.createHumanTaskReferentieTabel
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.exception.ErrorCode.ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED
import nl.info.zac.exception.InputValidationFailedException

class ReferenceTableAdminServiceTest : BehaviorSpec({
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQueryHumanTaskReferentieTabel = mockk<CriteriaQuery<HumanTaskReferentieTabel>>()
    val rootHumanTaskReferentieTabel = mockk<Root<HumanTaskReferentieTabel>>()
    val entityManager = mockk<EntityManager>()
    val referenceTableService = mockk<ReferenceTableService>()
    val referenceTableAdminService = ReferenceTableAdminService(
        entityManager = entityManager,
        referenceTableService = referenceTableService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
            A reference table that is not a system reference table and which is not in use by any human task reference tables
            """
    ) {
        val referenceTable = createReferenceTable(
            isSystemReferenceTable = false
        )
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(HumanTaskReferentieTabel::class.java)
        } returns criteriaQueryHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.from(HumanTaskReferentieTabel::class.java)
        } returns rootHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.select(rootHumanTaskReferentieTabel)
        } returns criteriaQueryHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.where(
                criteriaBuilder.equal(rootHumanTaskReferentieTabel.get<Any>("tabel").get<Any>("id"), referenceTable.id)
            )
        } returns criteriaQueryHumanTaskReferentieTabel
        every { entityManager.createQuery(criteriaQueryHumanTaskReferentieTabel).resultList } returns emptyList()
        every { entityManager.remove(referenceTable) } just Runs

        When("the reference table is deleted") {
            referenceTableAdminService.deleteReferenceTable(referenceTable.id!!)

            Then("the reference table should be successfully deleted") {
                verify(exactly = 1) {
                    entityManager.remove(referenceTable)
                }
            }
        }
    }

    Given(
        """
            A reference table that is not a system reference table and which is in use by a human task reference table
            """
    ) {
        val referenceTable = createReferenceTable(
            isSystemReferenceTable = false
        )
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(HumanTaskReferentieTabel::class.java)
        } returns criteriaQueryHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.from(HumanTaskReferentieTabel::class.java)
        } returns rootHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.select(rootHumanTaskReferentieTabel)
        } returns criteriaQueryHumanTaskReferentieTabel
        every {
            criteriaQueryHumanTaskReferentieTabel.where(
                criteriaBuilder.equal(rootHumanTaskReferentieTabel.get<Any>("tabel").get<Any>("id"), referenceTable.id)
            )
        } returns criteriaQueryHumanTaskReferentieTabel
        every {
            entityManager.createQuery(criteriaQueryHumanTaskReferentieTabel).resultList
        } returns listOf(createHumanTaskReferentieTabel())

        When("an attempt is made to delete the reference table") {
            val exception = shouldThrow<InputValidationFailedException> {
                referenceTableAdminService.deleteReferenceTable(referenceTable.id!!)
            }

            Then("an exception is thrown and the reference table is not deleted") {
                exception.errorCode shouldBe ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS
                exception.message shouldBe null
                verify(exactly = 0) {
                    entityManager.remove(referenceTable)
                }
            }
        }
    }

    Given("A system reference table") {
        val referenceTable = createReferenceTable(
            isSystemReferenceTable = true
        )
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable

        When("an attempt is made to delete the system reference table") {
            val exception = shouldThrow<InputValidationFailedException> {
                referenceTableAdminService.deleteReferenceTable(referenceTable.id!!)
            }

            Then(
                """
                    an exception should be thrown and the reference table is not deleted since it is not allowed
                    to delete system reference tables
                    """
            ) {
                exception.errorCode shouldBe ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED
                exception.message shouldBe null
            }
        }
    }
})
