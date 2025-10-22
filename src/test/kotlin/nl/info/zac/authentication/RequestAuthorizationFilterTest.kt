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
import nl.info.zac.identity.model.ZACRole

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

    Given("POST /rest/notificaties is public") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/rest/notificaties"
        every { httpServletRequest.method } returns "POST"
        every { filterChain.doFilter(any(), any()) } just runs

        When("doFilter is called") {
            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("request is allowed") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
                verify(exactly = 0) {
                    httpServletResponse.sendError(any())
                }
            }
        }
    }

    Given("GET /rest/notificaties is forbidden") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/rest/notificaties"
        every { httpServletRequest.method } returns "GET"
        every { httpServletResponse.sendError(any()) } just runs

        When("doFilter is called") {
            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("403 is returned") {
                verify(exactly = 1) {
                    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
                verify(exactly = 0) {
                    filterChain.doFilter(any(), any())
                }
            }
        }
    }

    Given("GET /rest/internal/* is public; other methods forbidden") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.requestURI } returns "/rest/internal/something"
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { filterChain.doFilter(any(), any()) } just runs

        When("GET is used") {
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("POST is used") {
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify { httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN) }
            }
        }
    }

    Given("GET /websocket is public; other methods forbidden") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/websocket"

        When("GET is used") {
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("POST is used") {
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify {
                    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
            }
        }
    }

    Given("/webdav/* is public (all methods)") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/webdav/path"
        every { httpServletRequest.method } returns "PUT"

        When("doFilter is called") {
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }
    }

    Given("SmartDocuments callbacks are POST-only public") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"

        When("cmmn-callback POST is used") {
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/cmmn-callback/xyz"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("cmmn-callback GET is used") {
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/cmmn-callback/xyz"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify { httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN) }
            }
        }

        When("bpmn-callback POST is used") {
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/bpmn-callback/abcd"
            every { httpServletRequest.method } returns "POST"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("bpmn-callback GET is used") {
            every { httpServletRequest.requestURI } returns "/rest/document-creation/smartdocuments/bpmn-callback/abcd"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify { httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN) }
            }
        }
    }

    Given("GET /static/smart-documents-result.html is public; other methods forbidden") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/static/smart-documents-result.html"

        When("GET is used") {
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("POST is used") {
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify {
                    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
            }
        }
    }

    Given("GET /assets/* is public; other methods forbidden") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/assets/app.css"

        When("GET is used") {
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }

        When("POST is used") {
            every { httpServletRequest.method } returns "POST"
            every { httpServletResponse.sendError(any()) } just runs

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("forbidden") {
                verify {
                    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
            }
        }
    }

    Context("PABC ON (application roles)") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)

        Given("Non-admin path allowed when user has any PABC app-role on any zaaktype") {
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype1" to setOf("raadpleger"))
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                    verify(exactly = 0) {
                        httpServletResponse.sendError(any())
                    }
                }
            }
        }

        Given("Non-admin path forbidden when user has no PABC app-roles at all") {
            val user = createLoggedInUser(applicationRolesPerZaaktype = emptyMap())
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("403") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        Given("Admin path allowed when user has beheerder app-role on at least one zaaktype") {
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("fakeZaaktype2" to setOf(ZACRole.BEHEERDER.value))
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/admin/util/health"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("Admin path forbidden when user lacks beheerder app-role") {
            val user = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf("zt-3" to setOf(ZACRole.RAADPLEGER.value))
            )
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/admin/settings"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("403") {
                    verify(exactly = 1) { httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN) }
                    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
                }
            }
        }
    }

    Context("PABC OFF (legacy roles from token)") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = false)

        Given("Non-admin path allowed when user has any legacy ZAC role") {
            val user = createLoggedInUser(roles = setOf(ZACRole.RAADPLEGER.value))
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("Non-admin path forbidden when user has no legacy roles") {
            val user = createLoggedInUser(roles = emptySet())
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/app/home"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("return 403") {
                    verify(exactly = 1) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
                    verify(exactly = 0) {
                        filterChain.doFilter(any(), any())
                    }
                }
            }
        }

        Given("Admin path allowed when user has legacy beheerder") {
            val user = createLoggedInUser(roles = setOf(ZACRole.BEHEERDER.value))
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/rest/admin/cmmn/path"
            every { httpServletRequest.method } returns "GET"
            every { filterChain.doFilter(any(), any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("allowed") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, httpServletResponse)
                    }
                }
            }
        }

        Given("Admin path forbidden without legacy beheerder") {
            val user = createLoggedInUser(roles = setOf(ZACRole.RAADPLEGER.value))
            setSessionUser(user)
            every { httpServletRequest.contextPath } returns "fakeContextPath"
            every { httpServletRequest.requestURI } returns "/admin"
            every { httpServletRequest.method } returns "GET"
            every { httpServletResponse.sendError(any()) } just runs

            When("doFilter is called") {
                filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

                Then("return 403") {
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

    Given("No session - request allowed (bypassed)") {
        val filter = RequestAuthorizationFilter(pabcIntegrationEnabled = true)
        setSessionUser(null)
        every { httpServletRequest.contextPath } returns "fakeContextPath"
        every { httpServletRequest.requestURI } returns "/app/home"
        every { httpServletRequest.method } returns "GET"
        every { filterChain.doFilter(any(), any()) } just runs

        When("doFilter is called") {
            filter.doFilter(httpServletRequest, httpServletResponse, filterChain)

            Then("allowed") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, httpServletResponse)
                }
            }
        }
    }
})
