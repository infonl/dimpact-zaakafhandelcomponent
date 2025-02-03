/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.GeometryToBeDeleted
import net.atos.client.zgw.zrc.model.Polygon
import net.atos.client.zgw.zrc.model.createPoint
import net.atos.client.zgw.zrc.model.createPoint2D
import java.lang.IllegalArgumentException

class GeometryJsonbSerializerTest : BehaviorSpec({
    val geometryJsonbSerializer = GeometryJsonbSerializer()
    val jsonGenerator = mockk<JsonGenerator>()
    val serializationContext = mockk<SerializationContext>()

    Given("A point geometry object") {
        val point = createPoint(
            coordinates = createPoint2D(
                latitude = 32.34,
                longitude = 45.67
            )
        )
        val jsonStringSlot = slot<String>()
        every { jsonGenerator.write(capture(jsonStringSlot)) } returns jsonGenerator

        When("the object is serialized using the geometry JSONB serializer") {
            geometryJsonbSerializer.serialize(point, jsonGenerator, serializationContext)

            Then("the object should be serialised to a point JSON string") {
                jsonStringSlot.captured shouldBe """
                    {"type":"Point","coordinates":[32.34,45.67]}
                """.trimIndent()
            }
        }
    }

    Given("A 'geometry to be deleted' object") {
        val geometryToBeDeleted = GeometryToBeDeleted()
        every { jsonGenerator.writeNull() } returns jsonGenerator

        When("the object is serialized using the geometry JSONB serializer") {
            geometryJsonbSerializer.serialize(geometryToBeDeleted, jsonGenerator, serializationContext)

            Then("the object should be serialised to a null value") {
                verify(exactly = 1) { jsonGenerator.writeNull() }
            }
        }
    }

    Given("A polygon geometry object") {
        val polygon = Polygon()

        When("the object is serialized using the geometry JSONB serializer") {
            val exception = shouldThrow<IllegalArgumentException> {
                geometryJsonbSerializer.serialize(polygon, jsonGenerator, serializationContext)
            }

            Then("an exception should be thrown") {
                exception.message shouldBe "Polygon serialization is not implemented"
            }
        }
    }

    Given("A geometry collection geometry object") {
        val geometryCollection = GeometryCollection()

        When("the object is serialized using the geometry JSONB serializer") {
            val exception = shouldThrow<IllegalArgumentException> {
                geometryJsonbSerializer.serialize(geometryCollection, jsonGenerator, serializationContext)
            }

            Then("an exception should be thrown") {
                exception.message shouldBe "GeometryCollection serialization is not implemented"
            }
        }
    }
})
