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
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse
import nl.info.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse
import nl.info.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse
import nl.info.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse
import nl.info.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse
import nl.info.zac.exception.InputValidationFailedException

class PersonenQueryResponseJsonbDeserializerTest : BehaviorSpec({
    val deserializer = PersonenQueryResponseJsonbDeserializer()

    context("deserialize") {
        given("JSON with type RaadpleegMetBurgerservicenummer") {
            val json = """{"type":"RaadpleegMetBurgerservicenummer","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a RaadpleegMetBurgerservicenummerResponse is returned") {
                    result.shouldBeInstanceOf<RaadpleegMetBurgerservicenummerResponse>()
                }
            }
        }

        given("JSON with type ZoekMetGeslachtsnaamEnGeboortedatum") {
            val json = """{"type":"ZoekMetGeslachtsnaamEnGeboortedatum","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a ZoekMetGeslachtsnaamEnGeboortedatumResponse is returned") {
                    result.shouldBeInstanceOf<ZoekMetGeslachtsnaamEnGeboortedatumResponse>()
                }
            }
        }

        given("JSON with type ZoekMetNaamEnGemeenteVanInschrijving") {
            val json = """{"type":"ZoekMetNaamEnGemeenteVanInschrijving","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a ZoekMetNaamEnGemeenteVanInschrijvingResponse is returned") {
                    result.shouldBeInstanceOf<ZoekMetNaamEnGemeenteVanInschrijvingResponse>()
                }
            }
        }

        given("JSON with type ZoekMetNummeraanduidingIdentificatie") {
            val json = """{"type":"ZoekMetNummeraanduidingIdentificatie","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a ZoekMetNummeraanduidingIdentificatieResponse is returned") {
                    result.shouldBeInstanceOf<ZoekMetNummeraanduidingIdentificatieResponse>()
                }
            }
        }

        given("JSON with type ZoekMetPostcodeEnHuisnummer") {
            val json = """{"type":"ZoekMetPostcodeEnHuisnummer","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a ZoekMetPostcodeEnHuisnummerResponse is returned") {
                    result.shouldBeInstanceOf<ZoekMetPostcodeEnHuisnummerResponse>()
                }
            }
        }

        given("JSON with type ZoekMetStraatHuisnummerEnGemeenteVanInschrijving") {
            val json = """{"type":"ZoekMetStraatHuisnummerEnGemeenteVanInschrijving","personen":[]}"""

            `when`("deserialize is called") {
                val result = deserializer.deserialize(createJsonParser(json), mockk(), mockk())

                then("a ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse is returned") {
                    result.shouldBeInstanceOf<ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse>()
                }
            }
        }

        given("JSON with an unknown type") {
            val json = """{"type":"OnbekendType"}"""

            `when`("deserialize is called") {
                val exception = shouldThrow<InputValidationFailedException> {
                    deserializer.deserialize(createJsonParser(json), mockk(), mockk())
                }

                then("InputValidationFailedException is thrown") {
                    exception.message!!.contains("OnbekendType") shouldBe true
                }
            }
        }
    }
})
