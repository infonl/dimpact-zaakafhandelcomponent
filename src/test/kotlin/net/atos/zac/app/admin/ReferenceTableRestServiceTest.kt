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
import net.atos.zac.admin.ReferenceTableAdminService
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.model.createReferenceTable
import net.atos.zac.admin.model.createReferenceTableValue
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createOverigeRechten

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
    Given("'beheren overige rechten' policy permissions") {
        val referenceTable = createReferenceTable()
        every { referenceTableAdminService.newReferenceTable() } returns referenceTable
        every { policyService.readOverigeRechten() } returns createOverigeRechten()

        When("a new reference table is created") {
            val restReferenceTable = referenceTableRestService.newReferenceTable()

            Then("the new reference table is created succesfully") {
                with(restReferenceTable) {
                    code shouldBe referenceTable.code
                    naam shouldBe referenceTable.name
                }
            }
        }
    }
})
