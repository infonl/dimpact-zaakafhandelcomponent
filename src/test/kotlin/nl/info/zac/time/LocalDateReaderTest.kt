/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.time

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalDateReaderTest : BehaviorSpec({
    val localDateReader = LocalDateReader()
    val noAnnotations = emptyArray<Annotation>()
    val multivaluedMap = MultivaluedHashMap<String, String>()

    fun read(inputStream: InputStream) =
        localDateReader.readFrom(
            aClass = LocalDate::class.java,
            type = LocalDate::class.java,
            annotations = noAnnotations,
            mediaType = MediaType.TEXT_PLAIN_TYPE,
            multivaluedMap = multivaluedMap,
            inputStream = inputStream
        )

    context("Checking whether a type can be read") {
        given("the LocalDate class") {
            `when`("asking whether it is readable") {
                val isReadable = localDateReader.isReadable(
                    aClass = LocalDate::class.java,
                    type = LocalDate::class.java,
                    annotations = noAnnotations,
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("it is readable") {
                    isReadable shouldBe true
                }
            }
        }

        given("another class") {
            `when`("asking whether it is readable") {
                val isReadable = localDateReader.isReadable(
                    aClass = String::class.java,
                    type = String::class.java,
                    annotations = noAnnotations,
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("it is not readable") {
                    isReadable shouldBe false
                }
            }
        }
    }

    context("Reading a date from a multipart form field") {
        given("a date without a time, as the frontend sends a picked date") {
            `when`("reading it") {
                val localDate = read("2026-01-30".byteInputStream())

                then("it is that date") {
                    localDate shouldBe LocalDate.of(2026, 1, 30)
                }
            }
        }

        given("a date with a time and the offset of Europe/Amsterdam") {
            `when`("reading it") {
                val localDate = read("2024-03-11T10:44+01:00".byteInputStream())

                then("it is the date that was written") {
                    localDate shouldBe LocalDate.parse("2024-03-11")
                }
            }
        }

        given("a date with a time and another offset") {
            `when`("reading it") {
                val localDate = read("2024-03-11T10:44+02:00".byteInputStream())

                then("it is the date that was written") {
                    localDate shouldBe LocalDate.parse("2024-03-11")
                }
            }
        }

        listOf("", "10.III.2024 @ 10:44+01:00").forEach { malformedDate ->
            given("the malformed date '$malformedDate'") {
                `when`("reading it") {
                    val dateTimeParseException = shouldThrow<DateTimeParseException> {
                        read(malformedDate.byteInputStream())
                    }

                    then("it is rejected") {
                        dateTimeParseException.parsedString shouldBe malformedDate
                    }
                }
            }
        }

        given("an empty stream") {
            `when`("reading it") {
                val dateTimeParseException = shouldThrow<DateTimeParseException> {
                    read(InputStream.nullInputStream())
                }

                then("it is rejected") {
                    dateTimeParseException.parsedString shouldBe ""
                }
            }
        }
    }
})
