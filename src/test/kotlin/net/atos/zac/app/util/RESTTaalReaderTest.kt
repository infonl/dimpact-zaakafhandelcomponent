package net.atos.zac.app.util

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import net.atos.zac.app.configuratie.model.RestTaal
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RESTTaalReaderTest : DescribeSpec({

    val taalReader = RESTTaalReader()
    val taal = RestTaal().apply {
        this.id = "id"
        this.code = "code"
        this.naam = "naam"
        this.name = "name"
        this.local = "local"
    }

    describe("isReadable") {
        it("can read RESTTaal class") {
            taalReader.isReadable(null, RestTaal::class.java, null, null) shouldBe true
        }

        it("cannot read unknown classes") {
            taalReader.isReadable(null, String::class.java, null, null) shouldBe false
        }

        it("cannot read null as type") {
            taalReader.isReadable(null, null, null, null) shouldBe false
        }
    }

    describe("readFrom") {
        it("parses a well formatted string") {
            val baos = ByteArrayOutputStream()
            ObjectMapper().writeValue(baos, taal)

            taalReader.readFrom(
                null, null, null, null, null,
                ByteArrayInputStream(baos.toByteArray())
            ) shouldBeEqualToComparingFields taal
        }

        describe("with mis-formatted data") {
            it("errors on missing data") {
                shouldThrow<IllegalArgumentException> {
                    taalReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                }
            }

            it("errors on incorrect data") {
                shouldThrow<JsonParseException> {
                    taalReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        ByteArrayInputStream(byteArrayOf(12, 13, 14))
                    )
                }
            }

            it("errors on mis-formatted string") {
                shouldThrow<JsonParseException> {
                    taalReader.readFrom(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "unrecognized data".byteInputStream()
                    )
                }
            }
        }
    }
})
