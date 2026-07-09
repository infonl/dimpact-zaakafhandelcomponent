/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.drc.model.generated.StatusEnum

class EnkelvoudigInformatieObjectStatusEnumReaderTest : DescribeSpec({
    val statusReader = EnkelvoudigInformatieObjectStatusEnumReader()

    describe("isReadable") {
        it("can read StatusEnum class") {
            statusReader.isReadable(null, StatusEnum::class.java, null, null) shouldBe true
        }

        it("cannot read unknown classes") {
            statusReader.isReadable(null, String::class.java, null, null) shouldBe false
        }

        it("cannot read null as type") {
            statusReader.isReadable(null, null, null, null) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted fully specified enum value") {
            statusReader.readFrom(
                null,
                null,
                null,
                null,
                null,
                "nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject.StatusEnum.in_bewerking".byteInputStream()
            ) shouldBe StatusEnum.valueOf("IN_BEWERKING")
        }

        it("parses a well formatted enum value") {
            statusReader.readFrom(
                null,
                null,
                null,
                null,
                null,
                "in_bewerking".byteInputStream()
            ) shouldBe StatusEnum.valueOf("IN_BEWERKING")
        }

        describe("with mis-formatted data") {
            it("errors on missing data") {
                shouldThrow<NullPointerException> {
                    statusReader.readFrom(
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
                    statusReader.readFrom(
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
                    statusReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "missingPackage.unknownStatus".byteInputStream()
                    )
                }
            }
        }
    }
})
