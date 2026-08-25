/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import nl.info.client.zgw.shared.model.ZgwValidationError
import nl.info.client.zgw.shared.model.createValidationZgwError

class ZgwValidationErrorResponseExceptionMapperTest : BehaviorSpec({
    val zgwValidationErrorResponseExceptionMapper = ZgwValidationErrorResponseExceptionMapper()

    afterEach { checkUnnecessaryStub() }

    given("A HTTP status code of 400") {
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwValidationErrorResponseExceptionMapper.handles(400, headers)

            then("The status code should be mapped") {
                returnValue shouldBe true
            }
        }
    }

    given("A HTTP status code of 402") {
        val headers = MultivaluedHashMap<String, Any>()

        `when`("the status code is handled") {
            val returnValue = zgwValidationErrorResponseExceptionMapper.handles(402, headers)

            then("The status code should not be mapped") {
                returnValue shouldBe false
            }
        }
    }

    given("A response with a ZGW validation error entity") {
        val zgwValidationError = createValidationZgwError()
        val response = mockk<Response>()
        every { response.readEntity(ZgwValidationError::class.java) } returns zgwValidationError

        `when`("toThrowable is called") {
            val exception = zgwValidationErrorResponseExceptionMapper.toThrowable(response)

            then("it should wrap the ZGW validation error in a ZgwValidationErrorException") {
                exception.shouldBeInstanceOf<ZgwValidationErrorException>()
                exception.validatieFout shouldBe zgwValidationError
            }
        }
    }
})
