/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.or.shared.model.ORFieldValidationError
import nl.info.client.or.shared.model.ORValidationError
import java.net.URI

class ORValidationErrorExceptionTest : BehaviorSpec({
    Given("An ORValidationError with multiple field errors") {
        val fieldError1 = ORFieldValidationError().apply {
            name = "naam"
            code = "required"
            reason = "This field is required"
        }
        val fieldError2 = ORFieldValidationError().apply {
            name = "omschrijving"
            code = "max_length"
            reason = "Value too long"
        }
        val validationError = ORValidationError().apply {
            title = "Bad Request"
            status = 400
            code = "invalid"
            detail = "Validation failed"
            instance = URI.create("https://example.com/api/resource")
            fieldValidationErrors = listOf(fieldError1, fieldError2)
        }

        When("the exception message is retrieved") {
            val exception = ORValidationErrorException(validationError)

            Then("The message contains all field errors joined by comma") {
                exception.message shouldBe "Bad Request [400 invalid] Validation failed: " +
                    "naam [required] This field is required, omschrijving [max_length] Value too long " +
                    "(https://example.com/api/resource)"
            }
        }
    }
    Given("An ORValidationError with no field errors") {
        val validationError = ORValidationError().apply {
            title = "Bad Request"
            status = 400
            code = "invalid"
            detail = "Validation failed"
            instance = null
            fieldValidationErrors = null
        }

        When("the exception message is retrieved") {
            val exception = ORValidationErrorException(validationError)

            Then("The message renders null for the field errors and instance") {
                exception.message shouldBe "Bad Request [400 invalid] Validation failed: null (null)"
            }
        }
    }
})
