package net.atos.zac.app.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject

class EnkelvoudigInformatieObjectStatusEnumReaderTest : DescribeSpec({
    val statusReader = EnkelvoudigInformatieObjectStatusEnumReader()

    describe("isReadable") {
        it("can read StatusEnum class") {
            statusReader.isReadable(null, EnkelvoudigInformatieObject.StatusEnum::class.java, null, null) shouldBe true
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
                "net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject.StatusEnum.in_bewerking".byteInputStream()
            ) shouldBe EnkelvoudigInformatieObject.StatusEnum.valueOf("IN_BEWERKING")
        }

        it("parses a well formatted enum value") {
            statusReader.readFrom(
                null,
                null,
                null,
                null,
                null,
                "in_bewerking".byteInputStream()
            ) shouldBe EnkelvoudigInformatieObject.StatusEnum.valueOf("IN_BEWERKING")
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
