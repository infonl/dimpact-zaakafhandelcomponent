/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createPabcGroupRepresentation
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.keycloak.representations.idm.createGroupRepresentation
import nl.info.test.org.keycloak.representations.idm.createUserRepresentation
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.exception.GroupNotFoundException
import nl.info.zac.identity.exception.UserNotFoundException
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.getFullName
import org.keycloak.admin.client.resource.RealmResource
import java.net.URI
import java.util.UUID

class IdentityServiceTest : BehaviorSpec({
    val zacKeycloakClientId = "fakeZacKeycloakClientId"
    val realmResource = mockk<RealmResource>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val configurationService = mockk<ConfigurationService>()
    val pabcClientService = mockk<PabcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val identityService = IdentityService(
        keycloakZacRealmResource = realmResource,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        configurationService = configurationService,
        zacKeycloakClientId = zacKeycloakClientId,
        pabcClientService = pabcClientService,
        ztcClientService = ztcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Listing users") {
        Given("Users in the Keycloak realm") {
            val userRepresentations = listOf(
                createUserRepresentation(
                    username = "fakeUsername3",
                    firstName = "fakeFirstName3",
                    lastName = "fakeLastName3",
                    email = "test3@example.com"
                ),
                createUserRepresentation(
                    username = "fakeUsername1",
                    firstName = "fakeFirstName1",
                    lastName = "fakeLastName1",
                    email = "test1@example.com"
                ),
                createUserRepresentation(
                    username = "fakeUsername2",
                    firstName = "fakeFirstName2",
                    lastName = "fakeLastName2",
                    email = "test2@example.com"
                )
            )
            every { realmResource.users().list() } returns userRepresentations

            When("the users are listed") {
                val users = identityService.listUsers()

                Then("the users are retrieved from Keycloak, sorted by full name") {
                    users.size shouldBe 3
                    with(users[0]) {
                        id shouldBe "fakeUsername1"
                        firstName shouldBe "fakeFirstName1"
                        lastName shouldBe "fakeLastName1"
                        getFullName() shouldBe "fakeFirstName1 fakeLastName1"
                        email shouldBe "test1@example.com"
                    }
                    with(users[1]) {
                        id shouldBe "fakeUsername2"
                        firstName shouldBe "fakeFirstName2"
                        lastName shouldBe "fakeLastName2"
                        getFullName() shouldBe "fakeFirstName2 fakeLastName2"
                        email shouldBe "test2@example.com"
                    }
                    with(users[2]) {
                        id shouldBe "fakeUsername3"
                        firstName shouldBe "fakeFirstName3"
                        lastName shouldBe "fakeLastName3"
                        getFullName() shouldBe "fakeFirstName3 fakeLastName3"
                        email shouldBe "test3@example.com"
                    }
                }
            }
        }
    }

    Context("Listing users in a group") {
        Given("Users in a group in the Keycloak realm") {
            val groupId = "fakeGroupId"
            val userName1 = "fakeUsername1"
            val userName2 = "fakeUsername2"
            val userRepresentation1 = createUserRepresentation(
                username = userName1,
                firstName = "fakeFirstName1",
                lastName = "fakeLastName1",
                email = "test1@example.com"
            )
            val userRepresentation2 = createUserRepresentation(
                username = userName2,
                firstName = "fakeFirstName2",
                lastName = "fakeLastName2",
                email = "test2@example.com"
            )
            val userRepresentations = listOf(userRepresentation1, userRepresentation2)
            val keycloakGroup = createGroupRepresentation(id = groupId)
            every { realmResource.groups().groups(groupId, true, 0, 1, true) } returns listOf(keycloakGroup)
            every { realmResource.groups().group(groupId).members() } returns userRepresentations

            When("the users in this group are listed, sorted by full name") {
                val users = identityService.listUsersInGroup(groupId)

                Then("the users in the group are retrieved from Keycloak, sorted by full name") {
                    users.size shouldBe 2
                    with(users[0]) {
                        id shouldBe "fakeUsername1"
                        firstName shouldBe "fakeFirstName1"
                        lastName shouldBe "fakeLastName1"
                        getFullName() shouldBe "fakeFirstName1 fakeLastName1"
                    }
                    with(users[1]) {
                        id shouldBe "fakeUsername2"
                        firstName shouldBe "fakeFirstName2"
                        lastName shouldBe "fakeLastName2"
                        getFullName() shouldBe "fakeFirstName2 fakeLastName2"
                    }
                }
            }
        }
    }

    Context("Reading a user") {
        Given("A user in the Keycloak realm") {
            val userRepresentation = createUserRepresentation(
                username = "fakeUsername",
                firstName = "fakeFirstName",
                lastName = "fakeLastName",
                email = "test@example.com"
            )
            every {
                realmResource.users().searchByUsername(userRepresentation.username, true)
            } returns listOf(userRepresentation)

            When("the user is retrieved") {
                val user = identityService.readUser(userRepresentation.username)

                Then("the user is retrieved from Keycloak") {
                    with(user) {
                        id shouldBe "fakeUsername"
                        firstName shouldBe "fakeFirstName"
                        lastName shouldBe "fakeLastName"
                        getFullName() shouldBe "fakeFirstName fakeLastName"
                        email shouldBe "test@example.com"
                    }
                }
            }
        }
    }

    Context("Listing group names for a user") {
        Given("Users in the same group in the Keycloak realm") {
            val groupId = "fakeGroupId"
            val userName1 = "fakeUsername1"
            val userName2 = "fakeUsername2"
            val userRepresentation1 = createUserRepresentation(
                username = userName1,
                firstName = "fakeFirstName1",
                lastName = "fakeLastName1",
                email = "test1@example.com"
            )
            val userRepresentation2 = createUserRepresentation(
                username = userName2,
                firstName = "fakeFirstName2",
                lastName = "fakeLastName2",
                email = "test2@example.com"
            )
            every {
                realmResource.users().searchByUsername(userName1, true)
            } returns listOf(
                userRepresentation1.apply {
                    groups = listOf(groupId)
                },
                userRepresentation2.apply {
                    groups = listOf(groupId)
                }
            )
            every {
                realmResource.users().get(userRepresentation1.id).groups()
            } returns listOf(createGroupRepresentation(name = groupId))

            When("the group names for a user are listed") {
                val groupNames = identityService.listGroupNamesForUser(userName1)

                Then("the groups for the user are retrieved") {
                    groupNames.size shouldBe 1
                    groupNames[0] shouldBe groupId
                }
            }

            When("a check if an existing user is in a group is performed") {
                identityService.validateIfUserIsInGroup(userName1, groupId)

                Then("the check succeeds") {}
            }
        }

        Given("An unknown user in the Keycloak realm") {
            val unknownUserName = "unknownUser"
            every {
                realmResource.users().searchByUsername(unknownUserName, true)
            } returns emptyList()

            When("listing group names of an unknown user ") {
                shouldThrow<UserNotFoundException> {
                    identityService.listGroupNamesForUser(unknownUserName)
                }

                Then("an exception is thrown") {}
            }
        }

        Given("An unknown group in the Keycloak realm") {
            val unknownGroupName = "unknownGroup"
            every {
                realmResource.groups().groups(unknownGroupName, true, 0, 1, true)
            } returns emptyList()

            When("listing users of an unknown user ") {
                shouldThrow<GroupNotFoundException> {
                    identityService.listUsersInGroup(unknownGroupName)
                }

                Then("an exception is thrown") {}
            }
        }
    }

    Context("Validating if a user is in a group") {
        Given("Users in the same group present in the Keycloak realm") {
            val groupId = "fakeGroupId"
            val userName1 = "fakeUsername1"
            val userName2 = "fakeUsername2"
            val unknownGroupName = "unknownGroupName"
            val userRepresentation1 = createUserRepresentation(
                username = userName1,
                firstName = "fakeFirstName1",
                lastName = "fakeLastName1",
                email = "test1@example.com"
            )
            val userRepresentation2 = createUserRepresentation(
                username = userName2,
                firstName = "fakeFirstName2",
                lastName = "fakeLastName2",
                email = "test2@example.com"
            )
            every {
                realmResource.users().searchByUsername(userName1, true)
            } returns listOf(
                userRepresentation1.apply {
                    groups = listOf(groupId)
                },
                userRepresentation2.apply {
                    groups = listOf(groupId)
                }
            )
            every {
                realmResource.users().get(userRepresentation1.id).groups()
            } returns listOf(createGroupRepresentation(name = groupId))

            When("a check if a user is in an unknown group is performed") {
                shouldThrow<UserNotInGroupException> {
                    identityService.validateIfUserIsInGroup(userName1, unknownGroupName)
                }

                Then("an exception is thrown") {}
            }
        }

        Given("A group without users in the Keycloak realm") {
            val groupId = "fakeGroupId"
            val unknownUserName = "unknownUser"
            every {
                realmResource.users().searchByUsername(unknownUserName, true)
            } returns emptyList()

            When("a check if an unknown user is in a group is performed") {
                shouldThrow<UserNotFoundException> {
                    identityService.validateIfUserIsInGroup(unknownUserName, groupId)
                }

                Then("an exception is thrown") {}
            }
        }
    }

    Context("Listing groups for a zaaktype") {
        Given(
            """
            Authorised groups for the 'behandelaar' application role and a zaaktype in PABC 
            and PABC feature flag on
            """
        ) {
            val zaaktypeUuid = UUID.randomUUID()
            val zaaktypeDescription = "fakeZaaktypeDescription"
            val zaaktype = createZaakType(
                uri = URI("https://example.com/zaaktypes/$zaaktypeUuid"),
                omschrijving = zaaktypeDescription
            )
            val pabcGroupRepresentation1 = createPabcGroupRepresentation(
                name = "fakeGroupId1",
                description = "fakeGroupDescription1"
            )
            val pabcGroupRepresentation2 = createPabcGroupRepresentation(
                name = "fakeGroupId2",
                description = null
            )
            every { configurationService.featureFlagPabcIntegration() } returns true
            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                    applicationRole = "behandelaar",
                    zaaktypeDescription = zaaktype.omschrijving
                )
            } returns listOf(pabcGroupRepresentation1, pabcGroupRepresentation2)

            When("groups for the zaaktype UUID are listed") {
                val groups = identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)

                Then("all groups are returned, sorted by name") {
                    groups.size shouldBe 2
                    groups[0].name shouldBe "fakeGroupId1"
                    groups[1].name shouldBe "fakeGroupId2"
                }
            }

            When("groups for the zaaktype are listed") {
                val groups = identityService.listGroupsForBehandelaarRoleAndZaaktype(zaaktypeDescription)

                Then("all groups are returned, sorted by name") {
                    groups.size shouldBe 2
                    groups[0].name shouldBe "fakeGroupId1"
                    groups[1].name shouldBe "fakeGroupId2"
                }
            }
        }

        Given(
            """
            One Keycloak group with a ZAC client role that is equal to the domein role configured in the 
            zaaktypeCmmnConfiguration for a zaaktype uuid, and another Keycloak group with a different ZAC client role,
            and PABC feature flag off
            """.trimIndent()
        ) {
            val zaaktypeUuid = UUID.randomUUID()
            val domeinRole = "fakeDomeinRole"
            val groupRepresentation1 = createGroupRepresentation(
                name = "fakeGroupName1",
                description = "fakeGroupDescription1",
                clientRoles = mapOf(zacKeycloakClientId to listOf(domeinRole))
            )
            val groupRepresentation2 = createGroupRepresentation(
                name = "fakeGroupName2",
                clientRoles = mapOf(zacKeycloakClientId to listOf("otherRole"))
            )
            every {
                realmResource.groups().groups("", 0, Integer.MAX_VALUE, false)
            } returns listOf(groupRepresentation1, groupRepresentation2)
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid).domein } returns domeinRole
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("groups for the zaaktype UUID are listed") {
                val groups = identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)

                Then("only groups with matching domain roles are returned") {
                    groups.size shouldBe 1
                    with(groups[0]) {
                        name shouldBe "fakeGroupName1"
                        description shouldBe "fakeGroupDescription1"
                        zacClientRoles shouldBe listOf(domeinRole)
                    }
                }
            }
        }

        Given("Zaaktype UUID with domain allowing all zaaktypes and PABC feature flag off") {
            val zaaktypeUuid = UUID.randomUUID()
            val groupRepresentation1 = createGroupRepresentation(
                name = "fakeGroupId1",
                clientRoles = mapOf(zacKeycloakClientId to listOf("fakeDomeinRole1")),
            )
            val groupRepresentation2 = createGroupRepresentation(
                name = "fakeGroupId2",
                clientRoles = mapOf(zacKeycloakClientId to listOf("fakeDomeinRole2")),
            )
            every {
                realmResource.groups().groups("", 0, Integer.MAX_VALUE, false)
            } returns listOf(groupRepresentation1, groupRepresentation2)
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid).domein
            } returns "domein_elk_zaaktype"
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("groups for the zaaktype UUID are listed") {
                val groups = identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)

                Then("all groups are returned, sorted by name") {
                    groups.size shouldBe 2
                    groups[0].name shouldBe "fakeGroupId1"
                    groups[1].name shouldBe "fakeGroupId2"
                }
            }
        }

        Given("Zaaktype UUID with empty group list and PABC feature flag off") {
            val zaaktypeUuid = UUID.randomUUID()
            val domain = "anyDomain"
            every { realmResource.groups().groups("", 0, Integer.MAX_VALUE, false) } returns emptyList()
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid).domein } returns domain
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("groups for the zaaktype UUID are listed") {
                val groups = identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid)

                Then("no groups are returned") {
                    groups.size shouldBe 0
                }
            }
        }
    }
})
