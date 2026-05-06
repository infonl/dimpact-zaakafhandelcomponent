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
import java.util.UUID

class IdentityRestServiceTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val identityRestService = IdentityRestService(identityService, loggedInUserInstance)

    beforeEach { checkUnnecessaryStub() }

    Context("Listing all groups") {
        Given("The identity service returns both active and inactive groups") {
            val activeGroup = createGroup(id = "active-group", name = "Active Group", active = true)
            val inactiveGroup = createGroup(id = "inactive-group", name = "Inactive Group", active = false)
            every { identityService.listGroups() } returns listOf(activeGroup, inactiveGroup)

            When("the 'list groups' endpoint is called") {
                val result = identityRestService.listGroups()

                Then("only the active group is returned and the inactive group is filtered out") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "active-group"
                }
            }
        }
    }

    Context("Listing behandelaar groups for a zaaktype UUID") {
        val zaaktypeUuid = UUID.randomUUID()

        Given("The identity service returns both active and inactive groups") {
            val activeGroup = createGroup(id = "active-group", name = "Active Group", active = true)
            val inactiveGroup = createGroup(id = "inactive-group", name = "Inactive Group", active = false)
            every {
                identityService.listActiveGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)
            } returns listOf(activeGroup, inactiveGroup)

            When("the deprecated 'list behandelaar groups for a zaaktype UUID' endpoint is called") {
                @Suppress("DEPRECATION")
                val result = identityRestService.listBehandelaarGroupsForZaaktypeUuid(zaaktypeUuid)

                Then("only the active group is returned and the inactive group is filtered out") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "active-group"
                }
            }
        }
    }

    Context("Listing behandelaar groups for a zaaktype description") {
        Given("The identity service returns both active and inactive groups") {
            val activeGroup = createGroup(id = "active-group", name = "Active Group", active = true)
            val inactiveGroup = createGroup(id = "inactive-group", name = "Inactive Group", active = false)
            every {
                identityService.listActiveGroupsForBehandelaarRoleAndZaaktype("fakeZaaktype")
            } returns listOf(activeGroup, inactiveGroup)

            When("the 'list behandelaar groups for a zaaktype' endpoint is called") {
                val result = identityRestService.listBehandelaarGroupsForZaaktype("fakeZaaktype")

                Then("only the active group is returned and the inactive group is filtered out") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "active-group"
                }
            }
        }
    }
})
