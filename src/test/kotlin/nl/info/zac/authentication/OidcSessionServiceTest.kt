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
import org.wildfly.security.http.oidc.OidcPrincipal
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.HttpURLConnection

// Helper data class and setup function for test context
private data class TestContext(
    val userPrincipalFilter: UserPrincipalFilter,
    val httpSession: HttpSession,
    val sessionInstance: Instance<HttpSession>,
    val mockConnection: HttpURLConnection,
    val outputStream: OutputStream,
    val service: OidcSessionService
)

private fun setupMocks(
    refreshToken: String? = "old-refresh-token",
    responseCode: Int = 200,
    responseBody: String = """{"access_token":"new-access-token","refresh_token":"new-refresh-token"}""",
    errorStream: ByteArrayInputStream? = null,
    inputStreamThrows: Exception? = null
): TestContext {
    val userPrincipalFilter = mockk<UserPrincipalFilter>()
    val httpSession = mockk<HttpSession>()
    val sessionInstance = mockk<Instance<HttpSession>>()
    val mockConnection = mockk<HttpURLConnection>(relaxed = true)
    val outputStream = mockk<OutputStream>()
    every { outputStream.write(any<ByteArray>()) } just runs
    every { outputStream.close() } just runs

    every { sessionInstance.get() } returns httpSession
    every { httpSession.getAttribute("refresh_token") } returns refreshToken
    every { mockConnection.outputStream } returns outputStream
    every { mockConnection.responseCode } returns responseCode

    if (inputStreamThrows != null) {
        every { mockConnection.inputStream } throws inputStreamThrows
    } else {
        every { mockConnection.inputStream } returns ByteArrayInputStream(responseBody.toByteArray())
    }
    if (errorStream != null) {
        every { mockConnection.errorStream } returns errorStream
    }

    val service = OidcSessionService(
        userPrincipalFilter,
        sessionInstance,
        authServer = "http://localhost:8080/auth",
        authRealm = "zac",
        clientId = "zac-client",
        clientSecret = "zac-secret",
        connectionFactory = { mockConnection }
    )

    return TestContext(
        userPrincipalFilter,
        httpSession,
        sessionInstance,
        mockConnection,
        outputStream,
        service
    )
}

class OidcSessionServiceTest : BehaviorSpec({
    // beforeTest { /* checkUnnecessaryStub() */ }

    Given("refresh token is present in session and Keycloak returns new tokens") {
        val ctx = setupMocks()
        val oidcPrincipal = mockk<OidcPrincipal<*>>()
        every { ctx.userPrincipalFilter.createOidcPrincipalFromAccessToken(any()) } returns oidcPrincipal
        every { ctx.userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, ctx.httpSession) } just runs
        every { ctx.httpSession.setAttribute("refresh_token", any()) } just runs

        When("refreshUserSession is called") {
            ctx.service.refreshUserSession()
            Then("session is updated with new refresh token and principal is set") {
                verify { ctx.httpSession.setAttribute("refresh_token", "new-refresh-token") }
                verify { ctx.userPrincipalFilter.createOidcPrincipalFromAccessToken("new-access-token") }
                verify { ctx.userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, ctx.httpSession) }
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns an error") {
        val errorResponse = """{"error":"invalid_grant","error_description":"Refresh token expired"}"""
        val ctx = setupMocks(
            responseBody = errorResponse,
            inputStreamThrows = java.io.IOException("HTTP 400 Bad Request"),
            errorStream = ByteArrayInputStream(errorResponse.toByteArray())
        )
        When("refreshUserSession is called") {
            Then("it throws an exception due to Keycloak error") {
                val ex = shouldThrow<Exception> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "invalid_grant"
            }
        }
    }

    Given("no refresh token in session") {
        val ctx = setupMocks(refreshToken = null)
        When("refreshUserSession is called") {
            Then("it throws an exception") {
                val ex = shouldThrow<IllegalStateException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldBe "No refresh token found in session"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns an error field in JSON") {
        val errorResponse = """{"error":"invalid_grant","error_description":"Refresh token expired"}"""
        val ctx = setupMocks(responseBody = errorResponse)
        When("refreshUserSession is called") {
            Then("it throws OidcSessionException due to error field in response") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "invalid_grant"
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
                ex.message shouldContain "missing access_token"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns invalid JSON") {
        val response = "not a json!"
        val ctx = setupMocks(responseBody = response)
        When("refreshUserSession is called") {
            Then("it throws OidcSessionException due to invalid JSON") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "Failed to parse Keycloak response as JSON"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns HTTP error") {
        val errorResponse = """{"error":"invalid_grant","error_description":"Refresh token expired"}"""
        val ctx = setupMocks(responseBody = errorResponse)
        When("refreshUserSession is called") {
            Then("it throws OidcSessionException due to error field in JSON response") {
                val ex = shouldThrow<OidcSessionException> {
                    ctx.service.refreshUserSession()
                }
                ex.message shouldContain "invalid_grant"
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns a non-2xx response code") {
        val errorResponse = """{"error":"invalid_grant","error_description":"Refresh token expired"}"""
        val ctx = setupMocks(
            responseCode = 401,
            errorStream = ByteArrayInputStream(errorResponse.toByteArray()),
            inputStreamThrows = java.io.IOException("HTTP 401 Unauthorized")
        )
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
