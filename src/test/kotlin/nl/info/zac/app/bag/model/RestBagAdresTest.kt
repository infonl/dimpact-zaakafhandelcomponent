/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.net.URI
import java.util.UUID
import nl.info.client.bag.model.createAdresIOHal
import nl.info.client.bag.model.createAdresIOLinks
import nl.info.client.bag.model.createHalLink
import nl.info.client.bag.model.createNummeraanduiding
import nl.info.client.bag.model.createOpenbareRuimte
import nl.info.client.bag.model.createPand
import nl.info.client.bag.model.createWoonplaats
import nl.info.client.bag.model.createWoonplaatsIOHalBasis
import nl.info.client.bag.model.generated.AdresIOEmbedded
import nl.info.client.bag.model.generated.Geconstateerd
import nl.info.client.bag.model.generated.NummeraanduidingIOHalBasis
import nl.info.client.bag.model.generated.NummeraanduidingLinks
import nl.info.client.bag.model.generated.OpenbareRuimteIOHalBasis
import nl.info.client.bag.model.generated.OpenbareRuimteLinks
import nl.info.client.bag.model.generated.PandIOHalBasis
import nl.info.client.bag.model.generated.StatusPlaats
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.bag.model.generated.WoonplaatsLinks
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import nl.info.zac.app.bag.createLigplaatsAdresseerbaarObject
import nl.info.zac.app.bag.createSurface
import nl.info.zac.app.zaak.model.RestCoordinates
import nl.info.zac.app.zaak.model.RestGeometry
import nl.info.zac.app.zaak.model.RestGeometryType

class RestBagAdresTest : BehaviorSpec({
    val adresseerbaarObjectGeometry = RestGeometry(
        type = RestGeometryType.POINT,
        point = RestCoordinates(5.1, 52.2)
    )
    val pandGeometry = RestGeometry(
        type = RestGeometryType.POLYGON,
        polygon = listOf(listOf(RestCoordinates(5.0, 52.0)))
    )

    context("The geometry of a REST address") {
        given("An address without an adresseerbaar object and without panden") {
            val restBagAdres = RestBagAdres()

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("there is no geometry") {
                    geometry.shouldBeNull()
                }
            }
        }

        given("An address with only an adresseerbaar object with a geometry") {
            val restBagAdres = RestBagAdres().apply {
                adresseerbaarObject = RestAdresseerbaarObject().apply { geometry = adresseerbaarObjectGeometry }
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("the geometry of the adresseerbaar object is returned") {
                    geometry shouldBe adresseerbaarObjectGeometry
                }
            }
        }

        given("An address with only panden of which the first has a geometry") {
            val restBagAdres = RestBagAdres().apply {
                panden = listOf(RestPand().apply { geometry = pandGeometry }, RestPand())
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("the geometry of the first pand is returned") {
                    geometry shouldBe pandGeometry
                }
            }
        }

        given("An address with an adresseerbaar object and a pand that both have a geometry") {
            val restBagAdres = RestBagAdres().apply {
                adresseerbaarObject = RestAdresseerbaarObject().apply { geometry = adresseerbaarObjectGeometry }
                panden = listOf(RestPand().apply { geometry = pandGeometry })
            }

            `when`("the geometry is requested") {
                val geometry = restBagAdres.geometry

                then("a geometry collection with both geometries is returned") {
                    geometry shouldBe RestGeometry(
                        type = RestGeometryType.GEOMETRY_COLLECTION,
                        geometrycollection = listOf(adresseerbaarObjectGeometry, pandGeometry)
                    )
                }
            }
        }
    }

    context("The description of a REST address") {
        given("An address") {
            val restBagAdres = RestBagAdres().apply {
                openbareRuimteNaam = "fakeStraat"
                huisnummerWeergave = "12A"
                postcode = "1234AB"
                woonplaatsNaam = "fakeWoonplaats"
            }

            `when`("the description is requested") {
                val omschrijving = restBagAdres.omschrijving

                then("it contains the street, huisnummer, postcode and woonplaats") {
                    omschrijving shouldBe "fakeStraat 12A, 1234AB fakeWoonplaats"
                }
            }
        }
    }
    context("Converting a BAG address") {
        given("A BAG address without embedded objects and without geconstateerd indication") {
            val adresIOHal = createAdresIOHal(
                huisnummer = 12,
                huisletter = "A",
                huisnummertoevoeging = "2",
                postcode = "1234AB",
                woonplaatsNaam = "fakeWoonplaatsNaam",
                links = createAdresIOLinks(
                    self = createHalLink(href = "https://example.com/adressen/1")
                )
            ).apply {
                nummeraanduidingIdentificatie = "fakeNummeraanduidingIdentificatie"
                openbareRuimteNaam = "fakeOpenbareRuimteNaam"
            }

            `when`("it is converted") {
                val restBagAdres = adresIOHal.toRestBagAdres()

                then("the address fields are set and the embedded objects are empty") {
                    with(restBagAdres) {
                        url shouldBe URI("https://example.com/adressen/1")
                        identificatie shouldBe "fakeNummeraanduidingIdentificatie"
                        postcode shouldBe "1234AB"
                        huisnummer shouldBe 12
                        huisletter shouldBe "A"
                        huisnummertoevoeging shouldBe "2"
                        huisnummerWeergave shouldBe "12A-2"
                        openbareRuimteNaam shouldBe "fakeOpenbareRuimteNaam"
                        woonplaatsNaam shouldBe "fakeWoonplaatsNaam"
                        isGeconstateerd shouldBe false
                        openbareRuimte.shouldBeNull()
                        nummeraanduiding.shouldBeNull()
                        woonplaats.shouldBeNull()
                        adresseerbaarObject.shouldBeNull()
                        panden.shouldBeEmpty()
                    }
                }
            }
        }

        given("A BAG address that is geconstateerd for all its parts") {
            val adresIOHal = createAdresIOHal().apply {
                geconstateerd = Geconstateerd().apply {
                    nummeraanduiding = true
                    woonplaats = true
                    openbareRuimte = true
                }
            }

            `when`("it is converted") {
                val restBagAdres = adresIOHal.toRestBagAdres()

                then("the address is geconstateerd") {
                    restBagAdres.isGeconstateerd shouldBe true
                }
            }
        }

        given("A BAG address that is geconstateerd for only some of its parts") {
            val adresIOHal = createAdresIOHal().apply {
                geconstateerd = Geconstateerd().apply {
                    nummeraanduiding = true
                    woonplaats = null
                    openbareRuimte = true
                }
            }

            `when`("it is converted") {
                val restBagAdres = adresIOHal.toRestBagAdres()

                then("the address is not geconstateerd") {
                    restBagAdres.isGeconstateerd shouldBe false
                }
            }
        }

        given("A BAG address with all embedded objects") {
            val adresIOHal = createAdresIOHal(woonplaatsNaam = "fakeAdresWoonplaatsNaam").apply {
                embedded = AdresIOEmbedded().apply {
                    openbareRuimte = OpenbareRuimteIOHalBasis().apply {
                        openbareRuimte = createOpenbareRuimte(naam = "fakeOpenbareRuimteNaam")
                        links = OpenbareRuimteLinks().apply {
                            self = createHalLink(href = "https://example.com/openbareruimten/1")
                        }
                    }
                    nummeraanduiding = NummeraanduidingIOHalBasis().apply {
                        nummeraanduiding = createNummeraanduiding(identificatie = "fakeNummeraanduidingId")
                        links = NummeraanduidingLinks().apply {
                            self = createHalLink(href = "https://example.com/nummeraanduidingen/1")
                        }
                    }
                    woonplaats = createWoonplaatsIOHalBasis(createWoonplaats(name = "fakeWoonplaatsNaam")).apply {
                        links = WoonplaatsLinks().apply {
                            self = createHalLink(href = "https://example.com/woonplaatsen/1")
                        }
                    }
                    panden = listOf(
                        PandIOHalBasis().apply {
                            pand = createPand().apply {
                                identificatie = "fakePandId"
                                geometrie = createSurface()
                            }
                        }
                    )
                    adresseerbaarObject = createLigplaatsAdresseerbaarObject(StatusPlaats.PLAATS_AANGEWEZEN)
                }
            }

            `when`("it is converted") {
                val restBagAdres = adresIOHal.toRestBagAdres()

                then("the openbare ruimte is set with the woonplaats name of the address") {
                    with(restBagAdres.openbareRuimte) {
                        shouldNotBeNull()
                        naam shouldBe "fakeOpenbareRuimteNaam"
                        url shouldBe URI("https://example.com/openbareruimten/1")
                        woonplaatsNaam shouldBe "fakeAdresWoonplaatsNaam"
                    }
                }

                and("the nummeraanduiding is set") {
                    with(restBagAdres.nummeraanduiding) {
                        shouldNotBeNull()
                        identificatie shouldBe "fakeNummeraanduidingId"
                        url shouldBe URI("https://example.com/nummeraanduidingen/1")
                    }
                }

                and("the woonplaats is set") {
                    with(restBagAdres.woonplaats) {
                        shouldNotBeNull()
                        naam shouldBe "fakeWoonplaatsNaam"
                        url shouldBe URI("https://example.com/woonplaatsen/1")
                    }
                }

                and("the panden are set") {
                    restBagAdres.panden shouldHaveSize 1
                    restBagAdres.panden.first().identificatie shouldBe "fakePandId"
                }

                and("the adresseerbaar object is set") {
                    restBagAdres.adresseerbaarObject?.typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.LIGPLAATS
                }
            }
        }
    }

    context("Converting an address zaakobject") {
        given("An address zaakobject") {
            val bagObjectUri = URI("https://example.com/adressen/1")
            val zaakobjectAdres = ZaakobjectAdres(
                zaak = URI("https://example.com/zaken/1"),
                bagobjectURI = bagObjectUri,
                adres = ObjectAdres(
                    identificatie = "fakeIdentificatie",
                    wplWoonplaatsNaam = "fakeWoonplaatsNaam",
                    gorOpenbareRuimteNaam = "fakeOpenbareRuimteNaam",
                    huisnummer = 12,
                    huisletter = "B",
                    huisnummertoevoeging = "3",
                    postcode = "1234AB"
                ),
                url = URI("https://example.com/zaakobjecten/1"),
                uuid = UUID.randomUUID()
            )

            `when`("it is converted") {
                val restBagAdres = zaakobjectAdres.toRestBagAdres()

                then("the address fields stored in the zaakobject are set") {
                    with(restBagAdres!!) {
                        url shouldBe bagObjectUri
                        identificatie shouldBe "fakeIdentificatie"
                        postcode shouldBe "1234AB"
                        huisnummerWeergave shouldBe "12B-3"
                        openbareRuimteNaam shouldBe "fakeOpenbareRuimteNaam"
                        woonplaatsNaam shouldBe "fakeWoonplaatsNaam"
                    }
                }
            }
        }

        given("An address zaakobject without object identificatie") {
            val zaakobjectAdres = ZaakobjectAdres(
                zaak = URI("https://example.com/zaken/1"),
                bagobjectURI = null,
                adres = null,
                url = URI("https://example.com/zaakobjecten/1"),
                uuid = UUID.randomUUID()
            )

            `when`("it is converted") {
                val restBagAdres = zaakobjectAdres.toRestBagAdres()

                then("no address is returned") {
                    restBagAdres.shouldBeNull()
                }
            }
        }
    }

    context("Converting a REST address to a zaakobject request") {
        given("A REST address and a zaak") {
            val zaak = createZaak()
            val restBagAdres = RestBagAdres().apply {
                url = URI("https://example.com/adressen/1")
                identificatie = "fakeIdentificatie"
                woonplaatsNaam = "fakeWoonplaatsNaam"
                openbareRuimteNaam = "fakeOpenbareRuimteNaam"
                huisnummer = 12
                huisletter = "B"
                huisnummertoevoeging = "3"
                postcode = "1234AB"
            }

            `when`("it is converted") {
                val zaakobjectAdresRequest = restBagAdres.toZaakobjectAdresRequest(zaak)

                then("the request links the address to the zaak and contains the address fields") {
                    with(zaakobjectAdresRequest) {
                        this.zaak shouldBe zaak.url
                        `object` shouldBe URI("https://example.com/adressen/1")
                        objectIdentificatie shouldBe ObjectAdres(
                            identificatie = "fakeIdentificatie",
                            wplWoonplaatsNaam = "fakeWoonplaatsNaam",
                            gorOpenbareRuimteNaam = "fakeOpenbareRuimteNaam",
                            huisnummer = 12,
                            huisletter = "B",
                            huisnummertoevoeging = "3",
                            postcode = "1234AB"
                        )
                    }
                }
            }
        }
    }
})
