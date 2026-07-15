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
import nl.info.client.brp.model.generated.DatumOnbekend
import nl.info.client.brp.model.generated.JaarDatum
import nl.info.client.brp.model.generated.JaarMaandDatum
import nl.info.client.brp.model.generated.VolledigeDatum
import nl.info.zac.exception.InputValidationFailedException

class AbstractDatumJsonbDeserializerTest : BehaviorSpec({
    val deserializer = AbstractDatumJsonbDeserializer()

    context("deserialize") {
        given("JSON with type Datum") {
            val json = """{"type":"Datum","datum":"2024-01-01"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a VolledigeDatum is returned") {
                    result.shouldBeInstanceOf<VolledigeDatum>()
                }
            }
        }

        given("JSON with type DatumOnbekend") {
            val json = """{"type":"DatumOnbekend"}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a DatumOnbekend is returned") {
                    result.shouldBeInstanceOf<DatumOnbekend>()
                }
            }
        }

        given("JSON with type JaarDatum") {
            val json = """{"type":"JaarDatum","jaar":2024}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a JaarDatum is returned") {
                    result.shouldBeInstanceOf<JaarDatum>()
                }
            }
        }

        given("JSON with type JaarMaandDatum") {
            val json = """{"type":"JaarMaandDatum","jaar":2024,"maand":6}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a JaarMaandDatum is returned") {
                    result.shouldBeInstanceOf<JaarMaandDatum>()
                }
            }
        }

        given("JSON with an unknown datum type") {
            val json = """{"type":"OnbekendDatumType"}"""

            `when`("deserialize is called") {
                val exception = shouldThrow<InputValidationFailedException> {
                    deserializer.deserialize(createJsonParser(json), mockk(), mockk())
                }
                then("InputValidationFailedException is thrown") {
                    exception.message!!.contains("OnbekendDatumType") shouldBe true
                }
            }
        }
    }
})
