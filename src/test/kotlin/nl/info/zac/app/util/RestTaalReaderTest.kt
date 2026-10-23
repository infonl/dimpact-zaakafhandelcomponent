/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.util

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import nl.info.zac.app.configuration.model.RestTaal
import java.io.ByteArrayInputStream

class RestTaalReaderTest : BehaviorSpec({
    val restTaalReader = RestTaalReader()
    val restTaal = RestTaal(
        id = "fakeId",
        code = "fakeCode",
        naam = "fakeNaam",
        name = "fakeName",
        local = "fakeLocal"
    )

    fun readFrom(bytes: ByteArray) = restTaalReader.readFrom(
        type = RestTaal::class.java,
        genericType = RestTaal::class.java,
        annotations = emptyArray(),
        mediaType = MediaType.TEXT_PLAIN_TYPE,
        httpHeaders = MultivaluedHashMap(),
        entityStream = ByteArrayInputStream(bytes)
    )

    context("Checking whether a type is readable") {
        given("the RestTaal type") {
            `when`("isReadable is called") {
                val isReadable = restTaalReader.isReadable(
                    type = RestTaal::class.java,
                    genericType = RestTaal::class.java,
                    annotations = emptyArray(),
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("it is readable") {
                    isReadable shouldBe true
                }
            }
        }

        given("another type") {
            `when`("isReadable is called") {
                val isReadable = restTaalReader.isReadable(
                    type = String::class.java,
                    genericType = String::class.java,
                    annotations = emptyArray(),
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("it is not readable") {
                    isReadable shouldBe false
                }
            }
        }
    }

    context("Reading a RestTaal") {
        given("a JSON serialized RestTaal") {
            `when`("readFrom is called") {
                val readRestTaal = readFrom(ObjectMapper().writeValueAsBytes(restTaal))

                then("the RestTaal is returned") {
                    readRestTaal.shouldNotBeNull() shouldBeEqualToComparingFields restTaal
                }
            }
        }

        given("a JSON null") {
            `when`("readFrom is called") {
                val readRestTaal = readFrom("null".toByteArray())

                then("null is returned") {
                    readRestTaal shouldBe null
                }
            }
        }

        given("binary data that is not JSON") {
            `when`("readFrom is called") {
                val jsonParseException = shouldThrow<JsonParseException> { readFrom(byteArrayOf(12, 13, 14)) }

                then("a JSON parse exception is thrown") {
                    jsonParseException.message shouldStartWith "Illegal character ((CTRL-CHAR, code 12))"
                }
            }
        }

        given("a string that is not JSON") {
            `when`("readFrom is called") {
                val jsonParseException = shouldThrow<JsonParseException> { readFrom("unrecognized data".toByteArray()) }

                then("a JSON parse exception is thrown") {
                    jsonParseException.message shouldStartWith "Unrecognized token 'unrecognized'"
                }
            }
        }
    }
})
