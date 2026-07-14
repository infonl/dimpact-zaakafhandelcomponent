/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.mail.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MailAdresTest : BehaviorSpec({

    given("a valid email address") {
        `when`("constructing and object") {
            MailAdres("valid@example.com", "fakeName")
            then("it should succeed") {}
        }
    }

    given("an invalid email address") {
        `when`("constructing and object") {
            val exception = shouldThrow<IllegalArgumentException> {
                MailAdres("fake email", null)
            }

            then("it should error") {
                exception.message shouldBe "Email 'fake email' is not valid"
            }
        }
    }
})
