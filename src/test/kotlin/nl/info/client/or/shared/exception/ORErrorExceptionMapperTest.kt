/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.MultivaluedHashMap

class ORErrorExceptionMapperTest : BehaviorSpec({
    val mapper = ORErrorExceptionMapper()

    Given("A HTTP status code of 400") {
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val result = mapper.handles(400, headers)

            Then("The status code should not be mapped") {
                result shouldBe false
            }
        }
    }
    Given("A HTTP status code of 401") {
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val result = mapper.handles(401, headers)

            Then("The status code should be mapped") {
                result shouldBe true
            }
        }
    }
    Given("A HTTP status code of 499") {
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val result = mapper.handles(499, headers)

            Then("The status code should be mapped") {
                result shouldBe true
            }
        }
    }
    Given("A HTTP status code of 500") {
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val result = mapper.handles(500, headers)

            Then("The status code should not be mapped") {
                result shouldBe false
            }
        }
    }
})
