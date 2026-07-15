/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ReferenceTableValueTest : BehaviorSpec({

    given("Two equal objects") {
        val referenceTableValue1 = createReferenceTableValue()
        val referenceTableValue2 = createReferenceTableValue()

        `when`("The values of the two objects are compared") {
            val transitiveEqualityResult = referenceTableValue1 == referenceTableValue2 &&
                referenceTableValue2 == referenceTableValue1

            then("The objects should be equal") {
                transitiveEqualityResult shouldBe true
            }
        }
    }

    given("Two objects that differ in the sortOrder") {
        val referenceTableValue1 = createReferenceTableValue()
        val referenceTableValue2 = createReferenceTableValue(sortOrder = 100)

        `when`("The values of the two objects are compared") {
            val equalityResult = referenceTableValue1 == referenceTableValue2

            then("The objects should not be equal") {
                equalityResult shouldBe false
            }
        }
    }
})
