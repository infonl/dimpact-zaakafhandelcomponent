/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser

class DocumentCreationUserStoreTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val documentCreationUserStore = DocumentCreationUserStore(loggedInUserInstance)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Recovering the user that started a document creation") {
        given("a token created while the user session was still in scope") {
            val loggedInUser = createLoggedInUser()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("the token is exchanged for a user") {
                val token = documentCreationUserStore.createToken()
                val result = documentCreationUserStore.findUser(token)

                then("it returns the user that started the document creation") {
                    result shouldBe loggedInUser
                }
            }
        }

        given("two document creations started by different users") {
            val firstUser = createLoggedInUser(id = "fakeUserId1")
            val secondUser = createLoggedInUser(id = "fakeUserId2")

            `when`("both tokens are exchanged for a user") {
                every { loggedInUserInstance.get() } returns firstUser
                val firstToken = documentCreationUserStore.createToken()
                every { loggedInUserInstance.get() } returns secondUser
                val secondToken = documentCreationUserStore.createToken()

                then("each token returns the user that started that document creation") {
                    firstToken shouldNotBe secondToken
                    documentCreationUserStore.findUser(firstToken) shouldBe firstUser
                    documentCreationUserStore.findUser(secondToken) shouldBe secondUser
                }
            }
        }

        given("a token that was never created") {
            `when`("the token is exchanged for a user") {
                val result = documentCreationUserStore.findUser("fakeUnknownToken")

                then("it returns null, so the callback can fall back to the functionele gebruiker") {
                    result shouldBe null
                }
            }
        }
    }
})
