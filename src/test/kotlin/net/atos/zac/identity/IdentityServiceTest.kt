/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.identity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.test.org.keycloak.representations.idm.createGroupRepresentation
import nl.info.test.org.keycloak.representations.idm.createUserRepresentation
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
                username = "dummyUsername3",
                firstName = "dummyFirstName3",
                lastName = "dummyLastName3",
                email = "test3@example.com"
            ),
            createUserRepresentation(
                username = "dummyUsername1",
                firstName = "dummyFirstName1",
                lastName = "dummyLastName1",
                email = "test1@example.com"
            ),
            createUserRepresentation(
                username = "dummyUsername2",
                firstName = "dummyFirstName2",
                lastName = "dummyLastName2",
                email = "test2@example.com"
            )
        )
        every { realmResource.users().list() } returns userRepresentations

        When("the users are listed") {
            val users = identityService.listUsers()

            Then("the users are retrieved from Keycloak, sorted by full name") {
                users.size shouldBe 3
                with(users[0]) {
                    id shouldBe "dummyUsername1"
                    firstName shouldBe "dummyFirstName1"
                    lastName shouldBe "dummyLastName1"
                    fullName shouldBe "dummyFirstName1 dummyLastName1"
                    email shouldBe "test1@example.com"
                }
                with(users[1]) {
                    id shouldBe "dummyUsername2"
                    firstName shouldBe "dummyFirstName2"
                    lastName shouldBe "dummyLastName2"
                    fullName shouldBe "dummyFirstName2 dummyLastName2"
                    email shouldBe "test2@example.com"
                }
                with(users[2]) {
                    id shouldBe "dummyUsername3"
                    firstName shouldBe "dummyFirstName3"
                    lastName shouldBe "dummyLastName3"
                    fullName shouldBe "dummyFirstName3 dummyLastName3"
                    email shouldBe "test3@example.com"
                }
            }
        }
    }
    Given("A user in the Keycloak realm") {
        val userRepresentation = createUserRepresentation(
            username = "dummyUsername",
            firstName = "dummyFirstName",
            lastName = "dummyLastName",
            email = "test@example.com"
        )
        every { realmResource.users().searchByUsername(userRepresentation.username, true) } returns listOf(userRepresentation)

        When("the user is retrieved") {
            val user = identityService.readUser(userRepresentation.username)

            Then("the user is retrieved from Keycloak") {
                with(user) {
                    id shouldBe "dummyUsername"
                    firstName shouldBe "dummyFirstName"
                    lastName shouldBe "dummyLastName"
                    fullName shouldBe "dummyFirstName dummyLastName"
                    email shouldBe "test@example.com"
                }
            }
        }
    }
    Given("Users in a group in the Keycloak realm") {
        val groupId = "dummyGroupId"
        val userRepresentations = listOf(
            createUserRepresentation(
                username = "dummyUsername1",
                firstName = "dummyFirstName1",
                lastName = "dummyLastName1",
                email = "test1@example.com"
            ),
            createUserRepresentation(
                username = "dummyUsername2",
                firstName = "dummyFirstName2",
                lastName = "dummyLastName2",
                email = "test2@example.com"
            )
        )
        val keycloakGroup = createGroupRepresentation(id = groupId)
        every { realmResource.groups().groups(groupId, true, 0, 1, true) } returns listOf(keycloakGroup)
        every { realmResource.groups().group(groupId).members() } returns userRepresentations

        When("the users in this group are listed, sorted by full name") {
            val users = identityService.listUsersInGroup(groupId)

            Then("the users in the group are retrieved from Keycloak, sorted by full name") {
                users.size shouldBe 2
                with(users[0]) {
                    id shouldBe "dummyUsername1"
                    firstName shouldBe "dummyFirstName1"
                    lastName shouldBe "dummyLastName1"
                    fullName shouldBe "dummyFirstName1 dummyLastName1"
                }
                with(users[1]) {
                    id shouldBe "dummyUsername2"
                    firstName shouldBe "dummyFirstName2"
                    lastName shouldBe "dummyLastName2"
                    fullName shouldBe "dummyFirstName2 dummyLastName2"
                }
            }
        }
    }
})
