/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.json.Json
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.stream.JsonParser
import nl.info.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging
import java.lang.reflect.Type

class AuditWijzigingJsonbDeserializerTest : BehaviorSpec({
    val deserializer = AuditWijzigingJsonbDeserializer()
    val parser = mockk<JsonParser>()
    val ctx = mockk<DeserializationContext>()
    val rtType = mockk<Type>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a JsonParser whose current value is an empty string") {
        // Old inbox documents and detached documents produce audit trail records
        // where the `wijzigingen` field is stored as "" instead of a JSON object
        every { parser.value } returns Json.createValue("")

        When("the deserializer is called") {
            val result = deserializer.deserialize(parser, ctx, rtType)

            Then("result should be null") {
                result.shouldBeNull()
            }
        }
    }

    Given("a JsonParser whose current value is a JSON object with a null oud and a nieuw enkelvoudiginformatieobject") {
        val nieuw = Json.createObjectBuilder()
            .add("url", "https://example.com/documenten/api/v1/enkelvoudiginformatieobjecten/abc")
            .build()
        val wijzigingen = Json.createObjectBuilder()
            .addNull("oud")
            .add("nieuw", nieuw)
            .build()
        every { parser.value } returns wijzigingen

        When("the deserializer is called") {
            val result = deserializer.deserialize(parser, ctx, rtType)

            Then("result should not be null") {
                result.shouldNotBeNull()
            }

            Then("result should be an EnkelvoudigInformatieobjectWijziging") {
                result.shouldBeInstanceOf<EnkelvoudigInformatieobjectWijziging>()
            }
        }
    }
})
