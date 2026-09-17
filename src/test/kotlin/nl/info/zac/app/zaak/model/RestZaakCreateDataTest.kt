/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.json.bind.JsonbBuilder

class RestZaakCreateDataTest : BehaviorSpec({
    val jsonb = JsonbBuilder.create()

    given("a JSON body that marks the zaak as zaakspecifiek geautoriseerd") {
        val json = """
            {
                "omschrijving": "fakeOmschrijving",
                "zaaktype": { "uuid": "6b2f0b4e-b6a1-4f3c-9c04-9d5ca6f03f1f" },
                "isZaakspecifiekGeautoriseerd": true
            }
        """.trimIndent()

        `when`("it is deserialized into rest zaak create data") {
            val restZaakCreateData = jsonb.fromJson(json, RestZaakCreateData::class.java)

            then("the zaakspecifieke autorisatie request is preserved") {
                restZaakCreateData.isZaakspecifiekGeautoriseerd shouldBe true
            }
        }
    }

    given("a JSON body that explicitly does not mark the zaak as zaakspecifiek geautoriseerd") {
        val json = """
            {
                "omschrijving": "fakeOmschrijving",
                "zaaktype": { "uuid": "6b2f0b4e-b6a1-4f3c-9c04-9d5ca6f03f1f" },
                "isZaakspecifiekGeautoriseerd": false
            }
        """.trimIndent()

        `when`("it is deserialized into rest zaak create data") {
            val restZaakCreateData = jsonb.fromJson(json, RestZaakCreateData::class.java)

            then("it is distinguishable from a body that omits the field") {
                restZaakCreateData.isZaakspecifiekGeautoriseerd shouldBe false
            }
        }
    }

    given("a JSON body that omits the zaakspecifieke autorisatie field") {
        val json = """
            {
                "omschrijving": "fakeOmschrijving",
                "zaaktype": { "uuid": "6b2f0b4e-b6a1-4f3c-9c04-9d5ca6f03f1f" }
            }
        """.trimIndent()

        `when`("it is deserialized into rest zaak create data") {
            val restZaakCreateData = jsonb.fromJson(json, RestZaakCreateData::class.java)

            then("no zaakspecifieke autorisatie change is requested") {
                restZaakCreateData.isZaakspecifiekGeautoriseerd shouldBe null
            }
        }
    }

    given("rest zaak create data that marks the zaak as zaakspecifiek geautoriseerd") {
        val restZaakCreateData = createRestZaakCreateData(isZaakspecifiekGeautoriseerd = true)

        `when`("it is serialized") {
            val json = jsonb.toJson(restZaakCreateData)

            then("the field keeps the name the client sends") {
                json shouldContain "\"isZaakspecifiekGeautoriseerd\":true"
            }
        }
    }
})
