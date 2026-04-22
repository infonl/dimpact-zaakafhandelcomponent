/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.filter.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class StaticCacheFilterTest : BehaviorSpec({
    beforeEach { checkUnnecessaryStub() }

    val filter = StaticCacheFilter()

    fun request(path: String, queryParam: String? = null) = mockk<HttpServletRequest> {
        every { requestURI } returns path
        every { contextPath } returns ""
        every { getParameter("v") } returns queryParam
    }

    fun filterChainThatOpensOutputStream() = mockk<FilterChain> {
        every { doFilter(any(), any()) } answers {
            (secondArg() as HttpServletResponse).outputStream
        }
    }

    Given("a hashed JS chunk") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            filter.doFilter(request("/chunk-A1B2C3D4.js"), response, filterChainThatOpensOutputStream())
            Then("Cache-Control is set to immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    Given("a hashed CSS file") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            filter.doFilter(request("/styles-FF009900.css"), response, filterChainThatOpensOutputStream())
            Then("Cache-Control is set to immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    Given("a versioned asset with valid MD5 v param") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            filter.doFilter(request("/assets/i18n/nl.json", "395afa0f"), response, filterChainThatOpensOutputStream())
            Then("Cache-Control is set to immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    Given("a versioned asset with invalid v param") {
        val response = StubHttpServletResponse()
        val responseSlot = slot<HttpServletResponse>()
        val chain = mockk<FilterChain> { every { doFilter(any(), capture(responseSlot)) } just runs }
        When("the filter processes the request") {
            filter.doFilter(request("/assets/i18n/nl.json", "notvalid!"), response, chain)
            Then("the original response is passed to the chain unwrapped") {
                responseSlot.captured shouldBeSameInstanceAs response
            }
        }
    }

    Given("index.html") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            filter.doFilter(request("/index.html"), response, filterChainThatOpensOutputStream())
            Then("Cache-Control is set to no-cache") {
                response.headers["Cache-Control"] shouldBe "no-cache"
            }
        }
    }

    Given("an unmatched path") {
        val response = StubHttpServletResponse()
        val responseSlot = slot<HttpServletResponse>()
        val chain = mockk<FilterChain> { every { doFilter(any(), capture(responseSlot)) } just runs }
        When("the filter processes the request") {
            filter.doFilter(request("/rest/zaken"), response, chain)
            Then("the original response is passed to the chain unwrapped") {
                responseSlot.captured shouldBeSameInstanceAs response
            }
        }
    }

    Given("a browser sends a conditional request (If-None-Match) for a hashed JS chunk") {
        val requestSlot = slot<ServletRequest>()
        val chain = mockk<FilterChain> { every { doFilter(capture(requestSlot), any()) } just runs }
        When("the filter processes the request") {
            filter.doFilter(
                mockk<HttpServletRequest> {
                    every { requestURI } returns "/chunk-A1B2C3D4.js"
                    every { contextPath } returns ""
                    every { getParameter("v") } returns null
                    every { getHeader("If-None-Match") } returns "some-etag"
                },
                StubHttpServletResponse(),
                chain
            )
            Then("If-None-Match is stripped so Undertow cannot respond with 304") {
                (requestSlot.captured as HttpServletRequest).getHeader("If-None-Match") shouldBe null
            }
        }
    }

    Given("a hashed JS chunk served as 304 (no body, stream never opened)") {
        val response = StubHttpServletResponse()
        When("the filter processes the request without accessing the output stream") {
            filter.doFilter(
                request("/chunk-A1B2C3D4.js"),
                response,
                mockk { every { doFilter(any(), any()) } just runs }
            )
            // 304 responses never call getOutputStream/getWriter, so our header is not injected.
            // This is acceptable: the browser already holds the correct immutable Cache-Control
            // from the original 200 response and serves subsequent requests from memory.
            Then("no Cache-Control header is set") {
                response.headers.containsKey("Cache-Control") shouldBe false
            }
        }
    }

    Given("a resource served via getWriter") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            val chain =
                mockk<FilterChain> { every { doFilter(any(), any()) } answers { (secondArg() as HttpServletResponse).writer } }
            filter.doFilter(request("/index.html"), response, chain)
            Then("Cache-Control is set to no-cache") {
                response.headers["Cache-Control"] shouldBe "no-cache"
            }
        }
    }

    Given("Undertow attempts to set cache-related headers on a matched response") {
        val response = StubHttpServletResponse()
        val responseSlot = slot<HttpServletResponse>()
        val chain = mockk<FilterChain> {
            every { doFilter(any(), capture(responseSlot)) } answers {
                responseSlot.captured.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                responseSlot.captured.addHeader("Pragma", "no-cache")
                responseSlot.captured.setIntHeader("Expires", 0)
                responseSlot.captured.addIntHeader("Expires", 0)
                responseSlot.captured.setDateHeader("Expires", 0L)
                responseSlot.captured.addDateHeader("Expires", 0L)
                responseSlot.captured.outputStream
            }
        }
        When("the filter processes the request") {
            filter.doFilter(request("/chunk-A1B2C3D4.js"), response, chain)
            Then("Undertow's Cache-Control is suppressed and replaced with immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
            Then("Undertow's Pragma is suppressed") {
                response.headers.containsKey("Pragma") shouldBe false
            }
            Then("Undertow's Expires is suppressed") {
                response.headers.containsKey("Expires") shouldBe false
            }
        }
    }
})
