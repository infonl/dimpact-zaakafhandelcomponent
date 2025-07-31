/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
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
import nl.info.client.pabc.PabcClientService
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.identity.model.FunctionalRole
import nl.info.zac.identity.model.getFullName
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import java.time.ZonedDateTime

class UserPrincipalFilterTest : BehaviorSpec({
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val pabcClientService = mockk<PabcClientService>()
    val userPrincipalFilter = UserPrincipalFilter(zaakafhandelParameterService, pabcClientService)

    val httpServletRequest = mockk<HttpServletRequest>()
    val servletResponse = mockk<ServletResponse>()
    val filterChain = mockk<FilterChain>()
    val httpSession = mockk<HttpSession>()
    val oidcPrincipal = mockkClass(OidcPrincipal::class, relaxed = true)
    val oidcSecurityContext = mockk<OidcSecurityContext>(relaxed = true)
    val accessToken = mockk<AccessToken>(relaxed = true)

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
    }

    Given(
        """
        A logged-in user is present in the HTTP session and a servlet request containing 
        a user principal with the same id as the logged-in user in the HTTP session
    """
    ) {
        val userId = "fakeId"
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
        val userId = "fakeId"
        val loggedInUser = createLoggedInUser(
            id = userId
        )
        val roles = arrayListOf(
            "fakeRole1"
        )
        val zaakafhandelParameters = listOf(createZaakafhandelParameters())
        val newHttpSession = mockk<HttpSession>()
        val expectedFunctionalRoles = roles.toList()
        val expectedRolesToBeFiltered = listOf(
            FunctionalRole.DOMEIN_ELK_ZAAKTYPE,
            FunctionalRole.ZAAKAFHANDELCOMPONENT_USER
        )
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
        every { accessToken.rolesClaim } returns roles
        every { accessToken.preferredUsername } returns "fakeUserName"
        every { accessToken.givenName } returns "fakeGivenName"
        every { accessToken.familyName } returns "fakeFamilyName"
        every { accessToken.name } returns "fakeFullName"
        every { accessToken.email } returns "fakeemail@example.com"
        every { accessToken.getStringListClaimValue("group_membership") } returns emptyList()
        every { zaakafhandelParameterService.listZaakafhandelParameters() } returns zaakafhandelParameters
        every { newHttpSession.setAttribute(any(), any()) } just runs
        every {
            pabcClientService.getApplicationRoles(
                match { it.toSet() == expectedFunctionalRoles.toSet() && it.size == expectedFunctionalRoles.size },
                match { it == expectedRolesToBeFiltered }
            )
        } returns mockk(relaxed = true)

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
        val userName = "fakeUserName"
        val givenName = "fakeGivenName"
        val familyName = "fakeFamilyName"
        val fullName = "fakeFullName"
        val email = "fake@example.com"
        val domein1 = "fakeDomein1"
        val groups = arrayListOf(
            "fakeGroup1",
            "fakeGroup2"
        )
        val roles = arrayListOf(
            "fakeRole1",
            "fakeRole2",
            domein1
        )
        val zaakafhandelParameters = listOf(
            createZaakafhandelParameters(
                domein = domein1,
                zaaktypeOmschrijving = "fakeZaaktypeOmschrijving1"
            ),
            // zaakafhandelparameters for an old version of zaaktype2
            createZaakafhandelParameters(
                creationDate = ZonedDateTime.now().minusDays(1),
                domein = domein1,
                zaaktypeOmschrijving = "fakeZaaktypeOmschrijving2"
            ),
            // zaakafhandelparameters for the current version of zaaktype2
            createZaakafhandelParameters(
                creationDate = ZonedDateTime.now(),
                domein = "fakeDomein2",
                zaaktypeOmschrijving = "fakeZaaktypeOmschrijving2"
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
        every {
            pabcClientService.getApplicationRoles(any(), any())
        } returns mockk(relaxed = true)

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
                    this.geautoriseerdeZaaktypen shouldContainExactly listOf("fakeZaaktypeOmschrijving1")
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
        val userName = "fakeUserName"
        val givenName = "fakeGivenName"
        val familyName = "fakeFamilyName"
        val fullName = "fakeFullName"
        val email = "fake@example.com"
        val groups = arrayListOf(
            "fakeGroup1",
        )
        val roles = arrayListOf(
            "fakeRole1",
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
        every {
            pabcClientService.getApplicationRoles(any(), any())
        } returns mockk(relaxed = true)

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
