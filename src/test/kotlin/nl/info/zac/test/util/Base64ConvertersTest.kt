/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.test.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.util.decodedBase64StringLength
import java.util.Base64

class Base64ConvertersTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a base64 encoded string") {
        val testDataThatRequiresPadding = "testData12"
        val base64encoded = Base64.getEncoder().encodeToString(testDataThatRequiresPadding.toByteArray())

        When("length of decoded string is requested") {
            val length = base64encoded.decodedBase64StringLength()

            Then("length is correct") {
                length shouldBe testDataThatRequiresPadding.length
            }
        }
    }
})
