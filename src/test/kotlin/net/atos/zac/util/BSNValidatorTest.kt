/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import java.lang.IllegalArgumentException

class BSNValidatorTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid BSN") {
        val validBSN = "316245124"

        When("it is validated") {
            validBSN.validateBSN()

            Then("no exception is thrown") {}
        }
    }

    Given("an invalid BSN") {
        val invalidBSN = "123456789"

        When("it is validated") {
            val exception = shouldThrow<IllegalArgumentException> {
                invalidBSN.validateBSN()
            }

            Then("exception is thrown") {
                exception.message shouldBe "Invalid BSN '$invalidBSN'"
            }
        }
    }

    Given("BSN that is shorter than expected") {
        val invalidBSN = "12345678"

        When("it is validated") {
            val exception = shouldThrow<IllegalArgumentException> {
                invalidBSN.validateBSN()
            }

            Then("exception is thrown") {
                exception.message shouldBe "BSN '$invalidBSN' length must be 9"
            }
        }
    }

    Given("BSN that is longer than expected") {
        val invalidBSN = "1234567890"

        When("it is validated") {
            val exception = shouldThrow<IllegalArgumentException> {
                invalidBSN.validateBSN()
            }

            Then("exception is thrown") {
                exception.message shouldBe "BSN '$invalidBSN' length must be 9"
            }
        }
    }

    Given("BSN that contains non-numeric characters") {
        val invalidBSN = "123A56789"

        When("it is validated") {
            val exception = shouldThrow<IllegalArgumentException> {
                invalidBSN.validateBSN()
            }

            Then("exception is thrown") {
                exception.message shouldBe "Character on index 3 in BSN '$invalidBSN' is not a digit"
            }
        }
    }
})
