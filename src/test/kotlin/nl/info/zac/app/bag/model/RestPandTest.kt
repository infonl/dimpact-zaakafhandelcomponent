/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import nl.info.zac.app.bag.createSurface
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

class RestPandTest : BehaviorSpec({
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
                val restPand = pandIOHal.toRestPand()

                then("the pand fields are set") {
                    with(restPand) {
                        url shouldBe URI("https://example.com/panden/1")
                        identificatie shouldBe "fakePandId"
                        oorspronkelijkBouwjaar shouldBe "1990"
                        status shouldBe StatusPand.PAND_IN_GEBRUIK
                        statusWeergave shouldBe StatusPand.PAND_IN_GEBRUIK.toString()
                        isGeconstateerd shouldBe true
                        geometry?.type shouldBe RestGeometryType.POLYGON
                    }
                }
            }
        }

        given("A BAG pand without a status") {
            val pand = createPand().apply { geometrie = createSurface() }

            `when`("it is converted") {
                val restPand = PandIOHalBasis().apply { this.pand = pand }.toRestPand()

                then("no status is set") {
                    restPand.status.shouldBeNull()
                    restPand.statusWeergave.shouldBeNull()
                    restPand.isGeconstateerd shouldBe false
                }
            }
        }
    }

    context("Converting a pand zaakobject") {
        given("A pand zaakobject") {
            val zaakobjectPand = createZaakobjectPand(objectPand = createObjectPand(identificatie = "fakePandId"))

            `when`("it is converted") {
                val restPand = zaakobjectPand.toRestPand()

                then("the identificatie is set") {
                    restPand!!.identificatie shouldBe "fakePandId"
                }
            }
        }

        given("A pand zaakobject without object identificatie") {
            val zaakobjectPand = ZaakobjectPand(
                zaak = URI("https://example.com/zaken/1"),
                bagobjectUri = null,
                pand = null,
                url = URI("https://example.com/zaakobjecten/1"),
                uuid = UUID.randomUUID()
            )

            `when`("it is converted") {
                val restPand = zaakobjectPand.toRestPand()

                then("no pand is returned") {
                    restPand.shouldBeNull()
                }
            }
        }
    }

    context("Converting a REST pand to a zaakobject request") {
        given("A REST pand and a zaak") {
            val zaak = createZaak()
            val restPand = RestPand().apply {
                url = URI("https://example.com/panden/1")
                identificatie = "fakePandId"
            }

            `when`("it is converted") {
                val zaakobjectPandRequest = restPand.toZaakobjectPandRequest(zaak)

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
