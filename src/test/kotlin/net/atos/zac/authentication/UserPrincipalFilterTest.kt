/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
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
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.identity.model.getFullName
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext

class UserPrincipalFilterTest : BehaviorSpec({
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val userPrincipalFilter = UserPrincipalFilter(zaakafhandelParameterService)

    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<ServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val loggedInUser = mockk<LoggedInUser>()
    val oidcPrincipal = mockkClass(OidcPrincipal::class)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A logged-in user is present in the http session") {
        val userId = "dummyId"
        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession
        every { filterChain.doFilter(any(), any()) } just runs
        every { SecurityUtil.getLoggedInUser(httpSession) } returns loggedInUser
        every { loggedInUser.id } returns userId
        every { oidcPrincipal.name } returns userId

        When("doFilter is called") {
            userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

            Then("filterChain is invoked") {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                }
            }
        }
    }
    Given(
        """
            No logged-in user is present in the HTTP session and an OIDC security context is present with a token
            that contains user information including a role for a domain which is also present in one of the zaakafhandelparameters
            """
    ) {
        val userName = "dummyUserName"
        val givenName = "dummyGivenName"
        val familyName = "dummyFamilyName"
        val fullName = "dummyFullName"
        val email = "dummy@example.com"
        val domein1 = "dummyDomein1"
        val groups = arrayListOf(
            "dummyGroup1",
            "dummyGroup2"
        )
        val roles = arrayListOf(
            "dummyRole1",
            "dummyRole2",
            domein1
        )
        val zaakafhandelParameters = listOf(
            createZaakafhandelParameters(
                domein = domein1,
                zaaktypeOmschrijving = "dummyZaaktypeOmschrijving1"
            ),
            createZaakafhandelParameters(
                domein = "dummyDomein2"
            )
        )
        val oidcSecurityContext = mockk<OidcSecurityContext>()
        val accessToken = mockk<AccessToken>()
        val loggedInUserSlot = slot<LoggedInUser>()

        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession
        every { filterChain.doFilter(any(), any()) } just runs
        every { SecurityUtil.getLoggedInUser(httpSession) } returns null
        every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
        every { oidcSecurityContext.token } returns accessToken
        every { accessToken.preferredUsername } returns userName
        every { accessToken.givenName } returns givenName
        every { accessToken.familyName } returns familyName
        every { accessToken.name } returns fullName
        every { accessToken.email } returns email
        every { accessToken.getStringListClaimValue("group_membership") } returns groups
        every { accessToken.rolesClaim } returns roles
        every { zaakafhandelParameterService.listZaakafhandelParameters() } returns zaakafhandelParameters
        every { httpSession.setAttribute(any(), any()) } just runs

        When("doFilter is called") {
            userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

            Then(
                """
                the user is retrieved from security context and is added to the HTTP session and the user should have 
                access to only that zaaktype for which zaakafhandelparameters are defined with the same domain as
                is present in the user's roles
                """
            ) {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                    httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                }
                with(loggedInUserSlot.captured) {
                    this.id shouldBe userName
                    this.firstName shouldBe givenName
                    this.lastName shouldBe familyName
                    this.getFullName() shouldBe fullName
                    this.email shouldBe email
                    this.roles shouldContainAll roles
                    this.groupIds shouldContainAll groups
                    this.geautoriseerdeZaaktypen shouldContainExactly listOf("dummyZaaktypeOmschrijving1")
                }
            }
        }
    }
})
