/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.NotBlank

class ValidationUtilTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    data class FakeValidatedObject(
        @field:NotBlank
        val naam: String
    )

    given("an object that satisfies its Jakarta Validation constraints") {
        val fakeValidatedObject = FakeValidatedObject(naam = "fakeNaam")

        `when`("it is validated") {
            ValidationUtil.validateObject(fakeValidatedObject)

            then("no exception is thrown") {}
        }
    }

    given("an object that violates its Jakarta Validation constraints") {
        val fakeValidatedObject = FakeValidatedObject(naam = "")

        `when`("it is validated") {
            val constraintViolationException = shouldThrow<ConstraintViolationException> {
                ValidationUtil.validateObject(fakeValidatedObject)
            }

            then("a ConstraintViolationException is thrown listing the violation") {
                constraintViolationException.constraintViolations shouldHaveSize 1
            }
        }
    }

    given("repeated validation calls") {
        val fakeValidatedObject = FakeValidatedObject(naam = "fakeNaam")

        `when`("an object is validated many times in a row") {
            repeat(50) { ValidationUtil.validateObject(fakeValidatedObject) }

            then("no exception is thrown, since the underlying ValidatorFactory is reused") {}
        }
    }

    given("an email address") {
        data class TestCase(val email: String, val expectedIsValidEmail: Boolean)

        withData(
            nameFn = { "email '${it.email}' is valid: ${it.expectedIsValidEmail}" },
            listOf(
                TestCase(email = "fake.user@example.com", expectedIsValidEmail = true),
                TestCase(email = "not-an-email-address", expectedIsValidEmail = false)
            )
        ) { (email, expectedIsValidEmail) ->
            ValidationUtil.isValidEmail(email) shouldBe expectedIsValidEmail
        }
    }
})
