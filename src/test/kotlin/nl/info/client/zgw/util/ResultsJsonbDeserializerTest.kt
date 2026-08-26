/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.json.Json
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.stream.JsonParser
import nl.info.client.zgw.shared.model.FieldValidationError
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.model.ZgwError
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URI

private fun resultsTypeOf(itemType: Class<*>): Type = object : ParameterizedType {
    override fun getRawType() = Results::class.java
    override fun getActualTypeArguments() = arrayOf<Type>(itemType)
    override fun getOwnerType() = null
}

class ResultsJsonbDeserializerTest : BehaviorSpec({
    given(
        "the shared Jsonb instance used by all ZGW REST clients, which deserializes " +
            "'Results<T>' for many different concrete T"
    ) {
        val jsonb = JsonbConfiguration().getContext(Results::class.java)

        val zgwErrorJson = """
            {"count":1,"results":[
                {"type":null,"code":"fakeCode","title":"fakeTitle","status":404,"detail":"fakeDetail","instance":null}
            ],"next":null,"previous":null}
        """.trimIndent()
        val fieldValidationErrorJson = """
            {"count":1,"results":[
                {"name":"fakeName","code":"fakeCode","reason":"fakeReason"}
            ],"next":null,"previous":null}
        """.trimIndent()

        `when`("Results<ZgwError> is deserialized, followed by Results<FieldValidationError>") {
            val zgwErrorResults: Results<ZgwError> = jsonb.fromJson(zgwErrorJson, resultsTypeOf(ZgwError::class.java))
            val fieldValidationErrorResults: Results<FieldValidationError> =
                jsonb.fromJson(fieldValidationErrorJson, resultsTypeOf(FieldValidationError::class.java))

            then("each Results still holds its own item type, not a leftover from the other deserialization") {
                zgwErrorResults.results().single().shouldBeInstanceOf<ZgwError>()
                zgwErrorResults.results().single().status shouldBe 404

                fieldValidationErrorResults.results().single().shouldBeInstanceOf<FieldValidationError>()
                fieldValidationErrorResults.results().single().reason shouldBe "fakeReason"
            }
        }
    }

    given("the ResultsJsonbDeserializer used directly") {
        val deserializer = ResultsJsonbDeserializer()
        val parser = mockk<JsonParser>()
        val deserializationContext = mockk<DeserializationContext>()
        val runtimeType = resultsTypeOf(FieldValidationError::class.java)

        afterEach { checkUnnecessaryStub() }

        `when`("the parser's current value is not a JSON object") {
            every { parser.value } returns Json.createValue("not an object")

            then("it returns null") {
                deserializer.deserialize(parser, deserializationContext, runtimeType).shouldBeNull()
            }
        }

        `when`("results is explicitly JSON null") {
            val jsonObject = Json.createObjectBuilder()
                .add("count", 0)
                .addNull("results")
                .build()
            every { parser.value } returns jsonObject

            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it does not throw and results defaults to an empty list") {
                result?.results() shouldBe emptyList()
            }
        }

        `when`("results is absent and next/previous are real URIs") {
            val jsonObject = Json.createObjectBuilder()
                .add("count", 0)
                .add("next", "https://example.com/next")
                .add("previous", "https://example.com/previous")
                .build()
            every { parser.value } returns jsonObject

            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("results defaults to an empty list and next/previous are parsed as URIs") {
                result?.results() shouldBe emptyList()
                result?.next() shouldBe URI("https://example.com/next")
                result?.previous() shouldBe URI("https://example.com/previous")
            }
        }
    }
})
