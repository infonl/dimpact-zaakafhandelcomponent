/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.atos.zac.app.bag.createSurface
import net.atos.zac.app.bag.model.RESTPand
import nl.info.client.bag.model.createHalLink
import nl.info.client.bag.model.createPand
import nl.info.client.bag.model.createPandIOHal
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.PandIOHalBasis
import nl.info.client.bag.model.generated.PandLinks
import nl.info.client.bag.model.generated.StatusPand
import nl.info.client.zgw.model.createObjectPand
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import nl.info.zac.app.zaak.model.RestGeometryType
import java.net.URI
import java.util.UUID

class RestPandConverterTest : BehaviorSpec({
    context("Converting a BAG pand") {
        given("A BAG pand with a status and a geometry") {
            val pandIOHal = createPandIOHal(
                pand = createPand(oorspronkelijkBouwjaar = "1990").apply {
                    identificatie = "fakePandId"
                    status = StatusPand.PAND_IN_GEBRUIK
                    geconstateerd = Indicatie.J
                    geometrie = createSurface()
                }
            ).apply {
                links = PandLinks().apply { self = createHalLink(href = "https://example.com/panden/1") }
            }

            `when`("it is converted") {
                val restPand = RestPandConverter.convertToREST(pandIOHal)

                then("the pand fields are set") {
                    with(restPand) {
                        url shouldBe URI("https://example.com/panden/1")
                        identificatie shouldBe "fakePandId"
                        oorspronkelijkBouwjaar shouldBe "1990"
                        status shouldBe StatusPand.PAND_IN_GEBRUIK
                        statusWeergave shouldBe StatusPand.PAND_IN_GEBRUIK.toString()
                        geconstateerd shouldBe true
                        geometry.type shouldBe RestGeometryType.POLYGON
                    }
                }
            }
        }

        given("A BAG pand without a status") {
            val pand = createPand().apply { geometrie = createSurface() }

            `when`("it is converted") {
                val restPand = RestPandConverter.convertToREST(pand)

                then("no status is set") {
                    restPand.status.shouldBeNull()
                    restPand.statusWeergave.shouldBeNull()
                    restPand.geconstateerd shouldBe false
                }
            }
        }

        given("A list of BAG panden") {
            val panden = listOf(
                PandIOHalBasis().apply {
                    pand = createPand(oorspronkelijkBouwjaar = "1990").apply { geometrie = createSurface() }
                },
                PandIOHalBasis().apply {
                    pand = createPand(oorspronkelijkBouwjaar = "2000").apply { geometrie = createSurface() }
                }
            )

            `when`("they are converted") {
                val restPanden = RestPandConverter.convertToREST(panden)

                then("each pand is converted") {
                    restPanden.map { it.oorspronkelijkBouwjaar } shouldBe listOf("1990", "2000")
                }
            }
        }

        given("No list of BAG panden") {
            `when`("it is converted") {
                val restPanden = RestPandConverter.convertToREST(null as List<PandIOHalBasis>?)

                then("an empty list is returned") {
                    restPanden.shouldBeEmpty()
                }
            }
        }
    }

    context("Converting a pand zaakobject") {
        given("A pand zaakobject") {
            val zaakobjectPand = createZaakobjectPand(objectPand = createObjectPand(identificatie = "fakePandId"))

            `when`("it is converted") {
                val restPand = RestPandConverter.convertToREST(zaakobjectPand)

                then("the identificatie is set") {
                    restPand!!.identificatie shouldBe "fakePandId"
                }
            }
        }

        given("A pand zaakobject without object identificatie") {
            val zaakobjectPand = ZaakobjectPand(
                URI("https://example.com/zaken/1"),
                null,
                null,
                URI("https://example.com/zaakobjecten/1"),
                UUID.randomUUID()
            )

            `when`("it is converted") {
                val restPand = RestPandConverter.convertToREST(zaakobjectPand)

                then("no pand is returned") {
                    restPand.shouldBeNull()
                }
            }
        }
    }

    context("Converting a REST pand to a zaakobject request") {
        given("A REST pand and a zaak") {
            val zaak = createZaak()
            val restPand = RESTPand().apply {
                url = URI("https://example.com/panden/1")
                identificatie = "fakePandId"
            }

            `when`("it is converted") {
                val zaakobjectPandRequest = RestPandConverter.convertToZaakobject(restPand, zaak)

                then("the request links the pand to the zaak") {
                    with(zaakobjectPandRequest) {
                        this.zaak shouldBe zaak.url
                        `object` shouldBe URI("https://example.com/panden/1")
                        objectIdentificatie shouldBe ObjectPand("fakePandId")
                    }
                }
            }
        }
    }
})
