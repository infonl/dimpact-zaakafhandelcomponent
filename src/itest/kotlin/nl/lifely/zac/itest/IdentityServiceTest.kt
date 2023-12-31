/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.config.ItestConfiguration

class IdentityServiceTest : BehaviorSpec() {
    companion object {
        const val TEST_GROUP_A_ID = "test-group-a"
        const val TEST_GROUP_A_DESCRIPTION = "Test groep A"
        const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-functioneel-beheerders"
        const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test groep functioneel beheerders"
    }

    init {
        given(
            "Keycloak contains 'test group a' and 'test group functional beheerders'"
        ) {
            When("the 'list groups' endpoint is called") {
                then(
                    "'test group a' and 'test group functional beheerders' are returned"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/groups",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        )
                    ).apply {
                        statusCode shouldBe HttpStatus.SC_OK
                        text shouldEqualJson """
                            [
                                {
                                    "id": "$TEST_GROUP_A_ID",
                                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                                },
                                  {
                                    "id": "$TEST_GROUP_FUNCTIONAL_ADMINS_ID",
                                    "naam": "$TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION"
                                }
                            ]
                        """.trimIndent()
                    }
                }
            }
        }
        given(
            "Keycloak contains 'test user 1' and 'test user 2'"
        ) {
            When("the 'list users' endpoint is called") {
                then(
                    "'test user 1' and 'test user 2' are returned"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/users",
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
        given(
            "Keycloak contains 'test group a' with 'test user 1' and 'test user 2' as members"
        ) {
            When("the 'list users in group' endpoint is called for 'test group a'") {
                then(
                    "'testuser 1' and 'testuser 2' are returned"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/groups/$TEST_GROUP_A_ID/users",
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
        given(
            "Keycloak contains groups and users as group members"
        ) {
            When(
                "the 'list users in group' endpoint is called with an illegal LDAP filter character using group name '*'"
            ) {
                then(
                    "an empty list is returned"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/groups/*/users",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        )
                    ).apply {
                        statusCode shouldBe HttpStatus.SC_OK
                        text shouldEqualJson """
                            [
                        ]
                        """.trimIndent()
                    }
                }
            }
        }
        given(
            "'test user 1' is logged in to ZAC and is part of two groups"
        ) {
            When("the 'get logged in user' endpoint is called") {
                then(
                    "both groups are returned"
                ) {
                    khttp.get(
                        url = "${ItestConfiguration.ZAC_API_URI}/identity/loggedInUser",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        )
                    ).apply {
                        statusCode shouldBe HttpStatus.SC_OK
                        text shouldEqualSpecifiedJsonIgnoringOrder """
                            {
                                "id": "testuser1",
                                "naam": "Test User1",
                                "groupIds": [
                                    "$TEST_GROUP_A_ID",
                                    "$TEST_GROUP_FUNCTIONAL_ADMINS_ID"
                                ]
                            }
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
