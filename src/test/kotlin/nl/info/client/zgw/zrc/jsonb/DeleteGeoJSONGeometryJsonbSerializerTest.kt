/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry

class DeleteGeoJSONGeometryJsonbSerializerTest : BehaviorSpec({
    val deleteGeoJSONGeometryJsonbSerializer = DeleteGeoJSONGeometryJsonbSerializer()
    val jsonGenerator = mockk<JsonGenerator>()
    val serializationContext = mockk<SerializationContext>()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A 'deleted Geo JSON geometry' object") {
        val deleteGeoJSONGeometry = DeleteGeoJSONGeometry()
        every { jsonGenerator.writeNull() } returns jsonGenerator

        When("the object is serialized using the 'geometry to be deleted' JSONB serializer") {
            deleteGeoJSONGeometryJsonbSerializer.serialize(
                deleteGeoJSONGeometry,
                jsonGenerator,
                serializationContext
            )

            Then("the object should be serialised to a null value") {
                verify(exactly = 1) { jsonGenerator.writeNull() }
            }
        }
    }
})
