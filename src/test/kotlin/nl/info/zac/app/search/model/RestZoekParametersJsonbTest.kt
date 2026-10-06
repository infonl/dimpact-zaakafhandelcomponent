/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.json.bind.JsonbBuilder
import nl.info.zac.search.model.FilterParameters

class RestZoekParametersJsonbTest : BehaviorSpec({
    val jsonb = JsonbBuilder.create()

    given("search parameters as a saved zoekopdracht stores them") {
        val json = """
            {
                "page": 0,
                "rows": 25,
                "alleenMijnZaken": true,
                "alleenOpenstaandeZaken": true,
                "alleenAfgeslotenZaken": true,
                "alleenMijnTaken": true
            }
        """.trimIndent()

        `when`("they are deserialized") {
            val restZoekParameters = jsonb.fromJson(json, RestZoekParameters::class.java)

            then("the filters of a zoekopdracht saved before the rename are still applied") {
                restZoekParameters.isAlleenMijnZaken shouldBe true
                restZoekParameters.isAlleenOpenstaandeZaken shouldBe true
                restZoekParameters.isAlleenAfgeslotenZaken shouldBe true
                restZoekParameters.isAlleenMijnTaken shouldBe true
            }
        }
    }

    given("search parameters that only show the zaken of the logged-in user") {
        val restZoekParameters = RestZoekParameters(page = 0, rows = 25, isAlleenMijnZaken = true)

        `when`("they are serialized") {
            val json = jsonb.toJson(restZoekParameters)

            then("the field keeps the name that saved zoekopdrachten use") {
                json.contains("\"alleenMijnZaken\":true") shouldBe true
            }
        }
    }

    given("an inverted filter as a saved zoekopdracht stores it") {
        val json = """{ "values": ["fakeValue"], "inverse": true }"""

        `when`("it is deserialized") {
            val filterParameters = jsonb.fromJson(json, FilterParameters::class.java)

            then("the filter is still inverted") {
                filterParameters.isInverse shouldBe true
            }
        }

        `when`("it is serialized again") {
            val serializedJson = jsonb.toJson(jsonb.fromJson(json, FilterParameters::class.java))

            then("the field keeps the name that saved zoekopdrachten use") {
                serializedJson.contains("\"inverse\":true") shouldBe true
            }
        }
    }
})
