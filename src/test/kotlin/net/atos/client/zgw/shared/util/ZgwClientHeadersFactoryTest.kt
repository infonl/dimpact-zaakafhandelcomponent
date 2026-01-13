/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser

class ZgwClientHeadersFactoryTest: BehaviorSpec ({
    val zgwClientId = "fakeZgwClientId"
    val zgwApiSecret = "fakeZgwApiSecret"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val zgwClientHeadersFactory = ZgwClientHeadersFactory(
        loggedInUserInstance,
        zgwClientId,
        zgwApiSecret
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Updating headers for ZGW client") {
        Given("A logged in user is available") {
            val loggedInUser = createLoggedInUser()
            val incomingHeaders = MultivaluedHashMap<String, String>()
            every { loggedInUserInstance.get() } returns loggedInUser

            When("update is called without an audit explanation set") {
                val outgoingHeaders = MultivaluedHashMap<String, String>()
                zgwClientHeadersFactory.update(incomingHeaders, outgoingHeaders)

                Then("it should add an Authorization header starting with Bearer and no X-Audit-Toelichting header") {
                    val authorizationHeader = outgoingHeaders.getFirst(HttpHeaders.AUTHORIZATION)
                    with(authorizationHeader) {
                        this shouldNotBe null
                        startsWith("Bearer ") shouldBe true
                    }
                    outgoingHeaders.containsKey("X-Audit-Toelichting") shouldBe false
                }

                And("the incoming headers should remain unchanged") {
                    incomingHeaders.isEmpty() shouldBe true
                }
            }

            When("an audit explanation is set and update is called twice") {
                val auditExplanation = "fakeAuditExplanation"
                val outgoingHeadersFirst = MultivaluedHashMap<String, String>()

                zgwClientHeadersFactory.setAuditExplanation(auditExplanation)
                zgwClientHeadersFactory.update(incomingHeaders, outgoingHeadersFirst)

                Then("the first update should include the X-Audit-Toelichting header") {
                    outgoingHeadersFirst.getFirst("X-Audit-Toelichting") shouldBe auditExplanation
                }

                And("the incoming headers should remain unchanged") {
                    incomingHeaders.isEmpty() shouldBe true
                }

                When("update is called again after the first call") {
                    val incomingHeaders = MultivaluedHashMap<String, String>()
                    val outgoingHeadersSecond = MultivaluedHashMap<String, String>()
                    zgwClientHeadersFactory.update(incomingHeaders, outgoingHeadersSecond)

                    Then("the second update should not include the X-Audit-Toelichting header because it was cleared") {
                        outgoingHeadersSecond.containsKey("X-Audit-Toelichting") shouldBe false
                    }
                }
            }
        }
    }
})
