/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.filter.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.time.Duration.Companion.days

class StaticCacheFilterTest : BehaviorSpec({
    val filter = StaticCacheFilter()
    val expectedMaxAge = 365.days.inWholeSeconds

    afterEach { checkUnnecessaryStub() }

    // Relaxed so any unstubbed getParameter() call auto-returns null (no explicit stub = no unnecessary-stub warning).
    // getParameter("v") is only stubbed on tests that exercise the /assets/ path.
    fun request(path: String, contextPath: String = "", vParam: String? = null): HttpServletRequest =
        mockk(relaxed = true) {
            every { requestURI } returns "$contextPath$path"
            every { this@mockk.contextPath } returns contextPath
            if (vParam != null) every { getParameter("v") } returns vParam
        }

    given("hashed JS bundle path") {
        val req = request("/main-A1B2C3D4.js")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper; getOutputStream triggers immutable Cache-Control") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "public, max-age=$expectedMaxAge, immutable") }
            }
        }
    }

    given("hashed CSS bundle path") {
        val req = request("/styles-B2C3D4E5.css")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper with immutable cache-control") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "public, max-age=$expectedMaxAge, immutable") }
            }
        }
    }

    given("hashed JS source map path") {
        val req = request("/chunk-C3D4E5F6.js.map")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper with immutable cache-control") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "public, max-age=$expectedMaxAge, immutable") }
            }
        }
    }

    given("versioned asset path with valid 8-char hex v param") {
        val req = request("/assets/logo.svg", vParam = "395afa0f")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper with immutable cache-control") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "public, max-age=$expectedMaxAge, immutable") }
            }
        }
    }

    given("versioned asset path with invalid v param (too short)") {
        val req = request("/assets/logo.svg", vParam = "abc123")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives original response without wrapping") {
                verify { chain.doFilter(req, res) }
            }
        }
    }

    given("versioned asset path with non-hex v param") {
        val req = request("/assets/logo.svg", vParam = "ZZZZZZZZ")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives original response without wrapping") {
                verify { chain.doFilter(req, res) }
            }
        }
    }

    given("root path /") {
        val req = request("/")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper with no-cache") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "no-cache") }
            }
        }
    }

    given("index.html path") {
        val req = request("/index.html")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives ResponseWrapper with no-cache") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "no-cache") }
            }
        }
    }

    given("index.html under non-empty context path") {
        val req = request("/index.html", contextPath = "/zac")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("context prefix is stripped correctly; no-cache applied") {
                verify { chain.doFilter(any(), capture(responseSlot)) }
                responseSlot.captured.getOutputStream()
                verify { res.setHeader("Cache-Control", "no-cache") }
            }
        }
    }

    given("unmatched path /api/zaken") {
        val req = request("/api/zaken")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)

            then("chain receives original request and response without wrapping") {
                verify { chain.doFilter(req, res) }
            }
        }
    }

    given("ResponseWrapper suppresses Undertow cache headers set before getOutputStream") {
        val req = request("/main-A1B2C3D4.js")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val responseSlot = slot<HttpServletResponse>()

        `when`("downstream code sets Cache-Control/Pragma/Expires on wrapper then calls getOutputStream") {
            filter.doFilter(req, res, chain)
            verify { chain.doFilter(any(), capture(responseSlot)) }
            val wrapped = responseSlot.captured
            wrapped.setHeader("Cache-Control", "no-cache, no-store")
            wrapped.setHeader("Pragma", "no-cache")
            wrapped.setHeader("Expires", "0")
            wrapped.getOutputStream()

            then("Undertow values blocked; only our immutable value applied") {
                verify(exactly = 0) { res.setHeader("Cache-Control", "no-cache, no-store") }
                verify { res.setHeader("Cache-Control", "public, max-age=$expectedMaxAge, immutable") }
            }
        }
    }

    given("immutable resource path") {
        val req = request("/main-A1B2C3D4.js")
        val res = mockk<HttpServletResponse>(relaxed = true)
        val chain = mockk<FilterChain>(relaxed = true)
        val requestSlot = slot<HttpServletRequest>()

        `when`("doFilter is called") {
            filter.doFilter(req, res, chain)
            verify { chain.doFilter(capture(requestSlot), any()) }

            then("RequestWrapper strips If-None-Match") {
                requestSlot.captured.getHeader("If-None-Match").shouldBe(null)
            }

            then("RequestWrapper strips If-Modified-Since") {
                requestSlot.captured.getDateHeader("If-Modified-Since").shouldBe(-1L)
            }
        }
    }
})
