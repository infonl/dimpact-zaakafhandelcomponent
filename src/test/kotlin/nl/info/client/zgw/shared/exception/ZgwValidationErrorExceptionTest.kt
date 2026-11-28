/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import nl.info.client.zgw.shared.model.createFieldValidationError
import nl.info.client.zgw.shared.model.createValidationZgwError

class ZgwValidationErrorExceptionTest : BehaviorSpec({
    context("The message of a ZGW validation error") {
        given("Open Zaak rejects an email address with a reason and a detail that hold the rejected value") {
            val zgwValidationErrorException = ZgwValidationErrorException(
                createValidationZgwError(
                    code = "invalid",
                    title = "Invalid input geen-email",
                    status = 400,
                    detail = "geen-email is not valid",
                    invalidParams = listOf(
                        createFieldValidationError(
                            name = "emailadres",
                            code = "invalid",
                            reason = "Voer een geldig e-mailadres in: geen-email"
                        )
                    )
                )
            )

            `when`("the message is read") {
                val message = zgwValidationErrorException.message

                then("it holds the status, the error code and the name and code of the invalid parameter only") {
                    message shouldBe "400 invalid: emailadres [invalid]"
                    message shouldNotContain "geen-email"
                }
            }
        }
    }
})
