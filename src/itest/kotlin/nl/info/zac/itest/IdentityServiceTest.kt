/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_COORDINATOR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_COORDINATOR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_FUNCTIONAL_ADMIN_1_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_FUNCTIONAL_ADMIN_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUPS_ALL
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_DOMEIN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_DOMEIN_TEST_1_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_FUNCTIONAL_ADMINS_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_RAADPLEGER_1_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_RAADPLEGER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_RECORD_MANAGER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_RECORD_MANAGER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_DOMEIN_TEST_1_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_DOMEIN_TEST_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI

class IdentityServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("The ZAC Keycloak realm contains several groups") {
        When("the 'list groups' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups"
            )
            Then(
                "all groups are returned"
            ) {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualSpecifiedJsonIgnoringOrder TEST_GROUPS_ALL.trimIndent()
            }
        }
    }
    Given(
        """
            Groups in the Keycloak ZAC realm with a Keycloak role which is also configured in the 
            zaakafhandelparameters for a given zaaktype UUID
        """.trimIndent()
    ) {
        When("the 'list groups for a zaaktype' endpoint is called for this zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/zaaktype/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
            )
            Then(
                "only those groups which have the domain role are returned"
            ) {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualSpecifiedJson """
                            [                               
                                {
                                    "id": "$TEST_GROUP_DOMEIN_TEST_1_ID",
                                    "naam": "$TEST_GROUP_DOMEIN_TEST_1_DESCRIPTION"
                                }
                            ]
                """.trimIndent()
            }
        }
    }
    Given(
        """
            Groups in the Keycloak ZAC realm and a zaaktype UUID which is not configured in any
            zaakafhandelparameters for a given domein role
        """.trimIndent()
    ) {
        When("the 'list groups for a zaaktype' endpoint is called for this zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/zaaktype/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
            )
            Then(
                "all groups are returned"
            ) {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualSpecifiedJsonIgnoringOrder TEST_GROUPS_ALL.trimIndent()
            }
        }
    }
    Given("Keycloak contains all provisioned test users") {
        When("the 'list users' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/users"
            )
            Then("All 8 specific users are returned") {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualSpecifiedJsonIgnoringOrder """
                            [
                                {
                                    "id": "$TEST_FUNCTIONAL_ADMIN_1_ID",
                                    "naam": "$TEST_FUNCTIONAL_ADMIN_1_NAME"
                                },
                                {
                                    "id": "$TEST_RECORD_MANAGER_1_USERNAME",
                                    "naam": "$TEST_RECORD_MANAGER_1_NAME"
                                },
                                {
                                    "id": "$TEST_COORDINATOR_1_USERNAME",
                                    "naam": "$TEST_COORDINATOR_1_NAME"
                                },
                                {
                                    "id": "$TEST_BEHANDELAAR_1_USERNAME",
                                    "naam": "$TEST_BEHANDELAAR_1_NAME"
                                },
                                {
                                    "id": "$TEST_RAADPLEGER_1_ID",
                                    "naam": "$TEST_RAADPLEGER_1_NAME"
                                },
                                {
                                    "id": "$TEST_USER_1_USERNAME",
                                    "naam": "$TEST_USER_1_NAME"
                                },                          
                                {
                                    "id": "$TEST_USER_2_ID",
                                    "naam": "$TEST_USER_2_NAME"
                                },
                                                                {
                                    "id": "$TEST_USER_DOMEIN_TEST_1_ID",
                                    "naam": "$TEST_USER_DOMEIN_TEST_1_NAME"
                                }
                            ]
                """.trimIndent()
            }
        }
    }
    Given("Keycloak contains 'test group a' with 'test user 1' and 'test user 2' as members") {
        When("the 'list users in group' endpoint is called for 'test group a'") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/$TEST_GROUP_A_ID/users"
            )
            Then("'testuser 1' and 'testuser 2' are returned") {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualJson """
                        [
                            {
                                "id": "$TEST_USER_1_USERNAME",
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
    Given("'test user 1' is logged in to ZAC and is part of two groups") {
        When("the 'get logged in user' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/loggedInUser"
            )
            Then("both groups are returned") {
                response.isSuccessful shouldBe true
                response.body.string() shouldEqualSpecifiedJsonIgnoringOrder """
                            {
                                "id": "$TEST_USER_1_USERNAME",
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
})
