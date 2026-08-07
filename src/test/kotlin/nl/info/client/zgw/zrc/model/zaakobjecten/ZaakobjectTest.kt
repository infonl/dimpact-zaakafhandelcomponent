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
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

class ZaakobjectTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a Zaakobject of type ADRES") {
        val zaakobject = ZaakobjectAdres(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/adres/${UUID.randomUUID()}"),
            null
        )

        `when`("isBagObject is called") {
            then("it returns true") {
                zaakobject.isBagObject shouldBe true
            }
        }
    }

    given("a Zaakobject of type PAND") {
        val zaakobject = ZaakobjectPand(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/pand/${UUID.randomUUID()}"),
            createObjectPand()
        )

        `when`("isBagObject is called") {
            then("it returns true") {
                zaakobject.isBagObject shouldBe true
            }
        }
    }

    given("a Zaakobject of type OPENBARE_RUIMTE") {
        val zaakobject = ZaakobjectOpenbareRuimte(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/openbareruimte/${UUID.randomUUID()}"),
            createObjectOpenbareRuimte()
        )

        `when`("isBagObject is called") {
            then("it returns true") {
                zaakobject.isBagObject shouldBe true
            }
        }
    }

    given("a Zaakobject of type WOONPLAATS") {
        val zaakobject = ZaakobjectWoonplaats(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/woonplaats/${UUID.randomUUID()}"),
            null
        )

        `when`("isBagObject is called") {
            then("it returns true") {
                zaakobject.isBagObject shouldBe true
            }
        }
    }

    given("a Zaakobject of type OVERIGE with the nummeraanduiding marker") {
        val zaakobject = ZaakobjectNummeraanduiding(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/nummeraanduiding/${UUID.randomUUID()}"),
            null
        )

        `when`("isBagObject is called") {
            then("it returns true") {
                zaakobject.isBagObject shouldBe true
            }
        }
    }

    given("a Zaakobject of type OVERIGE without the nummeraanduiding marker") {
        val zaakobject = ZaakobjectProductaanvraag(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/productaanvraag/${UUID.randomUUID()}")
        )

        `when`("isBagObject is called") {
            then("it returns false") {
                zaakobject.isBagObject shouldBe false
            }
        }
    }

    given("a Zaakobject of a type other than ADRES/PAND/OPENBARE_RUIMTE/WOONPLAATS/OVERIGE") {
        val zaakobject = ZaakobjectAdres(
            URI("https://example.com/zaak/${UUID.randomUUID()}"),
            URI("https://example.com/medewerker/${UUID.randomUUID()}"),
            null
        ).apply { objectType = ObjectTypeEnum.MEDEWERKER }

        `when`("isBagObject is called") {
            then("it returns false") {
                zaakobject.isBagObject shouldBe false
            }
        }
    }

    given("two Zaakobject instances of the same subclass with equal zaak, object, objectType and objectTypeOverige") {
        val zaakURI = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val bagobjectURI = URI("https://example.com/pand/${UUID.randomUUID()}")
        val zaakobjectA = ZaakobjectPand(zaakURI, bagobjectURI, createObjectPand(identificatie = "fakeIdentificatieA"))
        val zaakobjectB = ZaakobjectPand(zaakURI, bagobjectURI, createObjectPand(identificatie = "fakeIdentificatieB"))

        `when`("equals is called") {
            val isEqual = zaakobjectA == zaakobjectB

            then("the instances are equal regardless of objectIdentificatie") {
                isEqual shouldBe true
            }
        }
    }

    given("a ZaakobjectAdres compared to a ZaakobjectPand with the same zaak and object") {
        val zaakURI = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val objectURI = URI("https://example.com/object/${UUID.randomUUID()}")
        val zaakobjectAdres = ZaakobjectAdres(zaakURI, objectURI, null)
        val zaakobjectPand = ZaakobjectPand(zaakURI, objectURI, createObjectPand())

        `when`("equals is called") {
            val isEqual = zaakobjectAdres.equals(zaakobjectPand)

            then("the instances are not equal because their runtime classes differ") {
                isEqual shouldBe false
            }
        }
    }
})
