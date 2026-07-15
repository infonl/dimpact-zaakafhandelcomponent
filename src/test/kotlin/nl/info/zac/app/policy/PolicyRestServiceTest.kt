/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import nl.info.client.pabc.ROLE_NAME_BRP_ZOEKEN
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createNotitieRechten
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createWerklijstRechten

class PolicyRestServiceTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyRestService = PolicyRestService(policyService, loggedInUserInstance)

    given("Existing werklijst rechten") {
        val werklijstRechten = createWerklijstRechten()
        every { policyService.readWerklijstRechten() } returns werklijstRechten

        `when`("readWerklijstRechten is called") {
            val restWerklijstRechten = policyRestService.readWerklijstRechten()

            then("it should return the converted RestWerklijstRechten") {
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

    given("Existing overige rechten") {
        val overigeRechten = createOverigeRechten()
        every { policyService.readOverigeRechten() } returns overigeRechten

        `when`("readOverigeRechten is called") {
            val restOverigeRechten = policyRestService.readOverigeRechten()

            then("it should return the converted RestOverigeRechten") {
                with(restOverigeRechten) {
                    startenZaak shouldBe overigeRechten.startenZaak
                    beheren shouldBe overigeRechten.beheren
                    zoeken shouldBe overigeRechten.zoeken
                }
            }
        }
    }

    given("Existing notitie rechten") {
        val notitieRechten = createNotitieRechten()
        every { policyService.readNotitieRechten() } returns notitieRechten

        `when`("readOverigeRechten is called") {
            val restNotitieRechten = policyRestService.readNotitieRechten()

            then("it should return the converted RestOverigeRechten") {
                with(restNotitieRechten) {
                    lezen shouldBe notitieRechten.lezen
                    wijzigen shouldBe notitieRechten.wijzigen
                }
            }
        }
    }

    given("A logged-in user with BRP zoeken overall role") {
        val loggedInUser = createLoggedInUser(
            overallRoles = setOf(ROLE_NAME_BRP_ZOEKEN)
        )
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("readBrpRechten is called") {
            val restBrpRechten = policyRestService.readBrpRechten()

            then("it should return RestBrpRechten with zoeken set to true") {
                restBrpRechten.zoeken shouldBe true
            }
        }
    }

    given("A logged-in user with BRP gemeenten but without BRP zoeken overall role") {
        val loggedInUser = createLoggedInUser(
            overallRoles = emptySet(),
            brpGemeenten = mapOf("0344" to "fakeGemeenteNaam")
        )
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("readBrpRechten is called") {
            val restBrpRechten = policyRestService.readBrpRechten()

            then("it should return RestBrpRechten with zoeken set to true because gemeenten are present") {
                restBrpRechten.zoeken shouldBe true
            }
        }
    }

    given("A logged-in user without BRP zoeken overall role and without BRP gemeenten") {
        val loggedInUser = createLoggedInUser(
            overallRoles = emptySet(),
            brpGemeenten = emptyMap()
        )
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("readBrpRechten is called") {
            val restBrpRechten = policyRestService.readBrpRechten()

            then("it should return RestBrpRechten with zoeken set to false") {
                restBrpRechten.zoeken shouldBe false
            }
        }
    }
})
