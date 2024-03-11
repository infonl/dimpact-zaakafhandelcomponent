/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration

class IdentityServiceTest : BehaviorSpec() {
    companion object {
        const val TEST_USER_1_ID = "testuser1"
        const val TEST_USER_1_NAME = "Test User1"
        const val TEST_USER_2_ID = "testuser2"
        const val TEST_USER_2_NAME = "Test User2"
        const val TEST_RECORD_MANAGER_1_ID = "recordmanager1"
        const val TEST_RECORD_MANAGER_1_NAME = "Test Recordmanager1"
        const val TEST_FUNCTIONAL_ADMIN_1_ID = "functioneelbeheerder1"
        const val TEST_FUNCTIONAL_ADMIN_1_NAME = "Test Functioneelbeheerder1"
        const val TEST_GROUP_A_ID = "test-group-a"
        const val TEST_GROUP_A_DESCRIPTION = "Test group A"
        const val TEST_GROUP_FUNCTIONAL_ADMINS_ID = "test-group-fb"
        const val TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION = "Test group functional admins"
        const val TEST_GROUP_RECORD_MANAGERS_ID = "test-group-rm"
        const val TEST_GROUP_RECORD_MANAGERS_DESCRIPTION = "Test group record managers"
    }

    private val itestHttpClient = ItestHttpClient()

    init {
        Given(
            "Keycloak contains 'test group a' and 'test group functional beheerders'"
        ) {
            When("the 'list groups' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/identity/groups"
                )
                Then(
                    "'test group a' and 'test group functional beheerders' are returned"
                ) {
                    response.isSuccessful shouldBe true
                    response.body!!.string() shouldEqualJson """
                            [
                                {
                                    "id": "$TEST_GROUP_A_ID",
                                    "naam": "$TEST_GROUP_A_DESCRIPTION"
                                },
                                {
                                    "id": "$TEST_GROUP_FUNCTIONAL_ADMINS_ID",
                                    "naam": "$TEST_GROUP_FUNCTIONAL_ADMINS_DESCRIPTION"
                                },
                                {
                                    "id": "$TEST_GROUP_RECORD_MANAGERS_ID",
                                    "naam": "$TEST_GROUP_RECORD_MANAGERS_DESCRIPTION"
                                }
                            ]
                    """.trimIndent()
                }
            }
        }
        Given(
            "Keycloak contains all provisioned test users"
        ) {
            When("the 'list users' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/identity/users"
                )
                Then(
                    "'test user 1' and 'test user 2' are returned"
                ) {
                    response.isSuccessful shouldBe true
                    response.body!!.string() shouldEqualJson """
                            [
                                {
                                    "id": "$TEST_FUNCTIONAL_ADMIN_1_ID",
                                    "naam": "$TEST_FUNCTIONAL_ADMIN_1_NAME"
                                },
                                {
                                    "id": "$TEST_RECORD_MANAGER_1_ID",
                                    "naam": "$TEST_RECORD_MANAGER_1_NAME"
                                },
                                {
                                    "id": "$TEST_USER_1_ID",
                                    "naam": "$TEST_USER_1_NAME"
                                },
                                {
                                    "id": "$TEST_USER_2_ID",
                                    "naam": "$TEST_USER_2_NAME"
                                }
                            ]
                    """.trimIndent()
                }
            }
        }
        Given(
            "Keycloak contains 'test group a' with 'test user 1' and 'test user 2' as members"
        ) {
            When("the 'list users in group' endpoint is called for 'test group a'") {
                val response = itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/identity/groups/$TEST_GROUP_A_ID/users"
                )
                Then(
                    "'testuser 1' and 'testuser 2' are returned"
                ) {
                    response.isSuccessful shouldBe true
                    response.body!!.string() shouldEqualJson """
                        [
                            {
                                "id": "$TEST_USER_1_ID",
                                "naam": "$TEST_USER_1_NAME"
                            },
                            {
                                "id": "$TEST_USER_2_ID",
                                "naam": "$TEST_USER_2_NAME"
                            }
                        ]
                    """.trimIndent()
                }
            }
        }
        Given(
            "Keycloak contains groups and users as group members"
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "${ItestConfiguration.ZAC_API_URI}/identity/groups/*/users"
            )
            When(
                "the 'list users in group' endpoint is called with an illegal LDAP filter character using group name '*'"
            ) {
                Then(
                    "an empty list is returned"
                ) {
                    response.isSuccessful shouldBe true
                    response.body!!.string() shouldEqualJson """
                            []
                    """.trimIndent()
                }
            }
        }
        Given(
            "'test user 1' is logged in to ZAC and is part of two groups"
        ) {
            When("the 'get logged in user' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/identity/loggedInUser"
                )
                Then(
                    "both groups are returned"
                ) {
                    response.isSuccessful shouldBe true
                    response.body!!.string() shouldEqualSpecifiedJsonIgnoringOrder """
                            {
                                "id": "$TEST_USER_1_ID",
                                "naam": "$TEST_USER_1_NAME",
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
