/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.app.bag.createPuntOfVlak
import net.atos.zac.app.bag.createSurface
import net.atos.zac.app.bag.model.RESTAdresseerbaarObject
import net.atos.zac.app.bag.model.RESTBAGAdres
import net.atos.zac.app.bag.model.RESTNummeraanduiding
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.bag.model.RESTWoonplaats
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

class RestBagConverterTest : BehaviorSpec({
    val fakeZaakUuid = UUID.randomUUID()
    val zaakUri = URI("https://example.com/zaken/$fakeZaakUuid")
    val bagObjectUri = URI("https://example.com/bag/fakeBagObject")
    val zaakobjectUri = URI("https://example.com/zaakobjecten/fakeZaakobject")

    context("Converting a REST BAG object to a zaakobject request") {
        val zaak = createZaak()

        given("REST BAG objects of every type that can be linked to a zaak") {
            val restBagObjects = listOf(
                RESTBAGAdres(),
                RESTPand(),
                RESTWoonplaats(),
                RESTOpenbareRuimte(),
                RESTNummeraanduiding()
            ).onEach { it.url = bagObjectUri }

            `when`("they are converted to zaakobject requests") {
                val zaakobjectRequests = restBagObjects.map { RestBagConverter.convertToZaakobject(it, zaak) }

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
                    RestBagConverter.convertToZaakobject(RESTAdresseerbaarObject(), zaak)
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
                    zaakUri,
                    bagObjectUri,
                    ObjectAdres(identificatie = "fakeAdresId", huisnummer = 1),
                    zaakobjectUri,
                    UUID.randomUUID()
                ),
                createZaakobjectPand(bagobjectURI = bagObjectUri),
                ZaakobjectWoonplaats(
                    zaakUri,
                    bagObjectUri,
                    ObjectWoonplaats("fakeWoonplaatsId", "fakeWoonplaatsNaam"),
                    zaakobjectUri,
                    UUID.randomUUID()
                ),
                createZaakobjectOpenbareRuimte(bagobjectURI = bagObjectUri),
                ZaakobjectNummeraanduiding(
                    zaakUri,
                    bagObjectUri,
                    ObjectNummeraanduiding(
                        identificatie = "fakeNummeraanduidingId",
                        huisnummer = 1,
                        typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT.toString(),
                        status = StatusNaamgeving.NAAMGEVING_UITGEGEVEN.toString()
                    ),
                    zaakobjectUri,
                    UUID.randomUUID()
                )
            )

            `when`("they are converted to REST BAG objects") {
                val restBagObjects = zaakobjecten.map { RestBagConverter.convertToRESTBAGObject(it) }

                then("a REST BAG object of the matching type is returned for each of them") {
                    restBagObjects[0].shouldBeInstanceOf<RESTBAGAdres>()
                    restBagObjects[1].shouldBeInstanceOf<RESTPand>()
                    restBagObjects[2].shouldBeInstanceOf<RESTWoonplaats>()
                    restBagObjects[3].shouldBeInstanceOf<RESTOpenbareRuimte>()
                    restBagObjects[4].shouldBeInstanceOf<RESTNummeraanduiding>()
                }
            }
        }

        given("A zaakobject of a type that is not a BAG object type") {
            val zaakobject = mockk<Zaakobject> {
                every { objectType } returns ObjectTypeEnum.BUURT
            }

            `when`("it is converted to a REST BAG object") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    RestBagConverter.convertToRESTBAGObject(zaakobject)
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
                val restBagObjectGegevens = RestBagConverter.convertToRESTBAGObjectGegevens(zaakobjectPand)

                then("the uuids of the zaakobject and the zaak and the REST BAG object are set") {
                    with(restBagObjectGegevens) {
                        uuid shouldBe zaakobjectPand.uuid
                        zaakUuid shouldBe fakeZaakUuid
                        zaakobject.shouldBeInstanceOf<RESTPand>()
                        bagObject shouldBe zaakobject
                    }
                }
            }
        }
    }

    context("Formatting a huisnummer") {
        given("A huisnummer only") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = RestBagConverter.getHuisnummerWeergave(12, null, null)

                then("only the huisnummer is shown") {
                    huisnummerWeergave shouldBe "12"
                }
            }
        }

        given("A huisnummer with a blank huisletter and a blank huisnummertoevoeging") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = RestBagConverter.getHuisnummerWeergave(12, " ", "")

                then("only the huisnummer is shown") {
                    huisnummerWeergave shouldBe "12"
                }
            }
        }

        given("A huisnummer with a huisletter and a huisnummertoevoeging") {
            `when`("the huisnummer is formatted") {
                val huisnummerWeergave = RestBagConverter.getHuisnummerWeergave(12, "A", "2")

                then("the huisletter is appended and the huisnummertoevoeging follows after a dash") {
                    huisnummerWeergave shouldBe "12A-2"
                }
            }
        }
    }

    context("Converting a BAG punt or vlak to a REST geometry") {
        given("A punt") {
            val puntOfVlak = createPuntOfVlak().apply {
                punt.coordinates = listOf(BigDecimal("5.1"), BigDecimal("52.2"))
            }

            `when`("it is converted") {
                val restGeometry = RestBagConverter.convertPuntOrVlak(puntOfVlak)

                then("a point geometry with longitude and latitude is returned") {
                    with(restGeometry) {
                        type shouldBe RestGeometryType.POINT
                        point!!.longitude shouldBe 5.1
                        point!!.latitude shouldBe 52.2
                        polygon shouldBe null
                    }
                }
            }
        }

        given("A vlak") {
            val puntOfVlak = PuntOfVlak().apply { vlak = createSurface() }

            `when`("it is converted") {
                val restGeometry = RestBagConverter.convertPuntOrVlak(puntOfVlak)

                then("a polygon geometry is returned") {
                    with(restGeometry) {
                        type shouldBe RestGeometryType.POLYGON
                        point shouldBe null
                        polygon!!.size shouldBe 1
                    }
                }
            }
        }

        given("Neither a punt nor a vlak") {
            `when`("it is converted") {
                val restGeometry = RestBagConverter.convertPuntOrVlak(PuntOfVlak())

                then("no geometry is returned") {
                    restGeometry.shouldBeNull()
                }
            }
        }
    }
})
