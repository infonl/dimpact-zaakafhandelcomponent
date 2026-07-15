/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import net.atos.zac.app.bag.model.RESTWoonplaats
import nl.info.client.bag.model.generated.HalLink
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.StatusWoonplaats
import nl.info.client.bag.model.generated.Woonplaats
import nl.info.client.bag.model.generated.WoonplaatsIOHalBasis
import nl.info.client.bag.model.generated.WoonplaatsLinks
import nl.info.client.zgw.model.createZaak
import java.net.URI

class RestWoonplaatsConverterTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    context("convertToREST(ZaakobjectWoonplaats)") {
        given("a null ZaakobjectWoonplaats") {
            val zaakobjectWoonplaats: ZaakobjectWoonplaats? = null

            `when`("convertToREST is called") {
                val result = RestWoonplaatsConverter.convertToREST(zaakobjectWoonplaats)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("a valid ZaakobjectWoonplaats with identificatie and naam") {
            val fakeObjectUri = URI("https://example.com/bag/woonplaats/fakeObjectUri")
            val fakeZaakUri = URI("https://example.com/zaken/fakeZaakUri")
            val objectWoonplaats = ObjectWoonplaats("fakeIdentificatie", "fakeWoonplaatsNaam")
            val zaakobjectWoonplaats = ZaakobjectWoonplaats(fakeZaakUri, fakeObjectUri, objectWoonplaats)

            `when`("convertToREST is called") {
                val result = RestWoonplaatsConverter.convertToREST(zaakobjectWoonplaats)

                then("it should map the url from the object URI") {
                    result!!.url shouldBe fakeObjectUri
                }

                And("it should map the identificatie from the object identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                And("it should map the naam from the woonplaatsNaam") {
                    result.naam shouldBe "fakeWoonplaatsNaam"
                }
            }
        }
    }

    context("convertToZaakobject(RESTWoonplaats, Zaak)") {
        given("a RESTWoonplaats and a Zaak") {
            val fakeWoonplaatsUrl = URI("https://example.com/bag/woonplaats/fakeWoonplaatsUrl")
            val restWoonplaats = RESTWoonplaats().apply {
                url = fakeWoonplaatsUrl
                identificatie = "fakeIdentificatie"
                naam = "fakeWoonplaatsNaam"
            }
            val zaak = createZaak()

            `when`("convertToZaakobject is called") {
                val result = RestWoonplaatsConverter.convertToZaakobject(restWoonplaats, zaak)

                then("it should set the zaak URL from the provided zaak") {
                    result.zaak shouldBe zaak.url
                }

                And("it should set the object URI from the woonplaats url") {
                    result.`object` shouldBe fakeWoonplaatsUrl
                }

                And("it should populate the ObjectWoonplaats with identificatie and naam") {
                    result.objectIdentificatie.identificatie shouldBe "fakeIdentificatie"
                    result.objectIdentificatie.woonplaatsNaam shouldBe "fakeWoonplaatsNaam"
                }
            }
        }
    }

    context("convertToREST(WoonplaatsIOHalBasis)") {
        given("a null WoonplaatsIOHalBasis") {
            val woonplaatsIOHalBasis: WoonplaatsIOHalBasis? = null

            `when`("convertToREST is called") {
                val result = RestWoonplaatsConverter.convertToREST(woonplaatsIOHalBasis)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

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

            `when`("convertToREST is called") {
                val result = RestWoonplaatsConverter.convertToREST(woonplaatsIOHalBasis)

                then("it should set the url from the self link href") {
                    result!!.url shouldBe URI.create(fakeSelfHref)
                }

                And("it should map the identificatie from the woonplaats") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                And("it should map the naam from the woonplaats") {
                    result.naam shouldBe "fakeWoonplaatsNaam"
                }

                And("it should map the status from the woonplaats") {
                    result.status shouldBe StatusWoonplaats.WOONPLAATS_AANGEWEZEN
                }

                And("it should map geconstateerd as true when Indicatie is J") {
                    result.geconstateerd shouldBe true
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

            `when`("convertToREST is called") {
                val result = RestWoonplaatsConverter.convertToREST(woonplaatsIOHalBasis)

                then("it should map geconstateerd as false when Indicatie is N") {
                    result!!.geconstateerd shouldBe false
                }

                And("it should map the status from the woonplaats") {
                    result.status shouldBe StatusWoonplaats.WOONPLAATS_INGETROKKEN
                }
            }
        }
    }
})
