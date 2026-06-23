/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Locatie
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsOnbekend
import nl.info.zac.exception.InputValidationFailedException

class AbstractVerblijfplaatsJsonbDeserializerTest : BehaviorSpec({
    val deserializer = AbstractVerblijfplaatsJsonbDeserializer()

    Context("deserialize") {
        Given("JSON with type VerblijfplaatsBuitenland") {
            val json = """{"type":"VerblijfplaatsBuitenland"}"""

            When("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                Then("a VerblijfplaatsBuitenland is returned") {
                    result.shouldBeInstanceOf<VerblijfplaatsBuitenland>()
                }
            }
        }

        Given("JSON with type Adres") {
            val json = """{"type":"Adres"}"""

            When("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                Then("an Adres is returned") {
                    result.shouldBeInstanceOf<Adres>()
                }
            }
        }

        Given("JSON with type VerblijfplaatsOnbekend") {
            val json = """{"type":"VerblijfplaatsOnbekend"}"""

            When("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                Then("a VerblijfplaatsOnbekend is returned") {
                    result.shouldBeInstanceOf<VerblijfplaatsOnbekend>()
                }
            }
        }

        Given("JSON with type Locatie") {
            val json = """{"type":"Locatie"}"""

            When("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                Then("a Locatie is returned") {
                    result.shouldBeInstanceOf<Locatie>()
                }
            }
        }

        Given("JSON with an unknown verblijfplaats type") {
            val json = """{"type":"OnbekendVerblijfplaatsType"}"""

            When("deserialize is called") {
                val exception = shouldThrow<InputValidationFailedException> {
                    deserializer.deserialize(createJsonParser(json), mockk(), mockk())
                }

                Then("InputValidationFailedException is thrown") {
                    exception.message!!.contains("OnbekendVerblijfplaatsType")
                }
            }
        }
    }
})
