/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import net.atos.zac.util.JsonbConfiguration
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import nl.info.zac.app.bag.model.RestAdresseerbaarObject
import nl.info.zac.app.bag.model.RestBagAdres
import nl.info.zac.app.bag.model.RestBagObjectGegevens
import nl.info.zac.app.bag.model.RestNummeraanduiding
import nl.info.zac.app.bag.model.RestOpenbareRuimte
import nl.info.zac.app.bag.model.RestPand
import nl.info.zac.app.bag.model.RestWoonplaats
import java.net.URI

class RestBagObjectJsonbDeserializerTest : BehaviorSpec({
    val jsonb = JsonbConfiguration().getContext(RestBagObjectGegevens::class.java)

    context("Deserializing REST BAG object gegevens") {
        given("JSON with BAG objects of every type") {
            val bagObjectTypes = mapOf(
                "ADRES" to RestBagAdres::class,
                "NUMMERAANDUIDING" to RestNummeraanduiding::class,
                "WOONPLAATS" to RestWoonplaats::class,
                "PAND" to RestPand::class,
                "OPENBARE_RUIMTE" to RestOpenbareRuimte::class,
                "ADRESSEERBAAR_OBJECT" to RestAdresseerbaarObject::class
            )

            `when`("the JSON is deserialized") {
                val restBagObjectGegevensList = bagObjectTypes.keys.map {
                    jsonb.fromJson(
                        """
                        {
                          "zaakUuid": "6f1d5c6a-3c55-4a4b-9a4d-1f0d6b8e2a11",
                          "redenWijzigen": "fakeRedenWijzigen",
                          "zaakobject": {
                            "bagObjectType": "$it",
                            "url": "https://example.com/bag/1",
                            "identificatie": "fakeIdentificatie"
                          }
                        }
                        """.trimIndent(),
                        RestBagObjectGegevens::class.java
                    )
                }

                then("each BAG object is deserialized to the REST class matching its BAG object type") {
                    restBagObjectGegevensList.map { it.zaakobject.shouldNotBeNull()::class } shouldBe bagObjectTypes.values.toList()
                    restBagObjectGegevensList.forEach {
                        it.redenWijzigen shouldBe "fakeRedenWijzigen"
                        it.zaakobject?.url shouldBe URI("https://example.com/bag/1")
                        it.zaakobject?.identificatie shouldBe "fakeIdentificatie"
                    }
                }
            }
        }

        given("JSON with a BAG address with a huisnummer and a woonplaats") {
            val json = """
                {
                  "zaakobject": {
                    "bagObjectType": "ADRES",
                    "huisnummer": 12,
                    "postcode": "1234AB",
                    "woonplaats": { "naam": "fakeWoonplaatsNaam" }
                  }
                }
            """.trimIndent()

            `when`("the JSON is deserialized") {
                val restBagObjectGegevens = jsonb.fromJson(json, RestBagObjectGegevens::class.java)

                then("the address fields are deserialized") {
                    with(restBagObjectGegevens.zaakobject.shouldBeInstanceOf<RestBagAdres>()) {
                        huisnummer shouldBe 12
                        postcode shouldBe "1234AB"
                        woonplaats?.naam shouldBe "fakeWoonplaatsNaam"
                    }
                }
            }
        }
    }
})
