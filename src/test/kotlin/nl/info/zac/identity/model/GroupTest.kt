/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.pabc.model.createPabcGroupRepresentation
import nl.info.test.org.keycloak.representations.idm.createGroupRepresentation

class GroupTest : BehaviorSpec({
    val fakeKeycloakClientId = "fakeKeycloakClientId"

    Context("Mapping a Keycloak GroupRepresentation to a Group") {
        Given("a group representation with no 'active' attribute") {
            val groupRepresentation = createGroupRepresentation(
                attributes = emptyMap()
            )

            When("it is mapped to a Group") {
                val group = groupRepresentation.toGroup(keycloakClientId = fakeKeycloakClientId)

                Then("the group is active") {
                    group.active shouldBe true
                }
            }
        }

        Given("a group representation with 'active' attribute set to 'true'") {
            val groupRepresentation = createGroupRepresentation(
                attributes = mapOf("active" to listOf("true"))
            )

            When("it is mapped to a Group") {
                val group = groupRepresentation.toGroup(keycloakClientId = fakeKeycloakClientId)

                Then("the group is active") {
                    group.active shouldBe true
                }
            }
        }

        Given("a group representation with 'active' attribute set to 'false'") {
            val groupRepresentation = createGroupRepresentation(
                attributes = mapOf("active" to listOf("false"))
            )

            When("it is mapped to a Group") {
                val group = groupRepresentation.toGroup(keycloakClientId = fakeKeycloakClientId)

                Then("the group is inactive") {
                    group.active shouldBe false
                }
            }
        }
    }

    Context("Mapping a PABC GroupRepresentation to a Group") {
        Given("a PABC group representation with no attributes") {
            val groupRepresentation = createPabcGroupRepresentation()

            When("it is mapped to a Group") {
                val group = groupRepresentation.toGroup()

                Then("the group is active and has no email") {
                    group.active shouldBe true
                    group.email shouldBe null
                }
            }
        }

        Given("a PABC group representation with email and active=false attributes") {
            val groupRepresentation = createPabcGroupRepresentation(
                attributes = mapOf(
                    "email" to listOf("group@example.com"),
                    "active" to listOf("false")
                )
            )

            When("it is mapped to a Group") {
                val group = groupRepresentation.toGroup()

                Then("the group is inactive and has the correct email") {
                    group.active shouldBe false
                    group.email shouldBe "group@example.com"
                }
            }
        }
    }
})
