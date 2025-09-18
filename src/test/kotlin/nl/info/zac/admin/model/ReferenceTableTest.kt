/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ReferenceTableTest : BehaviorSpec({

    Given("Two equal objects") {
        val referenceTable1 = createReferenceTable()
        val referenceTable2 = createReferenceTable()

        When("The values of the two objects are compared") {
            val transitiveResult = referenceTable1 == referenceTable2 && referenceTable2 == referenceTable1

            Then("The objects should be considered equal") {
                transitiveResult shouldBe true
            }
        }

        When("The hashcode of the two objects are compared") {
            val hashcodeResult = referenceTable1.hashCode() == referenceTable2.hashCode()
            Then("The objects should have the same hashcode") {
                hashcodeResult shouldBe true
            }
        }
    }

    Given("Two different objects") {
        val referenceTable1 = createReferenceTable()
        val referenceTable2 = createReferenceTable(isSystemReferenceTable = true)

        When("The values of the two objects are compared") {
            val equalityResult = referenceTable1 == referenceTable2

            Then("The objects should be considered unequal") {
                equalityResult shouldBe false
            }
        }

        When("Transitive check is performed") {
            val transitiveResult = referenceTable2 == referenceTable1

            Then("The objects should be considered unequal") {
                transitiveResult shouldBe false
            }
        }

        When("The hashcode of the two objects are compared") {
            val hashcodeResult = referenceTable1.hashCode() == referenceTable2.hashCode()

            Then("The objects should have different hashcodes") {
                hashcodeResult shouldBe false
            }
        }
    }
})
