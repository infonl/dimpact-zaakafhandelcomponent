/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import nl.info.client.zgw.shared.model.FieldValidationError
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.model.ZgwError
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

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
})
