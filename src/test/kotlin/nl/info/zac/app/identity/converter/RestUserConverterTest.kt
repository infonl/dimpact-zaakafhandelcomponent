/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser

class RestUserConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val restUserConverter = RestUserConverter(identityService)

    afterEach {
        checkUnnecessaryStub()
    }

    Context("convertUserId") {
        Given("a user ID that exists in the identity service") {
            val user = createUser(id = "fakeUserId", fullName = "fakeFullName")
            every { identityService.readUser("fakeUserId") } returns user

            When("convertUserId is called") {
                val result = restUserConverter.convertUserId("fakeUserId")

                Then("it returns a RestUser with the correct id and full name") {
                    result.id shouldBe "fakeUserId"
                    result.naam shouldBe "fakeFullName"
                }
            }
        }
    }

    Context("convertUserIds") {
        Given("a list of user IDs") {
            val user1 = createUser(id = "fakeUserId1", fullName = "fakeFullName1")
            val user2 = createUser(id = "fakeUserId2", fullName = "fakeFullName2")
            every { identityService.readUser("fakeUserId1") } returns user1
            every { identityService.readUser("fakeUserId2") } returns user2

            When("convertUserIds is called with the list") {
                val result = with(restUserConverter) {
                    listOf("fakeUserId1", "fakeUserId2").convertUserIds()
                }

                Then("it returns a list of RestUsers with correct ids and names") {
                    result.size shouldBe 2
                    result[0].id shouldBe "fakeUserId1"
                    result[0].naam shouldBe "fakeFullName1"
                    result[1].id shouldBe "fakeUserId2"
                    result[1].naam shouldBe "fakeFullName2"
                }
            }
        }
    }
})
