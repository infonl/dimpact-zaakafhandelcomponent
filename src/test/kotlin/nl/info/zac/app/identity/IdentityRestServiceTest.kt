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
})
