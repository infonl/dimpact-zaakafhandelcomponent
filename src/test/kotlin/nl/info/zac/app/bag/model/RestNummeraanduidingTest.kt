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
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import nl.info.client.bag.model.createNummeraanduiding
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.zgw.model.createZaak
import java.net.URI
import java.util.UUID

class RestNummeraanduidingTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    context("Converting a Nummeraanduiding") {
        given("a valid Nummeraanduiding with all fields populated") {
            val nummeraanduiding = createNummeraanduiding(
                identificatie = "fakeIdentificatie",
                postcode = "1234AB",
                huisnummer = 10,
                status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN,
                typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT,
                geconstateerd = Indicatie.J
            )

            `when`("it is converted") {
                val result = nummeraanduiding.toRestNummeraanduiding().shouldNotBeNull()

                then("it should map the identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                and("it should map the postcode") {
                    result.postcode shouldBe "1234AB"
                }

                and("it should map the huisnummer") {
                    result.huisnummer shouldBe 10
                }

                and("it should map the huisletter as null") {
                    result.huisletter shouldBe null
                }

                and("it should map the huisnummertoevoeging as null") {
                    result.huisnummertoevoeging shouldBe null
                }

                and("it should produce huisnummerWeergave as '10' when huisletter and toevoeging are null") {
                    result.huisnummerWeergave shouldBe "10"
                }

                and("it should map the status") {
                    result.status shouldBe StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                }

                and("it should map the typeAdresseerbaarObject") {
                    result.typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.VERBLIJFSOBJECT
                }

                and("it should map geconstateerd as true when Indicatie is J") {
                    result.isGeconstateerd shouldBe true
                }
            }
        }

        given("a Nummeraanduiding with huisletter and huisnummertoevoeging set") {
            val nummeraanduiding = createNummeraanduiding(
                identificatie = "fakeIdentificatie",
                postcode = "5678CD",
                huisnummer = 42,
                huisletter = "A",
                huisnummertoevoeging = "bis",
                status = StatusNaamgeving.NAAMGEVING_INGETROKKEN,
                typeAdresseerbaarObject = TypeAdresseerbaarObject.STANDPLAATS,
                geconstateerd = Indicatie.N
            )

            `when`("it is converted") {
                val result = nummeraanduiding.toRestNummeraanduiding().shouldNotBeNull()

                then("it should produce huisnummerWeergave combining huisnummer, huisletter and toevoeging") {
                    result.huisnummerWeergave shouldBe "42A-bis"
                }

                and("it should map geconstateerd as false when Indicatie is N") {
                    result.isGeconstateerd shouldBe false
                }
            }
        }
    }

    context("Converting a ZaakobjectNummeraanduiding") {
        given("a valid ZaakobjectNummeraanduiding with identificatie, postcode and huisnummer") {
            val fakeObjectUri = URI("https://example.com/bag/nummeraanduiding/fakeObjectUri")
            val fakeZaakUri = URI("https://example.com/zaken/fakeZaakUri")
            val objectNummeraanduiding = ObjectNummeraanduiding(
                identificatie = "fakeIdentificatie",
                huisnummer = 15,
                huisletter = "B",
                huisnummertoevoeging = "fakeToevoeging",
                postcode = "9876ZZ",
                typeAdresseerbaarObject = TypeAdresseerbaarObject.LIGPLAATS.toString(),
                status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
            )
            val zaakobjectNummeraanduiding = ZaakobjectNummeraanduiding(
                zaak = fakeZaakUri,
                bagObjectUri = fakeObjectUri,
                nummeraanduiding = objectNummeraanduiding,
                url = URI("https://example.com/zaakobjecten/${UUID.randomUUID()}"),
                uuid = UUID.randomUUID()
            )

            `when`("it is converted") {
                val result = zaakobjectNummeraanduiding.toRestNummeraanduiding().shouldNotBeNull()

                then("it should map the url from the object URI") {
                    result.url shouldBe fakeObjectUri
                }

                and("it should map the identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                and("it should map the postcode") {
                    result.postcode shouldBe "9876ZZ"
                }

                and("it should map the huisnummer") {
                    result.huisnummer shouldBe 15
                }

                and("it should map the huisletter") {
                    result.huisletter shouldBe "B"
                }

                and("it should map the huisnummertoevoeging") {
                    result.huisnummertoevoeging shouldBe "fakeToevoeging"
                }

                and("it should map the status from the string value") {
                    result.status shouldBe StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                }

                and("it should map the typeAdresseerbaarObject from the string value") {
                    result.typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.LIGPLAATS
                }
            }
        }
    }

    context("Converting a RestNummeraanduiding to a zaakobject request") {
        given("a RestNummeraanduiding and a Zaak") {
            val fakeNummeraanduidingUrl = URI("https://example.com/bag/nummeraanduiding/fakeNummeraanduidingUrl")
            val restNummeraanduiding = RestNummeraanduiding().apply {
                url = fakeNummeraanduidingUrl
                identificatie = "fakeIdentificatie"
                postcode = "1111AA"
                huisnummer = 7
                huisletter = "C"
                huisnummertoevoeging = "fakeToevoeging"
                typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT
                status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN
            }
            val zaak = createZaak()

            `when`("it is converted to a zaakobject request") {
                val result = restNummeraanduiding.toZaakobjectNummeraanduidingRequest(zaak)

                then("it should set the zaak URL from the provided zaak") {
                    result.zaak shouldBe zaak.url
                }

                and("it should set the object URI from the nummeraanduiding url") {
                    result.`object` shouldBe fakeNummeraanduidingUrl
                }

                and("it should populate ObjectNummeraanduiding with the correct identificatie") {
                    result.objectIdentificatie!!.overigeData!!.identificatie shouldBe "fakeIdentificatie"
                }

                and("it should populate ObjectNummeraanduiding with the correct postcode") {
                    result.objectIdentificatie!!.overigeData!!.postcode shouldBe "1111AA"
                }

                and("it should populate ObjectNummeraanduiding with the correct huisnummer") {
                    result.objectIdentificatie!!.overigeData!!.huisnummer shouldBe 7
                }

                and("it should populate ObjectNummeraanduiding with the correct typeAdresseerbaarObject string") {
                    result.objectIdentificatie!!.overigeData!!.typeAdresseerbaarObject shouldBe
                        TypeAdresseerbaarObject.VERBLIJFSOBJECT.toString()
                }

                and("it should populate ObjectNummeraanduiding with the correct status string") {
                    result.objectIdentificatie!!.overigeData!!.status shouldBe
                        StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
                }
            }
        }
    }

    context("The description of a REST nummeraanduiding") {
        given("A nummeraanduiding with a huisnummer and a postcode") {
            val restNummeraanduiding = RestNummeraanduiding().apply {
                huisnummerWeergave = "12A"
                postcode = "1234AB"
            }

            `when`("the description is requested") {
                val omschrijving = restNummeraanduiding.omschrijving

                then("it contains the huisnummer and the postcode") {
                    omschrijving shouldBe "12A 1234AB"
                }
            }
        }

        given("A nummeraanduiding without a postcode") {
            val restNummeraanduiding = RestNummeraanduiding().apply { huisnummerWeergave = "12A" }

            `when`("the description is requested") {
                val omschrijving = restNummeraanduiding.omschrijving

                then("it contains only the huisnummer, without 'null'") {
                    omschrijving shouldBe "12A"
                }
            }
        }
    }
})
