/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response
import nl.info.client.zgw.shared.model.ZgwError

class ZgwErrorExceptionMapperTest : BehaviorSpec({
    val zgwErrorExceptionMapper = ZgwErrorExceptionMapper()

    afterEach { checkUnnecessaryStub() }

    given("A HTTP status code of 400") {
        val statusCode = 400
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
    given("A HTTP status code of 402") {
        val statusCode = 402
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            then("The status code should be mapped") {
                returnValue shouldBe true
            }
        }
    }
    given("A HTTP status code of 404") {
        val statusCode = 404
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
    given("A HTTP status code of 500") {
        val statusCode = 500
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwErrorExceptionMapper.handles(statusCode, headers)

            then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }
    given("A response with a ZGW error entity") {
        val zgwError = ZgwError(
            type = null,
            code = "fakeCode",
            title = "fakeTitle",
            status = 402,
            detail = "fakeDetail",
            instance = null
        )
        val response = mockk<Response>()
        every { response.readEntity(ZgwError::class.java) } returns zgwError

        `when`("toThrowable is called") {
            val exception = zgwErrorExceptionMapper.toThrowable(response)

            then("it should wrap the ZGW error in a ZgwErrorException") {
                exception.shouldBeInstanceOf<ZgwErrorException>()
                exception.zgwError shouldBe zgwError
            }
        }
    }
})
