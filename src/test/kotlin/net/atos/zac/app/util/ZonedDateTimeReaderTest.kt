package net.atos.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.util.time.ZonedDateTimeReader
import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

class ZonedDateTimeReaderTest : DescribeSpec({

    val zonedDateTimeReader = ZonedDateTimeReader()

    describe("isReadable") {
        it("can read ZonedDateTime class") {
            zonedDateTimeReader.isReadable(null, ZonedDateTime::class.java, null, null) shouldBe true
        }

        it("cannot read unknown classes") {
            zonedDateTimeReader.isReadable(null, String::class.java, null, null) shouldBe false
        }

        it("cannot read null as type") {
            zonedDateTimeReader.isReadable(null, null, null, null) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted string") {
            zonedDateTimeReader.readFrom(
                null, null, null, null, null,
                "2024-03-11T10:44+01:00".byteInputStream()
            ) shouldBe ZonedDateTime.parse("2024-03-11T10:44+01:00")
        }

        describe("with mis-formatted data") {
            it("errors on missing data") {
                shouldThrow<NullPointerException> {
                    zonedDateTimeReader.readFrom(
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
                    zonedDateTimeReader.readFrom(
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
                    zonedDateTimeReader.readFrom(
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
                    zonedDateTimeReader.readFrom(
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
