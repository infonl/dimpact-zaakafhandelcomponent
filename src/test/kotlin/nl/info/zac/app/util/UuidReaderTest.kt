/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import java.util.UUID

class UuidReaderTest : BehaviorSpec({
    val uuidReader = UuidReader()

    fun readFrom(text: String) = uuidReader.readFrom(
        type = UUID::class.java,
        genericType = UUID::class.java,
        annotations = emptyArray(),
        mediaType = MediaType.TEXT_PLAIN_TYPE,
        httpHeaders = MultivaluedHashMap(),
        entityStream = text.byteInputStream()
    )

    context("Checking whether a type is readable") {
        given("the UUID type") {
            `when`("isReadable is called") {
                val isReadable = uuidReader.isReadable(
                    type = UUID::class.java,
                    genericType = UUID::class.java,
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
                val isReadable = uuidReader.isReadable(
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

    context("Reading a UUID") {
        given("a well formatted UUID string") {
            `when`("readFrom is called") {
                val uuid = readFrom("290bc260-42a7-4547-8c68-f4d12c2f05de")

                then("the UUID is returned") {
                    uuid shouldBe UUID.fromString("290bc260-42a7-4547-8c68-f4d12c2f05de")
                }
            }
        }

        given("an empty string") {
            `when`("readFrom is called") {
                then("an illegal argument exception is thrown") {
                    shouldThrow<IllegalArgumentException> { readFrom("") }
                }
            }
        }

        given("a mis-formatted UUID string") {
            `when`("readFrom is called") {
                then("an illegal argument exception is thrown") {
                    shouldThrow<IllegalArgumentException> { readFrom("aa-bbb-cccc-ddddd-ee-ff") }
                }
            }
        }
    }
})
