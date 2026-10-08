/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response
import java.net.URI

class SmartDocumentsResponseExceptionMapperTest : BehaviorSpec({
    val mapper = SmartDocumentsResponseExceptionMapper()

    given("an HTTP status code") {
        `when`("the status is 500 or higher") {
            val handlesServerErrors = listOf(500, 503).map { mapper.handles(it, MultivaluedHashMap()) }

            then("the mapper handles it") {
                handlesServerErrors shouldBe listOf(true, true)
            }
        }

        `when`("the status is below 500") {
            val handlesOtherStatuses = listOf(200, 400, 404).map { mapper.handles(it, MultivaluedHashMap()) }

            then("the mapper leaves it to the default handling") {
                handlesOtherStatuses shouldBe listOf(false, false, false)
            }
        }
    }

    given("a server error response from SmartDocuments") {
        val response = mockk<Response>()
        every { response.location } returns URI("https://example.com/smartdocuments")
        every { response.status } returns 503
        every { response.statusInfo } returns Response.Status.SERVICE_UNAVAILABLE

        `when`("toThrowable is called") {
            val exception = mapper.toThrowable(response)

            then("it returns a SmartDocumentsRuntimeException that names the location and status") {
                exception.shouldBeInstanceOf<SmartDocumentsRuntimeException>()
                exception.message shouldBe
                    "Server response from SmartDocuments: https://example.com/smartdocuments 503 (Service Unavailable)"
            }
        }
    }
})
