/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpSession

class LoggedInUserProviderTest : BehaviorSpec({
    val httpSession = mockk<HttpSession>()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Get logged-in user") {
        given("a valid session with a logged-in user attribute") {
            val loggedInUser = mockk<LoggedInUser>()
            every { httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns loggedInUser

            `when`("getLoggedInUser is called") {
                val result = getLoggedInUser(httpSession)

                then("it returns the logged-in user") {
                    result shouldBe loggedInUser
                }
            }
        }

        given("a valid session without a logged-in user attribute") {
            every { httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns null

            `when`("getLoggedInUser is called") {
                val result = getLoggedInUser(httpSession)

                then("it returns null") {
                    result shouldBe null
                }
            }
        }

        given("a session that has been invalidated (user logged out while request was in-flight)") {
            every {
                httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE)
            } throws IllegalStateException("UT000010: Session is invalid")

            `when`("getLoggedInUser is called") {
                val result = getLoggedInUser(httpSession)

                then("it returns null instead of propagating the IllegalStateException") {
                    result shouldBe null
                }
            }
        }
    }
})
