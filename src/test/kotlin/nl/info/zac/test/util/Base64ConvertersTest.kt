/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.test.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.util.decodedBase64StringLength
import nl.info.zac.util.toBase64String
import java.util.Base64

class Base64ConvertersTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    given("byte array with unicode characters") {
        val fileNameUnicode = "testTextDocümënt.pdf"
        val byteArrayWithUnicodeCharacters = fileNameUnicode.toByteArray(Charsets.UTF_8)

        `when`("converted to base64 string") {
            val base64String = byteArrayWithUnicodeCharacters.toBase64String()

            then("the encoded base64 string is correct") {
                Base64.getDecoder().decode(base64String).decodeToString() shouldBe fileNameUnicode
            }
        }
    }

    given("byte array with escaped unicode characters") {
        val fileNameEscapedUnicode = "\\u0048\\u0065\\u006C\\u006C\\u006FWorld.txt"
        val byteArrayWithEscapedUnicodeCharacters = fileNameEscapedUnicode.toByteArray(Charsets.UTF_8)

        `when`("converted to base64 string") {
            val base64String = byteArrayWithEscapedUnicodeCharacters.toBase64String()

            then("the encoded base64 string is correct") {
                Base64.getDecoder().decode(base64String).decodeToString() shouldBe fileNameEscapedUnicode
            }
        }
    }

    given("a base64 encoded string") {
        val testDataThatRequiresPadding = "testData12"
        val base64encoded = Base64.getEncoder().encodeToString(testDataThatRequiresPadding.toByteArray())

        `when`("length of decoded string is requested") {
            val length = base64encoded.decodedBase64StringLength()

            then("length is correct") {
                length shouldBe testDataThatRequiresPadding.length
            }
        }
    }
})
