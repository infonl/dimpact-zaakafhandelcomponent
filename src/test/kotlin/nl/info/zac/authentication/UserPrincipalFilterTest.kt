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
import io.mockk.mockkClass
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.generated.ApplicationRoleModel
import nl.info.client.pabc.model.generated.EntityTypeModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesResponseModel
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.identity.model.getFullName
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext

class UserPrincipalFilterTest : BehaviorSpec({
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val pabcClientService = mockk<PabcClientService>()

    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<HttpServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val newHttpSession = mockk<HttpSession>()
    val oidcPrincipal = mockkClass(OidcPrincipal::class, relaxed = true)
    val oidcSecurityContext = mockk<OidcSecurityContext>(relaxed = true)
    val accessToken = mockk<AccessToken>(relaxed = true)

    fun pabcRolesResponse(zaaktypeName: String, vararg names: String): GetApplicationRolesResponse {
        val applicationRolesResponse = mockk<GetApplicationRolesResponse>()
        val responseModel = mockk<GetApplicationRolesResponseModel>()
        val entityType = mockk<EntityTypeModel>()
        every { entityType.type } returns "zaaktype"
        every { entityType.id } returns zaaktypeName
        every { responseModel.entityType } returns entityType

        val roleModels = names.map { roleName ->
            mockk<ApplicationRoleModel>().also { every { it.name } returns roleName }
        }
        every { responseModel.applicationRoles } returns roleModels
        every { applicationRolesResponse.results } returns listOf(responseModel)
        return applicationRolesResponse
    }

    every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
    every { oidcSecurityContext.token } returns accessToken
    every { accessToken.rolesClaim } returns listOf("role1")
    every { accessToken.preferredUsername } returns "user"
    every { accessToken.givenName } returns "Given"
    every { accessToken.familyName } returns "Family"
    every { accessToken.name } returns "Full Name"
    every { accessToken.email } returns "user@example.com"
    every { accessToken.getStringListClaimValue(any()) } returns listOf("group1")

    beforeEach {
        checkUnnecessaryStub()
        every { filterChain.doFilter(any(), any()) } just runs
        every { httpServletRequest.servletContext.contextPath } returns "fakeContextPath"
    }

    Context("PABC integration is enabled") {
        val userPrincipalFilter = UserPrincipalFilter(
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
            zaaktypeBpmnConfigurationService = zaaktypeBpmnConfigurationService,
            pabcClientService = pabcClientService,
            pabcIntegrationEnabled = true
        )
        val pabcRoleNames = listOf("applicationRoleA", "applicationRoleB")

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

            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { oidcPrincipal.name } returns userId
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
            val userId = "fakeId"
            val loggedInUser = createLoggedInUser(
                id = userId,
                applicationRolesPerZaaktype = mapOf("testZaaktype" to setOf("fakeAppRole1"))
            )
            val roles = listOf("fakeRole1", "fakeRole2")
            val zaaktypeName = "fakeZaaktypeOmschrijving1"
            val newHttpSession = mockk<HttpSession>()
            val capturedLoggedInUser = slot<LoggedInUser>()

            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession andThen newHttpSession
            every { httpServletRequest.servletContext.contextPath } returns "fakeContextPath"
            every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
            every { httpSession.invalidate() } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every { oidcPrincipal.name } returns "aDifferentUserId"
            every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
            every { oidcSecurityContext.token } returns accessToken
            every { accessToken.rolesClaim } returns roles
            every { accessToken.preferredUsername } returns "fakeUserName"
            every { accessToken.givenName } returns "fakeGivenName"
            every { accessToken.familyName } returns "fakeFamilyName"
            every { accessToken.name } returns "fakeFullName"
            every { accessToken.email } returns "fakeemail@example.com"
            every { accessToken.getStringListClaimValue("group_membership") } returns emptyList()
            every {
                pabcClientService.getApplicationRoles(roles)
            } returns pabcRolesResponse(zaaktypeName, *pabcRoleNames.toTypedArray())
            every { newHttpSession.setAttribute("logged-in-user", capture(capturedLoggedInUser)) } just runs

            every {
                zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration()
            } returns listOf(createZaaktypeCmmnConfiguration())
            every { zaaktypeBpmnConfigurationService.listConfigurations() } returns emptyList()

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
                        this.applicationRolesPerZaaktype[zaaktypeName]?.shouldContainAll(pabcRoleNames)
                    }
                }
            }
        }
        Given(
            """
            No logged-in user is present in the HTTP session and an OIDC security context is present with a token that 
            contains user information; PABC roles are mapped and set
        """
        ) {
            val userName = "fakeUserName"
            val givenName = "fakeGivenName"
            val familyName = "fakeFamilyName"
            val fullName = "fakeFullName"
            val email = "fake@example.com"
            val groups = arrayListOf("fakeGroup1")
            val roles = arrayListOf("coordinator")
            val zaaktypeId = "fakeZaaktypeId1"
            val pabcRoleNames = listOf("testApplicationRole1")
            val loggedInUserSlot = slot<LoggedInUser>()

            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession
            every { httpSession.getAttribute("logged-in-user") } returns null
            every { accessToken.rolesClaim } returns roles
            every { accessToken.preferredUsername } returns userName
            every { accessToken.givenName } returns givenName
            every { accessToken.familyName } returns familyName
            every { accessToken.name } returns fullName
            every { accessToken.email } returns email
            every { accessToken.getStringListClaimValue("group_membership") } returns groups
            every { httpSession.setAttribute(any(), any()) } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every {
                pabcClientService.getApplicationRoles(any())
            } returns pabcRolesResponse(zaaktypeId, *pabcRoleNames.toTypedArray())

            every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns emptyList()
            every { zaaktypeBpmnConfigurationService.listConfigurations() } returns listOf(
                createZaaktypeBpmnConfiguration(zaaktypeOmschrijving = "bpmn1")
            )

            When("doFilter is called") {
                val refreshableCtx = mockk<RefreshableOidcSecurityContext>(relaxed = true)
                every { refreshableCtx.token } returns accessToken
                every { refreshableCtx.refreshToken } returns "test-refresh-token"
                every { oidcPrincipal.oidcSecurityContext } returns refreshableCtx

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
    }

    Context("PABC integration is disabled") {
        val userPrincipalFilter = UserPrincipalFilter(
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
            zaaktypeBpmnConfigurationService = zaaktypeBpmnConfigurationService,
            pabcClientService = pabcClientService,
            pabcIntegrationEnabled = false
        )

        Given(" switching user does not call PABC - session is swapped") {
            val userId = "testUserId"
            val loggedInUser = createLoggedInUser(id = userId)

            every { httpServletRequest.userPrincipal } returns oidcPrincipal
            every { httpServletRequest.getSession(true) } returns httpSession andThen newHttpSession
            every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
            every { httpSession.invalidate() } just runs
            every { filterChain.doFilter(any(), any()) } just runs
            every { oidcPrincipal.name } returns "differentUserId"
            every { newHttpSession.setAttribute(any(), any()) } just runs
            every { httpServletRequest.servletContext.contextPath } returns "fakeContextPath"

            every { zaaktypeCmmnConfigurationService.listZaaktypeCmmnConfiguration() } returns emptyList()
            every { zaaktypeBpmnConfigurationService.listConfigurations() } returns emptyList()

            When("doFilter is called") {
                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                Then("PABC service is not called - session recreated and chain continues") {
                    verify(exactly = 0) { pabcClientService.getApplicationRoles(any()) }
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
