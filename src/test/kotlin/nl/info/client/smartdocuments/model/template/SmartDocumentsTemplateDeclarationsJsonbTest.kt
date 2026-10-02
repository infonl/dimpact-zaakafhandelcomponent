/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.model.template

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.json.bind.JsonbBuilder

class SmartDocumentsTemplateDeclarationsJsonbTest : BehaviorSpec({
    val jsonb = JsonbBuilder.create()

    given("a template group as the SmartDocuments API returns it") {
        val json = """
            {
                "id": "fakeGroupId",
                "name": "fakeGroupName",
                "allDescendants": true,
                "accessible": true,
                "templates": [ { "id": "fakeTemplateId", "name": "fakeTemplateName", "favorite": true } ]
            }
        """.trimIndent()

        `when`("it is deserialized") {
            val templateGroup = jsonb.fromJson(json, SmartDocumentsResponseTemplateGroup::class.java)

            then("the boolean fields are read from the field names of the SmartDocuments API") {
                templateGroup.hasAllDescendants shouldBe true
                templateGroup.isAccessible shouldBe true
                templateGroup.templates?.single()?.isFavorite shouldBe true
            }
        }
    }
})
