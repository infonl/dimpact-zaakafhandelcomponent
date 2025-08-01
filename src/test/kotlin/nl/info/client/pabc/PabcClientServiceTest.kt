/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse

class PabcClientServiceTest : BehaviorSpec({

    val pabcClient = mockk<PabcClient>()

    val roles = listOf(
        "behandelaar",
        "coordinator",
        "beheerder",
        "recordmanager",
        "raadpleger"
    )

    val mockResponse = GetApplicationRolesResponse()

    afterTest {
        clearMocks(pabcClient)
    }

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("invoke client with list of functional roles") {
        val requestSlot = slot<GetApplicationRolesRequest>()
        every {
            pabcClient.getApplicationRolesPerEntityType(capture(requestSlot))
        } returns mockResponse

        val service = PabcClientService(pabcClient, true)

        When("getApplicationRoles is called") {
            val result = service.getApplicationRoles(roles)

            Then("it should invoke the client with the given roles") {
                result shouldBe mockResponse
                requestSlot.isCaptured shouldBe true
                requestSlot.captured.functionalRoleNames shouldBe listOf(
                    "behandelaar",
                    "coordinator",
                    "beheerder",
                    "recordmanager",
                    "raadpleger"
                )
            }
        }
    }

    Given("do not invoke client when feature flag is disabled") {
        val pabcClient = mockk<PabcClient>(relaxed = true)
        val service = PabcClientService(pabcClient, false)

        When("getApplicationRoles is called") {
            val result = service.getApplicationRoles(listOf("behandelaar"))

            Then("pabcClient should not be called") {
                result shouldBe null
                verify(exactly = 0) {
                    pabcClient.getApplicationRolesPerEntityType(any())
                }
            }
        }
    }
})
