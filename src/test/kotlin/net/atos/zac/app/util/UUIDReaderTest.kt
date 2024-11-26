/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class UUIDReaderTest : DescribeSpec({

    val uuidReader = UUIDReader()

    describe("isReadable") {
        it("can read UUID class") {
            uuidReader.isReadable(null, UUID::class.java, null, null) shouldBe true
        }

        it("cannot read unknown classes") {
            uuidReader.isReadable(null, String::class.java, null, null) shouldBe false
        }

        it("cannot read null as type") {
            uuidReader.isReadable(null, null, null, null) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted string") {
            uuidReader.readFrom(
                null, null, null, null, null,
                "290bc260-42a7-4547-8c68-f4d12c2f05de".byteInputStream()
            ) shouldBe UUID.fromString("290bc260-42a7-4547-8c68-f4d12c2f05de")
        }

        describe("with mis-formatted data") {
            it("errors on missing data") {
                shouldThrow<NullPointerException> {
                    uuidReader.readFrom(
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
                shouldThrow<IllegalArgumentException> {
                    uuidReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "".byteInputStream()
                    )
                }
            }

            it("errors on mis-formatted string") {
                shouldThrow<IllegalArgumentException> {
                    uuidReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "aa-bbb-cccc-ddddd-ee-ff".byteInputStream()
                    )
                }
            }
        }
    }
})
