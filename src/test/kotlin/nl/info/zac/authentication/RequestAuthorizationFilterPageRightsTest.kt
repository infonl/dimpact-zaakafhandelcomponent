/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createWerklijstRechten

class RequestAuthorizationFilterPageRightsTest : BehaviorSpec({
    val httpServletRequest = mockk<HttpServletRequest>()
    val httpServletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val requestDispatcher = mockk<RequestDispatcher>()
    val policyService = mockk<PolicyService>()

    fun setSessionUser(user: LoggedInUser) {
        every { httpServletRequest.getSession(false) } returns httpSession
        every { httpSession.getAttribute("logged-in-user") } returns user
    }

    context("Pages that need a right") {
        val readUser = createLoggedInUser(
            applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("behandelaar")),
            hasReadApplicationRole = true
        )
        val inboxPages = listOf(
            "/documenten",
            "/documenten/inbox",
            "/documenten/ontkoppelde",
            "/productaanvragen/inbox",
            "/informatie-objecten",
            "/informatie-objecten/"
        )

        inboxPages.forEach { path ->
            given("A user with a read role but without the 'inbox' werklijst right requests '$path'") {
                val filter = RequestAuthorizationFilter(policyService)
                setSessionUser(readUser)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.servletPath } returns path
                every { httpServletRequest.pathInfo } returns null
                every { httpServletRequest.method } returns "GET"
                every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = false)
                every { httpServletResponse.sendError(any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("a 403 with the generic no-permission error page is returned") {
                        verify(exactly = 1) {
                            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                        }
                        verify(exactly = 0) {
                            filterChain.doFilter(any(), any())
                        }
                    }
                }
            }

            given("A user with the 'inbox' werklijst right requests '$path'") {
                val filter = RequestAuthorizationFilter(policyService)
                setSessionUser(readUser)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.servletPath } returns path
                every { httpServletRequest.pathInfo } returns null
                every { httpServletRequest.method } returns "GET"
                every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = true)
                every { filterChain.doFilter(any(), any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("the request is allowed") {
                        verify(exactly = 1) {
                            filterChain.doFilter(httpServletRequest, httpServletResponse)
                        }
                    }
                }
            }
        }

        given("A user with a read role but without the 'startenZaak' right requests '/zaken/create'") {
            val filter = RequestAuthorizationFilter(policyService)
            setSessionUser(readUser)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/zaken/create"
            every { httpServletRequest.servletPath } returns "/zaken/create"
            every { httpServletRequest.pathInfo } returns null
            every { httpServletRequest.method } returns "GET"
            every { policyService.readOverigeRechten() } returns createOverigeRechten(startenZaak = false)
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 with the generic no-permission error page is returned") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        given("A user with the 'startenZaak' right requests '/zaken/create'") {
            val filter = RequestAuthorizationFilter(policyService)
            setSessionUser(readUser)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/zaken/create"
            every { httpServletRequest.servletPath } returns "/zaken/create"
            every { httpServletRequest.pathInfo } returns null
            every { httpServletRequest.method } returns "GET"
            every { policyService.readOverigeRechten() } returns createOverigeRechten(startenZaak = true)
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        listOf(
            "/informatie-objecten/fakeUuid",
            "/zaken/werkvoorraad",
            "/zaken/ZAAK-2026-0000000001",
            "/documentenx",
            "/rest/inboxdocumenten"
        ).forEach { path ->
            given("A user with a read role requests '$path', which needs no right besides a read role") {
                val filter = RequestAuthorizationFilter(policyService)
                setSessionUser(readUser)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.servletPath } returns path
                every { httpServletRequest.pathInfo } returns null
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("the request is allowed without reading any right from OPA") {
                        verify(exactly = 1) {
                            filterChain.doFilter(httpServletRequest, httpServletResponse)
                        }
                        verify(exactly = 0) {
                            policyService.readWerklijstRechten()
                            policyService.readOverigeRechten()
                        }
                    }
                }
            }
        }

        mapOf(
            "/%64ocumenten/inbox" to "/documenten/inbox",
            "/documenten/inbox;view=list" to "/documenten/inbox",
            "/zaken/%63reate" to "/zaken/create",
            "/zaken/create;view=form" to "/zaken/create"
        ).forEach { (requestUri, servletPath) ->
            given("A user without the right for '$servletPath' requests it as '$requestUri'") {
                val filter = RequestAuthorizationFilter(policyService)
                setSessionUser(readUser)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns requestUri
                every { httpServletRequest.servletPath } returns servletPath
                every { httpServletRequest.pathInfo } returns null
                every { httpServletRequest.method } returns "GET"
                if (servletPath == "/zaken/create") {
                    every { policyService.readOverigeRechten() } returns createOverigeRechten(startenZaak = false)
                } else {
                    every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = false)
                }
                every { httpServletResponse.sendError(any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("a 403 with the generic no-permission error page is returned, as for '$servletPath'") {
                        verify(exactly = 1) {
                            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                        }
                        verify(exactly = 0) {
                            filterChain.doFilter(any(), any())
                        }
                    }
                }
            }
        }

        given("A user without a read role requests '/documenten/inbox'") {
            val filter = RequestAuthorizationFilter(policyService)
            setSessionUser(
                createLoggedInUser(
                    applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken")),
                    hasReadApplicationRole = false
                )
            )
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/documenten/inbox"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.status = any() } just runs
            every { httpServletResponse.setHeader(any(), any()) } just runs
            every {
                httpServletRequest.getRequestDispatcher("/static/error-403-no-read-role.html")
            } returns requestDispatcher
            every { requestDispatcher.forward(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 with the no-read-role error page is returned without reading any right from OPA") {
                    verify(exactly = 1) {
                        requestDispatcher.forward(httpServletRequest, httpServletResponse)
                    }
                    verify(exactly = 0) {
                        policyService.readWerklijstRechten()
                    }
                }
            }
        }
    }
})
