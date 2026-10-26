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

    context("convertUsername") {
        given("a username that exists in the identity service") {
            val user = createUser(username = "fakeUsername", fullName = "fakeFullName")
            every { identityService.readUser("fakeUsername") } returns user

            `when`("convertUsername is called") {
                val result = restUserConverter.convertUsername("fakeUsername")

                then("it returns a RestUser with the correct id and full name") {
                    result.username shouldBe "fakeUsername"
                    result.naam shouldBe "fakeFullName"
                }
            }
        }
    }

    context("convertUsernames") {
        given("a list of usernames") {
            val user1 = createUser(username = "fakeUsername1", fullName = "fakeFullName1")
            val user2 = createUser(username = "fakeUsername2", fullName = "fakeFullName2")
            every { identityService.readUser("fakeUsername1") } returns user1
            every { identityService.readUser("fakeUsername2") } returns user2

            `when`("convertUsernames is called with the list") {
                val result = with(restUserConverter) {
                    listOf("fakeUsername1", "fakeUsername2").convertUsernames()
                }

                then("it returns a list of RestUsers with correct ids and names") {
                    result.size shouldBe 2
                    result[0].username shouldBe "fakeUsername1"
                    result[0].naam shouldBe "fakeFullName1"
                    result[1].username shouldBe "fakeUsername2"
                    result[1].naam shouldBe "fakeFullName2"
                }
            }
        }
    }
})
