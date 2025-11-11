/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createApplicationRolesResponseModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.identity.model.getFullName
import org.jose4j.jwt.JwtClaims
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext

class UserPrincipalFilterTest : BehaviorSpec({
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val pabcClientService = mockk<PabcClientService>()
    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val newHttpSession = mockk<HttpSession>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("PABC integration is enabled") {
        val userPrincipalFilter = UserPrincipalFilter(
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
            zaaktypeBpmnConfigurationBeheerService = zaaktypeBpmnConfigurationBeheerService,
            pabcClientService = pabcClientService,
            pabcIntegrationEnabled = true
        )

        Given(
            """
            A logged-in user is present in the HTTP session and a servlet request containing a user 
            principal with the same id as the logged-in user in the HTTP session
            """
        ) {
            val userId = "fakeId"
            val loggedInUser = createLoggedInUser(
                id = userId,
                applicationRolesPerZaaktype = mapOf("testZaaktype" to setOf("fakeAppRole1"))
            )
            val oidcSecurityContext = OidcSecurityContext()
            val oidcPrincipal = OidcPrincipal(userId, oidcSecurityContext)
            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession
            every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
            every { filterChain.doFilter(any(), any()) } just runs

            When(" doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("filterChain is invoked; no invalidate; no PABC call triggered by this branch") {
                    verify(exactly = 1) { filterChain.doFilter(httpServletRequest, servletResponse) }
                    verify(exactly = 0) { httpSession.invalidate() }
                }
            }
        }
        Given(
            """A logged-in user is present in the HTTP session and a servlet request containing 
            a user principal with a different id as the logged-in user"""
        ) {
            val roles = listOf("fakeRole1", "fakeRole2")
            val loggedInUser = createLoggedInUser()
            val entityTypeId = "fakeZaaktypeOmschrijving1"
            val pabcRoleNames = listOf("applicationRoleA", "applicationRoleB")
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                    "name": "fakeFullName",
                    "given_name": "fakeGivenName",
                    "family_name": "fakeFamilyName",
                    "preferred_username": "fakeUserName",
                    "roles": [ "${roles.joinToString(separator = "\", \"")}" ]
                    }                    
                    """.trimMargin(),
                    null
                )
            )
            val capturedLoggedInUser = slot<LoggedInUser>()
            val oidcSecurityContext = OidcSecurityContext("fakeTokenString", accessToken, null, null)
            val oidcPrincipal = OidcPrincipal("aDifferentUserId", oidcSecurityContext)
            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession andThen newHttpSession
            every { httpServletRequest.servletContext.contextPath } returns "fakeContextPath"
            every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
            every { httpSession.invalidate() } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every {
                pabcClientService.getApplicationRoles(roles)
            } returns GetApplicationRolesResponse().apply {
                results = listOf(createApplicationRolesResponseModel(entityTypeId, pabcRoleNames))
            }
            every { newHttpSession.setAttribute("logged-in-user", capture(capturedLoggedInUser)) } just runs
            every {
                zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration()
            } returns listOf(createZaaktypeCmmnConfiguration())
            every { zaaktypeBpmnConfigurationBeheerService.listConfigurations() } returns emptyList()

            When("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("the existing HTTP session is invalidated and the user is added to a new HTTP session") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, servletResponse)
                        httpSession.invalidate()
                        newHttpSession.setAttribute("logged-in-user", any())
                        pabcClientService.getApplicationRoles(roles)
                    }
                    with(capturedLoggedInUser.captured) {
                        this.roles shouldContainAll roles
                        this.applicationRolesPerZaaktype[entityTypeId]?.shouldContainAll(pabcRoleNames)
                    }
                }
            }
        }
        Given(
            """
            No logged-in user is present in the HTTP session and an OIDC security context is present with a token that 
            contains user information and PABC authorisation mappings exist for the user's functional role. 
        """
        ) {
            val userName = "fakeUserName"
            val givenName = "fakeGivenName"
            val familyName = "fakeFamilyName"
            val fullName = "fakeFullName"
            val email = "fake@example.com"
            val groups = listOf("fakeGroup1")
            val roles = listOf("coordinator")
            val zaaktypeId = "fakeZaaktypeId1"
            val pabcRoleNames = listOf("testApplicationRole1")
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                    "name": "$fullName",
                    "given_name": "$givenName",
                    "family_name": "$familyName",
                    "preferred_username": "$userName",
                    "roles": [ "${roles.joinToString(separator = "\", \"")}" ],
                    "email": "$email",
                    "group_membership": [ "${groups.joinToString(separator = "\", \"")}" ]
                    }                    
                    """.trimMargin(),
                    null
                )
            )
            val refreshableOidcSecurityContext = RefreshableOidcSecurityContext(
                null,
                null,
                "fakeTokenString",
                accessToken,
                "fakeIdTokenString",
                null,
                "test-refresh-token"
            )
            val oidcPrincipal = OidcPrincipal("fakeUserId", refreshableOidcSecurityContext)
            val loggedInUserSlot = slot<LoggedInUser>()
            every { httpServletRequest.getSession(true) } returns httpSession
            every { httpSession.getAttribute("logged-in-user") } returns null
            every { httpSession.setAttribute(any(), any()) } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every {
                pabcClientService.getApplicationRoles(any())
            } returns GetApplicationRolesResponse().apply {
                results = listOf(createApplicationRolesResponseModel(zaaktypeId, pabcRoleNames))
            }
            every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns emptyList()
            every { zaaktypeBpmnConfigurationBeheerService.listConfigurations() } returns listOf(
                createZaaktypeBpmnConfiguration(zaaktypeOmschrijving = "bpmn1")
            )
            every { httpServletRequest.userPrincipal } returns oidcPrincipal

            When("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("the user is created from OIDC and stored on the session - set refresh_token") {
                    verify(exactly = 1) {
                        httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                        httpSession.setAttribute(REFRESH_TOKEN_ATTRIBUTE, "test-refresh-token")
                        filterChain.doFilter(httpServletRequest, servletResponse)
                    }
                    with(loggedInUserSlot.captured) {
                        this.id shouldBe userName
                        this.firstName shouldBe givenName
                        this.lastName shouldBe familyName
                        this.getFullName() shouldBe fullName
                        this.email shouldBe email
                        this.roles shouldContainAll roles
                        this.groupIds shouldContainAll groups
                        this.applicationRolesPerZaaktype[zaaktypeId]?.shouldContainAll(pabcRoleNames)
                    }
                }
            }
        }

        Given(
            """
                User details in the OIDC token in the security context and
                PABC authorisation mappings for an application role without an entity type exists for the functional role of this user
                """
        ) {
            val loggedInUserSlot = slot<LoggedInUser>()
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                    "preferred_username": "fakeUserName",
                    "roles": [ "fakeFunctionalRole" ]
                    }                    
                    """.trimMargin(),
                    null
                )
            )
            val oidcSecurityContext = OidcSecurityContext("fakeTokenString", accessToken, null, null)
            val oidcPrincipal = OidcPrincipal("fakeUserId", oidcSecurityContext)
            every { httpSession.getAttribute("logged-in-user") } returns null
            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession
            every { httpSession.setAttribute(any(), any()) } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every { pabcClientService.getApplicationRoles(any()) } returns GetApplicationRolesResponse().apply {
                results = listOf(createApplicationRolesResponseModel(null, listOf("fakeApplicationRole")))
            }
            every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns listOf(
                createZaaktypeCmmnConfiguration(zaaktypeOmschrijving = "fakeZaaktype1"),
                createZaaktypeCmmnConfiguration(zaaktypeOmschrijving = "fakeZaaktype2")
            )
            every { zaaktypeBpmnConfigurationBeheerService.listConfigurations() } returns emptyList()

            When("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("the logged-in user is added to the HTTP session") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                }

                And(
                    "the application roles per zaaktype for the logged-in user contain application roles for all available zaaktypes"
                ) {
                    with(loggedInUserSlot.captured) {
                        this.applicationRolesPerZaaktype["fakeZaaktype1"]?.shouldContainAll(
                            setOf("fakeApplicationRole")
                        )
                        this.applicationRolesPerZaaktype["fakeZaaktype2"]?.shouldContainAll(
                            setOf("fakeApplicationRole")
                        )
                    }
                }
            }

            When("doFilter is called for a mix of roles per-zaaktype and without zaaktype") {
                clearMocks(httpSession, answers = false, recordedCalls = true)
                val loggedInUserSlot2 = slot<LoggedInUser>()
                val applicationRolesResponse = GetApplicationRolesResponse().apply {
                    results = listOf(
                        createApplicationRolesResponseModel("fakeZaaktype1", listOf("raadpleger", "behandelaar")),
                        createApplicationRolesResponseModel(null, listOf("recordmanager"))
                    )
                }
                every { pabcClientService.getApplicationRoles(any()) } returns applicationRolesResponse

                every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns listOf(
                    createZaaktypeCmmnConfiguration(zaaktypeOmschrijving = "fakeZaaktype1"),
                    createZaaktypeCmmnConfiguration(zaaktypeOmschrijving = "fakeZaaktype2"),
                    createZaaktypeCmmnConfiguration(zaaktypeOmschrijving = "fakeZaaktype3")
                )
                every { zaaktypeBpmnConfigurationBeheerService.listConfigurations() } returns emptyList()

                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("stores loggedInUser and merges roles per-zaaktype and for all zaaktypes into the session") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot2)) }
                    with(loggedInUserSlot2.captured) {
                        this.applicationRolesPerZaaktype["fakeZaaktype1"]?.shouldContainAll(
                            setOf("raadpleger", "behandelaar", "recordmanager")
                        )
                        this.applicationRolesPerZaaktype["fakeZaaktype2"]?.shouldContainAll(setOf("recordmanager"))
                        this.applicationRolesPerZaaktype["fakeZaaktype3"]?.shouldContainAll(setOf("recordmanager"))
                    }
                }
            }
        }
    }

    Context("PABC integration is disabled") {
        val userPrincipalFilter = UserPrincipalFilter(
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
            zaaktypeBpmnConfigurationBeheerService = zaaktypeBpmnConfigurationBeheerService,
            pabcClientService = pabcClientService,
            pabcIntegrationEnabled = false
        )

        Given(" switching user does not call PABC - session is swapped") {
            val userId = "testUserId"
            val loggedInUser = createLoggedInUser(id = userId)
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                    "preferred_username": "testUserName"
                    }                    
                    """.trimMargin(),
                    null
                )
            )
            val oidcSecurityContext = OidcSecurityContext("fakeTokenString", accessToken, null, null)
            val oidcPrincipal = OidcPrincipal("differentUserId", oidcSecurityContext)
            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession andThen newHttpSession
            every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
            every { httpSession.invalidate() } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every { newHttpSession.setAttribute(any(), any()) } just runs
            every { httpServletRequest.servletContext.contextPath } returns "fakeContextPath"
            every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns emptyList()
            every { zaaktypeBpmnConfigurationBeheerService.listConfigurations() } returns emptyList()

            When("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("PABC service is not called") {
                    verify(exactly = 0) { pabcClientService.getApplicationRoles(any()) }
                }
                And("and the session is invalidated and the filter chain is passed on") {
                    verify(exactly = 1) {
                        httpSession.invalidate()
                        newHttpSession.setAttribute("logged-in-user", any())
                        filterChain.doFilter(httpServletRequest, servletResponse)
                    }
                }
            }
        }
    }
})
