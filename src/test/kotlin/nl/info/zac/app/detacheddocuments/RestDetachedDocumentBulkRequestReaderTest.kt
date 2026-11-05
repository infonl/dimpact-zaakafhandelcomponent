/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
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
            then("it only reads the bulk requests") {
                listOf(
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
                } shouldBe listOf(true, true, false)
            }
        }
    }

    context("Reading valid requests") {
        given("a bulk delete request with whole numbers") {
            then("it holds those ids") {
                read(RestDetachedDocumentBulkDeleteRequest::class.java, """{"ids":[1,9007199254740993]}""")
                    .ids shouldBe listOf(1L, 9_007_199_254_740_993L)
            }
        }

        given("a bulk link request") {
            then("it holds the ids and the zaak") {
                val request = read(
                    RestDetachedDocumentBulkLinkRequest::class.java,
                    """{"ids":[2],"nieuweZaakID":"fakeZaakId"}"""
                )
                request.ids shouldBe listOf(2L)
                request.nieuweZaakID shouldBe "fakeZaakId"
            }
        }

        given("a request without ids and without a zaak") {
            then("the validation of the request gets to refuse the empty values") {
                val request = read(RestDetachedDocumentBulkLinkRequest::class.java, "{}")
                request.ids shouldBe emptyList()
                request.nieuweZaakID shouldBe ""
            }
        }
    }

    context("Refusing a body that could delete the wrong document or is not a request") {
        withData(
            nameFn = { "body: '$it'" },
            """{"ids":[1.5]}""",
            """{"ids":[1.0]}""",
            """{"ids":[1e2]}""",
            """{"ids":[18446744073709551619]}""",
            """{"ids":[99999999999999999999]}""",
            """{"ids":["a"]}""",
            """{"ids":[null]}""",
            """{"ids":3}""",
            "[1]",
            "not json",
            ""
        ) { body ->
            shouldThrow<BadRequestException> {
                read(RestDetachedDocumentBulkDeleteRequest::class.java, body)
            }
        }
    }
})
