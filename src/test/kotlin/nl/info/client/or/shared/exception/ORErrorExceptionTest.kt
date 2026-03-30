/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.or.shared.model.ORError
import java.net.URI

class ORErrorExceptionTest : BehaviorSpec({
    Given("An ORError with all fields set") {
        val orError = ORError().apply {
            title = "Not Found"
            status = 404
            code = "not_found"
            detail = "The requested resource was not found"
            instance = URI.create("https://example.com/api/resource/1")
        }

        When("the exception message is retrieved") {
            val exception = ORErrorException(orError)

            Then("The message contains all error fields") {
                exception.message shouldBe
                    "Not Found [404 not_found] The requested resource was not found (https://example.com/api/resource/1)"
            }
        }
    }
    Given("An ORError with a null instance") {
        val orError = ORError().apply {
            title = "Forbidden"
            status = 403
            code = "forbidden"
            detail = "Access denied"
            instance = null
        }

        When("the exception message is retrieved") {
            val exception = ORErrorException(orError)

            Then("The message contains null for the instance") {
                exception.message shouldBe "Forbidden [403 forbidden] Access denied (null)"
            }
        }
    }
})
