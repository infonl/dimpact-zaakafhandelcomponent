/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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

class OidcSessionServiceTest : BehaviorSpec({
    val userPrincipalFilter = mockk<UserPrincipalFilter>()
    val httpSession = mockk<HttpSession>()
    val sessionInstance = mockk<Instance<HttpSession>>()

    beforeTest {
//        checkUnnecessaryStub()
    }

    Given("refresh token is present in session and Keycloak returns new tokens") {
        val refreshToken = "old-refresh-token"
        val newAccessToken = "new-access-token"
        val newRefreshToken = "new-refresh-token"
        val keycloakResponse = """{"access_token":"$newAccessToken","refresh_token":"$newRefreshToken"}"""

        every { sessionInstance.get() } returns httpSession
        every { httpSession.getAttribute("refresh_token") } returns refreshToken
        every { httpSession.setAttribute("refresh_token", newRefreshToken) } just runs

        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        val outputStream = mockk<OutputStream>()
        every { outputStream.write(any<ByteArray>()) } just runs
        every { outputStream.close() } just runs
        every { mockConnection.outputStream } returns outputStream
        every { mockConnection.inputStream } returns ByteArrayInputStream(keycloakResponse.toByteArray())
        every { mockConnection.setRequestProperty(any(), any()) } just runs
        every { mockConnection.setRequestMethod(any()) } just runs
        every { mockConnection.setDoOutput(any()) } just runs

        val oidcPrincipal = mockk<OidcPrincipal<*>>()
        every { userPrincipalFilter.createOidcPrincipalFromAccessToken(newAccessToken) } returns oidcPrincipal
        every { userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, httpSession) } just runs

        val service = OidcSessionService(
            userPrincipalFilter,
            sessionInstance,
            authServer = "http://localhost:8080/auth",
            authRealm = "zac",
            clientId = "zac-client",
            clientSecret = "zac-secret",
            connectionFactory = { mockConnection }
        )

        When("refreshUserSession is called") {
            service.refreshUserSession()

            Then("session is updated with new refresh token and principal is set") {
                verify { httpSession.setAttribute("refresh_token", newRefreshToken) }
                verify { userPrincipalFilter.createOidcPrincipalFromAccessToken(newAccessToken) }
                verify { userPrincipalFilter.setLoggedInUserOnHttpSession(oidcPrincipal, httpSession) }
            }
        }
    }

    Given("refresh token is present in session but Keycloak returns an error") {
        val refreshToken = "old-refresh-token"
        val errorResponse = """{"error":"invalid_grant","error_description":"Refresh token expired"}"""

        every { sessionInstance.get() } returns httpSession
        every { httpSession.getAttribute("refresh_token") } returns refreshToken

        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        val outputStream = mockk<OutputStream>()
        every { outputStream.write(any<ByteArray>()) } just runs
        every { outputStream.close() } just runs
        every { mockConnection.outputStream } returns outputStream
        every { mockConnection.inputStream } throws java.io.IOException("HTTP 400 Bad Request")
        every { mockConnection.errorStream } returns ByteArrayInputStream(errorResponse.toByteArray())
        every { mockConnection.responseCode } returns 400
        every { mockConnection.setRequestProperty(any(), any()) } just runs
        every { mockConnection.setRequestMethod(any()) } just runs
        every { mockConnection.setDoOutput(any()) } just runs

        val service = OidcSessionService(
            userPrincipalFilter,
            sessionInstance,
            authServer = "http://localhost:8080/auth",
            authRealm = "zac",
            clientId = "zac-client",
            clientSecret = "zac-secret",
            connectionFactory = { mockConnection }
        )

        When("refreshUserSession is called") {
            Then("it throws an exception due to Keycloak error") {
                val ex = shouldThrow<Exception> {
                    service.refreshUserSession()
                }
                ex.message shouldBe "HTTP 400 Bad Request"
            }
        }
    }

    Given("no refresh token in session") {
        every { sessionInstance.get() } returns httpSession
        every { httpSession.getAttribute("refresh_token") } returns null
        val service = OidcSessionService(
            userPrincipalFilter,
            sessionInstance,
            authServer = "http://localhost:8080/auth",
            authRealm = "zac",
            clientId = "zac-client",
            clientSecret = "zac-secret"
        )

        When("refreshUserSession is called") {
            Then("it throws an exception") {
                val ex = shouldThrow<IllegalStateException> {
                    service.refreshUserSession()
                }
                ex.message shouldBe "No refresh token found in session"
            }
        }
    }
})
