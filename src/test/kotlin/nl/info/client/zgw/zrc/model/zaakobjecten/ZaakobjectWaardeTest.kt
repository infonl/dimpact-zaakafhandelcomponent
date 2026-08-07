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

    given("a ZaakobjectAdres wrapping an ObjectAdres") {
        val objectAdres = ObjectAdres("fakeIdentificatie", "fakeWoonplaats", "fakeOpenbareRuimte", 1, null, null, null)
        val zaakobject = ZaakobjectAdres(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/adres/${UUID.randomUUID()}"),
            objectAdres
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectAdres identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectNummeraanduiding wrapping an ObjectNummeraanduiding") {
        val objectNummeraanduiding = ObjectNummeraanduiding("fakeIdentificatie", 1, null, null, null, null, null)
        val zaakobject = ZaakobjectNummeraanduiding(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/nummeraanduiding/${UUID.randomUUID()}"),
            objectNummeraanduiding
        )

        `when`("getWaarde is called") {
            then("it returns the wrapped ObjectNummeraanduiding identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectOpenbareRuimte wrapping an ObjectOpenbareRuimte") {
        val zaakobject = ZaakobjectOpenbareRuimte(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/openbareruimte/${UUID.randomUUID()}"),
            createObjectOpenbareRuimte(identificatie = "fakeIdentificatie")
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectOpenbareRuimte identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectPand wrapping an ObjectPand") {
        val zaakobject = ZaakobjectPand(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/pand/${UUID.randomUUID()}"),
            createObjectPand(identificatie = "fakeIdentificatie")
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
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            productaanvraagURI
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
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/woonplaats/${UUID.randomUUID()}"),
            objectWoonplaats
        )

        `when`("getWaarde is called") {
            then("it returns the ObjectWoonplaats identificatie") {
                zaakobject.waarde shouldBe "fakeIdentificatie"
            }
        }
    }

    given("a ZaakobjectProductaanvraag without a productaanvraag URI") {
        val zaakobject = ZaakobjectProductaanvraag(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            null
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
