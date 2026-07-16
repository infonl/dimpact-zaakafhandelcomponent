/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.stream.JsonParser
import java.lang.reflect.Type
import java.net.URISyntaxException

class URIJsonbDeserializerTest : BehaviorSpec({
    val deserializer = URIJsonbDeserializer()
    val parser = mockk<JsonParser>()
    val deserializationContext = mockk<DeserializationContext>()
    val runtimeType = mockk<Type>()

    afterEach {
        checkUnnecessaryStub()
    }

    given("a JsonParser whose current value is a non-empty URI string") {
        every { parser.string } returns "https://example.com/path"

        `when`("deserialize is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("the parsed URI is returned") {
                result.shouldNotBeNull()
                result.toString() shouldBe "https://example.com/path"
            }
        }
    }

    given("a JsonParser whose current value is an empty string") {
        every { parser.string } returns ""

        `when`("deserialize is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("null is returned") {
                result.shouldBeNull()
            }
        }
    }

    given("a JsonParser that raises IllegalStateException because it is in the VALUE_NULL state") {
        every { parser.string } throws IllegalStateException("VALUE_NULL")

        `when`("deserialize is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("null is returned instead of the exception propagating") {
                result.shouldBeNull()
            }
        }
    }

    given("a JsonParser whose current value is an invalid URI string") {
        every { parser.string } returns "not a valid uri \\"

        `when`("deserialize is called") {
            val exception = shouldThrow<RuntimeException> {
                deserializer.deserialize(parser, deserializationContext, runtimeType)
            }

            then("a RuntimeException wrapping URISyntaxException is thrown") {
                exception.cause.shouldNotBeNull()
                exception.cause!!::class shouldBe URISyntaxException::class
            }
        }
    }
})
