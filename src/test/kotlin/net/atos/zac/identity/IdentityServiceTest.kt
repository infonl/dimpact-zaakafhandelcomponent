/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.identity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.createUserRepresentation

class IdentityServiceTest : BehaviorSpec({
    val realmResource = mockk<RealmResource>()
    val identityService = IdentityService(realmResource)

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

        When("the users are listed, sorted by full name") {
            val users = identityService.listUsers()

            Then("the users are retrieved from Keycloak") {
                users.size shouldBe 3
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
                with(users[2]) {
                    id shouldBe "dummyUsername3"
                    firstName shouldBe "dummyFirstName3"
                    lastName shouldBe "dummyLastName3"
                    fullName shouldBe "dummyFirstName3 dummyLastName3"
                }
            }
        }
    }
})
