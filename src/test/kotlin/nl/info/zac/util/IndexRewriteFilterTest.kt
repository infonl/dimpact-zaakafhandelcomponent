/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class IndexRewriteFilterTest : BehaviorSpec({
    val indexRewriteFilter = IndexRewriteFilter()

    afterEach {
        checkUnnecessaryStub()
    }

    listOf("/rest/zaken", "/assets/logo.svg", "/static/file", "/websocket", "/webdav/document", "/main.js", "/main.js.map", "/styles.css")
        .forEach { path ->
            given("a request for resource path '$path'") {
                val httpServletRequest = mockk<HttpServletRequest>()
                val httpServletResponse = mockk<HttpServletResponse>()
                val filterChain = mockk<FilterChain>()
                every { httpServletRequest.servletPath } returns path
                every { filterChain.doFilter(httpServletRequest, httpServletResponse) } just Runs

                `when`("the filter is applied") {
                    indexRewriteFilter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("the request is passed on to the filter chain") {
                        verify(exactly = 1) { filterChain.doFilter(httpServletRequest, httpServletResponse) }
                    }
                }
            }
        }

    given("a request for the sign-out path") {
        val httpServletRequest = mockk<HttpServletRequest>()
        val httpServletResponse = mockk<HttpServletResponse>()
        val filterChain = mockk<FilterChain>()
        every { httpServletRequest.servletPath } returns "/sign-out"
        every { httpServletRequest.logout() } just Runs
        every { httpServletResponse.sendRedirect("/") } just Runs

        `when`("the filter is applied") {
            indexRewriteFilter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            then("the user is logged out and redirected to the root path") {
                verify(exactly = 1) {
                    httpServletRequest.logout()
                    httpServletResponse.sendRedirect("/")
                }
                verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            }
        }
    }

    given("a request for an application route that is not a resource") {
        val httpServletRequest = mockk<HttpServletRequest>()
        val httpServletResponse = mockk<HttpServletResponse>()
        val filterChain = mockk<FilterChain>()
        val requestDispatcher = mockk<RequestDispatcher>()
        every { httpServletRequest.servletPath } returns "/zaken/ZAAK-2026-0000000001"
        every { httpServletRequest.getRequestDispatcher("/index.html") } returns requestDispatcher
        every { requestDispatcher.forward(httpServletRequest, httpServletResponse) } just Runs

        `when`("the filter is applied") {
            indexRewriteFilter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            then("the request is forwarded to index.html") {
                verify(exactly = 1) { requestDispatcher.forward(httpServletRequest, httpServletResponse) }
                verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            }
        }
    }

    given("a request that is not an HTTP request") {
        val servletRequest = mockk<ServletRequest>()
        val servletResponse = mockk<ServletResponse>()
        val filterChain = mockk<FilterChain>()

        `when`("the filter is applied") {
            indexRewriteFilter.doFilter(servletRequest, servletResponse, filterChain)

            then("nothing is done with the request") {
                verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            }
        }
    }
})
