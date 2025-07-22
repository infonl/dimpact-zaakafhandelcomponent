/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.wildfly.security.http.oidc.OidcPrincipal

// Helper data class and setup function for test context
private data class TestContext(
    val userPrincipalFilter: UserPrincipalFilter,
    val httpSession: HttpSession,
    val sessionInstance: Instance<HttpSession>,
    val okHttpClient: OkHttpClient,
    val mockCall: Call,
    val mockResponse: Response,
    val service: OidcSessionService
)

private fun setupMocks(
    refreshToken: String? = "old-refresh-token",
    responseCode: Int = 200,
    responseBody: String = "" +
        "{\"$ACCESS_TOKEN_ATTRIBUTE\":\"new-access-token\",\"$REFRESH_TOKEN_ATTRIBUTE\":\"new-refresh-token\"}",
): TestContext {
    val userPrincipalFilter = mockk<UserPrincipalFilter>()
    val httpSession = mockk<HttpSession>()
    val sessionInstance = mockk<Instance<HttpSession>>()
    val okHttpClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()

    every { sessionInstance.get() } returns httpSession
    every { httpSession.getAttribute(REFRESH_TOKEN_ATTRIBUTE) } returns refreshToken
    
    val mediaType = "application/json".toMediaTypeOrNull()

    val mockResponse = Response.Builder()
        .request(
            Request.Builder()
                .url("http://localhost:8080/auth/realms/zac/protocol/openid-connect/token")
                .build()
        )
        .protocol(Protocol.HTTP_1_1)
        .code(responseCode)
        .message("")
        .body(responseBody.toResponseBody(mediaType))
        .build()

    every { okHttpClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse

    val service = OidcSessionService(
        userPrincipalFilter,
        sessionInstance,
        authServer = "http://localhost:8080/auth",
        authRealm = "zac",
        clientId = "zac-client",
        clientSecret = "zac-secret",
        okHttpClient = okHttpClient
    )

    return TestContext(
        userPrincipalFilter,
        httpSession,
        sessionInstance,
        okHttpClient,
        mockCall,
        mockResponse,
        service
    )
}

class OidcSessionServiceTest : BehaviorSpec({
    Given("refresh token is present in session and Keycloak returns new tokens") {
        val ctx = setupMocks()
        val oidcPrincipal = mockk<OidcPrincipal<*>>()
        every { ctx.userPrincipalFilter.createOidcPrincipalFromAccessToken(any()) } returns oidcPrincipal
        every { ctx.userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, ctx.httpSession) } just runs
        every { ctx.httpSession.setAttribute(REFRESH_TOKEN_ATTRIBUTE, any()) } just runs

        When("refreshUserSession is called") {
            ctx.service.refreshUserSession()
            Then("session is updated with new refresh token and principal is set") {
                verify { ctx.httpSession.setAttribute(REFRESH_TOKEN_ATTRIBUTE, "new-refresh-token") }
                verify { ctx.userPrincipalFilter.createOidcPrincipalFromAccessToken("new-access-token") }
                verify { ctx.userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, ctx.httpSession) }
            }
        }
    }

    Given("no refresh token in session") {
        val ctx = setupMocks(refreshToken = null)

        When("refreshUserSession is called") {
            Then("it throws an exception") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldBe "No $REFRESH_TOKEN_ATTRIBUTE found in session"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns missing tokens") {
        val response = """{"not_access_token":"foo"}"""
        val ctx = setupMocks(responseBody = response)

        When("refreshUserSession is called") {
            Then("it throws OidcSessionException due to missing tokens") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "missing $ACCESS_TOKEN_ATTRIBUTE"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns a non-2xx response code") {
        val ctx = setupMocks(responseCode = 401)

        When("refreshUserSession is called") {
            Then("it throws OidcSessionException due to non-2xx response code") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "HTTP 401"
            }
        }
    }
})
