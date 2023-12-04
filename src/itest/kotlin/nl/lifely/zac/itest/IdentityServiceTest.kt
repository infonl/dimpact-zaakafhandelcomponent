/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.config.ItestConfiguration

class IdentityServiceTest : BehaviorSpec() {
    companion object {
        const val TEST_GROUP_A = "test-group-a"
    }

    init {
        given(
            "Keycloak contains 'test group a' with 'test user 1' and 'test user 2' as members"
        ) {
            When("the 'list users in group' endpoint is called for 'test group a'") {
                then(
                    "'testuser 1' and 'testuser 2'"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/groups/$TEST_GROUP_A/users",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        )
                    ).apply {
                        statusCode shouldBe HttpStatus.SC_OK
                        text shouldEqualJson """
                            [
                                {
                                    "id": "testuser1",
                                    "naam": "Test User1"
                                },
                                {
                                    "id": "testuser2",
                                    "naam": "Test User2"
                                }
                            ]
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
