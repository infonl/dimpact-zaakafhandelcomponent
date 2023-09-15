package net.atos.zac.authentication

import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
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
import javax.servlet.FilterChain
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

class UserPrincipalFilterTest : WordSpec() {

    @MockK
    lateinit var zaakafhandelParameterService: ZaakafhandelParameterService

    @InjectMockKs
    lateinit var userPrincipalFilter: UserPrincipalFilter

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    init {
        "doFilter" should {
            "invoke filterChain with logged-in user with valid session" {
                val userId = "dummyId"

                val httpServletRequest = mockk<HttpServletRequest>()
                val servletResponse = mockk<ServletResponse>()
                val filterChain = mockk<FilterChain>()
                val httpSession = mockk<HttpSession>()
                val loggedInUser = mockk<LoggedInUser>()
                val oidcPrincipal = mockkClass(OidcPrincipal::class)

                every { httpServletRequest.userPrincipal } returns oidcPrincipal
                every { httpServletRequest.getSession(true) } returns httpSession
                every { SecurityUtil.getLoggedInUser(httpSession) } returns loggedInUser
                every { loggedInUser.id } returns userId
                every { oidcPrincipal.name } returns userId
                every { filterChain.doFilter(any(), any()) } just runs

                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                }
            }
            "get user from security context and add user to http session when no user is present in session" {
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

                val httpServletRequest = mockk<HttpServletRequest>()
                val servletResponse = mockk<ServletResponse>()
                val filterChain = mockk<FilterChain>()
                val httpSession = mockk<HttpSession>()
                val oidcPrincipal = mockkClass(OidcPrincipal::class)
                val oidcSecurityContext = mockk<OidcSecurityContext>()
                val accessToken = mockk<AccessToken>()
                val realmAccessClaim = mockk<RealmAccessClaim>()
                val loggedInUserSlot = slot<LoggedInUser>()

                every { httpServletRequest.userPrincipal } returns oidcPrincipal
                every { httpServletRequest.getSession(true) } returns httpSession
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
                every { filterChain.doFilter(any(), any()) } just runs

                userPrincipalFilter.doFilter(httpServletRequest, servletResponse, filterChain)

                verify(exactly = 1) {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                    httpSession.setAttribute("logged-in-user", capture(loggedInUserSlot))
                }
                val loggedInUser = loggedInUserSlot.captured
                loggedInUser.id shouldBe userName
                loggedInUser.firstName shouldBe givenName
                loggedInUser.lastName shouldBe familyName
                loggedInUser.fullName shouldBe fullName
                loggedInUser.email shouldBe email
                loggedInUser.roles shouldContainAll roles
                loggedInUser.groupIds shouldContainAll groups
            }
        }
    }
}
