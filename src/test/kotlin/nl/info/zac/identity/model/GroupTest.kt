/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
})
