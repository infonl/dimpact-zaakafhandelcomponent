/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EmployeeKeycloakUserIdsTest : BehaviorSpec({
    val startInstant = Instant.parse("2026-11-23T10:00:00Z")

    fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

    beforeEach {
        EmployeeKeycloakUserIds.reset()
    }

    context("Resolving the Keycloak user ID of an employee") {
        given("no employee lookup yet, as during early startup") {
            `when`("an employee who has not logged in is resolved") {
                val keycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("the employee is unknown") {
                    keycloakUserId shouldBe UNKNOWN_EMPLOYEE
                }
            }
        }

        given("a registered employee") {
            var lookupCount = 0
            EmployeeKeycloakUserIds.useEmployeeLookup {
                lookupCount++
                "fakeOtherKeycloakUserId"
            }
            EmployeeKeycloakUserIds.register("fakeUsername", "fakeKeycloakUserId")

            `when`("the employee is resolved") {
                val keycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("the registered ID is returned without a lookup") {
                    keycloakUserId shouldBe "fakeKeycloakUserId"
                    lookupCount shouldBe 0
                }
            }
        }

        given("an employee who has not logged in and whom the lookup finds") {
            val lookedUpUsernames = mutableListOf<String>()
            EmployeeKeycloakUserIds.useEmployeeLookup {
                lookedUpUsernames += it
                "fakeKeycloakUserId"
            }

            `when`("the employee is resolved twice") {
                val keycloakUserIds = List(2) { EmployeeKeycloakUserIds.resolve(Employee("fakeUsername")) }

                then("both return the found ID, and the lookup runs only once") {
                    keycloakUserIds shouldBe listOf("fakeKeycloakUserId", "fakeKeycloakUserId")
                    lookedUpUsernames shouldBe listOf("fakeUsername")
                }
            }
        }

        given("an employee whom the lookup does not find") {
            var lookupCount = 0
            EmployeeKeycloakUserIds.useEmployeeLookup {
                lookupCount++
                null
            }

            `when`("the employee is resolved, again within 5 minutes, and again after 5 minutes") {
                EmployeeKeycloakUserIds.clock = clockAt(startInstant)
                val firstKeycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))
                EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofMinutes(4)))
                val secondKeycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))
                val lookupCountWithinFiveMinutes = lookupCount
                EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofMinutes(5)))
                EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("it is unknown, not looked up again within 5 minutes, and looked up again after them") {
                    firstKeycloakUserId shouldBe UNKNOWN_EMPLOYEE
                    secondKeycloakUserId shouldBe UNKNOWN_EMPLOYEE
                    lookupCountWithinFiveMinutes shouldBe 1
                    lookupCount shouldBe 2
                }
            }
        }

        given("an employee who has not logged in and whom a slow lookup finds") {
            val lookupCount = AtomicInteger()
            val lookupStarted = CountDownLatch(1)
            val lookupMayFinish = CountDownLatch(1)
            EmployeeKeycloakUserIds.useEmployeeLookup {
                lookupCount.incrementAndGet()
                lookupStarted.countDown()
                lookupMayFinish.await(5, TimeUnit.SECONDS)
                "fakeKeycloakUserId"
            }

            `when`("two threads resolve the employee at the same time") {
                val keycloakUserIds = CopyOnWriteArrayList<String>()
                fun resolvingThread() = Thread { keycloakUserIds += EmployeeKeycloakUserIds.resolve(Employee("fakeUsername")) }
                val firstThread = resolvingThread().apply { start() }
                lookupStarted.await(5, TimeUnit.SECONDS)
                val secondThread = resolvingThread().apply { start() }
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (secondThread.state != Thread.State.BLOCKED && lookupCount.get() < 2 && System.nanoTime() < deadline) {
                    Thread.onSpinWait()
                }
                lookupMayFinish.countDown()
                firstThread.join(5_000)
                secondThread.join(5_000)

                then("both threads get the found ID, and Keycloak is called only once") {
                    keycloakUserIds shouldBe listOf("fakeKeycloakUserId", "fakeKeycloakUserId")
                    lookupCount.get() shouldBe 1
                }
            }
        }

        given("a lookup that throws, as when Keycloak cannot be reached") {
            EmployeeKeycloakUserIds.useEmployeeLookup { throw IllegalStateException("fakeKeycloakError") }

            `when`("the employee is resolved") {
                val keycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("the employee is unknown and nothing is thrown to the caller") {
                    keycloakUserId shouldBe UNKNOWN_EMPLOYEE
                }
            }
        }

        given("a lookup that resolves another employee while it runs") {
            val lookedUpUsernames = mutableListOf<String>()
            var innerKeycloakUserId: String? = null
            EmployeeKeycloakUserIds.useEmployeeLookup { username ->
                lookedUpUsernames += username
                if (username == "fakeUsername") {
                    innerKeycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeOtherUsername"))
                }
                "fakeKeycloakUserId"
            }

            `when`("the employee is resolved") {
                val keycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("the other employee is unknown without a second lookup") {
                    keycloakUserId shouldBe "fakeKeycloakUserId"
                    innerKeycloakUserId shouldBe UNKNOWN_EMPLOYEE
                    lookedUpUsernames shouldBe listOf("fakeUsername")
                }
            }
        }
    }
})
