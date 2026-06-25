/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.Response
import nl.info.zac.app.identity.model.RestBehandelaarGroupsRequest
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup

class IdentityRestServiceTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val identityRestService = IdentityRestService(identityService, loggedInUserInstance)

    afterEach { checkUnnecessaryStub() }

    Context("Listing all active groups") {
        Given("The identity service returns an active group") {
            val activeGroup = createGroup(id = "active-group", name = "Active Group", active = true)
            every { identityService.listActiveGroups() } returns listOf(activeGroup)

            When("the 'list active groups' endpoint is called") {
                val result = identityRestService.listActiveGroups()

                Then("the active group is returned") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "active-group"
                }
            }
        }
    }

    Context("Listing active behandelaar groups for a zaaktype description") {
        Given("The identity service returns an active group") {
            val activeGroup = createGroup(id = "active-group", name = "Active Group", active = true)
            every {
                identityService.listActiveGroupsForBehandelaarRoleAndZaaktype("fakeZaaktype")
            } returns listOf(activeGroup)

            When("the 'list behandelaar groups for a zaaktype' endpoint is called") {
                val result = identityRestService.listBehandelaarGroupsForZaaktype("fakeZaaktype")

                Then("the active group is returned") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "active-group"
                }
            }
        }
    }

    Context("Listing behandelaar groups for multiple zaaktype descriptions") {
        Given("The identity service returns a common group for the given zaaktype descriptions") {
            val commonGroup = createGroup(id = "fakeCommonGroupId", name = "fakeCommonGroupName")
            val zaaktypeDescriptions = listOf("fakeZaaktypeDescription1", "fakeZaaktypeDescription2")
            every {
                identityService.listActiveGroupsForBehandelaarRoleAndZaaktypes(zaaktypeDescriptions)
            } returns listOf(commonGroup)

            When("the 'list behandelaar groups for multiple zaaktypes' endpoint is called") {
                val response = identityRestService.listBehandelaarGroupsForZaaktypes(
                    RestBehandelaarGroupsRequest(zaaktypeDescriptions = zaaktypeDescriptions)
                )

                Then("HTTP 200 is returned with the common group") {
                    response.status shouldBe Response.Status.OK.statusCode
                    @Suppress("UNCHECKED_CAST")
                    (response.entity as List<*>).shouldHaveSize(1)
                }
            }
        }

        Given("An empty list of zaaktype descriptions") {
            When("the 'list behandelaar groups for multiple zaaktypes' endpoint is called") {
                val response = identityRestService.listBehandelaarGroupsForZaaktypes(
                    RestBehandelaarGroupsRequest(zaaktypeDescriptions = emptyList())
                )

                Then("HTTP 400 is returned") {
                    response.status shouldBe Response.Status.BAD_REQUEST.statusCode
                }
            }
        }
    }
})
