/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue

class ReferenceTableServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val referenceTableService = ReferenceTableService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A reference table") {
        val referenceTableID = 1234L
        val referenceTable = createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(id = 1, name = "dummyValue1", sortOrder = 1),
                createReferenceTableValue(id = 2, name = "dummyValue2", sortOrder = 0),
                createReferenceTableValue(id = 3, name = "dummyValue2", sortOrder = 2)
            )
        )
        every { entityManager.find(ReferenceTable::class.java, referenceTableID) } returns referenceTable

        When("the reference table is requested by id") {
            val returnedReferenceTable = referenceTableService.readReferenceTable(referenceTableID)

            Then("the reference table is returned") {
                returnedReferenceTable shouldBe referenceTable
            }
        }

        When("the reference table values are requested") {
            val referenceTableValues = referenceTableService.listReferenceTableValuesSorted(referenceTable)

            Then("the reference table values are returned sorted by sort order") {
                referenceTableValues shouldBe referenceTable.values.sortedBy { it.sortOrder }
            }
        }
    }
    Given("No reference table for the given id") {
        val referenceTableID = 1234L
        every { entityManager.find(ReferenceTable::class.java, referenceTableID) } returns null

        When("the reference table is requested by id") {
            val exception = shouldThrow<RuntimeException> {
                referenceTableService.readReferenceTable(referenceTableID)
            }

            Then("the reference table is returned") {
                exception.message shouldBe "No reference table found with id '$referenceTableID'"
            }
        }
    }
})
