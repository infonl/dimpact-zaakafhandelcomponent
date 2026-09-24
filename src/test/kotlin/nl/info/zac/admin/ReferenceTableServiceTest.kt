/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.exception.SystemReferenceTableNotConfiguredException
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.AFZENDER
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue

class ReferenceTableServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val referenceTableService = ReferenceTableService(entityManager)

    afterEach {
        checkUnnecessaryStub()
    }

    given("A reference table") {
        val referenceTableID = 1234L
        val referenceTable = createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(id = 1, name = "fakeValue1", sortOrder = 1),
                createReferenceTableValue(id = 2, name = "fakeValue2", sortOrder = 0),
                createReferenceTableValue(id = 3, name = "fakeValue2", sortOrder = 2)
            )
        )
        every { entityManager.find(ReferenceTable::class.java, referenceTableID) } returns referenceTable

        `when`("the reference table is requested by id") {
            val returnedReferenceTable = referenceTableService.readReferenceTable(referenceTableID)

            then("the reference table is returned") {
                returnedReferenceTable shouldBe referenceTable
            }
        }

        `when`("the reference table values are requested") {
            val referenceTableValues = referenceTableService.listReferenceTableValuesSorted(referenceTable)

            then("the reference table values are returned sorted by sort order") {
                referenceTableValues shouldBe referenceTable.values.sortedBy { it.sortOrder }
            }
        }
    }
    given("No reference table for the given id") {
        val referenceTableID = 1234L
        every { entityManager.find(ReferenceTable::class.java, referenceTableID) } returns null

        `when`("the reference table is requested by id") {
            val exception = shouldThrow<RuntimeException> {
                referenceTableService.readReferenceTable(referenceTableID)
            }

            then("the reference table is returned") {
                exception.message shouldBe "No reference table found with id '$referenceTableID'"
            }
        }
    }
    given("A system reference table that is configured") {
        val referenceTable = createReferenceTable(code = AFZENDER.name, isSystemReferenceTable = true)
        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<ReferenceTable>>()
        val root = mockk<Root<ReferenceTable>>()
        val codePath = mockk<Path<Any>>()
        val predicate = mockk<Predicate>()
        val typedQuery = mockk<TypedQuery<ReferenceTable>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ReferenceTable::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ReferenceTable::class.java) } returns root
        every { root.get<Any>("code") } returns codePath
        every { criteriaBuilder.equal(codePath, AFZENDER.name) } returns predicate
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns listOf(referenceTable)

        `when`("the system reference table is requested") {
            val returnedReferenceTable = referenceTableService.readSystemReferenceTable(AFZENDER)

            then("the reference table is returned") {
                returnedReferenceTable shouldBe referenceTable
            }
        }
    }
    given("A system reference table that has not been configured") {
        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<ReferenceTable>>()
        val root = mockk<Root<ReferenceTable>>()
        val codePath = mockk<Path<Any>>()
        val predicate = mockk<Predicate>()
        val typedQuery = mockk<TypedQuery<ReferenceTable>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ReferenceTable::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ReferenceTable::class.java) } returns root
        every { root.get<Any>("code") } returns codePath
        every { criteriaBuilder.equal(codePath, AFZENDER.name) } returns predicate
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        `when`("the system reference table is requested") {
            val exception = shouldThrow<SystemReferenceTableNotConfiguredException> {
                referenceTableService.readSystemReferenceTable(AFZENDER)
            }

            then("it should throw SystemReferenceTableNotConfiguredException naming the system reference table") {
                exception.message shouldBe
                    "No system reference table found for 'AFZENDER'. It must be seeded by an administrator."
            }
        }
    }
    given("A reference table row with the reserved code but not flagged as a system reference table") {
        val referenceTable = createReferenceTable(code = AFZENDER.name, isSystemReferenceTable = false)
        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<ReferenceTable>>()
        val root = mockk<Root<ReferenceTable>>()
        val codePath = mockk<Path<Any>>()
        val predicate = mockk<Predicate>()
        val typedQuery = mockk<TypedQuery<ReferenceTable>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ReferenceTable::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ReferenceTable::class.java) } returns root
        every { root.get<Any>("code") } returns codePath
        every { criteriaBuilder.equal(codePath, AFZENDER.name) } returns predicate
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns listOf(referenceTable)

        `when`("the system reference table is requested") {
            val exception = shouldThrow<SystemReferenceTableNotConfiguredException> {
                referenceTableService.readSystemReferenceTable(AFZENDER)
            }

            then("it should throw SystemReferenceTableNotConfiguredException rather than silently using the row") {
                exception.message shouldBe
                    "No system reference table found for 'AFZENDER'. It must be seeded by an administrator."
            }
        }
    }
})
