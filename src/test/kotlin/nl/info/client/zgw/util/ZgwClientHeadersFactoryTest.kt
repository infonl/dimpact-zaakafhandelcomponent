/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import java.util.concurrent.TimeUnit.SECONDS
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.authentication.createLoggedInUser
import java.util.concurrent.CountDownLatch

private const val LATCH_TIMEOUT_SECONDS = 5L

class ZgwClientHeadersFactoryTest : BehaviorSpec({
    val zgwClientId = "fakeZgwClientId"
    val zgwApiSecret = "fakeZgwApiSecret"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val zgwClientHeadersFactory = ZgwClientHeadersFactory(
        loggedInUserInstance,
        zgwClientId,
        zgwApiSecret
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Updating headers for ZGW client") {
        given("A logged in user is available") {
            val loggedInUser = createLoggedInUser()
            val incomingHeaders = MultivaluedHashMap<String, String>()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("update is called without an audit explanation set") {
                val outgoingHeaders = MultivaluedHashMap<String, String>()
                zgwClientHeadersFactory.update(incomingHeaders, outgoingHeaders)

                then("it should add an Authorization header starting with Bearer and no X-Audit-Toelichting header") {
                    val authorizationHeader = outgoingHeaders.getFirst(HttpHeaders.AUTHORIZATION)
                    with(authorizationHeader) {
                        this shouldNotBe null
                        startsWith("Bearer ") shouldBe true
                    }
                    outgoingHeaders.containsKey("X-Audit-Toelichting") shouldBe false
                }

                and("the incoming headers should remain unchanged") {
                    incomingHeaders.isEmpty() shouldBe true
                }
            }

            `when`("an audit explanation is set and update is called twice") {
                val auditExplanation = "fakeAuditExplanation"
                val outgoingHeadersFirst = MultivaluedHashMap<String, String>()

                zgwClientHeadersFactory.withAuditExplanation(auditExplanation) {
                    zgwClientHeadersFactory.update(incomingHeaders, outgoingHeadersFirst)
                }

                then("the first update should include the X-Audit-Toelichting header") {
                    outgoingHeadersFirst.getFirst("X-Audit-Toelichting") shouldBe auditExplanation
                }

                and("the incoming headers should remain unchanged") {
                    incomingHeaders.isEmpty() shouldBe true
                }

                and("update is called again after the first call") {
                    val incomingHeaders = MultivaluedHashMap<String, String>()
                    val outgoingHeadersSecond = MultivaluedHashMap<String, String>()
                    zgwClientHeadersFactory.update(incomingHeaders, outgoingHeadersSecond)

                    then("the second update should not include the X-Audit-Toelichting header because it was cleared") {
                        outgoingHeadersSecond.containsKey("X-Audit-Toelichting") shouldBe false
                    }
                }
            }
        }

        given("two background operations that both run as the functionele gebruiker") {
            every { loggedInUserInstance.get() } returns LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER

            `when`("both set their own audit explanation before either sends its request") {
                val firstOutgoingHeaders = MultivaluedHashMap<String, String>()
                val secondOutgoingHeaders = MultivaluedHashMap<String, String>()
                val firstExplanationSet = CountDownLatch(1)
                val secondExplanationSet = CountDownLatch(1)

                var isSecondExplanationSetInTime = false
                var isFirstExplanationSetInTime = false

                val firstOperation = Thread {
                    zgwClientHeadersFactory.withAuditExplanation("fakeFirstAuditExplanation") {
                        firstExplanationSet.countDown()
                        isSecondExplanationSetInTime = secondExplanationSet.await(LATCH_TIMEOUT_SECONDS, SECONDS)
                        zgwClientHeadersFactory.update(MultivaluedHashMap(), firstOutgoingHeaders)
                    }
                }
                val secondOperation = Thread {
                    isFirstExplanationSetInTime = firstExplanationSet.await(LATCH_TIMEOUT_SECONDS, SECONDS)
                    zgwClientHeadersFactory.withAuditExplanation("fakeSecondAuditExplanation") {
                        secondExplanationSet.countDown()
                        zgwClientHeadersFactory.update(MultivaluedHashMap(), secondOutgoingHeaders)
                    }
                }
                firstOperation.start()
                secondOperation.start()
                firstOperation.join()
                secondOperation.join()

                then("both operations reach each other in time instead of hanging the build") {
                    isFirstExplanationSetInTime shouldBe true
                    isSecondExplanationSetInTime shouldBe true
                }

                and("each request carries its own audit explanation") {
                    firstOutgoingHeaders.getFirst("X-Audit-Toelichting") shouldBe "fakeFirstAuditExplanation"
                    secondOutgoingHeaders.getFirst("X-Audit-Toelichting") shouldBe "fakeSecondAuditExplanation"
                }
            }
        }

        given("work that sets an audit explanation and then fails before its request is sent") {
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("a later request is made on the same thread") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    zgwClientHeadersFactory.withAuditExplanation("fakeLeakedAuditExplanation") {
                        throw IllegalStateException("fakeFailureBeforeTheRequest")
                    }
                }
                val outgoingHeaders = MultivaluedHashMap<String, String>()
                zgwClientHeadersFactory.update(MultivaluedHashMap(), outgoingHeaders)

                then("that request does not carry the explanation of the failed work") {
                    illegalStateException.message shouldBe "fakeFailureBeforeTheRequest"
                    outgoingHeaders.containsKey("X-Audit-Toelichting") shouldBe false
                }
            }
        }
    }
})
