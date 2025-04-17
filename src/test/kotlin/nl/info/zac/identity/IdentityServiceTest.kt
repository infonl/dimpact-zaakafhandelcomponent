/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.test.org.keycloak.representations.idm.createGroupRepresentation
import nl.info.test.org.keycloak.representations.idm.createUserRepresentation
import nl.info.zac.identity.exception.GroupNotFoundException
import nl.info.zac.identity.exception.UserNotFoundException
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.getFullName
import org.keycloak.admin.client.resource.RealmResource

class IdentityServiceTest : BehaviorSpec({
    val realmResource = mockk<RealmResource>()
    val identityService = IdentityService(realmResource)

    beforeEach {
        checkUnnecessaryStub()
    }

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
            identityService.checkIfUserIsInGroup(userName1, groupId)

            Then("the check succeeds") {}
        }
    }

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
                identityService.checkIfUserIsInGroup(userName1, unknownGroupName)
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
                identityService.checkIfUserIsInGroup(unknownUserName, groupId)
            }

            Then("an exception is thrown") {}
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
})
