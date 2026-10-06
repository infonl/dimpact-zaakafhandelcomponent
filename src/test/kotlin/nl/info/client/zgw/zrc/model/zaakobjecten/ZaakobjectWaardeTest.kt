/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createObjectOpenbareRuimte
import nl.info.client.zgw.model.createObjectPand
import java.net.URI
import java.util.UUID

class ZaakobjectWaardeTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    // Every read Zaakobject requires a url and uuid, since both are always present on an actual deserialized
    // result; their concrete value is irrelevant to the scenarios below, so a single fake pair is reused.
    val fakeUrl = URI("https://example.com/zaakobjecten/${UUID.randomUUID()}")
    val fakeUuid = UUID.randomUUID()

    given("a ZaakobjectAdres wrapping an ObjectAdres") {
        val objectAdres = ObjectAdres(
            identificatie = "fakeIdentificatie",
            wplWoonplaatsNaam = "fakeWoonplaats",
            gorOpenbareRuimteNaam = "fakeOpenbareRuimte",
            huisnummer = 1,
            huisletter = null,
            huisnummertoevoeging = null,
            postcode = null
        )
        val zaakobject = ZaakobjectAdres(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            bagobjectURI = URI("https://example.com/adres/${UUID.randomUUID()}"),
            adres = objectAdres,
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectAdres identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectNummeraanduiding wrapping an ObjectNummeraanduiding") {
        val objectNummeraanduiding = ObjectNummeraanduiding(
            identificatie = "fakeIdentificatie",
            huisnummer = 1,
            huisletter = null,
            huisnummertoevoeging = null,
            postcode = null,
            typeAdresseerbaarObject = null,
            status = null
        )
        val zaakobject = ZaakobjectNummeraanduiding(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            bagObjectUri = URI("https://example.com/nummeraanduiding/${UUID.randomUUID()}"),
            nummeraanduiding = objectNummeraanduiding,
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the wrapped ObjectNummeraanduiding identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectOpenbareRuimte wrapping an ObjectOpenbareRuimte") {
        val zaakobject = ZaakobjectOpenbareRuimte(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            bagobjectURI = URI("https://example.com/openbareruimte/${UUID.randomUUID()}"),
            objectOpenbareRuimte = createObjectOpenbareRuimte(identificatie = "fakeIdentificatie"),
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectOpenbareRuimte identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectPand wrapping an ObjectPand") {
        val zaakobject = ZaakobjectPand(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
            pand = createObjectPand(identificatie = "fakeIdentificatie"),
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectPand identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectProductaanvraag") {
        val productaanvraagURI = URI("https://example.com/productaanvraag/${UUID.randomUUID()}")
        val zaakobject = ZaakobjectProductaanvraag(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            productaanvraag = productaanvraagURI,
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the last path segment of the productaanvraag URI") {
                zaakobject.waarde shouldBe productaanvraagURI.path.substringAfterLast("/")
            }
        }
    }

    given("a ZaakobjectWoonplaats wrapping an ObjectWoonplaats") {
        val objectWoonplaats = ObjectWoonplaats("fakeIdentificatie", "fakeWoonplaatsNaam")
        val zaakobject = ZaakobjectWoonplaats(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            bagobjectUri = URI("https://example.com/woonplaats/${UUID.randomUUID()}"),
            woonplaats = objectWoonplaats,
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectWoonplaats identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectProductaanvraag without a productaanvraag URI") {
        val zaakobject = ZaakobjectProductaanvraag(
            zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
            productaanvraag = null,
            url = fakeUrl,
            uuid = fakeUuid
        )

        `when`("getWaarde is called") {
            then("it returns null since there is no object URI") {
                zaakobject.waarde shouldBe null
            }
        }
    }

    given("Zaakobject* leaf classes created via the no-arg constructor") {
        `when`("getWaarde is called") {
            then("it returns null since there is no objectIdentificatie") {
                ZaakobjectAdres().waarde shouldBe null
                ZaakobjectNummeraanduiding().waarde shouldBe null
                ZaakobjectOpenbareRuimte().waarde shouldBe null
                ZaakobjectPand().waarde shouldBe null
                ZaakobjectProductaanvraag().waarde shouldBe null
                ZaakobjectWoonplaats().waarde shouldBe null
            }
        }
    }
})
