/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.input

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.authentication.createLoggedInUser

class UserInputTest : BehaviorSpec({

    Given("Permission checks constructs user input ") {

        When("no logged in user is provided") {
            val exception = shouldThrow<NullPointerException> {
                UserInput(null)
            }

            Then("it throws exception with correct message") {
                exception.message shouldStartWith "No logged in user found"
            }
        }

        When("logged in user is provided") {
            val user = createLoggedInUser()
            val input = UserInput(user)

            Then("no exception is thrown") {
                input.user.id shouldBeEqual user.id
                input.user.rollen shouldBeEqual user.roles
                input.user.zaaktypen shouldBeEqual user.geautoriseerdeZaaktypen!!
            }
        }
    }
})
