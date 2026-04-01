/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response

class DrcRuntimeResponseExceptionMapperTest : BehaviorSpec({
    val mapper = DrcRuntimeResponseExceptionMapper()

    Given("an HTTP status code") {
        When("status is exactly 500 (Internal Server Error)") {
            Then("handles() should return true") {
                mapper.handles(500, MultivaluedHashMap()) shouldBe true
            }
        }

        When("status is above 500 (e.g. 503)") {
            Then("handles() should return true") {
                mapper.handles(503, MultivaluedHashMap()) shouldBe true
            }
        }

        When("status is 404 (Not Found)") {
            Then("handles() should return false") {
                mapper.handles(404, MultivaluedHashMap()) shouldBe false
            }
        }

        When("status is 200 (OK)") {
            Then("handles() should return false") {
                mapper.handles(200, MultivaluedHashMap()) shouldBe false
            }
        }
    }

    Given("a server error response") {
        val response = mockk<Response>()
        every { response.status } returns 503
        every { response.statusInfo } returns Response.Status.SERVICE_UNAVAILABLE

        When("toThrowable() is called") {
            val exception = mapper.toThrowable(response)

            Then("it should return a DrcRuntimeException") {
                exception.shouldBeInstanceOf<DrcRuntimeException>()
            }

            And("the message should contain the status code") {
                exception.message shouldContain "503"
            }
        }
    }
})
