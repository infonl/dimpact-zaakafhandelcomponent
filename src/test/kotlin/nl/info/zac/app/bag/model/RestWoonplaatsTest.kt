/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import nl.info.client.bag.model.generated.HalLink
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.StatusWoonplaats
import nl.info.client.bag.model.generated.Woonplaats
import nl.info.client.bag.model.generated.WoonplaatsIOHalBasis
import nl.info.client.bag.model.generated.WoonplaatsLinks
import nl.info.client.zgw.model.createZaak
import java.net.URI
import java.util.UUID

class RestWoonplaatsTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    context("Converting a ZaakobjectWoonplaats") {
        given("a valid ZaakobjectWoonplaats with identificatie and naam") {
            val fakeObjectUri = URI("https://example.com/bag/woonplaats/fakeObjectUri")
            val fakeZaakUri = URI("https://example.com/zaken/fakeZaakUri")
            val objectWoonplaats = ObjectWoonplaats("fakeIdentificatie", "fakeWoonplaatsNaam")
            val zaakobjectWoonplaats = ZaakobjectWoonplaats(
                zaak = fakeZaakUri,
                bagobjectUri = fakeObjectUri,
                woonplaats = objectWoonplaats,
                url = URI("https://example.com/zaakobjecten/${UUID.randomUUID()}"),
                uuid = UUID.randomUUID()
            )

            `when`("it is converted") {
                val result = zaakobjectWoonplaats.toRestWoonplaats().shouldNotBeNull()

                then("it should map the url from the object URI") {
                    result.url shouldBe fakeObjectUri
                }

                and("it should map the identificatie from the object identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                and("it should map the naam from the woonplaatsNaam") {
                    result.naam shouldBe "fakeWoonplaatsNaam"
                }
            }
        }
    }

    context("Converting a RestWoonplaats to a zaakobject request") {
        given("a RestWoonplaats and a Zaak") {
            val fakeWoonplaatsUrl = URI("https://example.com/bag/woonplaats/fakeWoonplaatsUrl")
            val restWoonplaats = RestWoonplaats().apply {
                url = fakeWoonplaatsUrl
                identificatie = "fakeIdentificatie"
                naam = "fakeWoonplaatsNaam"
            }
            val zaak = createZaak()

            `when`("it is converted to a zaakobject request") {
                val result = restWoonplaats.toZaakobjectWoonplaatsRequest(zaak)

                then("it should set the zaak URL from the provided zaak") {
                    result.zaak shouldBe zaak.url
                }

                and("it should set the object URI from the woonplaats url") {
                    result.`object` shouldBe fakeWoonplaatsUrl
                }

                and("it should populate the ObjectWoonplaats with identificatie and naam") {
                    result.objectIdentificatie!!.identificatie shouldBe "fakeIdentificatie"
                    result.objectIdentificatie!!.woonplaatsNaam shouldBe "fakeWoonplaatsNaam"
                }
            }
        }
    }

    context("Converting a WoonplaatsIOHalBasis") {
        given("a valid WoonplaatsIOHalBasis with all Woonplaats fields and a self link") {
            val fakeSelfHref = "https://example.com/bag/woonplaats/fakeSelfHref"
            val woonplaats = Woonplaats().apply {
                setIdentificatie("fakeIdentificatie")
                setNaam("fakeWoonplaatsNaam")
                setStatus(StatusWoonplaats.WOONPLAATS_AANGEWEZEN)
                setGeconstateerd(Indicatie.J)
            }
            val halLink = HalLink().apply { setHref(fakeSelfHref) }
            val woonplaatsLinks = WoonplaatsLinks().apply { setSelf(halLink) }
            val woonplaatsIOHalBasis = WoonplaatsIOHalBasis().apply {
                setWoonplaats(woonplaats)
                setLinks(woonplaatsLinks)
            }

            `when`("it is converted") {
                val result = woonplaatsIOHalBasis.toRestWoonplaats().shouldNotBeNull()

                then("it should set the url from the self link href") {
                    result.url shouldBe URI.create(fakeSelfHref)
                }

                and("it should map the identificatie from the woonplaats") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                and("it should map the naam from the woonplaats") {
                    result.naam shouldBe "fakeWoonplaatsNaam"
                }

                and("it should map the status from the woonplaats") {
                    result.status shouldBe StatusWoonplaats.WOONPLAATS_AANGEWEZEN
                }

                and("it should map geconstateerd as true when Indicatie is J") {
                    result.isGeconstateerd shouldBe true
                }
            }
        }

        given("a WoonplaatsIOHalBasis with geconstateerd set to N") {
            val fakeSelfHref = "https://example.com/bag/woonplaats/fakeSelfHrefN"
            val woonplaats = Woonplaats().apply {
                setIdentificatie("fakeIdentificatie")
                setNaam("fakeWoonplaatsNaam")
                setStatus(StatusWoonplaats.WOONPLAATS_INGETROKKEN)
                setGeconstateerd(Indicatie.N)
            }
            val halLink = HalLink().apply { setHref(fakeSelfHref) }
            val woonplaatsLinks = WoonplaatsLinks().apply { setSelf(halLink) }
            val woonplaatsIOHalBasis = WoonplaatsIOHalBasis().apply {
                setWoonplaats(woonplaats)
                setLinks(woonplaatsLinks)
            }

            `when`("it is converted") {
                val result = woonplaatsIOHalBasis.toRestWoonplaats().shouldNotBeNull()

                then("it should map geconstateerd as false when Indicatie is N") {
                    result.isGeconstateerd shouldBe false
                }

                and("it should map the status from the woonplaats") {
                    result.status shouldBe StatusWoonplaats.WOONPLAATS_INGETROKKEN
                }
            }
        }
    }
})
