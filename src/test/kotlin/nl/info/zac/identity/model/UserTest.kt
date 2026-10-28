/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UserTest : BehaviorSpec({
    context("Getting the full name of a user") {
        given("a user with a display name") {
            val user = User(
                username = "fakeUsername",
                firstName = "fakeFirstName",
                lastName = "fakeLastName",
                displayName = "fakeDisplayName"
            )

            `when`("the full name is requested") {
                val fullName = user.getFullName()

                then("the display name is returned") {
                    fullName shouldBe "fakeDisplayName"
                }
            }
        }

        given("a user without a display name but with a first and last name") {
            val user = User(username = "fakeUsername", firstName = "fakeFirstName", lastName = "fakeLastName")

            `when`("the full name is requested") {
                val fullName = user.getFullName()

                then("the first and last name are returned") {
                    fullName shouldBe "fakeFirstName fakeLastName"
                }
            }
        }

        given("a user with only a last name") {
            val user = User(username = "fakeUsername", lastName = "fakeLastName")

            `when`("the full name is requested") {
                val fullName = user.getFullName()

                then("the last name is returned") {
                    fullName shouldBe "fakeLastName"
                }
            }
        }

        given("a user without any name") {
            val user = User(username = "fakeUsername", firstName = "fakeFirstName", lastName = " ")

            `when`("the full name is requested") {
                val fullName = user.getFullName()

                then("the username is returned") {
                    fullName shouldBe "fakeUsername"
                }
            }
        }

        given("a user that could not be found in Keycloak") {
            val user = User("fakeUsername")

            `when`("the full name is requested") {
                val fullName = user.getFullName()

                then("the username is returned") {
                    fullName shouldBe "fakeUsername"
                }
            }
        }
    }
})
