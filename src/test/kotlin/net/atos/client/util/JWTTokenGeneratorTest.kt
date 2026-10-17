/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.util

import com.auth0.jwt.JWT
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.model.getFullName

class JWTTokenGeneratorTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("client id and secret and no logged-in user") {
        val clientId = "clientId"
        val clientSecret = "clientSecret"

        When("token is generated") {
            val token = JWTTokenGenerator.generate(clientId, clientSecret, null)

            Then("it should be a valid JWT token") {
                val tokenParts = token.split(" ")
                tokenParts[0] shouldBe "Bearer"
                val decodedToken = JWT.decode(tokenParts[1])
                with(decodedToken) {
                    issuer shouldBe clientId
                    getHeaderClaim("client_identifier").asString() shouldBe clientId
                }
            }
        }
    }

    Given("client credentials and logged-in user") {
        val clientId = "clientId"
        val clientSecret = "clientSecret"
        val loggedInUser = mockk<LoggedInUser>()

        every { loggedInUser.id } returns "id"
        mockkStatic(LoggedInUser::getFullName)
        every { loggedInUser.getFullName() } returns "fullName"

        When("token is generated") {
            val token = JWTTokenGenerator.generate(clientId, clientSecret, loggedInUser)

            Then("it should be a valid JWT token") {
                val tokenParts = token.split(" ")
                tokenParts[0] shouldBe "Bearer"
                val decodedToken = JWT.decode(tokenParts[1])
                with(decodedToken) {
                    issuer shouldBe clientId
                    getHeaderClaim("client_identifier").asString() shouldBe clientId
                    getClaim("user_id").asString() shouldBe "id"
                    getClaim("user_representation").asString() shouldBe "fullName"
                }
            }
        }
    }
})
