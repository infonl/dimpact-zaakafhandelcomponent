/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
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
import nl.info.client.pabc.APPLICATION_NAME_ZAC
import nl.info.client.pabc.ENTITY_TYPE_GEMEENTE
import nl.info.client.pabc.ENTITY_TYPE_ZAAKTYPE
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.ROLE_NAME_BRP_ZOEKEN
import nl.info.client.pabc.model.createApplicationRolesResponseModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.identity.model.getFullName
import org.jose4j.jwt.JwtClaims
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext

class UserPrincipalFilterTest : BehaviorSpec({
    val pabcClientService = mockk<PabcClientService>()
    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val newHttpSession = mockk<HttpSession>()

    afterEach {
        checkUnnecessaryStub()
    }

    context("PABC integration is enabled") {
        val userPrincipalFilter = UserPrincipalFilter(
            pabcClientService = pabcClientService
        )

        given(
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

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("filterChain is invoked; no invalidate; no PABC call triggered by this branch") {
                    verify(exactly = 1) { filterChain.doFilter(httpServletRequest, servletResponse) }
                    verify(exactly = 0) { httpSession.invalidate() }
                }
            }
        }

        given(
            """A logged-in user is present in the HTTP session with a token containing functional roles
                as realm roles and a servlet request containing a user principal with a different id 
                as the logged-in user"""
        ) {
            val functionalRoles = listOf("fakeFunctionalRole1", "fakeFunctionalRole2")
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
                        "realm_access": {
                            "roles": [ "${functionalRoles.joinToString(separator = "\", \"")}" ]
                        }
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
                pabcClientService.getApplicationRoles(functionalRoles)
            } returns GetApplicationRolesResponse().apply {
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = entityTypeId,
                        roleNames = pabcRoleNames
                    )
                )
            }
            every { newHttpSession.setAttribute("logged-in-user", capture(capturedLoggedInUser)) } just runs

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the existing HTTP session is invalidated and the user is added to a new HTTP session") {
                    verify(exactly = 1) {
                        filterChain.doFilter(httpServletRequest, servletResponse)
                        httpSession.invalidate()
                        newHttpSession.setAttribute("logged-in-user", any())
                        pabcClientService.getApplicationRoles(functionalRoles)
                    }
                    with(capturedLoggedInUser.captured) {
                        this.roles shouldContainAll functionalRoles
                        this.applicationRolesPerZaaktype[entityTypeId]?.shouldContainAll(pabcRoleNames)
                    }
                }
            }
        }
        given(
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
            val functionalRoles = listOf("fakeFunctionalRole")
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
                        "realm_access": {
                            "roles": [ "${functionalRoles.joinToString(separator = "\", \"")}" ]
                        },
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
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = zaaktypeId,
                        roleNames = pabcRoleNames
                    )
                )
            }
            every { httpServletRequest.userPrincipal } returns oidcPrincipal

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the user is created from OIDC and stored on the session") {
                    verify(exactly = 1) {
                        httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                        filterChain.doFilter(httpServletRequest, servletResponse)
                    }
                    with(loggedInUserSlot.captured) {
                        this.id shouldBe userName
                        this.firstName shouldBe givenName
                        this.lastName shouldBe familyName
                        this.getFullName() shouldBe fullName
                        this.email shouldBe email
                        this.roles shouldContainAll functionalRoles
                        this.groupIds shouldContainAll groups
                        this.applicationRolesPerZaaktype[zaaktypeId]?.shouldContainAll(pabcRoleNames)
                    }
                }
            }
        }

        given(
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
                        "realm_access": {
                            "roles": [ "fakeFunctionalRole" ]
                        }
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
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = null,
                        roleNames = listOf("fakeApplicationRole")
                    )
                )
            }

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the logged-in user is added to the HTTP session") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                }

                And("applicationRolesPerZaaktype is empty because the mapping has no entity type") {
                    loggedInUserSlot.captured.applicationRolesPerZaaktype shouldBe emptyMap()
                }

                And("the overall roles contain the application roles without an entity type") {
                    loggedInUserSlot.captured.overallRoles shouldContainAll setOf("fakeApplicationRole")
                }
            }
        }

        given(
            """
            User details in the OIDC token in the security context and
            PABC authorisation mappings contain both entity-type-specific roles and overall roles (without entity type)
            """
        ) {
            val loggedInUserSlot = slot<LoggedInUser>()
            val zaaktypeId = "fakeZaaktypeId"
            val entityTypeRoleNames = listOf("fakeEntityTypeRole1", "fakeEntityTypeRole2")
            val overallRoleNames = listOf("fakeOverallRole1", "fakeOverallRole2")
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                        "preferred_username": "fakeUserName",
                        "realm_access": {
                            "roles": [ "fakeFunctionalRole" ]
                        }
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
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = zaaktypeId,
                        entityTypeType = ENTITY_TYPE_ZAAKTYPE,
                        roleNames = entityTypeRoleNames
                    ),
                    createApplicationRolesResponseModel(
                        entityTypeId = null,
                        roleNames = overallRoleNames
                    )
                )
            }

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the entity-type-specific roles are stored in applicationRolesPerZaaktype on the logged-in user") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                    loggedInUserSlot.captured.applicationRolesPerZaaktype[zaaktypeId]
                        ?.shouldContainAll(entityTypeRoleNames)
                }

                And("the overall roles contain only the roles without an entity type") {
                    with(loggedInUserSlot.captured) {
                        overallRoles shouldContainAll overallRoleNames
                        overallRoles.none { it in entityTypeRoleNames } shouldBe true
                    }
                }
            }
        }

        given(
            """
            User details in the OIDC token and PABC authorisation mappings contain
            a GEMEENTE entity type with BRP zoeken application role
            """
        ) {
            val loggedInUserSlot = slot<LoggedInUser>()
            val gemeenteCode = "0344"
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                        "preferred_username": "fakeUserName",
                        "realm_access": {
                            "roles": [ "fakeFunctionalRole" ]
                        }
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
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = gemeenteCode,
                        entityTypeType = ENTITY_TYPE_GEMEENTE,
                        roleNames = listOf(ROLE_NAME_BRP_ZOEKEN),
                        applicationName = APPLICATION_NAME_ZAC
                    )
                )
            }

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the logged-in user should have the BRP gemeente in brpGemeenten") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                    with(loggedInUserSlot.captured) {
                        brpGemeenten.size shouldBe 1
                        brpGemeenten[gemeenteCode] shouldBe gemeenteCode
                    }
                }
            }
        }

        given(
            """
            User details in the OIDC token and PABC authorisation mappings contain
            a GEMEENTE entity type but without the BRP zoeken application role
            """
        ) {
            val loggedInUserSlot = slot<LoggedInUser>()
            val gemeenteCode = "0344"
            val accessToken = AccessToken(
                JwtClaims.parse(
                    """
                    {
                        "preferred_username": "fakeUserName",
                        "realm_access": {
                            "roles": [ "fakeFunctionalRole" ]
                        }
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
                results = listOf(
                    createApplicationRolesResponseModel(
                        entityTypeId = gemeenteCode,
                        entityTypeType = ENTITY_TYPE_GEMEENTE,
                        roleNames = listOf("some_other_role"),
                        applicationName = APPLICATION_NAME_ZAC
                    )
                )
            }

            `when`("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                then("the logged-in user should have an empty brpGemeenten map") {
                    verify { httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot)) }
                    with(loggedInUserSlot.captured) {
                        brpGemeenten shouldBe emptyMap()
                    }
                }
            }
        }
    }
})
