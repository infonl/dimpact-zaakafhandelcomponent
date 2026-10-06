/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.model.createObjectOpenbareRuimte
import nl.info.client.zgw.model.createObjectPand
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

class ZaakobjectTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    // Every read Zaakobject requires a url and uuid, since both are always present on an actual deserialized
    // result; their concrete value is irrelevant to the scenarios below, so a single fake pair is reused.
    val fakeUrl = URI("https://example.com/zaakobjecten/${UUID.randomUUID()}")
    val fakeUuid = UUID.randomUUID()

    context("isBagObject") {
        given("a Zaakobject of type ADRES") {
            val zaakobject = ZaakobjectAdres(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectURI = URI("https://example.com/adres/${UUID.randomUUID()}"),
                adres = null,
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns true") {
                    zaakobject.isBagObject shouldBe true
                }
            }
        }

        given("a Zaakobject of type PAND") {
            val zaakobject = ZaakobjectPand(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns true") {
                    zaakobject.isBagObject shouldBe true
                }
            }
        }

        given("a Zaakobject of type OPENBARE_RUIMTE") {
            val zaakobject = ZaakobjectOpenbareRuimte(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectURI = URI("https://example.com/openbareruimte/${UUID.randomUUID()}"),
                objectOpenbareRuimte = createObjectOpenbareRuimte(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns true") {
                    zaakobject.isBagObject shouldBe true
                }
            }
        }

        given("a Zaakobject of type WOONPLAATS") {
            val zaakobject = ZaakobjectWoonplaats(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/woonplaats/${UUID.randomUUID()}"),
                woonplaats = null,
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns true") {
                    zaakobject.isBagObject shouldBe true
                }
            }
        }

        given("a Zaakobject of type OVERIGE with the nummeraanduiding marker") {
            val zaakobject = ZaakobjectNummeraanduiding(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagObjectUri = URI("https://example.com/nummeraanduiding/${UUID.randomUUID()}"),
                nummeraanduiding = null,
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns true") {
                    zaakobject.isBagObject shouldBe true
                }
            }
        }

        given("a Zaakobject of type OVERIGE without the nummeraanduiding marker") {
            val zaakobject = ZaakobjectProductaanvraag(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                productaanvraag = URI("https://example.com/productaanvraag/${UUID.randomUUID()}"),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("isBagObject is called") {
                then("it returns false") {
                    zaakobject.isBagObject shouldBe false
                }
            }
        }

        given("a Zaakobject of a type other than ADRES/PAND/OPENBARE_RUIMTE/WOONPLAATS/OVERIGE") {
            val zaakobject = ZaakobjectAdres(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectURI = URI("https://example.com/medewerker/${UUID.randomUUID()}"),
                adres = null,
                url = fakeUrl,
                uuid = fakeUuid
            ).apply { objectType = ObjectTypeEnum.MEDEWERKER }

            `when`("isBagObject is called") {
                then("it returns false") {
                    zaakobject.isBagObject shouldBe false
                }
            }
        }
    }

    context("equals") {
        given("two Zaakobject instances of the same subclass with equal zaak, object, objectType and objectTypeOverige") {
            val zaakURI = URI("https://example.com/zaak/${UUID.randomUUID()}")
            val bagobjectURI = URI("https://example.com/pand/${UUID.randomUUID()}")
            val zaakobjectA = ZaakobjectPand(
                zaak = zaakURI,
                bagobjectUri = bagobjectURI,
                pand = createObjectPand(identificatie = "fakeIdentificatieA"),
                url = fakeUrl,
                uuid = fakeUuid
            )
            val zaakobjectB = ZaakobjectPand(
                zaak = zaakURI,
                bagobjectUri = bagobjectURI,
                pand = createObjectPand(identificatie = "fakeIdentificatieB"),
                url = fakeUrl,
                uuid = fakeUuid
            )

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
            val zaakobjectAdres = ZaakobjectAdres(zaak = zaakURI, bagobjectURI = objectURI, adres = null, url = fakeUrl, uuid = fakeUuid)
            val zaakobjectPand = ZaakobjectPand(
                zaak = zaakURI,
                bagobjectUri = objectURI,
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("equals is called") {
                val isEqual = zaakobjectAdres.equals(zaakobjectPand)

                then("the instances are not equal because their runtime classes differ") {
                    isEqual shouldBe false
                }
            }
        }

        given("a Zaakobject compared to itself") {
            val zaakobject = ZaakobjectPand(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("equals is called with the same reference") {
                val isEqual = zaakobject.equals(zaakobject)

                then("the instances are equal") {
                    isEqual shouldBe true
                }
            }
        }

        given("a Zaakobject compared to null") {
            val zaakobject = ZaakobjectPand(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("equals is called with null") {
                val other: Any? = null
                val isEqual = zaakobject.equals(other)

                then("the instances are not equal") {
                    isEqual shouldBe false
                }
            }
        }

        given("two Zaakobject instances with different zaak, object or objectTypeOverige") {
            val zaakobjectA = ZaakobjectPand(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )
            val zaakobjectB = ZaakobjectPand(
                zaak = URI("https://example.com/zaak/${UUID.randomUUID()}"),
                bagobjectUri = URI("https://example.com/pand/${UUID.randomUUID()}"),
                pand = createObjectPand(),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("equals and hashCode are called") {
                then("the instances are not equal") {
                    (zaakobjectA == zaakobjectB) shouldBe false
                    zaakobjectA.hashCode() shouldBe zaakobjectA.hashCode()
                    zaakobjectB.hashCode() shouldBe zaakobjectB.hashCode()
                }
            }
        }
    }

    context("waarde") {
        given("a Zaakobject created via the no-arg constructor") {
            val zaakobject = ZaakobjectPand()

            `when`("getWaarde is called") {
                val waarde = zaakobject.waarde

                then("it returns null since there is no objectIdentificatie") {
                    waarde shouldBe null
                }
            }
        }
    }

    context("toString") {
        given("a Zaakobject") {
            val zaakURI = URI("https://example.com/zaak/${UUID.randomUUID()}")
            val bagobjectURI = URI("https://example.com/pand/${UUID.randomUUID()}")
            val zaakobject = ZaakobjectPand(
                zaak = zaakURI,
                bagobjectUri = bagobjectURI,
                pand = createObjectPand(identificatie = "fakeIdentificatie"),
                url = fakeUrl,
                uuid = fakeUuid
            )

            `when`("toString is called") {
                val stringRepresentation = zaakobject.toString()

                then("it includes the zaak and object URIs") {
                    stringRepresentation shouldContain "zaak=$zaakURI"
                    stringRepresentation shouldContain "object=$bagobjectURI"
                }
            }
        }
    }
})
