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
    val identityRestService = IdentityRestService(
        identityService = identityService,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Listing all groups") {
        Given("a mix of active and inactive groups") {
            val activeGroup = createGroup(id = "fakeActiveGroupId", name = "fakeActiveGroupName")
            val inactiveGroup = createGroup(id = "fakeInactiveGroupId", name = "fakeInactiveGroupName", active = false)
            every { identityService.listGroups() } returns listOf(activeGroup, inactiveGroup)

            When("listGroups is called") {
                val result = identityRestService.listGroups()

                Then("only active groups are returned") {
                    result shouldHaveSize 1
                    result[0].id shouldBe "fakeActiveGroupId"
                }
            }
        }

        Given("only active groups") {
            val activeGroup1 = createGroup(id = "fakeActiveGroupId1", name = "fakeActiveGroupName1")
            val activeGroup2 = createGroup(id = "fakeActiveGroupId2", name = "fakeActiveGroupName2")
            every { identityService.listGroups() } returns listOf(activeGroup1, activeGroup2)

            When("listGroups is called") {
                val result = identityRestService.listGroups()

                Then("all groups are returned") {
                    result shouldHaveSize 2
                }
            }
        }
    }

    Context("Listing behandelaar groups for a zaaktype UUID") {
        Given("a mix of active and inactive groups") {
            val zaaktypeUuid = UUID.randomUUID()
            val activeGroup = createGroup(id = "fakeActiveGroupId", name = "fakeActiveGroupName")
            val inactiveGroup = createGroup(id = "fakeInactiveGroupId", name = "fakeInactiveGroupName", active = false)
            every {
                identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)
            } returns listOf(activeGroup, inactiveGroup)

            When("listBehandelaarGroupsForZaaktypeUuid is called") {
                val result = identityRestService.listBehandelaarGroupsForZaaktypeUuid(zaaktypeUuid)

                Then("only active groups are returned") {
                    result shouldHaveSize 1
                    result[0].id shouldBe "fakeActiveGroupId"
                }
            }
        }
    }

    Context("Listing behandelaar groups for a zaaktype description") {
        Given("a mix of active and inactive groups") {
            val zaaktypeDescription = "fakeZaaktypeDescription"
            val activeGroup = createGroup(id = "fakeActiveGroupId", name = "fakeActiveGroupName")
            val inactiveGroup = createGroup(id = "fakeInactiveGroupId", name = "fakeInactiveGroupName", active = false)
            every {
                identityService.listGroupsForBehandelaarRoleAndZaaktype(zaaktypeDescription)
            } returns listOf(activeGroup, inactiveGroup)

            When("listBehandelaarGroupsForZaaktype is called") {
                val result = identityRestService.listBehandelaarGroupsForZaaktype(zaaktypeDescription)

                Then("only active groups are returned") {
                    result shouldHaveSize 1
                    result[0].id shouldBe "fakeActiveGroupId"
                }
            }
        }
    }
})
