/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Locatie
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsOnbekend
import nl.info.zac.exception.InputValidationFailedException

class AbstractVerblijfplaatsJsonbDeserializerTest : BehaviorSpec({
    val deserializer = AbstractVerblijfplaatsJsonbDeserializer()

    context("deserialize") {
        given("JSON with type VerblijfplaatsBuitenland") {
            val json = """{"type":"VerblijfplaatsBuitenland"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a VerblijfplaatsBuitenland is returned") {
                    result.shouldBeInstanceOf<VerblijfplaatsBuitenland>()
                }
            }
        }

        given("JSON with type Adres") {
            val json = """{"type":"Adres"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("an Adres is returned") {
                    result.shouldBeInstanceOf<Adres>()
                }
            }
        }

        given("JSON with type VerblijfplaatsOnbekend") {
            val json = """{"type":"VerblijfplaatsOnbekend"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a VerblijfplaatsOnbekend is returned") {
                    result.shouldBeInstanceOf<VerblijfplaatsOnbekend>()
                }
            }
        }

        given("JSON with type Locatie") {
            val json = """{"type":"Locatie"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a Locatie is returned") {
                    result.shouldBeInstanceOf<Locatie>()
                }
            }
        }

        given("JSON with an unknown verblijfplaats type") {
            val json = """{"type":"OnbekendVerblijfplaatsType"}"""

            `when`("deserialize is called") {
                val exception = shouldThrow<InputValidationFailedException> {
                    deserializer.deserialize(createJsonParser(json), mockk(), mockk())
                }
                then("InputValidationFailedException is thrown") {
                    exception.message!!.contains("OnbekendVerblijfplaatsType") shouldBe true
                }
            }
        }
    }
})
