/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import jakarta.json.bind.JsonbBuilder

class RestDetachedDocumentJsonbTest : BehaviorSpec({
    given("a locked detached document that no longer exists in Open Zaak") {
        val restDetachedDocument = RestDetachedDocument(isVergrendeld = true, isOntbreektInOpenZaak = true)

        `when`("it is converted to JSON") {
            val json = JsonbBuilder.create().use { it.toJson(restDetachedDocument) }

            then("the fields have the names that the frontend reads") {
                json shouldContain "\"isVergrendeld\":true"
                json shouldContain "\"isOntbreektInOpenZaak\":true"
            }
        }
    }
})
