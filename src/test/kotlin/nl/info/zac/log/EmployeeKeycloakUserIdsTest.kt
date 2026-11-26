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

private fun internalMapSize(fieldName: String) =
    EmployeeKeycloakUserIds::class.java.getDeclaredField(fieldName)
        .apply { isAccessible = true }
        .get(EmployeeKeycloakUserIds)
        .let { it as Map<*, *> }
        .size

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

            `when`("the employee is resolved, again within 1 minute, and again after 1 minute") {
                EmployeeKeycloakUserIds.clock = clockAt(startInstant)
                val firstKeycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))
                EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofSeconds(59)))
                val secondKeycloakUserId = EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))
                val lookupCountWithinOneMinute = lookupCount
                EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofMinutes(1)))
                EmployeeKeycloakUserIds.resolve(Employee("fakeUsername"))

                then("it is unknown, not looked up again within 1 minute, and looked up again after it") {
                    firstKeycloakUserId shouldBe UNKNOWN_EMPLOYEE
                    secondKeycloakUserId shouldBe UNKNOWN_EMPLOYEE
                    lookupCountWithinOneMinute shouldBe 1
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

        given("a lookup that finds none of many unknown usernames") {
            EmployeeKeycloakUserIds.useEmployeeLookup { null }

            `when`("each username is resolved, and after 1 minute one more username is resolved") {
                EmployeeKeycloakUserIds.clock = clockAt(startInstant)
                repeat(100) { EmployeeKeycloakUserIds.resolve(Employee("fakeUnknownUsername$it")) }
                val missedLookupCountBefore = internalMapSize("missedLookups")
                EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofMinutes(2)))
                EmployeeKeycloakUserIds.resolve(Employee("fakeOtherUnknownUsername"))
                val missedLookupCountAfter = internalMapSize("missedLookups")
                val lookupLockCount = internalMapSize("lookupLocks")

                then(
                    "the expired misses are removed and no lookup lock is kept, so that unknown usernames do not " +
                        "pile up in memory"
                ) {
                    missedLookupCountBefore shouldBe 100
                    missedLookupCountAfter shouldBe 1
                    lookupLockCount shouldBe 0
                }
            }
        }

        given("a lookup that throws, as when Keycloak cannot be reached") {
            var lookupCount = 0
            EmployeeKeycloakUserIds.useEmployeeLookup {
                lookupCount++
                error("fakeKeycloakError")
            }

            `when`(
                "an employee is resolved, two other employees within 1 minute, and one more employee after 1 minute"
            ) {
                lateinit var keycloakUserIds: List<String>
                var lookupCountWithinOneMinute = 0
                val records = LogCapture(EmployeeKeycloakUserIds::class).use { logCapture ->
                    EmployeeKeycloakUserIds.clock = clockAt(startInstant)
                    keycloakUserIds = listOf("fakeUsername1", "fakeUsername2").map {
                        EmployeeKeycloakUserIds.resolve(Employee(it))
                    } + EmployeeKeycloakUserIds.also {
                        it.clock = clockAt(startInstant.plus(Duration.ofSeconds(59)))
                    }.resolve(Employee("fakeUsername3"))
                    lookupCountWithinOneMinute = lookupCount
                    EmployeeKeycloakUserIds.clock = clockAt(startInstant.plus(Duration.ofMinutes(1)))
                    EmployeeKeycloakUserIds.resolve(Employee("fakeUsername4"))
                    logCapture.records
                }

                then("every employee is unknown and nothing is thrown to the caller") {
                    keycloakUserIds shouldBe List(3) { UNKNOWN_EMPLOYEE }
                }

                then("Keycloak is not called for any employee within 1 minute after the failure, and again after it") {
                    lookupCountWithinOneMinute shouldBe 1
                    lookupCount shouldBe 2
                }

                then("one warning is logged per failure after the minute, not one per employee") {
                    records.map { it.message } shouldBe List(2) {
                        KeycloakUnreachableForEmployeeLookup.description
                    }
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
