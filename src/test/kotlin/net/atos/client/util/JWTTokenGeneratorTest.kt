/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.util

import com.auth0.jwt.JWT
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class JWTTokenGeneratorTest : BehaviorSpec({

    Given("client id and secret and no logged-in user") {

        When("token is generated") {
            val token = JWTTokenGenerator.generate("clientId", "secret", null)

            Then("it should be a valid JWT token") {
                println(token)
                val tokenParts = token.split(" ")
                tokenParts[0] shouldBe "Bearer"
                val decodedToken = JWT.decode(tokenParts[1])
                with(decodedToken) {
                    issuer shouldBe "clientId"
                    getHeaderClaim("client_identifier").asString() shouldBe "clientId"
                }
            }
        }
    }
})
