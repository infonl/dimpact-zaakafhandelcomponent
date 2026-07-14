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
import nl.info.zac.admin.model.ReferenceTable
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
})
