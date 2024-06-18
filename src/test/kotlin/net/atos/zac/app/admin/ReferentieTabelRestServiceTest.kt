/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.app.admin.converter.RESTReferentieTabelConverter
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
