/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.atos.zac.app.bag.model.RESTAdresseerbaarObject
import net.atos.zac.app.bag.model.RESTBAGAdres
import net.atos.zac.app.bag.model.RESTBAGObjectGegevens
import net.atos.zac.app.bag.model.RESTNummeraanduiding
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.bag.model.RESTWoonplaats
import java.net.URI

class RestBagObjectJsonbDeserializerTest : BehaviorSpec({
    val jsonb = JsonbConfiguration().getContext(RESTBAGObjectGegevens::class.java)

    context("Deserializing REST BAG object gegevens") {
        given("JSON with BAG objects of every type") {
            val bagObjectTypes = mapOf(
                "ADRES" to RESTBAGAdres::class,
                "NUMMERAANDUIDING" to RESTNummeraanduiding::class,
                "WOONPLAATS" to RESTWoonplaats::class,
                "PAND" to RESTPand::class,
                "OPENBARE_RUIMTE" to RESTOpenbareRuimte::class,
                "ADRESSEERBAAR_OBJECT" to RESTAdresseerbaarObject::class
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
                        RESTBAGObjectGegevens::class.java
                    )
                }

                then("each BAG object is deserialized to the REST class matching its BAG object type") {
                    restBagObjectGegevensList.map { it.zaakobject::class } shouldBe bagObjectTypes.values.toList()
                    restBagObjectGegevensList.forEach {
                        it.redenWijzigen shouldBe "fakeRedenWijzigen"
                        it.zaakobject.url shouldBe URI("https://example.com/bag/1")
                        it.zaakobject.identificatie shouldBe "fakeIdentificatie"
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
                val restBagObjectGegevens = jsonb.fromJson(json, RESTBAGObjectGegevens::class.java)

                then("the address fields are deserialized") {
                    with(restBagObjectGegevens.zaakobject.shouldBeInstanceOf<RESTBAGAdres>()) {
                        huisnummer shouldBe 12
                        postcode shouldBe "1234AB"
                        woonplaats.naam shouldBe "fakeWoonplaatsNaam"
                    }
                }
            }
        }
    }
})
