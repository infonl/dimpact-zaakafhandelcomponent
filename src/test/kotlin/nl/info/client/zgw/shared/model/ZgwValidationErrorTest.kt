/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ZgwValidationErrorTest : BehaviorSpec({
    given("a ZGW validation error with one invalid parameter") {
        val zgwValidationError = createValidationZgwError(
            title = "fakeTitle",
            status = 400,
            detail = "fakeDetail",
            invalidParams = listOf(
                createFieldValidationError(name = "fakeFieldName", code = "fakeCode", reason = "fakeReason")
            )
        )

        `when`("toString is called") {
            val result = zgwValidationError.toString()

            then("it should format the base error followed by each invalid parameter") {
                result shouldBe "(400) Title: fakeTitle, Detail: fakeDetail\n" +
                    "Name: fakeFieldName, Code: fakeCode, Reason: fakeReason\n"
            }
        }
    }
})
