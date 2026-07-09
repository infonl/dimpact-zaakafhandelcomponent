/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

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
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.identity.model.getFullName
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import java.time.ZonedDateTime

class UserPrincipalFilterTest : BehaviorSpec({
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val userPrincipalFilter = UserPrincipalFilter(zaakafhandelParameterService)

    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<ServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val oidcPrincipal = mockkClass(OidcPrincipal::class)
    val oidcSecurityContext = mockk<OidcSecurityContext>()
    val accessToken = mockk<AccessToken>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
            A logged-in user is present in the HTTP session and a servlet request containing 
            a user principal with the same id as the logged-in user in the HTTP session
            """
    ) {
        val userId = "dummyId"
        val loggedInUser = createLoggedInUser(
            id = userId
        )
        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession
        every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
        every { filterChain.doFilter(any(), any()) } just runs
        every { oidcPrincipal.name } returns userId

        When(
            """
                doFilter is called
                """
        ) {
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
                A logged-in user is present in the HTTP session and a servlet request containing 
                a user principal with a different id as the logged-in user
                """
    ) {
        val userId = "dummyId"
        val loggedInUser = createLoggedInUser(
            id = userId
        )
        val roles = arrayListOf(
            "dummyRole1"
        )
        val zaakafhandelParameters = listOf(createZaakafhandelParameters())
        val newHttpSession = mockk<HttpSession>()
        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession andThen newHttpSession
        every { httpServletRequest.servletContext.contextPath } returns "dummyContextPath"
        every { httpSession.getAttribute("logged-in-user") } returns loggedInUser
        every { httpSession.invalidate() } just runs
        every { filterChain.doFilter(any(), any()) } just runs
        every { oidcPrincipal.name } returns "aDifferentUserId"
        every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
        every { oidcSecurityContext.token } returns accessToken
        every { accessToken.rolesClaim } returns roles
        every { accessToken.rolesClaim } returns roles
        every { accessToken.preferredUsername } returns "dummyUserName"
        every { accessToken.givenName } returns "dummyGivenName"
        every { accessToken.familyName } returns "dummyFamilyName"
        every { accessToken.name } returns "dummyFullName"
        every { accessToken.email } returns "dummyemail@example.com"
        every { accessToken.getStringListClaimValue("group_membership") } returns emptyList()
        every { zaakafhandelParameterService.listZaakafhandelParameters() } returns zaakafhandelParameters
        every { newHttpSession.setAttribute(any(), any()) } just runs

        When("doFilter is called") {
            userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

            Then(
                """
                    the existing HTTP session is invalidated and the user is added to a new HTTP session
                    """
            ) {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                    httpSession.invalidate()
                    newHttpSession.setAttribute("logged-in-user", any())
                }
            }
        }
    }
    Given(
        """
            No logged-in user is present in the HTTP session and an OIDC security context is present with a token
            that contains user information including a role for a domain which is also present in one of 
            the currently active zaakafhandelparameters
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
            // zaakafhandelparameters for an old version of zaaktype2
            createZaakafhandelParameters(
                creationDate = ZonedDateTime.now().minusDays(1),
                domein = domein1,
                zaaktypeOmschrijving = "dummyZaaktypeOmschrijving2"
            ),
            // zaakafhandelparameters for the current version of zaaktype2
            createZaakafhandelParameters(
                creationDate = ZonedDateTime.now(),
                domein = "dummyDomein2",
                zaaktypeOmschrijving = "dummyZaaktypeOmschrijving2"
            )
        )
        val loggedInUserSlot = slot<LoggedInUser>()

        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession
        // no logged-in user present in HTTP session
        every { httpSession.getAttribute("logged-in-user") } returns null
        every { filterChain.doFilter(any(), any()) } just runs
        every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
        every { oidcSecurityContext.token } returns accessToken
        every { accessToken.rolesClaim } returns roles
        every { accessToken.preferredUsername } returns userName
        every { accessToken.givenName } returns givenName
        every { accessToken.familyName } returns familyName
        every { accessToken.name } returns fullName
        every { accessToken.email } returns email
        every { accessToken.getStringListClaimValue("group_membership") } returns groups
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
    Given(
        """
            No logged-in user is present in the HTTP session and an OIDC security context is present with a token
            that contains user information including a role 'domein_elk_zaaktype'
            """
    ) {
        val userName = "dummyUserName"
        val givenName = "dummyGivenName"
        val familyName = "dummyFamilyName"
        val fullName = "dummyFullName"
        val email = "dummy@example.com"
        val groups = arrayListOf(
            "dummyGroup1",
        )
        val roles = arrayListOf(
            "dummyRole1",
            "domein_elk_zaaktype"
        )
        val loggedInUserSlot = slot<LoggedInUser>()

        every { httpServletRequest.userPrincipal } returns oidcPrincipal
        every { httpServletRequest.getSession(true) } returns httpSession
        // no logged-in user present in HTTP session
        every { httpSession.getAttribute("logged-in-user") } returns null
        every { filterChain.doFilter(any(), any()) } just runs
        every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
        every { oidcSecurityContext.token } returns accessToken
        every { accessToken.rolesClaim } returns roles
        every { accessToken.preferredUsername } returns userName
        every { accessToken.givenName } returns givenName
        every { accessToken.familyName } returns familyName
        every { accessToken.name } returns fullName
        every { accessToken.email } returns email
        every { accessToken.getStringListClaimValue("group_membership") } returns groups
        every { httpSession.setAttribute(any(), any()) } just runs

        When("doFilter is called") {
            userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

            Then(
                """
                the user is retrieved from security context and is added to the HTTP session and 
                the user should have access to all zaakttypes
                """
            ) {
                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                    httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                }
                with(loggedInUserSlot.captured) {
                    this.id shouldBe userName
                    // null indicates that the user has access to all zaaktypen
                    this.geautoriseerdeZaaktypen shouldBe null
                }
            }
        }
    }
})
