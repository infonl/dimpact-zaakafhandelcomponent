/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import nl.info.client.pabc.model.createApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import java.util.UUID

class PabcClientServiceTest : BehaviorSpec({
    val pabcClient = mockk<PabcClient>()
    val pabcClientService = PabcClientService(pabcClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Getting application roles") {
        Given("A PABC application roles response for a certain PABC request") {
            val applicationId = UUID.randomUUID()
            val applicationRolesResponse = createApplicationRolesResponse(
                applicationId = applicationId,
            )
            val getApplicationRolesRequestSlot = slot<GetApplicationRolesRequest>()
            every {
                pabcClient.getApplicationRolesPerEntityType(capture(getApplicationRolesRequestSlot))
            } returns applicationRolesResponse

            When("getApplicationRoles is called for a list of functional roles") {
                val result = pabcClientService.getApplicationRoles(
                    listOf(
                        "fakeRole1",
                        "fakeRole2",
                        "fakeRole3"
                    )
                )

                Then("it should invoke the client with the given roles") {
                    result shouldBe applicationRolesResponse
                    getApplicationRolesRequestSlot.isCaptured shouldBe true
                    getApplicationRolesRequestSlot.captured.functionalRoleNames shouldBe listOf(
                        "fakeRole1",
                        "fakeRole2",
                        "fakeRole3"
                    )

                    val responseModel = result.results[0]
                    responseModel.entityType.id shouldBe "zaaktype_test_1"
                    responseModel.entityType.name shouldBe "Test zaaktype 1"
                    responseModel.entityType.type shouldBe "zaaktype"
                    responseModel.applicationRoles.forEach {
                        it.applicationId shouldBe applicationId
                    }
                }
            }
        }
    }
})
