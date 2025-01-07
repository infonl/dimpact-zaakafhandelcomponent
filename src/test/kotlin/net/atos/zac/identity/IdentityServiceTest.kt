package net.atos.zac.identity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.identity.IdentityService
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.createUserRepresentation

class IdentityServiceTest: BehaviorSpec ({
    val realmResource = mockk<RealmResource>()
    val identityService = IdentityService(realmResource)

    Given("Users in the Keycloak realm") {
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
        every { realmResource.users().list() } returns userRepresentations

        When("the users are listed") {
            val users = identityService.listUsers()

            Then("the users are retrieved from Keycloak") {
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
