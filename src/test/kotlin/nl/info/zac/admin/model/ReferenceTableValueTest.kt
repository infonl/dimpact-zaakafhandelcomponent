/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ReferenceTableValueTest : BehaviorSpec({

    Given("Two equal objects") {
        val referenceTableValue1 = createReferenceTableValue()
        val referenceTableValue2 = createReferenceTableValue()

        When("The values of the two objects are compared") {
            val transitiveEqualityResult = referenceTableValue1 == referenceTableValue2 &&
                referenceTableValue2 == referenceTableValue1

            Then("The objects should be equal") {
                transitiveEqualityResult shouldBe true
            }
        }
    }

    Given("Two objects that differ in the sortOrder") {
        val referenceTableValue1 = createReferenceTableValue()
        val referenceTableValue2 = createReferenceTableValue(sortOrder = 100)

        When("The values of the two objects are compared") {
            val equalityResult = referenceTableValue1 == referenceTableValue2

            Then("The objects should not be equal") {
                equalityResult shouldBe false
            }
        }
    }
})
