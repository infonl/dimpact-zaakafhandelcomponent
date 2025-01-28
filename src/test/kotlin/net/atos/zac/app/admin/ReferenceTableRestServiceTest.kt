/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.atos.zac.admin.ReferenceTableAdminService
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.createReferenceTable
import net.atos.zac.admin.model.createReferenceTableValue
import net.atos.zac.policy.PolicyService
import nl.info.zac.exception.InputValidationFailedException

class ReferenceTableRestServiceTest : BehaviorSpec({
    val referenceTableService = mockk<ReferenceTableService>()
    val referenceTableAdminService = mockk<ReferenceTableAdminService>()
    val policyService = mockk<PolicyService>()
    val referenceTableRestService = ReferenceTableRestService(
        referenceTableService,
        referenceTableAdminService,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Two communication channels including E-formulier") {
        val referenceTable = createReferenceTable(
            id = 1L,
            code = "dummyCode",
            name = "dummyName"
        )
        val referenceValue1 = "dummyValue1"
        val referenceValue2 = "E-formulier"
        val referenceTableValue1 = createReferenceTableValue(
            id = 1L,
            name = referenceValue1,
            sortOrder = 1
        )
        val referenceTableValue2 = createReferenceTableValue(
            id = 2L,
            name = referenceValue2,
            sortOrder = 2
        )
        val referenceTableValues = listOf(
            referenceTableValue1,
            referenceTableValue2
        )
        referenceTable.values = referenceTableValues.toMutableList()
        every { referenceTableService.readReferenceTable("COMMUNICATIEKANAAL") } returns referenceTable

        When("the communication channels are retrieved including E-formulier") {
            val communicationChannels = referenceTableRestService.listCommunicationChannels(true)

            Then("the communication channels are returned successfully including E-formulier") {
                communicationChannels.size shouldBe referenceTableValues.size
                communicationChannels[0] shouldBe referenceValue1
                communicationChannels[1] shouldBe referenceValue2
            }
        }

        When("the communication channels are retrieved excluding E-formulier") {
            val communicationChannels = referenceTableRestService.listCommunicationChannels(false)

            Then("the communication channels are returned successfully excluding E-formulier") {
                communicationChannels.size shouldBe referenceTableValues.size - 1
                communicationChannels[0] shouldBe referenceValue1
            }
        }
    }

    Given("Two server error page texts") {
        val referenceTable = createReferenceTable(
            id = 1L,
            code = "dummyCode",
            name = "dummyName"
        )
        val referenceValue1 = "dummyValue1"
        val referenceValue2 = "dummyValue2"
        val referenceTableValue1 = createReferenceTableValue(
            id = 1L,
            name = referenceValue1,
            sortOrder = 1
        )
        val referenceTableValue2 = createReferenceTableValue(
            id = 2L,
            name = referenceValue2,
            sortOrder = 2
        )
        val referenceTableValues = listOf(
            referenceTableValue1,
            referenceTableValue2
        )
        referenceTable.values = referenceTableValues.toMutableList()

        every { referenceTableService.readReferenceTable("SERVER_ERROR_ERROR_PAGINA_TEKST") } returns referenceTable

        When("the server error page texts are retrieved") {
            val serverErrorPageTexts = referenceTableRestService.listServerErrorPageTexts()

            Then("the server error page texts are returned") {
                serverErrorPageTexts.size shouldBe referenceTableValues.size
                serverErrorPageTexts[0] shouldBe referenceValue1
                serverErrorPageTexts[1] shouldBe referenceValue2
            }
        }
    }

    Given("An existing reference table with one value") {
        val referenceTable = createReferenceTable()
        val updatedReferenceTable = createReferenceTable()
        val updatedReferenceTableSlot = slot<ReferenceTable>()
        every { policyService.readOverigeRechten().beheren } returns true
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable
        every {
            referenceTableAdminService.updateReferenceTable(capture(updatedReferenceTableSlot))
        } returns updatedReferenceTable

        When("the reference table is updated with two new values and a new name") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                naam = "dummyUpdatedName",
                waarden = listOf(
                    createRestReferenceTableValue(
                        name = "dummyValue1"
                    ),
                    createRestReferenceTableValue(
                        name = "dummyValue2"
                    )
                )
            )

            val updatedRestReferenceTable = referenceTableRestService.updateReferenceTable(
                id = referenceTable.id!!,
                restReferenceTableUpdate
            )

            Then(
                """
                    the reference table is updated successfully and only contains the two new values
                    and the new name thereby completely replacing the existing value(s) and the existing name
                """
            ) {
                with(updatedRestReferenceTable) {
                    id shouldBe updatedReferenceTable.id
                    code shouldBe updatedReferenceTable.code
                    naam shouldBe updatedReferenceTable.name
                    systeem shouldBe updatedReferenceTable.isSystemReferenceTable
                    aantalWaarden shouldBe updatedReferenceTable.values.size
                    waarden.size shouldBe updatedReferenceTable.values.size
                }
                with(updatedReferenceTableSlot.captured) {
                    code shouldBe referenceTable.code
                    name shouldBe "dummyUpdatedName"
                    isSystemReferenceTable shouldBe referenceTable.isSystemReferenceTable
                    values.size shouldBe 2
                    values[0].name shouldBe "dummyValue1"
                    values[1].name shouldBe "dummyValue2"
                }
            }
        }
    }

    Given("An existing reference table with two values of which one is a system value") {
        val referenceTable = createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(
                    name = "dummyValue1",
                    isSystemValue = true
                ),
                createReferenceTableValue(
                    name = "dummyValue2",
                    isSystemValue = false
                )
            )
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable

        When("the reference table is updated with two new values not including the existing system value") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                naam = "dummyUpdatedName",
                waarden = listOf(
                    createRestReferenceTableValue(
                        name = "dummyValue3",
                        isSystemValue = false
                    ),
                    createRestReferenceTableValue(
                        name = "dummyValue4",
                        isSystemValue = false
                    )
                )
            )

            val exception = shouldThrow<InputValidationFailedException> {
                referenceTableRestService.updateReferenceTable(
                    id = referenceTable.id!!,
                    restReferenceTableUpdate
                )
            }

            Then("an exception should be thrown indicating that system values cannot be updated") {
                exception.message shouldBe "msg.error.system.reference.table.system.values.cannot.be.changed"
            }
        }
    }
})
