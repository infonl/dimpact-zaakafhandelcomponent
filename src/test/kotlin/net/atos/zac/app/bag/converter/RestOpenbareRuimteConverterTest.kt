/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import nl.info.client.bag.model.createAdresIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHalBasis
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import java.net.URI

class RestOpenbareRuimteConverterTest : BehaviorSpec({

    context("convertToREST") {
        given("A null openbareRuimteIO") {
            `when`("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(
                    null as OpenbareRuimteIOHalBasis?,
                    createAdresIOHal()
                )

                then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }

        given("A null OpenbareRuimteIOHalBasis") {
            `when`("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(null as OpenbareRuimteIOHalBasis?)

                then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }

        given("A null OpenbareRuimteIOHal") {
            `when`("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(null as OpenbareRuimteIOHal?)

                then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }

        given("A ZaakobjectOpenbareRuimte with null objectIdentificatie") {
            val zaakobject = mockk<ZaakobjectOpenbareRuimte> {
                every { objectIdentificatie } returns null
            }

            `when`("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(zaakobject)

                then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }

        given("A RESTOpenbareRuimte and a Zaak") {
            val fakeOpenbareRuimteUrl = URI("https://example.com/openbareruimte/fakeId")
            val zaak = createZaak()
            val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(
                createZaakobjectOpenbareRuimte(bagobjectURI = fakeOpenbareRuimteUrl)
            )!!

            `when`("convertToZaakobject is called") {
                val result = RestOpenbareRuimteConverter.convertToZaakobject(restOpenbareRuimte, zaak)

                then("the result is a ZaakobjectOpenbareRuimte with the openbareRuimte URL set") {
                    result.getObject() shouldBe fakeOpenbareRuimteUrl
                }
            }
        }
    }
})
