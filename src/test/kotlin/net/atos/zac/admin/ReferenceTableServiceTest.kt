/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.createReferenceTable

class ReferenceTableServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val referenceTableService = ReferenceTableService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A reference table") {
        val referenceTableID = 1234L
        val referenceTable = createReferenceTable()
        every { entityManager.find(ReferenceTable::class.java, referenceTableID) } returns referenceTable

        When("the reference table is requested by id") {
            val returnedReferenceTable = referenceTableService.readReferenceTable(referenceTableID)

            Then("the reference table is returned") {
                returnedReferenceTable shouldBe referenceTable
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
                exception.message shouldBe "Reference table with id '$referenceTableID' not found"
            }
        }
    }
})
