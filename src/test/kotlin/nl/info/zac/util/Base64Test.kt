/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.Base64

class Base64Test : BehaviorSpec({

    Given("byte array with unicode characters") {
        val fileNameUnicode = "testTextDocümënt.pdf"
        val byteArrayWithUnicodeCharacters = fileNameUnicode.toByteArray(Charsets.UTF_8)

        When("converted to base64 string") {
            val base64String = byteArrayWithUnicodeCharacters.toBase64String()

            Then("the encoded base64 string is correct") {
                Base64.getDecoder().decode(base64String).decodeToString() shouldBe fileNameUnicode
            }
        }
    }

    Given("byte array with escaped unicode characters") {
        val fileNameEscapedUnicode = "\\u0048\\u0065\\u006C\\u006C\\u006FWorld.txt"
        val byteArrayWithEscapedUnicodeCharacters = fileNameEscapedUnicode.toByteArray(Charsets.UTF_8)

        When("converted to base64 string") {
            val base64String = byteArrayWithEscapedUnicodeCharacters.toBase64String()

            Then("the encoded base64 string is correct") {
                Base64.getDecoder().decode(base64String).decodeToString() shouldBe fileNameEscapedUnicode
            }
        }
    }
})
