package net.atos.zac.authentication

import io.kotest.core.spec.style.WordSpec
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.runs
import io.mockk.verify
import org.wildfly.security.http.oidc.OidcPrincipal
import javax.servlet.FilterChain
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

class UserPrincipalFilterTest : WordSpec() {

    @InjectMockKs
    private val userPrincipalFilter = UserPrincipalFilter()

    init {
        "doFilter with logged-in user with valid session" should {
            "invoke filterChain" {
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

                verify {
                    filterChain.doFilter(httpServletRequest, servletResponse)
                }
            }
        }
    }
}
