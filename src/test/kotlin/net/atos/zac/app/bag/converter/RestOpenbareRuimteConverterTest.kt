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

    Context("convertToREST(OpenbareRuimteIOHalBasis, AdresIOHal) with null input") {
        Given("A null openbareRuimteIO") {
            When("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(
                    null as OpenbareRuimteIOHalBasis?,
                    createAdresIOHal()
                )

                Then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }
    }

    Context("convertToREST(OpenbareRuimteIOHalBasis) with null input") {
        Given("A null OpenbareRuimteIOHalBasis") {
            When("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(null as OpenbareRuimteIOHalBasis?)

                Then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }
    }

    Context("convertToREST(OpenbareRuimteIOHal) with null input") {
        Given("A null OpenbareRuimteIOHal") {
            When("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(null as OpenbareRuimteIOHal?)

                Then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }
    }

    Context("convertToREST(ZaakobjectOpenbareRuimte) with null objectIdentificatie") {
        Given("A ZaakobjectOpenbareRuimte with null objectIdentificatie") {
            val zaakobject = mockk<ZaakobjectOpenbareRuimte> {
                every { objectIdentificatie } returns null
            }

            When("convertToREST is called") {
                val result = RestOpenbareRuimteConverter.convertToREST(zaakobject)

                Then("null is returned") {
                    result.shouldBeNull()
                }
            }
        }
    }

    Context("convertToREST(ZaakobjectOpenbareRuimte) with valid data") {
        Given("A valid ZaakobjectOpenbareRuimte") {
            val fakeObjectUri = URI("https://example.com/openbareruimte/fakeId")
            val zaakobject = createZaakobjectOpenbareRuimte(bagobjectURI = fakeObjectUri)

            Then("the result fields are mapped from the object identificatie") {
                result!!.url shouldBe fakeObjectUri
                result.identificatie shouldBe "fakeIdentificatie"
                result.naam shouldBe "fakeopenbareRuimteNaam"
                result.woonplaatsNaam shouldBe "fakePlaatsNaam"
            }
        }
    }

    Context("convertToZaakobject") {
        Given("A RESTOpenbareRuimte and a Zaak") {
            val fakeOpenbareRuimteUrl = URI("https://example.com/openbareruimte/fakeId")
            val zaak = createZaak()
            val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(
                createZaakobjectOpenbareRuimte(bagobjectURI = fakeOpenbareRuimteUrl)
            )!!

            When("convertToZaakobject is called") {
                val result = RestOpenbareRuimteConverter.convertToZaakobject(restOpenbareRuimte, zaak)

                Then("the result is a ZaakobjectOpenbareRuimte with the openbareRuimte URL set") {
                    result.getObject() shouldBe fakeOpenbareRuimteUrl
                }
            }
        }
    }
})
