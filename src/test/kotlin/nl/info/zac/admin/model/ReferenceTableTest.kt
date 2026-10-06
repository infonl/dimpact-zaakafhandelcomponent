/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ReferenceTableTest : BehaviorSpec({

    given("Two equal objects") {
        val referenceTable1 = createReferenceTable()
        val referenceTable2 = createReferenceTable()

        `when`("The values of the two objects are compared") {
            val isTransitivelyEqual = referenceTable1 == referenceTable2 && referenceTable2 == referenceTable1

            then("The objects should be considered equal") {
                isTransitivelyEqual shouldBe true
            }
        }

        `when`("The hashcode of the two objects are compared") {
            val hashcodeResult = referenceTable1.hashCode() == referenceTable2.hashCode()
            then("The objects should have the same hashcode") {
                hashcodeResult shouldBe true
            }
        }
    }

    given("Two different objects") {
        val referenceTable1 = createReferenceTable()
        val referenceTable2 = createReferenceTable(isSystemReferenceTable = true)

        `when`("The values of the two objects are compared") {
            val isEqual = referenceTable1 == referenceTable2

            then("The objects should be considered unequal") {
                isEqual shouldBe false
            }
        }

        `when`("Transitive check is performed") {
            val isTransitivelyEqual = referenceTable2 == referenceTable1

            then("The objects should be considered unequal") {
                isTransitivelyEqual shouldBe false
            }
        }

        `when`("The hashcode of the two objects are compared") {
            val hashcodeResult = referenceTable1.hashCode() == referenceTable2.hashCode()

            then("The objects should have different hashcodes") {
                hashcodeResult shouldBe false
            }
        }
    }

    given("A reference table and a value that have not been persisted yet") {
        val referenceTable = createReferenceTable(
            id = null,
            values = mutableListOf(createReferenceTableValue(id = null))
        )

        `when`("the reference table is converted to a REST reference table including its values") {
            val restReferenceTable = referenceTable.toRestReferenceTable(inclusiefWaarden = true)

            then("the REST reference table and its value have no id") {
                restReferenceTable.id.shouldBeNull()
                restReferenceTable.values.single().id.shouldBeNull()
            }
        }
    }
})
