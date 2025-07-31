/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.identity.model.FunctionalRole

class PabcClientServiceTest : BehaviorSpec({

    val pabcClient = mockk<PabcClient>()
    val service = PabcClientService(pabcClient, true)

    val allRoles = listOf(
        "behandelaar",
        "domein_elk_zaaktype",
        "coordinator",
        "zaakafhandelcomponent_user",
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

    Given("a list of roles and some roles to be filtered") {
        val rolesToFilter = listOf(
            FunctionalRole.DOMEIN_ELK_ZAAKTYPE,
            FunctionalRole.ZAAKAFHANDELCOMPONENT_USER
        )

        val requestSlot = slot<GetApplicationRolesRequest>()
        every {
            pabcClient.getApplicationRolesPerEntityType(capture(requestSlot))
        } returns mockResponse

        When("getApplicationRoles is called with a non-empty filter") {
            val result = service.getApplicationRoles(allRoles, rolesToFilter)

            Then("it should exclude the filtered roles") {
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

    Given("a list of roles and null as rolesToBeFiltered") {
        val requestSlot = slot<GetApplicationRolesRequest>()
        every {
            pabcClient.getApplicationRolesPerEntityType(capture(requestSlot))
        } returns mockResponse

        When("getApplicationRoles is called with null filter") {
            val result = service.getApplicationRoles(allRoles)

            Then("it should not filter out any roles") {
                result shouldBe mockResponse
                requestSlot.isCaptured shouldBe true
                requestSlot.captured.functionalRoleNames shouldBe allRoles
            }
        }
    }

    Given("a list of roles and an empty list as rolesToBeFiltered") {
        val requestSlot = slot<GetApplicationRolesRequest>()
        every {
            pabcClient.getApplicationRolesPerEntityType(capture(requestSlot))
        } returns mockResponse

        When("getApplicationRoles is called with empty filter list") {
            val result = service.getApplicationRoles(allRoles, emptyList())

            Then("it should not filter out any roles") {
                result shouldBe mockResponse
                requestSlot.isCaptured shouldBe true
                requestSlot.captured.functionalRoleNames shouldBe allRoles
            }
        }
    }
})
