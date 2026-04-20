/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

/**
 * Minimal HttpServletResponse stub that records headers and exposes an output stream,
 * allowing us to verify the headers set by [StaticResourceCacheFilter]'s response wrapper.
 */
private class StubHttpServletResponse : HttpServletResponse {
    val headers = mutableMapOf<String, String>()
    private var committed = false

    override fun setHeader(name: String, value: String) { headers[name] = value }
    override fun addHeader(name: String, value: String) { headers[name] = value }
    override fun containsHeader(name: String) = headers.containsKey(name)
    override fun getHeader(name: String) = headers[name]
    override fun getHeaders(name: String) = headers[name]?.let { listOf(it) } ?: emptyList()
    override fun getHeaderNames() = headers.keys
    override fun isCommitted() = committed
    override fun getOutputStream() = object : ServletOutputStream() {
        override fun isReady() = true
        override fun setWriteListener(listener: WriteListener?) = Unit
        override fun write(b: Int) { committed = true }
    }
    override fun getWriter() = PrintWriter(StringWriter()).also { committed = true }
    override fun setStatus(sc: Int) = Unit
    override fun setContentType(type: String?) = Unit
    override fun setContentLength(len: Int) = Unit
    override fun setContentLengthLong(len: Long) = Unit
    override fun setCharacterEncoding(charset: String?) = Unit
    override fun setBufferSize(size: Int) = Unit
    override fun setIntHeader(name: String, value: Int) { headers[name] = value.toString() }
    override fun addIntHeader(name: String, value: Int) { headers[name] = value.toString() }
    override fun setDateHeader(name: String, date: Long) { headers[name] = date.toString() }
    override fun addDateHeader(name: String, date: Long) { headers[name] = date.toString() }
    override fun getBufferSize() = 0
    override fun flushBuffer() { committed = true }
    override fun resetBuffer() = Unit
    override fun reset() = Unit
    override fun getCharacterEncoding() = "UTF-8"
    override fun getContentType() = "text/plain"
    override fun getLocale(): Locale = Locale.getDefault()
    override fun setLocale(loc: Locale?) = Unit
    override fun sendError(sc: Int) = Unit
    override fun sendError(sc: Int, msg: String?) = Unit
    override fun sendRedirect(location: String?) = Unit
    override fun encodeURL(url: String?) = url
    override fun encodeRedirectURL(url: String?) = url
    override fun getStatus() = 200
    override fun addCookie(cookie: jakarta.servlet.http.Cookie?) = Unit
}

class StaticResourceCacheFilterTest : BehaviorSpec({
    val filter = StaticResourceCacheFilter()

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

    fun filterChainThatPassesThrough() = mockk<FilterChain> {
        every { doFilter(any(), any()) } just runs
    }

    fun filterChainThatOpensWriter() = mockk<FilterChain> {
        every { doFilter(any(), any()) } answers { (secondArg() as HttpServletResponse).writer }
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

    Given("a hashed JS chunk served without opening a stream (e.g. 304/HEAD)") {
        val response = StubHttpServletResponse()
        When("the filter processes the request without accessing the output stream") {
            filter.doFilter(request("/chunk-A1B2C3D4.js"), response, filterChainThatPassesThrough())
            // Headers are only injected when getOutputStream/getWriter is called. For 304/HEAD
            // responses this is acceptable — the browser already holds the correct immutable
            // Cache-Control from the original 200 response and uses it for subsequent requests.
            Then("no Cache-Control header is set") {
                response.headers.containsKey("Cache-Control") shouldBe false
            }
        }
    }

    Given("a resource served via getWriter") {
        val response = StubHttpServletResponse()
        When("the filter processes the request") {
            filter.doFilter(request("/index.html"), response, filterChainThatOpensWriter())
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
