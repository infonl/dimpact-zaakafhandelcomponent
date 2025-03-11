/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.MultivaluedHashMap
import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper

class ZgwErrorExceptionMapperTest : BehaviorSpec({
    val zgwErrorExceptionMapper = ZgwErrorExceptionMapper()

    Given("A HTTP status code of 400") {
        val statusCode = 400
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            Then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
    Given("A HTTP status code of 402") {
        val statusCode = 402
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            Then("The status code should be mapped") {
                returnValue shouldBe true
            }
        }
    }
    Given("A HTTP status code of 404") {
        val statusCode = 404
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            Then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
    Given("A HTTP status code of 500") {
        val statusCode = 500
        val headers = MultivaluedHashMap<String, Any>()

        When("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            Then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
})
