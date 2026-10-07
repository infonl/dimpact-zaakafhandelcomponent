/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.app.bag.createPuntOfVlak
import nl.info.zac.app.bag.createSurface
import nl.info.client.bag.model.generated.PuntOfVlak
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdresRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduidingRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimteRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPandRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaatsRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import nl.info.zac.app.zaak.model.RestGeometryType
import org.apache.commons.lang3.NotImplementedException
import java.math.BigDecimal
import java.net.URI
import java.util.UUID

class RestBagObjectTest : BehaviorSpec({
    val fakeZaakUuid = UUID.randomUUID()
    val zaakUri = URI("https://example.com/zaken/$fakeZaakUuid")
    val bagObjectUri = URI("https://example.com/bag/fakeBagObject")
    val zaakobjectUri = URI("https://example.com/zaakobjecten/fakeZaakobject")

    context("Converting a REST BAG object to a zaakobject request") {
        val zaak = createZaak()

        given("REST BAG objects of every type that can be linked to a zaak") {
            val restBagObjects = listOf(
                RestBagAdres(),
                RestPand(),
                RestWoonplaats(),
                RestOpenbareRuimte(),
                RestNummeraanduiding()
            ).onEach { it.url = bagObjectUri }

            `when`("they are converted to zaakobject requests") {
                val zaakobjectRequests = restBagObjects.map { it.toZaakobjectRequest(zaak) }

                then("a zaakobject request of the matching type is created for each of them") {
                    zaakobjectRequests[0].shouldBeInstanceOf<ZaakobjectAdresRequest>()
                    zaakobjectRequests[1].shouldBeInstanceOf<ZaakobjectPandRequest>()
                    zaakobjectRequests[2].shouldBeInstanceOf<ZaakobjectWoonplaatsRequest>()
                    zaakobjectRequests[3].shouldBeInstanceOf<ZaakobjectOpenbareRuimteRequest>()
                    zaakobjectRequests[4].shouldBeInstanceOf<ZaakobjectNummeraanduidingRequest>()
                    zaakobjectRequests.forEach {
                        it.zaak shouldBe zaak.url
                        it.`object` shouldBe bagObjectUri
                    }
                }
            }
        }

        given("A REST adresseerbaar object") {
            `when`("it is converted to a zaakobject request") {
                val notImplementedException = shouldThrow<NotImplementedException> {
                    RestAdresseerbaarObject().toZaakobjectRequest(zaak)
                }

                then("an exception is thrown because an adresseerbaar object cannot be linked to a zaak") {
                    notImplementedException.message.shouldBeNull()
                }
            }
        }
    }

    context("Converting a zaakobject to a REST BAG object") {
        given("Zaakobjecten of every BAG type") {
            val zaakobjecten = listOf(
                ZaakobjectAdres(
                    zaak = zaakUri,
                    bagobjectURI = bagObjectUri,
                    adres = ObjectAdres(identificatie = "fakeAdresId", huisnummer = 1),
                    url = zaakobjectUri,
                    uuid = UUID.randomUUID()
                ),
                createZaakobjectPand(bagobjectURI = bagObjectUri),
                ZaakobjectWoonplaats(
                    zaak = zaakUri,
                    bagobjectUri = bagObjectUri,
                    woonplaats = ObjectWoonplaats("fakeWoonplaatsId", "fakeWoonplaatsNaam"),
                    url = zaakobjectUri,
                    uuid = UUID.randomUUID()
                ),
                createZaakobjectOpenbareRuimte(bagobjectURI = bagObjectUri),
                ZaakobjectNummeraanduiding(
                    zaak = zaakUri,
                    bagObjectUri = bagObjectUri,
                    nummeraanduiding = ObjectNummeraanduiding(
                        identificatie = "fakeNummeraanduidingId",
                        huisnummer = 1,
                        typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT.toString(),
                        status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
                    ),
                    url = zaakobjectUri,
                    uuid = UUID.randomUUID()
                )
            )

            `when`("they are converted to REST BAG objects") {
                val restBagObjects = zaakobjecten.map { it.toRestBagObject() }

                then("a REST BAG object of the matching type is returned for each of them") {
                    restBagObjects[0].shouldBeInstanceOf<RestBagAdres>()
                    restBagObjects[1].shouldBeInstanceOf<RestPand>()
                    restBagObjects[2].shouldBeInstanceOf<RestWoonplaats>()
                    restBagObjects[3].shouldBeInstanceOf<RestOpenbareRuimte>()
                    restBagObjects[4].shouldBeInstanceOf<RestNummeraanduiding>()
                }
            }
        }

        given("A zaakobject of a type that is not a BAG object type") {
            val zaakobject = mockk<Zaakobject> {
                every { objectType } returns ObjectTypeEnum.BUURT
            }

            `when`("it is converted to a REST BAG object") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    zaakobject.toRestBagObject()
                }

                then("an exception is thrown") {
                    illegalStateException.message shouldBe "Unexpected objectType: buurt"
                }
            }
        }
    }

    context("Converting a zaakobject to REST BAG object gegevens") {
        given("A pand zaakobject") {
            val zaakobjectPand = createZaakobjectPand(zaakURI = zaakUri, bagobjectURI = bagObjectUri)

            `when`("it is converted to REST BAG object gegevens") {
                val restBagObjectGegevens = zaakobjectPand.toRestBagObjectGegevens()

                then("the uuids of the zaakobject and the zaak and the REST BAG object are set") {
                    with(restBagObjectGegevens) {
                        uuid shouldBe zaakobjectPand.uuid
                        zaakUuid shouldBe fakeZaakUuid
                        zaakobject.shouldBeInstanceOf<RestPand>()
                        bagObject shouldBe zaakobject
                    }
                }
            }
        }
    }

    context("Formatting a huisnummer") {
        given("A huisnummer only") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = createHuisnummerWeergave(12, null, null)

                then("only the huisnummer is shown") {
                    huisnummerWeergave shouldBe "12"
                }
            }
        }

        given("A huisnummer with a blank huisletter and a blank huisnummertoevoeging") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = createHuisnummerWeergave(12, " ", "")

                then("only the huisnummer is shown") {
                    huisnummerWeergave shouldBe "12"
                }
            }
        }

        given("A huisnummer with a huisletter and a huisnummertoevoeging") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = createHuisnummerWeergave(12, "A", "2")

                then("the huisletter is appended and the huisnummertoevoeging follows after a dash") {
                    huisnummerWeergave shouldBe "12A-2"
                }
            }
        }
    }
})
