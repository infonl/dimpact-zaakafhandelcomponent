/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
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

    context("Producing the logged-in user for the current thread") {
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)

        given("no HTTP session and a user carried into the async context") {
            val loggedInUser = createLoggedInUser()
            every { httpSessionInstance.get() } returns null

            `when`("getLoggedInUser is called") {
                LoggedInUserProvider.asyncContextUser.set(loggedInUser)
                val result = try {
                    loggedInUserProvider.getLoggedInUser()
                } finally {
                    LoggedInUserProvider.asyncContextUser.remove()
                }

                then("it returns the user that started the work instead of the functionele gebruiker") {
                    result shouldBe loggedInUser
                }
            }
        }

        given("no HTTP session and nothing carried into the async context") {
            every { httpSessionInstance.get() } returns null

            `when`("getLoggedInUser is called") {
                val result = loggedInUserProvider.getLoggedInUser()

                then("it returns the functionele gebruiker") {
                    result shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
                }
            }
        }

        given("an explicitly requested system user and a user carried into the async context") {
            val loggedInUser = createLoggedInUser()

            `when`("getLoggedInUser is called") {
                LoggedInUserProvider.asyncContextUser.set(loggedInUser)
                LoggedInUserProvider.systemUser.set(true)
                val result = try {
                    loggedInUserProvider.getLoggedInUser()
                } finally {
                    LoggedInUserProvider.systemUser.remove()
                    LoggedInUserProvider.asyncContextUser.remove()
                }

                then("the explicitly requested system user wins over the async context") {
                    result shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
                }
            }
        }
    }

    context("Running work as the system user") {
        val httpSessionInstance = mockk<Instance<HttpSession>>()
        val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)

        given("a logged-in user session") {
            val loggedInUser = createLoggedInUser()
            every { httpSessionInstance.get() } returns httpSession
            every { httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns loggedInUser

            `when`("work is run as the system user") {
                val userDuringSystemWork = runAsSystemUser { loggedInUserProvider.getLoggedInUser() }
                val userAfterSystemWork = loggedInUserProvider.getLoggedInUser()

                then("that work runs as the functionele gebruiker") {
                    userDuringSystemWork shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
                }

                and("the session user is restored afterwards") {
                    userAfterSystemWork shouldBe loggedInUser
                }
            }
        }

        given("work as the system user that throws") {
            `when`("the exception has propagated") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    runAsSystemUser { throw IllegalStateException("fakeFailure") }
                }

                then("the exception is propagated and the system user is no longer active") {
                    illegalStateException.message shouldBe "fakeFailure"
                    LoggedInUserProvider.systemUser.get() shouldBe false
                }
            }
        }
    }
})
