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
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import nl.info.client.bag.model.createAdresIOHal
import nl.info.client.bag.model.createHalLink
import nl.info.client.bag.model.createOpenbareRuimte
import nl.info.client.bag.model.createWoonplaats
import nl.info.client.bag.model.createWoonplaatsIOHalBasis
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.OpenbareRuimteEmbedded
import nl.info.client.bag.model.generated.OpenbareRuimteLinks
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeOpenbareRuimte
import nl.info.client.bag.model.generated.WoonplaatsLinks
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHalBasis
import nl.info.client.zgw.model.createObjectOpenbareRuimte
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
                    result.`object` shouldBe fakeOpenbareRuimteUrl
                }
            }
        }
    }

    context("Converting a BAG openbare ruimte") {
        given("A BAG openbare ruimte with an embedded woonplaats") {
            val openbareRuimteIOHal = OpenbareRuimteIOHal().apply {
                openbareRuimte = createOpenbareRuimte(naam = "fakeOpenbareRuimteNaam").apply {
                    identificatie = "fakeIdentificatie"
                    ligtIn = "fakeLigtIn"
                    type = TypeOpenbareRuimte.WEG
                    status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                    geconstateerd = Indicatie.J
                }
                links = OpenbareRuimteLinks().apply {
                    self = createHalLink(href = "https://example.com/openbareruimten/1")
                }
                embedded = OpenbareRuimteEmbedded().apply {
                    ligtInWoonplaats = createWoonplaatsIOHalBasis(createWoonplaats(name = "fakeWoonplaatsNaam")).apply {
                        links = WoonplaatsLinks().apply {
                            self = createHalLink(href = "https://example.com/woonplaatsen/1")
                        }
                    }
                }
            }

            `when`("it is converted") {
                val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(openbareRuimteIOHal)

                then("the openbare ruimte fields and the woonplaats are set") {
                    with(restOpenbareRuimte) {
                        url shouldBe URI("https://example.com/openbareruimten/1")
                        identificatie shouldBe "fakeIdentificatie"
                        naam shouldBe "fakeOpenbareRuimteNaam"
                        woonplaatsNaam shouldBe "fakeLigtIn"
                        type shouldBe TypeOpenbareRuimte.WEG
                        typeWeergave shouldBe TypeOpenbareRuimte.WEG.toString()
                        status shouldBe StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                        geconstateerd shouldBe true
                        woonplaats.naam shouldBe "fakeWoonplaatsNaam"
                    }
                }
            }
        }

        given("A BAG openbare ruimte without type and without embedded objects") {
            val openbareRuimteIOHal = OpenbareRuimteIOHal().apply {
                openbareRuimte = createOpenbareRuimte()
                links = OpenbareRuimteLinks().apply {
                    self = createHalLink(href = "https://example.com/openbareruimten/1")
                }
            }

            `when`("it is converted") {
                val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(openbareRuimteIOHal)

                then("no type and no woonplaats are set") {
                    with(restOpenbareRuimte) {
                        type.shouldBeNull()
                        typeWeergave.shouldBeNull()
                        woonplaats.shouldBeNull()
                        geconstateerd shouldBe false
                    }
                }
            }
        }

        given("A BAG openbare ruimte of an address") {
            val openbareRuimteIOHalBasis = OpenbareRuimteIOHalBasis().apply {
                openbareRuimte = createOpenbareRuimte().apply { ligtIn = "fakeLigtIn" }
                links = OpenbareRuimteLinks().apply {
                    self = createHalLink(href = "https://example.com/openbareruimten/1")
                }
            }

            `when`("it is converted with the address") {
                val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(
                    openbareRuimteIOHalBasis,
                    createAdresIOHal(woonplaatsNaam = "fakeAdresWoonplaatsNaam")
                )

                then("the woonplaats name of the address is used") {
                    restOpenbareRuimte.woonplaatsNaam shouldBe "fakeAdresWoonplaatsNaam"
                }
            }

            `when`("it is converted without an address") {
                val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(openbareRuimteIOHalBasis, null)

                then("the woonplaats the openbare ruimte lies in is used") {
                    restOpenbareRuimte.woonplaatsNaam shouldBe "fakeLigtIn"
                }
            }
        }
    }

    context("Converting an openbare ruimte zaakobject") {
        given("An openbare ruimte zaakobject") {
            val bagObjectUri = URI("https://example.com/openbareruimten/1")
            val zaakobjectOpenbareRuimte = createZaakobjectOpenbareRuimte(
                bagobjectURI = bagObjectUri,
                objectOpenbareRuimte = createObjectOpenbareRuimte(
                    identificatie = "fakeIdentificatie",
                    openbareRuimteNaam = "fakeOpenbareRuimteNaam",
                    plaatsNaam = "fakePlaatsNaam"
                )
            )

            `when`("it is converted") {
                val restOpenbareRuimte = RestOpenbareRuimteConverter.convertToREST(zaakobjectOpenbareRuimte)

                then("the openbare ruimte fields stored in the zaakobject are set") {
                    with(restOpenbareRuimte!!) {
                        url shouldBe bagObjectUri
                        identificatie shouldBe "fakeIdentificatie"
                        naam shouldBe "fakeOpenbareRuimteNaam"
                        woonplaatsNaam shouldBe "fakePlaatsNaam"
                    }
                }
            }
        }
    }
})
