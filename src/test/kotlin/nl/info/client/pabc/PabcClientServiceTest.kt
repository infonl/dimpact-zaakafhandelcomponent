/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import nl.info.client.pabc.model.createApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest

class PabcClientServiceTest : BehaviorSpec({

    val pabcClient = mockk<PabcClient>()
    val pabcClientService = PabcClientService(pabcClient)

    val roles = listOf(
        "fakeRole1",
        "fakeRole2",
        "fakeRole3"
    )

    val mockResponse = createApplicationRolesResponse()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("invoke client with list of functional roles") {
        val requestSlot = slot<GetApplicationRolesRequest>()
        every {
            pabcClient.getApplicationRolesPerEntityType(capture(requestSlot))
        } returns mockResponse

        When("getApplicationRoles is called") {
            val result = pabcClientService.getApplicationRoles(roles)

            Then("it should invoke the client with the given roles") {
                result shouldBe mockResponse
                requestSlot.isCaptured shouldBe true
                requestSlot.captured.functionalRoleNames shouldBe listOf(
                    "fakeRole1",
                    "fakeRole2",
                    "fakeRole3"
                )

                val responseModel = result.results[0]
                responseModel.entityType.id shouldBe "zaaktype_test_1"
                responseModel.entityType.name shouldBe "Test zaaktype 1"
                responseModel.entityType.type shouldBe "zaaktype"

                responseModel.applicationRoles.forEach {
                    it.application shouldBe "zaakafhandelcomponent"
                }
            }
        }
    }
})
