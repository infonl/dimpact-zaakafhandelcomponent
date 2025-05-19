/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import nl.info.zac.admin.ReferenceTableAdminService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue
import nl.info.zac.exception.ErrorCode.ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.policy.PolicyService

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
            code = "fakeCode",
            name = "fakeName"
        )
        val referenceValue1 = "fakeValue1"
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
            code = "fakeCode",
            name = "fakeName"
        )
        val referenceValue1 = "fakeValue1"
        val referenceValue2 = "fakeValue2"
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
                naam = "fakeUpdatedName",
                waarden = listOf(
                    createRestReferenceTableValue(
                        name = "fakeValue1"
                    ),
                    createRestReferenceTableValue(
                        name = "fakeValue2"
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
                    name shouldBe "fakeUpdatedName"
                    isSystemReferenceTable shouldBe referenceTable.isSystemReferenceTable
                    values.size shouldBe 2
                    values[0].name shouldBe "fakeValue1"
                    values[1].name shouldBe "fakeValue2"
                }
            }
        }
    }

    Given("An existing reference table with two values of which one is a system value") {
        val referenceTable = createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(
                    name = "fakeValue1",
                    isSystemValue = true
                ),
                createReferenceTableValue(
                    name = "fakeValue2",
                    isSystemValue = false
                )
            )
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every { referenceTableService.readReferenceTable(referenceTable.id!!) } returns referenceTable

        When("the reference table is updated with two new values not including the existing system value") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                naam = "fakeUpdatedName",
                waarden = listOf(
                    createRestReferenceTableValue(
                        name = "fakeValue3",
                        isSystemValue = false
                    ),
                    createRestReferenceTableValue(
                        name = "fakeValue4",
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
                exception.errorCode shouldBe ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED
                exception.message shouldBe null
            }
        }
    }
})
