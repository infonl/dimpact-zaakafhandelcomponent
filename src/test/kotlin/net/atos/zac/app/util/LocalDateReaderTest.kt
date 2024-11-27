/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.util.time.LocalDateReader
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalDateReaderTest : DescribeSpec({

    val localDateReader = LocalDateReader()

    describe("isReadable") {
        it("can read LocalDate class") {
            localDateReader.isReadable(null, LocalDate::class.java, null, null) shouldBe true
        }

        it("cannot read unknown classes") {
            localDateReader.isReadable(null, String::class.java, null, null) shouldBe false
        }

        it("cannot read null as type") {
            localDateReader.isReadable(null, null, null, null) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted string") {
            localDateReader.readFrom(
                null, null, null, null, null,
                "2024-03-11T10:44+01:00".byteInputStream()
            ) shouldBe LocalDate.parse("2024-03-11")
        }

        it("parses a different time zone") {
            localDateReader.readFrom(
                null, null, null, null, null,
                "2024-03-11T10:44+02:00".byteInputStream()
            ) shouldBe LocalDate.parse("2024-03-11")
        }

        describe("with mis-formatted data") {
            it("returns null on missing data") {
                shouldThrow<NullPointerException> {
                    localDateReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                }
            }

            it("errors on empty string") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "".byteInputStream()
                    )
                }
            }

            it("errors with empty stream") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        ByteArrayInputStream(byteArrayOf())
                    )
                }
            }

            it("errors on mis-formatted string") {
                shouldThrow<DateTimeParseException> {
                    localDateReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "10.III.2024 @ 10:44+01:00".byteInputStream()
                    )
                }
            }
        }
    }
})
