/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createWerklijstRechten

class PolicyRestServiceTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val policyRestService = PolicyRestService(policyService)

    Given("Existing werklijst rechten") {
        val werklijstRechten = createWerklijstRechten()
        every { policyService.readWerklijstRechten() } returns werklijstRechten

        When("readWerklijstRechten is called") {
            val restWerklijstRechten = policyRestService.readWerklijstRechten()

            Then("it should return the converted RestWerklijstRechten") {
                with(restWerklijstRechten) {
                    inbox shouldBe werklijstRechten.inbox
                    ontkoppeldeDocumentenVerwijderen shouldBe werklijstRechten.ontkoppeldeDocumentenVerwijderen
                    inboxProductaanvragenVerwijderen shouldBe werklijstRechten.inboxProductaanvragenVerwijderen
                    zakenTaken shouldBe werklijstRechten.zakenTaken
                    zakenTakenVerdelen shouldBe werklijstRechten.zakenTakenVerdelen
                    zakenTakenExporteren shouldBe werklijstRechten.zakenTakenExporteren
                }
            }
        }
    }

    Given("the policy service returns valid overige rechten") {
        val overigeRechten = createOverigeRechten()
        every { policyService.readOverigeRechten() } returns overigeRechten

        When("readOverigeRechten is called") {
            val restOverigeRechten = policyRestService.readOverigeRechten()

            Then("it should return the converted RestOverigeRechten") {
                with(restOverigeRechten) {
                    startenZaak shouldBe overigeRechten.startenZaak
                    beheren shouldBe overigeRechten.beheren
                    zoeken shouldBe overigeRechten.zoeken
                }
            }
        }
    }
})
