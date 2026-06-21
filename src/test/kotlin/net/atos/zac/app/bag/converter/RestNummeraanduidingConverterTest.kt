/* SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+ */
package net.atos.zac.app.bag.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectNummeraanduiding
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import net.atos.zac.app.bag.model.RESTNummeraanduiding
import nl.info.client.bag.model.createNummeraanduiding
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.zgw.model.createZaak
import java.net.URI

class RestNummeraanduidingConverterTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    Context("convertToREST(Nummeraanduiding)") {
        Given("a valid Nummeraanduiding with all fields populated") {
            val nummeraanduiding = createNummeraanduiding(
                identificatie = "fakeIdentificatie",
                postcode = "1234AB",
                huisnummer = 10,
                status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN,
                typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT,
                geconstateerd = Indicatie.J
            )

            When("convertToREST is called") {
                val result = RestNummeraanduidingConverter.convertToREST(nummeraanduiding)

                Then("it should map the identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                And("it should map the postcode") {
                    result.postcode shouldBe "1234AB"
                }

                And("it should map the huisnummer") {
                    result.huisnummer shouldBe 10
                }

                And("it should map the huisletter as null") {
                    result.huisletter shouldBe null
                }

                And("it should map the huisnummertoevoeging as null") {
                    result.huisnummertoevoeging shouldBe null
                }

                And("it should produce huisnummerWeergave as '10' when huisletter and toevoeging are null") {
                    result.huisnummerWeergave shouldBe "10"
                }

                And("it should map the status") {
                    result.status shouldBe StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                }

                And("it should map the typeAdresseerbaarObject") {
                    result.typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.VERBLIJFSOBJECT
                }

                And("it should map geconstateerd as true when Indicatie is J") {
                    result.geconstateerd shouldBe true
                }
            }
        }

        Given("a Nummeraanduiding with huisletter and huisnummertoevoeging set") {
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

            When("convertToREST is called") {
                val result = RestNummeraanduidingConverter.convertToREST(nummeraanduiding)

                Then("it should produce huisnummerWeergave combining huisnummer, huisletter and toevoeging") {
                    result.huisnummerWeergave shouldBe "42A-bis"
                }

                And("it should map geconstateerd as false when Indicatie is N") {
                    result.geconstateerd shouldBe false
                }
            }
        }
    }

    Context("convertToREST(ZaakobjectNummeraanduiding)") {
        Given("a null ZaakobjectNummeraanduiding") {
            val zaakobjectNummeraanduiding: ZaakobjectNummeraanduiding? = null

            When("convertToREST is called") {
                val result = RestNummeraanduidingConverter.convertToREST(zaakobjectNummeraanduiding)

                Then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        Given("a valid ZaakobjectNummeraanduiding with identificatie, postcode and huisnummer") {
            val fakeObjectUri = URI("https://example.com/bag/nummeraanduiding/fakeObjectUri")
            val fakeZaakUri = URI("https://example.com/zaken/fakeZaakUri")
            val objectNummeraanduiding = ObjectNummeraanduiding(
                "fakeIdentificatie",
                15,
                "B",
                "fakeToevoeging",
                "9876ZZ",
                TypeAdresseerbaarObject.LIGPLAATS.toString(),
                StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
            )
            val zaakobjectNummeraanduiding = ZaakobjectNummeraanduiding(
                fakeZaakUri,
                fakeObjectUri,
                objectNummeraanduiding
            )

            When("convertToREST is called") {
                val result = RestNummeraanduidingConverter.convertToREST(zaakobjectNummeraanduiding)

                Then("it should map the url from the object URI") {
                    result!!.url shouldBe fakeObjectUri
                }

                And("it should map the identificatie") {
                    result.identificatie shouldBe "fakeIdentificatie"
                }

                And("it should map the postcode") {
                    result.postcode shouldBe "9876ZZ"
                }

                And("it should map the huisnummer") {
                    result.huisnummer shouldBe 15
                }

                And("it should map the huisletter") {
                    result.huisletter shouldBe "B"
                }

                And("it should map the huisnummertoevoeging") {
                    result.huisnummertoevoeging shouldBe "fakeToevoeging"
                }

                And("it should map the status from the string value") {
                    result.status shouldBe StatusNaamgeving.NAAMGEVING_UITGEGEVEN
                }

                And("it should map the typeAdresseerbaarObject from the string value") {
                    result.typeAdresseerbaarObject shouldBe TypeAdresseerbaarObject.LIGPLAATS
                }
            }
        }
    }

    Context("convertToZaakobject(RESTNummeraanduiding, Zaak)") {
        Given("a RESTNummeraanduiding and a Zaak") {
            val fakeNummeraanduidingUrl = URI("https://example.com/bag/nummeraanduiding/fakeNummeraanduidingUrl")
            val restNummeraanduiding = RESTNummeraanduiding().apply {
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

            When("convertToZaakobject is called") {
                val result = RestNummeraanduidingConverter.convertToZaakobject(restNummeraanduiding, zaak)

                Then("it should set the zaak URL from the provided zaak") {
                    result.zaak shouldBe zaak.url
                }

                And("it should set the object URI from the nummeraanduiding url") {
                    result.`object` shouldBe fakeNummeraanduidingUrl
                }

                And("it should populate ObjectNummeraanduiding with the correct identificatie") {
                    result.objectIdentificatie.overigeData.identificatie shouldBe "fakeIdentificatie"
                }

                And("it should populate ObjectNummeraanduiding with the correct postcode") {
                    result.objectIdentificatie.overigeData.postcode shouldBe "1111AA"
                }

                And("it should populate ObjectNummeraanduiding with the correct huisnummer") {
                    result.objectIdentificatie.overigeData.huisnummer shouldBe 7
                }

                And("it should populate ObjectNummeraanduiding with the correct typeAdresseerbaarObject string") {
                    result.objectIdentificatie.overigeData.typeAdresseerbaarObject shouldBe
                        TypeAdresseerbaarObject.VERBLIJFSOBJECT.toString()
                }

                And("it should populate ObjectNummeraanduiding with the correct status string") {
                    result.objectIdentificatie.overigeData.status shouldBe
                        StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
                }
            }
        }
    }
})
