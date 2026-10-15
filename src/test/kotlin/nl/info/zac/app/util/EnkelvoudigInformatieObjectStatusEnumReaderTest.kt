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
import nl.info.client.zgw.drc.model.generated.StatusEnum

class EnkelvoudigInformatieObjectStatusEnumReaderTest : BehaviorSpec({
    val statusReader = EnkelvoudigInformatieObjectStatusEnumReader()

    fun readFrom(value: String) = statusReader.readFrom(
        type = StatusEnum::class.java,
        genericType = StatusEnum::class.java,
        annotations = emptyArray(),
        mediaType = MediaType.TEXT_PLAIN_TYPE,
        httpHeaders = MultivaluedHashMap(),
        entityStream = value.byteInputStream()
    )

    context("isReadable") {
        given("the StatusEnum type") {
            `when`("it is checked whether the reader can read it") {
                val isReadable = statusReader.isReadable(
                    type = StatusEnum::class.java,
                    genericType = StatusEnum::class.java,
                    annotations = emptyArray(),
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("the reader can read it") {
                    isReadable shouldBe true
                }
            }
        }

        given("another type") {
            `when`("it is checked whether the reader can read it") {
                val isReadable = statusReader.isReadable(
                    type = String::class.java,
                    genericType = String::class.java,
                    annotations = emptyArray(),
                    mediaType = MediaType.TEXT_PLAIN_TYPE
                )

                then("the reader cannot read it") {
                    isReadable shouldBe false
                }
            }
        }
    }

    context("readFrom") {
        given("a fully specified enum value") {
            `when`("it is read") {
                val status = readFrom(
                    "nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject.StatusEnum.in_bewerking"
                )

                then("the class prefix is removed and the enum value is returned") {
                    status shouldBe StatusEnum.IN_BEWERKING
                }
            }
        }

        given("an enum value without class prefix") {
            `when`("it is read") {
                val status = readFrom("in_bewerking")

                then("the enum value is returned") {
                    status shouldBe StatusEnum.IN_BEWERKING
                }
            }
        }

        given("an empty string") {
            `when`("it is read") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> { readFrom("") }

                then("an exception is thrown that no enum constant matches") {
                    illegalArgumentException.message shouldBe
                        "No enum constant nl.info.client.zgw.drc.model.generated.StatusEnum."
                }
            }
        }

        given("a value with an unknown prefix") {
            `when`("it is read") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    readFrom("missingPackage.unknownStatus")
                }

                then("an exception is thrown that no enum constant matches") {
                    illegalArgumentException.message shouldBe
                        "No enum constant nl.info.client.zgw.drc.model.generated.StatusEnum.MISSINGPACKAGE.UNKNOWNSTATUS"
                }
            }
        }
    }
})
