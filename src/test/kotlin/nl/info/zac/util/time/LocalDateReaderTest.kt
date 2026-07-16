/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util.time

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalDateReaderTest : DescribeSpec({

    val localDateReader = LocalDateReader()
    val noAnnotations = emptyArray<Annotation>()
    val multivaluedMap = MultivaluedHashMap<String, String>()

    describe("isReadable") {
        it("can read LocalDate class") {
            localDateReader.isReadable(
                LocalDate::class.java,
                LocalDate::class.java,
                noAnnotations,
                MediaType.TEXT_PLAIN_TYPE
            ) shouldBe true
        }

        it("cannot read unknown classes") {
            localDateReader.isReadable(
                String::class.java,
                String::class.java,
                noAnnotations,
                MediaType.TEXT_PLAIN_TYPE
            ) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted string") {
            localDateReader.readFrom(
                LocalDate::class.java, LocalDate::class.java, noAnnotations, MediaType.TEXT_PLAIN_TYPE, multivaluedMap,
                "2024-03-11T10:44+01:00".byteInputStream()
            ) shouldBe LocalDate.parse("2024-03-11")
        }

        it("parses a different time zone") {
            localDateReader.readFrom(
                LocalDate::class.java, LocalDate::class.java, noAnnotations, MediaType.TEXT_PLAIN_TYPE, multivaluedMap,
                "2024-03-11T10:44+02:00".byteInputStream()
            ) shouldBe LocalDate.parse("2024-03-11")
        }

        describe("with mis-formatted data") {
            it("errors on empty string") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        LocalDate::class.java,
                        LocalDate::class.java,
                        noAnnotations,
                        MediaType.TEXT_PLAIN_TYPE,
                        multivaluedMap,
                        "".byteInputStream()
                    )
                }
            }

            it("errors with empty stream") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        LocalDate::class.java,
                        LocalDate::class.java,
                        noAnnotations,
                        MediaType.TEXT_PLAIN_TYPE,
                        multivaluedMap,
                        ByteArrayInputStream(byteArrayOf())
                    )
                }
            }

            it("errors on mis-formatted string") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        LocalDate::class.java,
                        LocalDate::class.java,
                        noAnnotations,
                        MediaType.TEXT_PLAIN_TYPE,
                        multivaluedMap,
                        "10.III.2024 @ 10:44+01:00".byteInputStream()
                    )
                }
            }
        }
    }
})
