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

class RequestAuthorizationFilterTest : BehaviorSpec({
    val httpServletRequest = mockk<HttpServletRequest>()
    val httpServletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>(relaxed = true)

    beforeEach {
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

    Context("Public endpoints and method restrictions") {
        Given("An unauthenticated POST request on '/rest/notificaties'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/notificaties"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                    verify(exactly = 0) {
                        httpServletResponse.sendError(any())
                    }
                }
            }
        }

        Given("An unauthenticated GET request on '/rest/notificaties'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/notificaties"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        Given("An unauthenticated GET request on '/rest/internal/*'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.requestURI } returns "/rest/internal/something"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            When("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        Given("An unauthenticated GET request on '/websocket'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/websocket"

            When("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            When("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        Given("An unauthenticated GET request on '/logout'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/logout"

            When("the method is GET") {
                every { httpServletRequest.method } returns "GET"
                every { filterChain.doFilter(any(), any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }

            When("the method is POST") {
                every { httpServletRequest.method } returns "POST"
                every { httpServletResponse.sendError(any()) } just runs

                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        Given("An unauthenticated PUT request on '/webdav/*'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/webdav/path"
            every { httpServletRequest.method } returns "PUT"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An unauthenticated POST request on SmartDocuments '/cmmn-callback' endpoint") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/cmmn-callback/xyz"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An unauthenticated GET request on SmartDocuments '/cmmn-callback' endpoint") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/cmmn-callback/xyz"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        Given("An unauthenticated POST request on SmartDocuments '/bpmn-callback' endpoint") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/bpmn-callback/abcd"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An unauthenticated GET request on '/static/smart-documents-result.html'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/static/smart-documents-result.html"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An unauthenticated POST request on '/assets/*'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/assets/app.css"
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }

    Context("PABC ON — application-role based access") {
        Given("An authenticated user with any PABC role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("raadpleger"))
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An authenticated user without any PABC role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            val user = createLoggedInUser(applicationRolesPerZaaktype = emptyMap())
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }

        Given("An authenticated beheerder accesses '/rest/admin/*'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("zt2" to setOf(ZacApplicationRole.BEHEERDER.value))
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("A non-beheerder user accesses '/admin/settings'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("zt-3" to setOf(ZacApplicationRole.RAADPLEGER.value))
            )
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/admin/settings"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }

    Context("PABC OFF — legacy token role access") {

        Given("An authenticated user with legacy role accesses '/app/home'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = false)
            val user = createLoggedInUser(roles = setOf(ZacApplicationRole.RAADPLEGER.value))
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("the request is allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("An authenticated user without any legacy roles accesses '/admin'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = false)
            val user = createLoggedInUser(roles = emptySet())
            setSessionUser(user)
            every { httpServletRequest.requestURI } returns "/admin"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }

    Context("Unauthenticated requests to protected endpoints") {

        Given("An unauthenticated GET request on '/app/home'") {
            val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
            setSessionUser(null)
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("the filter processes the request") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("a 403 is returned") {
                    verify {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                }
            }
        }
    }
})
