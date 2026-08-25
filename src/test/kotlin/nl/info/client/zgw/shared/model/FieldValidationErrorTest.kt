/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class FieldValidationErrorTest : BehaviorSpec({
    given("a field validation error") {
        val fieldValidationError = createFieldValidationError(
            name = "fakeFieldName",
            code = "fakeCode",
            reason = "fakeReason"
        )

        `when`("toString is called") {
            val result = fieldValidationError.toString()

            then("it should format the name, code and reason") {
                result shouldBe "Name: fakeFieldName, Code: fakeCode, Reason: fakeReason"
            }
        }
    }
})
