/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response

class ZacApiKeyAuthFilterTest : BehaviorSpec({
    val apiKey = "validApiKey"
    val zacApiKeyAuthFilter = ZacApiKeyAuthFilter(
        zacInternalEndpointsApiKey = apiKey
    )
    val containerRequestContext = mockk<ContainerRequestContext>()

    afterEach {
        checkUnnecessaryStub()
    }

    given("a valid API key in the request header") {
        val validApiKey = "validApiKey"
        every { containerRequestContext.getHeaderString("X-API-KEY") } returns validApiKey

        `when`("filter is called") {
            zacApiKeyAuthFilter.filter(containerRequestContext)

            then("the request should not be aborted") {
                verify(exactly = 0) { containerRequestContext.abortWith(any()) }
            }
        }
    }

    given("an invalid API key in the request header") {
        val invalidApiKey = "invalidApiKey"
        every { containerRequestContext.getHeaderString("X-API-KEY") } returns invalidApiKey
        every { containerRequestContext.abortWith(any()) } just Runs

        `when`("filter is called") {
            zacApiKeyAuthFilter.filter(containerRequestContext)

            then("the request should be aborted with a 401 Unauthorized response") {
                verify(exactly = 1) {
                    containerRequestContext.abortWith(
                        match { it.status == Response.Status.UNAUTHORIZED.statusCode }
                    )
                }
            }
        }
    }

    given("no API key in the request header") {
        every { containerRequestContext.getHeaderString("X-API-KEY") } returns null
        every { containerRequestContext.abortWith(any()) } just Runs

        `when`("filter is called") {
            zacApiKeyAuthFilter.filter(containerRequestContext)

            then("the request should be aborted with a 401 Unauthorized response") {
                verify(exactly = 1) {
                    containerRequestContext.abortWith(
                        match { it.status == Response.Status.UNAUTHORIZED.statusCode }
                    )
                }
            }
        }
    }
})
