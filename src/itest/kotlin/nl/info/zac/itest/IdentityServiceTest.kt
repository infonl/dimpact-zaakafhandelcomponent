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
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_2
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.COORDINATOR_2
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_2
import nl.info.zac.itest.config.GROUP_BEHEERDERS_ELK_DOMEIN
import nl.info.zac.itest.config.GROUP_COORDINATORS_TEST_1
import nl.info.zac.itest.config.GROUP_COORDINATORS_TEST_2
import nl.info.zac.itest.config.GROUP_RAADPLEGERS_TEST_1
import nl.info.zac.itest.config.GROUP_RAADPLEGERS_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.OLD_IAM_BEHANDELAAR_1
import nl.info.zac.itest.config.OLD_IAM_COORDINATOR_1
import nl.info.zac.itest.config.OLD_IAM_FUNCTIONAL_ADMIN_1
import nl.info.zac.itest.config.OLD_IAM_GROUP_DOMEIN_TEST_1
import nl.info.zac.itest.config.OLD_IAM_GROUP_DOMEIN_TEST_2
import nl.info.zac.itest.config.OLD_IAM_RAADPLEGER_1
import nl.info.zac.itest.config.OLD_IAM_RECORD_MANAGER_1
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_A
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_BEHANDELAARS
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_COORDINATORS
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_RAADPLEGERS
import nl.info.zac.itest.config.OLD_IAM_TEST_GROUP_RECORD_MANAGERS
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_1
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_2
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_DOMEIN_TEST_1
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_DOMEIN_TEST_2
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.config.RAADPLEGER_2
import nl.info.zac.itest.config.RAADPLEGER_EN_BEHANDELAAR_1
import nl.info.zac.itest.config.USER_WITHOUT_ANY_ROLE
import java.net.HttpURLConnection.HTTP_OK

val TEST_GROUPS_ALL =
    """
            [
                {
                    "id": "${OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS.description}"
                },
                {
                    "id": "${OLD_IAM_TEST_GROUP_RECORD_MANAGERS.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_RECORD_MANAGERS.description}"
                },
                {
                    "id": "${OLD_IAM_TEST_GROUP_COORDINATORS.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_COORDINATORS.description}"
                },
                {
                    "id": "${OLD_IAM_TEST_GROUP_BEHANDELAARS.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_BEHANDELAARS.description}"
                },
                {
                    "id": "${OLD_IAM_TEST_GROUP_RAADPLEGERS.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_RAADPLEGERS.description}"
                },
                {
                    "id": "${OLD_IAM_TEST_GROUP_A.name}",
                    "naam": "${OLD_IAM_TEST_GROUP_A.description}"
                },
                {
                    "id": "${OLD_IAM_GROUP_DOMEIN_TEST_1.name}",
                    "naam": "${OLD_IAM_GROUP_DOMEIN_TEST_1.description}"
                },
                {
                    "id": "${OLD_IAM_GROUP_DOMEIN_TEST_2.name}",
                    "naam": "${OLD_IAM_GROUP_DOMEIN_TEST_2.description}"
                },
                {
                    "id": "${GROUP_RAADPLEGERS_TEST_1.name}",
                    "naam": "${GROUP_RAADPLEGERS_TEST_1.description}"
                },
                {
                    "id": "${GROUP_RAADPLEGERS_TEST_2.name}",
                    "naam": "${GROUP_RAADPLEGERS_TEST_2.description}"
                },
                {
                    "id": "${GROUP_BEHANDELAARS_TEST_1.name}",
                    "naam": "${GROUP_BEHANDELAARS_TEST_1.description}"
                },
                {
                    "id": "${GROUP_BEHANDELAARS_TEST_2.name}",
                    "naam": "${GROUP_BEHANDELAARS_TEST_2.description}"
                },
                {
                    "id": "${GROUP_COORDINATORS_TEST_1.name}",
                    "naam": "${GROUP_COORDINATORS_TEST_1.description}"
                },
                {
                    "id": "${GROUP_COORDINATORS_TEST_2.name}",
                    "naam": "${GROUP_COORDINATORS_TEST_2.description}"
                },
                {
                    "id": "${GROUP_BEHEERDERS_ELK_DOMEIN.name}",
                    "naam": "${GROUP_BEHEERDERS_ELK_DOMEIN.description}"
                }
            ]
        """

class IdentityServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    beforeSpec {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
    }

    Given("The ZAC Keycloak realm contains several groups and a logged-in beheerder") {
        When("the 'list groups' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups"
            )
            Then(
                "all groups are returned"
            ) {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder TEST_GROUPS_ALL.trimIndent()
            }
        }
    }

    Given(
        """
            Groups in the Keycloak ZAC realm with a Keycloak role which is also configured in the 
            zaaktypeCmmnConfiguration for a given zaaktype UUID
        """.trimIndent()
    ) {
        When("the 'list groups for a zaaktype' endpoint is called for this zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/zaaktype/$ZAAKTYPE_TEST_2_UUID"
            )
            Then(
                "only those groups which have the domain role are returned"
            ) {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualSpecifiedJson """
                            [                               
                                {
                                    "id": "${OLD_IAM_GROUP_DOMEIN_TEST_1.name}",
                                    "naam": "${OLD_IAM_GROUP_DOMEIN_TEST_1.description}"
                                }
                            ]
                """.trimIndent()
            }
        }
    }
    Given(

        """
            Groups in the Keycloak ZAC realm and a zaaktype UUID which is not configured in any
            zaaktypeCmmnConfiguration for a given domein role
        """.trimIndent()
    ) {
        When("the 'list groups for a zaaktype' endpoint is called for this zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/zaaktype/$ZAAKTYPE_TEST_3_UUID"
            )
            Then(
                "all groups are returned"
            ) {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder TEST_GROUPS_ALL.trimIndent()
            }
        }
    }

    Given("Keycloak contains all provisioned test users") {
        When("the 'list users' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/users"
            )
            Then("All specific users are returned") {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder """
                            [
                                {
                                    "id": "${OLD_IAM_FUNCTIONAL_ADMIN_1.username}",
                                    "naam": "${OLD_IAM_FUNCTIONAL_ADMIN_1.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_RECORD_MANAGER_1.username}",
                                    "naam": "${OLD_IAM_RECORD_MANAGER_1.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_COORDINATOR_1.username}",
                                    "naam": "${OLD_IAM_COORDINATOR_1.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_BEHANDELAAR_1.username}",
                                    "naam": "${OLD_IAM_BEHANDELAAR_1.displayName}"
                                },                          
                                {
                                    "id": "${OLD_IAM_RAADPLEGER_1.username}",
                                    "naam": "${OLD_IAM_RAADPLEGER_1.displayName}"
                                },
                                {
                                    "id": "${RAADPLEGER_1.username}",
                                    "naam": "${RAADPLEGER_1.displayName}"
                                },
                                {
                                    "id": "${RAADPLEGER_2.username}",
                                    "naam": "${RAADPLEGER_2.displayName}"
                                },
                                {
                                    "id": "${BEHANDELAAR_1.username}",
                                    "naam": "${BEHANDELAAR_1.displayName}"
                                },
                                {
                                    "id": "${BEHANDELAAR_2.username}",
                                    "naam": "${BEHANDELAAR_2.displayName}"
                                },
                                {
                                    "id": "${COORDINATOR_1.username}",
                                    "naam": "${COORDINATOR_1.displayName}"
                                },
                                {
                                    "id": "${COORDINATOR_2.username}",
                                    "naam": "${COORDINATOR_2.displayName}"
                                },
                                {
                                    "id": "${BEHEERDER_1.username}",
                                    "naam": "${BEHEERDER_1.displayName}"
                                },
                                {
                                    "id": "${RAADPLEGER_EN_BEHANDELAAR_1.username}",
                                    "naam": "${RAADPLEGER_EN_BEHANDELAAR_1.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_TEST_USER_1.username}",
                                    "naam": "${OLD_IAM_TEST_USER_1.displayName}"
                                },                          
                                {
                                    "id": "${OLD_IAM_TEST_USER_2.username}",
                                    "naam": "${OLD_IAM_TEST_USER_2.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_TEST_USER_DOMEIN_TEST_1.username}",
                                    "naam": "${OLD_IAM_TEST_USER_DOMEIN_TEST_1.displayName}"
                                },
                                {
                                    "id": "${OLD_IAM_TEST_USER_DOMEIN_TEST_2.username}",
                                    "naam": "${OLD_IAM_TEST_USER_DOMEIN_TEST_2.displayName}"
                                },
                                {
                                    "id": "${USER_WITHOUT_ANY_ROLE.username}",
                                    "naam": "${USER_WITHOUT_ANY_ROLE.displayName}"
                                }
                            ]
                """.trimIndent()
            }
        }
    }

    Given("Keycloak contains 'test group a' with 'test user 1' and 'test user 2' as members") {
        When("the 'list users in group' endpoint is called for 'test group a'") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/groups/${OLD_IAM_TEST_GROUP_A.name}/users"
            )
            Then("'testuser 1' and 'testuser 2' are returned") {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualJson """
                        [
                            {
                                "id": "${OLD_IAM_TEST_USER_1.username}",
                                "naam": "${OLD_IAM_TEST_USER_1.displayName}"
                            },
                            {
                                "id": "${OLD_IAM_TEST_USER_2.username}",
                                "naam": "${OLD_IAM_TEST_USER_2.displayName}"
                            }
                        ]
                """.trimIndent()
            }
        }
    }

    Given("the beheerder elk zaaktype is logged in to ZAC and is part one or more groups") {
        When("the 'get logged in user' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/identity/loggedInUser"
            )
            val expectedGroupsString = if (FEATURE_FLAG_PABC_INTEGRATION) {
                "\"${GROUP_BEHEERDERS_ELK_DOMEIN.name}\""
            } else {
                "\"${OLD_IAM_TEST_GROUP_A.name}\", \"${OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS.name}\""
            }
            Then("both groups are returned") {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder """
                            {
                                "id": "${BEHEERDER_ELK_ZAAKTYPE.username}",
                                "naam": "${BEHEERDER_ELK_ZAAKTYPE.displayName}",
                                "groupIds": [
                                    $expectedGroupsString
                                ]
                            }
                """.trimIndent()
            }
        }
    }
})
