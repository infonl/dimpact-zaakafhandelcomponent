/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy.input

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import nl.info.zac.authentication.createLoggedInUser

class UserInputTest : BehaviorSpec({

    Given("PABC is disabled") {

        When("a logged-in user is provided") {
            val user = createLoggedInUser()
            val input = UserInput(user)

            Then("rollen contains the user's token roles and zaaktypen contains the user's authorised zaaktypen") {
                input.user.id shouldBeEqual user.id
                input.user.rollen shouldBeEqual user.roles
                input.user.zaaktypen!! shouldBeEqual user.geautoriseerdeZaaktypen!!
            }
        }
    }

    Given("PABC is enabled") {

        When("a zaaktype is provided and the user has application roles for that zaaktype") {
            val zaaktype = "fakeZaaktype"
            val rolesForZaaktype = setOf("fakeRole1", "fakeRole2")
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype to rolesForZaaktype)
            )
            val input = UserInput(user, zaaktype = zaaktype, featureFlagPabcIntegration = true)

            Then("rollen contains the application roles for that zaaktype and zaaktypen contains only that zaaktype") {
                input.user.rollen shouldBeEqual rolesForZaaktype
                input.user.zaaktypen shouldBe setOf(zaaktype)
            }
        }

        When("a zaaktype is provided but the user has no application roles for that zaaktype") {
            val zaaktype = "fakeZaaktype"
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("otherZaaktype" to setOf("fakeRole1"))
            )
            val input = UserInput(user, zaaktype = zaaktype, featureFlagPabcIntegration = true)

            Then("rollen is empty and zaaktypen contains only that zaaktype") {
                input.user.rollen shouldBeEqual emptySet()
                input.user.zaaktypen shouldBe setOf(zaaktype)
            }
        }

        When("a zaaktype is provided and the user has overallRoles but no per-zaaktype roles for that zaaktype") {
            val zaaktype = "fakeZaaktype"
            val overallRoles = setOf("fakeOverallRole1", "fakeOverallRole2")
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = overallRoles
            )
            val input = UserInput(user, zaaktype = zaaktype, featureFlagPabcIntegration = true)

            Then("rollen contains only the overallRoles and zaaktypen contains only that zaaktype") {
                input.user.rollen shouldBeEqual overallRoles
                input.user.zaaktypen shouldBe setOf(zaaktype)
            }
        }

        When("a zaaktype is provided and the user has both overallRoles and per-zaaktype roles for that zaaktype") {
            val zaaktype = "fakeZaaktype"
            val rolesForZaaktype = setOf("fakeRole1", "fakeRole2")
            val overallRoles = setOf("fakeOverallRole1", "fakeOverallRole2")
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(zaaktype to rolesForZaaktype),
                overallRoles = overallRoles
            )
            val input = UserInput(user, zaaktype = zaaktype, featureFlagPabcIntegration = true)

            Then("rollen contains both per-zaaktype roles and overallRoles and zaaktypen contains only that zaaktype") {
                input.user.rollen shouldContainExactlyInAnyOrder (rolesForZaaktype + overallRoles).toList()
                input.user.zaaktypen shouldBe setOf(zaaktype)
            }
        }

        When("no zaaktype is provided and the user has both applicationRolesPerZaaktype and overallRoles") {
            val rolesForZaaktype1 = setOf("fakeRole1", "fakeRole2")
            val rolesForZaaktype2 = setOf("fakeRole2", "fakeRole3")
            val overallRoles = setOf("fakeOverallRole1", "fakeOverallRole2")
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    "fakeZaaktype1" to rolesForZaaktype1,
                    "fakeZaaktype2" to rolesForZaaktype2
                ),
                overallRoles = overallRoles
            )
            val input = UserInput(user, zaaktype = null, featureFlagPabcIntegration = true)

            Then("rollen contains all per-zaaktype roles deduplicated plus overallRoles and zaaktypen is null") {
                input.user.rollen shouldContainExactlyInAnyOrder
                    (rolesForZaaktype1 + rolesForZaaktype2 + overallRoles).toList()
                input.user.zaaktypen shouldBe null
            }
        }

        When("no zaaktype is provided and the user has only overallRoles") {
            val overallRoles = setOf("fakeOverallRole1", "fakeOverallRole2")
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = overallRoles
            )
            val input = UserInput(user, zaaktype = null, featureFlagPabcIntegration = true)

            Then("rollen contains only the overallRoles and zaaktypen is null") {
                input.user.rollen shouldBeEqual overallRoles
                input.user.zaaktypen shouldBe null
            }
        }
    }
})
