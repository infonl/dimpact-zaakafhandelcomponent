/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkDeleteRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest

class RestDetachedDocumentBulkRequestReaderTest : BehaviorSpec({
    val reader = RestDetachedDocumentBulkRequestReader()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> read(type: Class<T>, body: String) = reader.readFrom(
        type = type as Class<Any>,
        genericType = type,
        annotations = emptyArray(),
        mediaType = MediaType.APPLICATION_JSON_TYPE,
        httpHeaders = MultivaluedHashMap(),
        entityStream = body.byteInputStream()
    ) as T

    context("Deciding which types it reads") {
        given("the two bulk requests and another type") {
            `when`("it is asked whether it can read each type") {
                val readableTypes = listOf(
                    RestDetachedDocumentBulkDeleteRequest::class.java,
                    RestDetachedDocumentBulkLinkRequest::class.java,
                    String::class.java
                ).map {
                    reader.isReadable(
                        type = it,
                        genericType = it,
                        annotations = emptyArray(),
                        mediaType = MediaType.APPLICATION_JSON_TYPE
                    )
                }

                then("it only reads the bulk requests") {
                    readableTypes shouldBe listOf(true, true, false)
                }
            }
        }
    }

    context("Reading valid requests") {
        given("a bulk delete request with whole numbers") {
            `when`("the request is read") {
                val request = read(
                    RestDetachedDocumentBulkDeleteRequest::class.java,
                    """{"ids":[1,9007199254740993]}"""
                )

                then("it holds those ids") {
                    request.ids shouldBe listOf(1L, 9_007_199_254_740_993L)
                }
            }
        }

        given("a bulk link request") {
            `when`("the request is read") {
                val request = read(
                    RestDetachedDocumentBulkLinkRequest::class.java,
                    """{"ids":[2],"nieuweZaakID":"fakeZaakId"}"""
                )

                then("it holds the ids and the zaak") {
                    request.ids shouldBe listOf(2L)
                    request.nieuweZaakID shouldBe "fakeZaakId"
                }
            }
        }

        given("a request without ids and without a zaak") {
            `when`("the request is read") {
                val request = read(RestDetachedDocumentBulkLinkRequest::class.java, "{}")

                then("the validation of the request gets to refuse the empty values") {
                    request.ids shouldBe emptyList()
                    request.nieuweZaakID shouldBe ""
                }
            }
        }
    }

    context("Refusing a body that could delete the wrong document or is not a request") {
        val notAJsonObject = "The request body is not a valid JSON object"
        listOf(
            """{"ids":[1.5]}""" to "The id '1.5' is not a whole number",
            """{"ids":[1.0]}""" to "The id '1.0' is not a whole number",
            """{"ids":[1e2]}""" to "The id '1E+2' is not a whole number",
            """{"ids":[18446744073709551619]}""" to "The id '18446744073709551619' is too large",
            """{"ids":[99999999999999999999]}""" to "The id '99999999999999999999' is too large",
            """{"ids":["a"]}""" to "The id '\"a\"' is not a whole number",
            """{"ids":[null]}""" to "The id 'null' is not a whole number",
            """{"ids":3}""" to "The ids must be a list of whole numbers",
            "[1]" to notAJsonObject,
            "not json" to notAJsonObject,
            "" to notAJsonObject
        ).forEach { (body, expectedMessage) ->
            given("the body '$body'") {
                `when`("the request is read") {
                    val badRequestException = shouldThrow<BadRequestException> {
                        read(RestDetachedDocumentBulkDeleteRequest::class.java, body)
                    }

                    then("it is refused with the reason '$expectedMessage'") {
                        badRequestException.message shouldBe expectedMessage
                    }
                }
            }
        }
    }
})
