/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.policy.PolicyService
import net.atos.zac.zaaksturing.ReferentieTabelBeheerService
import net.atos.zac.zaaksturing.ReferentieTabelService
import net.atos.zac.zaaksturing.model.createReferentieTabel
import net.atos.zac.zaaksturing.model.createReferentieTabelWaarde

class ReferentieTabelRestServiceTest : BehaviorSpec({
    val referentieTabelService = mockk<ReferentieTabelService>()
    val referentieTabelBeheerService = mockk<ReferentieTabelBeheerService>()
    val policyService = mockk<PolicyService>()
    val referentieTabelRESTService = ReferentieTabelRestService(
        referentieTabelService,
        referentieTabelBeheerService,
        policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("Two communicatiekanalen including E-formulier") {
        val referentieTabel = createReferentieTabel(
            id = 1L,
            code = "COMMUNICATIEKANAAL",
            naam = "Communicatiekanalen"
        )
        val referentieWaarde1 = "dummyWaarde1"
        val referentieWaarde2 = "E-formulier"
        val referentieTabelWaarde1 = createReferentieTabelWaarde(
            id = 1L,
            naam = referentieWaarde1,
            volgorde = 1
        )
        val referentieTabelWaarde2 = createReferentieTabelWaarde(
            id = 2L,
            naam = referentieWaarde2,
            volgorde = 2
        )
        val referentieTabelWaarden = listOf(
            referentieTabelWaarde1,
            referentieTabelWaarde2
        )
        referentieTabelWaarden.forEach {
            referentieTabel.addWaarde(it)
        }
        every { referentieTabelService.readReferentieTabel("COMMUNICATIEKANAAL") } returns referentieTabel

        When("the communicatiekanalen are retrieved including E-formulier") {
            val communicatiekanalen = referentieTabelRESTService.listCommunicatiekanalen(true)

            Then("the communicatiekanalen are returned including E-formulier") {
                communicatiekanalen.size shouldBe referentieTabelWaarden.size
                communicatiekanalen[0] shouldBe referentieWaarde1
                communicatiekanalen[1] shouldBe referentieWaarde2
            }
        }

        When("the communicatiekanalen are retrieved excluding E-formulier") {
            val communicatiekanalen = referentieTabelRESTService.listCommunicatiekanalen(false)

            Then("the communicatiekanalen are returned excluding E-formulier") {
                communicatiekanalen.size shouldBe referentieTabelWaarden.size - 1
                communicatiekanalen[0] shouldBe referentieWaarde1
            }
        }
    }
    Given("Two server error page texts") {
        val referentieTabel = createReferentieTabel(
            id = 1L,
            code = "SERVER_ERROR_ERROR_PAGINA_TEKST",
            naam = "Server error page text"
        )
        val referentieWaarde1 = "dummyWaarde1"
        val referentieWaarde2 = "dummyWaarde2"
        val referentieTabelWaarde1 = createReferentieTabelWaarde(
            id = 1L,
            naam = referentieWaarde1,
            volgorde = 1
        )
        val referentieTabelWaarde2 = createReferentieTabelWaarde(
            id = 2L,
            naam = referentieWaarde2,
            volgorde = 2
        )
        val referentieTabelWaarden = listOf(
            referentieTabelWaarde1,
            referentieTabelWaarde2
        )
        referentieTabelWaarden.forEach {
            referentieTabel.addWaarde(it)
        }
        every { referentieTabelService.readReferentieTabel("SERVER_ERROR_ERROR_PAGINA_TEKST") } returns referentieTabel

        When("the server error page texts are retrieved") {
            val serverErrorPageTexts = referentieTabelRESTService.listServerErrorPageTexts()

            Then("the server error page texts are returned") {
                serverErrorPageTexts.size shouldBe referentieTabelWaarden.size
                serverErrorPageTexts[0] shouldBe referentieWaarde1
                serverErrorPageTexts[1] shouldBe referentieWaarde2
            }
        }
    }
})
