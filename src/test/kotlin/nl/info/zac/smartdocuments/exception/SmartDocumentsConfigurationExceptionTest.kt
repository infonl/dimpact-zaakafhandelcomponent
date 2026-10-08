/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.exception

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED

class SmartDocumentsConfigurationExceptionTest : BehaviorSpec({
    given("No message") {
        `when`("the exception is constructed") {
            val exception = SmartDocumentsConfigurationException()

            then("it falls back to a default message and keeps the SmartDocuments not configured error code") {
                exception.message shouldBe "SmartDocuments is not configured correctly"
                exception.errorCode shouldBe ERROR_CODE_SMARTDOCUMENTS_NOT_CONFIGURED
            }
        }
    }

    given("An explicit message") {
        `when`("the exception is constructed") {
            val exception = SmartDocumentsConfigurationException("fakeMessage")

            then("it keeps the given message") {
                exception.message shouldBe "fakeMessage"
            }
        }
    }
})
