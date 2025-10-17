/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy.input

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import nl.info.zac.authentication.createLoggedInUser

class UserInputTest : BehaviorSpec({

    Given("Permission checks constructs user input ") {

        When("logged in user is provided") {
            val user = createLoggedInUser()
            val input = UserInput(user)

            Then("no exception is thrown") {
                input.user.id shouldBeEqual user.id
                input.user.rollen!! shouldBeEqual user.roles
                input.user.zaaktypen!! shouldBeEqual user.geautoriseerdeZaaktypen!!
            }
        }
    }
})
