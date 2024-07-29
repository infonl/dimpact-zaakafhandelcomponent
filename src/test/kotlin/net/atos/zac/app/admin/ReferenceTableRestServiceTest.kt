/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

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

    Given("Two communicatiekanalen including E-formulier") {
        val referentieTabel = createReferenceTable(
            id = 1L,
            code = "COMMUNICATIEKANAAL",
            naam = "Communicatiekanalen"
        )
        val referentieWaarde1 = "dummyWaarde1"
        val referentieWaarde2 = "E-formulier"
        val referentieTabelWaarde1 = createReferenceTableValue(
            id = 1L,
            naam = referentieWaarde1,
            volgorde = 1
        )
        val referentieTabelWaarde2 = createReferenceTableValue(
            id = 2L,
            naam = referentieWaarde2,
            volgorde = 2
        )
        val referentieTabelWaarden = listOf(
            referentieTabelWaarde1,
            referentieTabelWaarde2
        )
        referentieTabel.values = referentieTabelWaarden.toMutableList()
        every { referenceTableService.readReferenceTable("COMMUNICATIEKANAAL") } returns referentieTabel

        When("the communicatiekanalen are retrieved including E-formulier") {
            val communicatiekanalen = referenceTableRestService.listCommunicationChannels(true)

            Then("the communicatiekanalen are returned including E-formulier") {
                communicatiekanalen.size shouldBe referentieTabelWaarden.size
                communicatiekanalen[0] shouldBe referentieWaarde1
                communicatiekanalen[1] shouldBe referentieWaarde2
            }
        }

        When("the communicatiekanalen are retrieved excluding E-formulier") {
            val communicatiekanalen = referenceTableRestService.listCommunicationChannels(false)

            Then("the communicatiekanalen are returned excluding E-formulier") {
                communicatiekanalen.size shouldBe referentieTabelWaarden.size - 1
                communicatiekanalen[0] shouldBe referentieWaarde1
            }
        }
    }

    Given("Two server error page texts") {
        val referentieTabel = createReferenceTable(
            id = 1L,
            code = "SERVER_ERROR_ERROR_PAGINA_TEKST",
            naam = "Server error page text"
        )
        val referentieWaarde1 = "dummyWaarde1"
        val referentieWaarde2 = "dummyWaarde2"
        val referentieTabelWaarde1 = createReferenceTableValue(
            id = 1L,
            naam = referentieWaarde1,
            volgorde = 1
        )
        val referentieTabelWaarde2 = createReferenceTableValue(
            id = 2L,
            naam = referentieWaarde2,
            volgorde = 2
        )
        val referentieTabelWaarden = listOf(
            referentieTabelWaarde1,
            referentieTabelWaarde2
        )
        referentieTabel.values = referentieTabelWaarden.toMutableList()

        every { referenceTableService.readReferenceTable("SERVER_ERROR_ERROR_PAGINA_TEKST") } returns referentieTabel

        When("the server error page texts are retrieved") {
            val serverErrorPageTexts = referenceTableRestService.listServerErrorPageTexts()

            Then("the server error page texts are returned") {
                serverErrorPageTexts.size shouldBe referentieTabelWaarden.size
                serverErrorPageTexts[0] shouldBe referentieWaarde1
                serverErrorPageTexts[1] shouldBe referentieWaarde2
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
                        name = "dummyWaarde100"
                    ),
                    createRestReferenceTableValue(
                        name = "dummyWaarde101"
                    )
                )
            )

            val updatedRestReferenceTable = referenceTableRestService.updateReferenceTable(
                id = referenceTable.id!!,
                restReferenceTableUpdate
            )

            Then("the reference table is updated successfully and only contains the two new values") {
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
                    values[0].name shouldBe "dummyWaarde100"
                    values[1].name shouldBe "dummyWaarde101"
                }
            }
        }
    }
})
