/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import org.wildfly.security.http.oidc.AccessToken
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RealmAccessClaim
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession

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
        clearAllMocks()
    }

    given("a logged-in user is present in the http session") {
        When("doFilter is called") {
            then("filterChain is invoked") {
                val userId = "dummyId"

                every { httpServletRequest.userPrincipal } returns oidcPrincipal
                every { httpServletRequest.getSession(true) } returns httpSession
                every { filterChain.doFilter(any(), any()) } just runs
                every { SecurityUtil.getLoggedInUser(httpSession) } returns loggedInUser
                every { loggedInUser.id } returns userId
                every { oidcPrincipal.name } returns userId

                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                }
            }
        }
    }
    given("no user is present in the http session") {
        When("doFilter is called") {
            then("user is retrieved from security context and added to the http session") {
                val userName = "dummyUserName"
                val givenName = "dummyGivenName"
                val familyName = "dummyFamilyName"
                val fullName = "dummyFullName"
                val email = "dummy@example.com"
                val groups = arrayListOf(
                    "dummyGroup1",
                    "dummyGroup2"
                )
                val roles = arrayListOf(
                    "dummyRole1",
                    "dummyRole2"
                )
                val zaakafhandelParameters = listOf(
                    ZaakafhandelParameters(),
                    ZaakafhandelParameters()
                )

                val oidcSecurityContext = mockk<OidcSecurityContext>()
                val accessToken = mockk<AccessToken>()
                val realmAccessClaim = mockk<RealmAccessClaim>()
                val loggedInUserSlot = slot<LoggedInUser>()

                every { httpServletRequest.userPrincipal } returns oidcPrincipal
                every { httpServletRequest.getSession(true) } returns httpSession
                every { filterChain.doFilter(any(), any()) } just runs
                every { SecurityUtil.getLoggedInUser(httpSession) } returns null
                every { oidcPrincipal.oidcSecurityContext } returns oidcSecurityContext
                every { oidcSecurityContext.token } returns accessToken
                every { accessToken.realmAccessClaim } returns realmAccessClaim
                every { accessToken.preferredUsername } returns userName
                every { accessToken.givenName } returns givenName
                every { accessToken.familyName } returns familyName
                every { accessToken.name } returns fullName
                every { accessToken.email } returns email
                every { accessToken.getStringListClaimValue("group_membership") } returns groups
                every { realmAccessClaim.roles } returns roles
                every { zaakafhandelParameterService.listZaakafhandelParameters() } returns zaakafhandelParameters
                every { httpSession.setAttribute(any(), any()) } just runs

                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                    httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                }
                with(loggedInUserSlot.captured) {
                    id shouldBe userName
                    firstName shouldBe givenName
                    lastName shouldBe familyName
                    fullName shouldBe fullName
                    email shouldBe email
                    roles shouldContainAll roles
                    groupIds shouldContainAll groups
                }
            }
        }
    }
})
