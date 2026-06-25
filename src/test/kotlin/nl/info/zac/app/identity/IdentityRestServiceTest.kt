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
import jakarta.validation.Validation
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
                val result = identityRestService.listBehandelaarGroupsForZaaktypes(
                    RestBehandelaarGroupsRequest(zaaktypeDescriptions = zaaktypeDescriptions)
                )

                Then("the common group is returned") {
                    result shouldHaveSize 1
                    result.first().id shouldBe "fakeCommonGroupId"
                }
            }
        }
    }

    Context("Validating RestBehandelaarGroupsRequest") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        Given("A request with a non-empty zaaktype descriptions list") {
            val request = RestBehandelaarGroupsRequest(
                zaaktypeDescriptions = listOf("fakeZaaktypeDescription")
            )

            When("the request is validated") {
                val violations = validator.validate(request)

                Then("there are no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        Given("A request with an empty zaaktype descriptions list") {
            val request = RestBehandelaarGroupsRequest(zaaktypeDescriptions = emptyList())

            When("the request is validated") {
                val violations = validator.validate(request)

                Then("there is a constraint violation on zaaktypeDescriptions") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "zaaktypeDescriptions"
                }
            }
        }
    }
})
