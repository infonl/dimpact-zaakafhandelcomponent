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
import io.kotest.matchers.string.shouldNotContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.encodeUrlPathSegment
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_2
import nl.info.zac.itest.config.BEHANDELAAR_INACTIVE_GROUP_1
import nl.info.zac.itest.config.BEHANDELAAR_LONG_NAME_TEST
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.COORDINATOR_2
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_LONG_NAME_TEST
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_2
import nl.info.zac.itest.config.GROUP_BEHEERDERS_ELK_DOMEIN
import nl.info.zac.itest.config.GROUP_COORDINATORS_TEST_1
import nl.info.zac.itest.config.GROUP_COORDINATORS_TEST_2
import nl.info.zac.itest.config.GROUP_INACTIVE_TEST_1
import nl.info.zac.itest.config.GROUP_RAADPLEGERS_TEST_1
import nl.info.zac.itest.config.GROUP_RAADPLEGERS_TEST_2
import nl.info.zac.itest.config.GROUP_RECORDMANAGERS_TEST_1
import nl.info.zac.itest.config.GROUP_RECORDMANAGERS_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.PABC_ADMIN
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.config.RAADPLEGER_2
import nl.info.zac.itest.config.RAADPLEGER_EN_BEHANDELAAR_1
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.config.RECORDMANAGER_2
import nl.info.zac.itest.config.USER_WITHOUT_ANY_ROLE
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

val TEST_GROUPS_ACTIVE =
    """
            [                
                {
                    "id": "${GROUP_RAADPLEGERS_TEST_1.name}",
                    "naam": "${GROUP_RAADPLEGERS_TEST_1.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_RAADPLEGERS_TEST_2.name}",
                    "naam": "${GROUP_RAADPLEGERS_TEST_2.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_BEHANDELAARS_TEST_1.name}",
                    "naam": "${GROUP_BEHANDELAARS_TEST_1.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_BEHANDELAARS_TEST_2.name}",
                    "naam": "${GROUP_BEHANDELAARS_TEST_2.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_COORDINATORS_TEST_1.name}",
                    "naam": "${GROUP_COORDINATORS_TEST_1.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_COORDINATORS_TEST_2.name}",
                    "naam": "${GROUP_COORDINATORS_TEST_2.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_RECORDMANAGERS_TEST_1.name}",
                    "naam": "${GROUP_RECORDMANAGERS_TEST_1.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_RECORDMANAGERS_TEST_2.name}",
                    "naam": "${GROUP_RECORDMANAGERS_TEST_2.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_BEHEERDERS_ELK_DOMEIN.name}",
                    "naam": "${GROUP_BEHEERDERS_ELK_DOMEIN.description}",
                    "active": true
                },
                {
                    "id": "${GROUP_BEHANDELAARS_LONG_NAME_TEST.name}",
                    "naam": "${GROUP_BEHANDELAARS_LONG_NAME_TEST.description}",
                    "active": true
                }
            ]
        """

class IdentityServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Context("Getting all available groups") {
        Given("The ZAC Keycloak realm contains several groups and a logged-in beheerder") {
            When("the 'list groups' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/identity/groups",
                    testUser = BEHEERDER_1
                )
                Then("only active groups are returned and the inactive group is absent") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder TEST_GROUPS_ACTIVE.trimIndent()
                    response.bodyAsString shouldNotContain GROUP_INACTIVE_TEST_1.name
                }
            }
        }
    }

    Context("Getting authorised behandelaar groups for a zaaktype") {
        Given(
            """
            Authorised groups for the application role 'behandelaar' and the given zaaktype, 
            using the groups' functional roles and the available PABC mappings, and a logged-in beheerder
        """
        ) {
            When(
                "the 'list behandelaar groups for a zaaktype' endpoint is called for this zaaktype"
            ) {
                val response = itestHttpClient.performGetRequest(
                    url =
                    "$ZAC_API_URI/identity/zaaktype/${ZAAKTYPE_CMMN_TEST_2_DESCRIPTION.encodeUrlPathSegment()}/behandelaar-groups",
                    testUser = BEHEERDER_1
                )
                Then(
                    """
                only the groups authorised for the application role 'behandelaar' and
                zaaktype test 2 (via the PABC mappings and the group's functional roles) are returned                
                """
                ) {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualSpecifiedJson """
                            [
                                {
                                    "id": "${GROUP_BEHANDELAARS_TEST_1.name}",
                                    "naam": "${GROUP_BEHANDELAARS_TEST_1.description}",
                                    "active": true
                                },
                                {
                                    "active": true,
                                    "id": "${GROUP_BEHANDELAARS_LONG_NAME_TEST.name}",
                                    "naam": "${GROUP_BEHANDELAARS_LONG_NAME_TEST.description}"
                                },
                                {
                                    "id": "${GROUP_BEHEERDERS_ELK_DOMEIN.name}",
                                    "naam": "${GROUP_BEHEERDERS_ELK_DOMEIN.description}",
                                    "active": true
                                },
                                {
                                    "id": "${GROUP_COORDINATORS_TEST_1.name}",
                                    "naam": "${GROUP_COORDINATORS_TEST_1.description}",
                                    "active": true
                                },
                                {
                                    "id": "${GROUP_RECORDMANAGERS_TEST_1.name}",
                                    "naam": "${GROUP_RECORDMANAGERS_TEST_1.description}",
                                    "active": true
                                }
                            ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Getting all available users") {
        Given("Keycloak contains all provisioned test users, and a logged-in beheerder") {
            When("the 'list users' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/identity/users",
                    testUser = BEHEERDER_1
                )
                Then("All available users in the Keycloak ZAC realm are returned") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder """
                            [                               
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
                                    "id": "${RECORDMANAGER_1.username}",
                                    "naam": "${RECORDMANAGER_1.displayName}"
                                },
                                {
                                    "id": "${RECORDMANAGER_2.username}",
                                    "naam": "${RECORDMANAGER_2.displayName}"
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
                                    "id": "${USER_WITHOUT_ANY_ROLE.username}",
                                    "naam": "${USER_WITHOUT_ANY_ROLE.displayName}"
                                },
                                {
                                   "id": "${PABC_ADMIN.username}",
                                    "naam": "${PABC_ADMIN.displayName}"
                                },
                                {
                                    "id": "${BEHANDELAAR_INACTIVE_GROUP_1.username}",
                                    "naam": "${BEHANDELAAR_INACTIVE_GROUP_1.displayName}"
                                },
                                {
                                    "id": "${BEHANDELAAR_LONG_NAME_TEST.username}",
                                    "naam": "${BEHANDELAAR_LONG_NAME_TEST.displayName}"
                                }
                            ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Getting users in a group") {
        Given(
            "Keycloak contains a test group with members, and a logged-in beheerder"
        ) {
            When("the 'list users in group' endpoint is called for the group") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/identity/groups/${GROUP_BEHANDELAARS_TEST_1.name}/users",
                    testUser = BEHEERDER_1
                )
                Then("the group members are returned") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJson """
                        [
                            {
                                "id": "${BEHANDELAAR_1.username}",
                                "naam": "${BEHANDELAAR_1.displayName}"
                            }
                        ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Getting users in an inactive group") {
        Given(
            "Keycloak contains an inactive group '${GROUP_INACTIVE_TEST_1.name}' with one member, and a logged-in beheerder"
        ) {
            When("the 'list users in group' endpoint is called for the inactive group") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/identity/groups/${GROUP_INACTIVE_TEST_1.name}/users",
                    testUser = BEHEERDER_1
                )
                Then("the member of the inactive group is returned") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJson """
                        [
                            {
                                "id": "${BEHANDELAAR_INACTIVE_GROUP_1.username}",
                                "naam": "${BEHANDELAAR_INACTIVE_GROUP_1.displayName}"
                            }
                        ]
                    """.trimIndent()
                }
            }
        }
    }

    Context("Getting authorised behandelaar groups for multiple zaaktypes") {
        Given(
            """
            Authorised groups for the application role 'behandelaar' for zaaktype 1 and zaaktype 2,
            with only 'beheerders-elk-domein' (beheerder_elk_domein functional role) authorised for both,
            and a logged-in beheerder
        """
        ) {
            When(
                """
                the 'list behandelaar groups for multiple zaaktypes'
                endpoint is called for zaaktype 1 and zaaktype 2
                """
            ) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/identity/behandelaar-groups",
                    requestBodyAsString = """
                        {
                            "zaaktypeDescriptions": [
                                "$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION",
                                "$ZAAKTYPE_CMMN_TEST_2_DESCRIPTION"
                            ]
                        }
                    """.trimIndent(),
                    testUser = BEHEERDER_1
                )
                Then(
                    "only the group authorised as behandelaar for both zaaktypes (beheerders-elk-domein) is returned"
                ) {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualSpecifiedJson """
                        [
                            {
                                "id": "${GROUP_BEHEERDERS_ELK_DOMEIN.name}",
                                "naam": "${GROUP_BEHEERDERS_ELK_DOMEIN.description}",
                                "active": true
                            }
                        ]
                    """.trimIndent()
                }
            }
        }

        Given("An empty zaaktype descriptions list and a logged-in beheerder") {
            When(
                "the 'list behandelaar groups for multiple zaaktypes' endpoint is called with an empty list"
            ) {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/identity/behandelaar-groups",
                    requestBodyAsString = """{"zaaktypeDescriptions": []}""",
                    testUser = BEHEERDER_1
                )
                Then("HTTP 400 is returned") {
                    response.code shouldBe HTTP_BAD_REQUEST
                }
            }
        }
    }

    Context("Getting the logged-in user") {
        Given("A beheerder is logged in to ZAC and is part one or more groups") {
            val expectedGroupsString = "\"${GROUP_BEHEERDERS_ELK_DOMEIN.name}\""

            When("the 'get logged in user' endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/identity/loggedInUser",
                    testUser = BEHEERDER_1
                )

                Then("the response is OK and the expected group IDs are returned") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualSpecifiedJsonIgnoringOrder """
                            {
                                "id": "${BEHEERDER_1.username}",
                                "naam": "${BEHEERDER_1.displayName}",
                                "groupIds": [
                                    $expectedGroupsString
                                ]
                            }
                    """.trimIndent()
                }
            }
        }
    }
})
