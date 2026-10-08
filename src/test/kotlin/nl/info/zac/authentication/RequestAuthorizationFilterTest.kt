/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
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
import nl.info.zac.identity.model.ZacApplicationRole

class RequestAuthorizationFilterTest : BehaviorSpec({
    val httpServletRequest = mockk<HttpServletRequest>()
    val httpServletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>(relaxed = true)
    val requestDispatcher = mockk<RequestDispatcher>()

    afterEach {
        checkUnnecessaryStub()
    }

    fun mockNoReadApplicationRoleErrorPage() {
        every { httpServletResponse.status = any() } just runs
        every { httpServletResponse.setHeader(any(), any()) } just runs
        every {
            httpServletRequest.getRequestDispatcher("/static/error-403-no-read-role.html")
        } returns requestDispatcher
        every { requestDispatcher.forward(any(), any()) } just runs
    }

    fun verifyNoReadApplicationRoleErrorPageIsShown() {
        verify(exactly = 1) {
            httpServletResponse.status = HttpServletResponse.SC_FORBIDDEN
            httpServletResponse.setHeader("Cache-Control", "no-store")
            requestDispatcher.forward(httpServletRequest, httpServletResponse)
        }
        verify(exactly = 0) {
            httpServletResponse.sendError(any())
            filterChain.doFilter(any(), any())
        }
    }

    fun setSessionUser(user: LoggedInUser?) {
        if (user == null) {
            every { httpServletRequest.getSession(false) } returns null
        } else {
            every { httpServletRequest.getSession(false) } returns httpSession
            every { httpSession.getAttribute("logged-in-user") } returns user
        }
    }

    context("Public endpoints and method restrictions") {
        given("An unauthenticated POST request on '/rest/notificaties'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/notificaties"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                    verify(exactly = 0) {
                        httpServletResponse.sendError(any())
                    }
                }
            }
        }

        given("An unauthenticated GET request on '/rest/notificaties'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/notificaties"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        given("An unauthenticated GET request on '/rest/internal/*'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.requestURI } returns "/rest/internal/something"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            `when`("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given("An unauthenticated GET request on '/websocket'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/websocket"

            `when`("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            `when`("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given("An unauthenticated GET request on '/sign-out'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/sign-out"

            `when`("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            `when`("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given("An unauthenticated PUT request on '/webdav/*'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/webdav/path"
            every { httpServletRequest.method } returns "PUT"
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

        given("An unauthenticated POST request on SmartDocuments '/callback' endpoint") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/callback/xyz"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        given("An unauthenticated GET request on SmartDocuments '/callback' endpoint") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/callback/xyz"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given("An unauthenticated GET request on '/static/smart-documents-result.html'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/static/smart-documents-result.html"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed") {
                    verify {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        listOf("/favicon.ico", "/favicon.svg", "/apple-touch-icon.png", "/site.webmanifest").forEach { path ->
            given("An unauthenticated GET request on '$path'") {
                val filter = RequestAuthorizationFilter()
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("the request is allowed") {
                        verify(exactly = 1) {
                            filterChain.doFilter(httpServletRequest, httpServletResponse)
                        }
                        verify(exactly = 0) {
                            httpServletResponse.sendError(any())
                        }
                    }
                }
            }

            given("An unauthenticated POST request on '$path'") {
                val filter = RequestAuthorizationFilter()
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then("a 403 is returned") {
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

        given("An unauthenticated POST request on '/assets/*'") {
            val filter = RequestAuthorizationFilter()
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/assets/app.css"
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }

    context("Application-role based access") {
        listOf("/app/home", "/rest/zaken/zaak/fakeUuid").forEach { path ->
            given("An authenticated user with a read application role requests '$path'") {
                val filter = RequestAuthorizationFilter()
                val user = createLoggedInUser(
                    applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("raadpleger")),
                    hasReadApplicationRole = true
                )
                setSessionUser(user)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.method } returns "GET"
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

            given("An authenticated user with only application roles without read rights requests '$path'") {
                val filter = RequestAuthorizationFilter()
                val user = createLoggedInUser(
                    applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken")),
                    overallRoles = setOf("brp_zoeken"),
                    hasReadApplicationRole = false
                )
                setSessionUser(user)
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns path
                every { httpServletRequest.method } returns "GET"
                mockNoReadApplicationRoleErrorPage()

                `when`("the filter processes the request") {
                    filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                    then(
                        "a 403 with the no-read-role error page is returned, instead of the dashboard " +
                            "or the generic no-permission error page"
                    ) {
                        verifyNoReadApplicationRoleErrorPageIsShown()
                    }
                }
            }
        }

        given("An authenticated user without any PABC role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(applicationRolesPerZaaktype = emptyMap())
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            mockNoReadApplicationRoleErrorPage()

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 with the no-read-role error page is returned") {
                    verifyNoReadApplicationRoleErrorPageIsShown()
                }
            }
        }

        given("An authenticated user with only application roles without read rights accesses '/admin/settings'") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken")),
                hasReadApplicationRole = false
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/admin/settings"
            every { httpServletRequest.method } returns "GET"
            mockNoReadApplicationRoleErrorPage()

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then(
                    "a 403 with the no-read-role error page is returned, and not the generic no-permission " +
                        "error page, whose home button would only lead to another 403"
                ) {
                    verifyNoReadApplicationRoleErrorPageIsShown()
                }
            }
        }

        given("An authenticated user without a read application role requests the server error texts") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken")),
                hasReadApplicationRole = false
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/referentietabellen/server-error-text"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed, so the no-read-role error page can show these texts") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        given("An authenticated user without a read application role changes the server error texts") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(hasReadApplicationRole = false)
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/referentietabellen/server-error-text"
            every { httpServletRequest.method } returns "PUT"
            mockNoReadApplicationRoleErrorPage()

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 with the no-read-role error page is returned, because only reading them is allowed") {
                    verifyNoReadApplicationRoleErrorPageIsShown()
                }
            }
        }

        given("An authenticated beheerder accesses '/rest/admin/*'") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    "fakeZaaktypeDescription" to setOf(ZacApplicationRole.BEHEERDER.value)
                ),
                hasReadApplicationRole = true
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
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

        given("A non-beheerder user accesses '/admin/settings'") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaakTypeDescription" to setOf("raadpleger")),
                hasReadApplicationRole = true
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/admin/settings"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 with the generic no-permission error page is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given(
            "An authenticated user with beheerder in overallRoles (no applicationRolesPerZaaktype) accesses '/rest/admin/*'"
        ) {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = setOf(ZacApplicationRole.BEHEERDER.value),
                hasReadApplicationRole = true
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
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

        given("An authenticated user with only a non-beheerder role in overallRoles accesses '/rest/admin/*'") {
            val filter = RequestAuthorizationFilter()
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = setOf("raadpleger"),
                hasReadApplicationRole = true
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }

    context("Unauthenticated requests to protected endpoints") {
        given("An unauthenticated GET request on the server error texts") {
            val filter = RequestAuthorizationFilter()
            setSessionUser(null)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/referentietabellen/server-error-text"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }


        given("An unauthenticated GET request on '/app/home'") {
            val filter = RequestAuthorizationFilter()
            setSessionUser(null)
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }
})
