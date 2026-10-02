/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.json.bind.JsonbBuilder
import net.atos.zac.signalering.model.SignaleringType

class RestSignaleringInstellingenJsonbTest : BehaviorSpec({
    val jsonb = JsonbBuilder.create()

    given("signalering settings sent by the frontend") {
        val json = """{ "type": "ZAAK_OP_NAAM", "isDashboardEnabled": true, "isMailEnabled": true }"""

        `when`("they are deserialized") {
            val restSignaleringInstellingen = jsonb.fromJson(json, RestSignaleringInstellingen::class.java)

            then("both is-prefixed fields are read under their Kotlin property name") {
                restSignaleringInstellingen.isDashboardEnabled shouldBe true
                restSignaleringInstellingen.isMailEnabled shouldBe true
            }
        }
    }

    given("signalering settings sent to the frontend") {
        val restSignaleringInstellingen = RestSignaleringInstellingen(
            type = SignaleringType.Type.ZAAK_OP_NAAM,
            isDashboardEnabled = true,
            isMailEnabled = false
        )

        `when`("they are serialized") {
            val json = jsonb.toJson(restSignaleringInstellingen)

            then("both is-prefixed fields are written under their Kotlin property name") {
                json shouldContain "\"isDashboardEnabled\":true"
                json shouldContain "\"isMailEnabled\":false"
            }
        }
    }
})
