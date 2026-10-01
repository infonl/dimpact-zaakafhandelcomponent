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
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import nl.info.zac.identity.model.ZacApplicationRole
import nl.info.zac.policy.PolicyService

class RequestAuthorizationFilterTest : BehaviorSpec({
    val httpServletRequest = mockk<HttpServletRequest>()
    val httpServletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>(relaxed = true)
    val policyService = mockk<PolicyService>()
    val leesrollen = setOf("raadpleger", "behandelaar", "coordinator", "recordmanager", "beheerder")

    afterEach {
        checkUnnecessaryStub()
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
                val filter = RequestAuthorizationFilter(policyService)
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
                val filter = RequestAuthorizationFilter(policyService)
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
            val filter = RequestAuthorizationFilter(policyService)
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
        given("An authenticated user with a read role for a zaaktype accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("raadpleger"))
            )
            setSessionUser(user)
            every { policyService.readLeesrollen() } returns leesrollen
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
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

        given("An authenticated user without any PABC role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(applicationRolesPerZaaktype = emptyMap())
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned without asking OPA for the read roles") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        policyService.readLeesrollen()
                    }
                }
            }
        }

        given("An authenticated user with only the brp_zoeken role, for a zaaktype and as overall role, accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken")),
                overallRoles = setOf("brp_zoeken")
            )
            setSessionUser(user)
            every { policyService.readLeesrollen() } returns leesrollen
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned, so the user gets the no-permission error page instead of the dashboard") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        given(
            "An authenticated user with only the brp_zoeken and zaakspecifiek_geautoriseerd roles calls a REST endpoint"
        ) {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    "fakeZaaktype1" to setOf("brp_zoeken", "zaakspecifiek_geautoriseerd")
                )
            )
            setSessionUser(user)
            every { policyService.readLeesrollen() } returns leesrollen
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/zaken/zaak/fakeUuid"
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

        given(
            "An authenticated user with only brp_zoeken for one zaaktype and raadpleger for another accesses '/app/home'"
        ) {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    "fakeZaaktype1" to setOf("brp_zoeken"),
                    "fakeZaaktype2" to setOf("raadpleger")
                )
            )
            setSessionUser(user)
            every { policyService.readLeesrollen() } returns leesrollen
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
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

        leesrollen.forEach { leesrol ->
            given("An authenticated user with only the '$leesrol' role for a zaaktype accesses '/app/home'") {
                val filter = RequestAuthorizationFilter(policyService)
                val user = createLoggedInUser(
                    applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf(leesrol))
                )
                setSessionUser(user)
                every { policyService.readLeesrollen() } returns leesrollen
                every { httpServletRequest.contextPath } returns "fakeContextPath"
                every { httpServletRequest.requestURI } returns "/app/home"
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
        }

        given("An authenticated user with only the systeemrol_behandelaar_alle_zaaktypen role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                overallRoles = setOf(ZacApplicationRole.SYSTEEMROL_BEHANDELAAR_ALLE_ZAAKTYPEN.value)
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("the request is allowed without asking OPA for the read roles") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                    verify(exactly = 0) {
                        policyService.readLeesrollen()
                    }
                }
            }
        }

        given("An authenticated user with only the brp_zoeken role accesses '/rest/admin/*'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("brp_zoeken"))
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            `when`("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                then("a 403 is returned") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        given("An authenticated beheerder accesses '/rest/admin/*'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    "fakeZaaktypeDescription" to setOf(ZacApplicationRole.BEHEERDER.value)
                )
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
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaakTypeDescription" to setOf("fakeApplicationRole"))
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/admin/settings"
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

        given("An authenticated user with only a read role in overallRoles (no applicationRolesPerZaaktype) accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = setOf("behandelaar")
            )
            setSessionUser(user)
            every { policyService.readLeesrollen() } returns leesrollen
            every { httpServletRequest.requestURI } returns "/app/home"
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

        given(
            "An authenticated user with beheerder in overallRoles (no applicationRolesPerZaaktype) accesses '/rest/admin/*'"
        ) {
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = setOf(ZacApplicationRole.BEHEERDER.value)
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
            val filter = RequestAuthorizationFilter(policyService)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = emptyMap(),
                overallRoles = setOf("fakeApplicationRole")
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

        given("An unauthenticated GET request on '/app/home'") {
            val filter = RequestAuthorizationFilter(policyService)
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
