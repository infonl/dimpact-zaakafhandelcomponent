/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.mail.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MailAdresTest : BehaviorSpec({

    Given("a valid email address") {
        When("constructing and object") {
            MailAdres("valid@email.address", "fakeName")
            Then("it should succeed") {}
        }
    }

    Given("an invalid email address") {
        When("constructing and object") {
            val exception = shouldThrow<IllegalArgumentException> {
                MailAdres("fake email", null)
            }

            Then("it should error") {
                exception.message shouldBe "Email 'fake email' is not valid"
            }
        }
    }
})
