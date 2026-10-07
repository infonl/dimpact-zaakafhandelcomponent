/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.bag.model.BAGObjectType
import net.atos.zac.app.bag.model.RESTBAGObjectGegevens
import net.atos.zac.app.bag.model.RESTNummeraanduiding
import net.atos.zac.app.bag.model.RESTOpenbareRuimte
import net.atos.zac.app.bag.model.RESTPand
import net.atos.zac.app.bag.model.RESTWoonplaats
import nl.info.client.bag.BagClientService
import nl.info.client.bag.model.BevraagAdressenParameters
import nl.info.client.bag.model.createAdresIOHal
import nl.info.client.bag.model.createHalLink
import nl.info.client.bag.model.createNummeraanduidingIOHal
import nl.info.client.bag.model.createOpenbareRuimteIOHal
import nl.info.client.bag.model.createPand
import nl.info.client.bag.model.createPandIOHal
import nl.info.client.bag.model.createWoonplaatsIOHal
import nl.info.client.bag.model.generated.NummeraanduidingLinks
import nl.info.client.bag.model.generated.OpenbareRuimteLinks
import nl.info.client.bag.model.generated.PandLinks
import nl.info.client.bag.model.generated.WoonplaatsLinks
import nl.info.client.zgw.model.createObjectPand
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.model.createZaakobjectProductaanvraag
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdresRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectRequest
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import java.net.URI
import java.util.UUID

class BagRestServiceTest : BehaviorSpec({
    val bagClientService = mockk<BagClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val bagRestService = BagRestService(
        bagClientService,
        zrcClientService,
        policyService,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("Two addresses") {
        val listAdressenParameters = createRESTListAdressenParameters(
            bagObjectType = BAGObjectType.ADRES,
            trefwoorden = "fakeText1, fakeText2",
            postcode = "fakePostcode",
            huisnummer = 123
        )
        val addresses = listOf(
            createAdresIOHal(
                huisnummer = 1,
                postcode = "fakePostcode1",
                woonplaatsNaam = "fakeWoonplaatsNaam2"
            ),
            createAdresIOHal(
                huisnummer = 2,
                postcode = "fakePostcode1",
                woonplaatsNaam = "fakeWoonplaatsNaam2"
            )
        )
        val bevraagAdressenParametersSlot = slot<BevraagAdressenParameters>()
        every { bagClientService.listAdressen(capture(bevraagAdressenParametersSlot)) } returns addresses

        `when`("listAdressen is called") {
            val result = bagRestService.listAdressen(listAdressenParameters)

            then(
                "it should invoke the BAG client service with the correct arguments and return the expected addresses"
            ) {
                with(result) {
                    totaal shouldBe 2
                    resultaten.forEachIndexed { index, restBagAdres ->
                        restBagAdres.huisnummer shouldBe addresses[index].huisnummer
                        restBagAdres.postcode shouldBe addresses[index].postcode
                        restBagAdres.woonplaatsNaam shouldBe addresses[index].woonplaatsNaam
                    }
                }
                // Currently only the provided 'trefwoorden' string is used
                // in the BAG client service call. The other parameters are not used at all.
                // The method under test probably needs refactoring..
                with(bevraagAdressenParametersSlot.captured) {
                    expand shouldBe "nummeraanduiding,openbareRuimte,panden,woonplaats"
                    q shouldBe "fakeText1, fakeText2"
                    postcode shouldBe null
                    huisnummer shouldBe null
                }
            }
        }
    }

    given("A BAG object of type address ") {
        val bagObjectId = "fakeBagObjectId"
        val bagAddress = createAdresIOHal()
        every { bagClientService.readAdres(bagObjectId) } returns bagAddress

        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.ADRES, bagObjectId)

            then(
                "the expected BAG object should be returned"
            ) {
                verify(exactly = 1) {
                    bagClientService.readAdres(bagObjectId)
                }
                with(restBagObject) {
                    url.toString() shouldBe bagAddress.links.self.href
                    identificatie shouldBe bagAddress.nummeraanduidingIdentificatie
                }
            }
        }
    }

    given("A BAG object of type woonplaats") {
        val bagObjectId = "fakeWoonplaatsId"
        val woonplaatsIOHal = createWoonplaatsIOHal().apply {
            links = WoonplaatsLinks().apply { self = createHalLink(href = "https://example.com/woonplaats/1") }
        }
        every { bagClientService.readWoonplaats(bagObjectId) } returns woonplaatsIOHal

        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.WOONPLAATS, bagObjectId)

            then("the woonplaats is returned") {
                restBagObject.shouldBeInstanceOf<RESTWoonplaats>()
                restBagObject.naam shouldBe woonplaatsIOHal.woonplaats.naam
                restBagObject.url shouldBe URI("https://example.com/woonplaats/1")
            }
        }
    }

    given("A BAG object of type pand") {
        val bagObjectId = "fakePandId"
        val pandIOHal = createPandIOHal(
            pand = createPand().apply { geometrie = createSurface() }
        ).apply {
            links = PandLinks().apply { self = createHalLink(href = "https://example.com/pand/1") }
        }
        every { bagClientService.readPand(bagObjectId) } returns pandIOHal

        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.PAND, bagObjectId)

            then("the pand is returned") {
                restBagObject.shouldBeInstanceOf<RESTPand>()
                restBagObject.oorspronkelijkBouwjaar shouldBe pandIOHal.pand.oorspronkelijkBouwjaar
                restBagObject.url shouldBe URI("https://example.com/pand/1")
            }
        }
    }

    given("A BAG object of type openbare ruimte") {
        val bagObjectId = "fakeOpenbareRuimteId"
        val openbareRuimteIOHal = createOpenbareRuimteIOHal().apply {
            links = OpenbareRuimteLinks().apply { self = createHalLink(href = "https://example.com/openbareruimte/1") }
        }
        every { bagClientService.readOpenbareRuimte(bagObjectId) } returns openbareRuimteIOHal

        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.OPENBARE_RUIMTE, bagObjectId)

            then("the openbare ruimte is returned") {
                restBagObject.shouldBeInstanceOf<RESTOpenbareRuimte>()
                restBagObject.naam shouldBe openbareRuimteIOHal.openbareRuimte.naam
                restBagObject.url shouldBe URI("https://example.com/openbareruimte/1")
            }
        }
    }

    given("A BAG object of type nummeraanduiding") {
        val bagObjectId = "fakeNummeraanduidingId"
        val nummeraanduidingIOHal = createNummeraanduidingIOHal().apply {
            links = NummeraanduidingLinks().apply {
                self = createHalLink(href = "https://example.com/nummeraanduiding/1")
            }
        }
        every { bagClientService.readNummeraanduiding(bagObjectId) } returns nummeraanduidingIOHal

        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.NUMMERAANDUIDING, bagObjectId)

            then("the nummeraanduiding is returned") {
                restBagObject.shouldBeInstanceOf<RESTNummeraanduiding>()
                restBagObject.identificatie shouldBe nummeraanduidingIOHal.nummeraanduiding.identificatie
                restBagObject.url shouldBe URI("https://example.com/nummeraanduiding/1")
            }
        }
    }

    given("A BAG object of type adresseerbaar object") {
        `when`("the BAG object is read") {
            val restBagObject = bagRestService.read(BAGObjectType.ADRESSEERBAAR_OBJECT, "fakeId")

            then("nothing is returned because an adresseerbaar object is not a standalone entity") {
                restBagObject.shouldBeNull()
                verify(exactly = 0) {
                    bagClientService.readAdres(any())
                }
            }
        }
    }

    context("Adding a BAG object to a zaak") {
        given("A BAG address that is not yet linked to the zaak and a user that may add BAG objects") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val restBagAdres = createRESTBAGAdres().apply {
                url = URI("https://example.com/adres/1")
                identificatie = "fakeIdentificatie"
            }
            val restBagObjectGegevens = RESTBAGObjectGegevens().apply {
                zaakUuid = zaak.uuid
                zaakobject = restBagAdres
            }
            val zaakobjectListParametersSlot = slot<ZaakobjectListParameters>()
            val zaakobjectRequestSlot = slot<ZaakobjectRequest>()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every {
                zrcClientService.listZaakobjecten(capture(zaakobjectListParametersSlot))
            } returns Results(0, emptyList())
            every { zrcClientService.createZaakobject(capture(zaakobjectRequestSlot)) } returns mockk()

            `when`("the BAG object is added") {
                bagRestService.create(restBagObjectGegevens)

                then("the existing links are searched for the BAG object of the zaak") {
                    with(zaakobjectListParametersSlot.captured) {
                        this.zaak shouldBe zaak.url
                        `object` shouldBe restBagAdres.url
                        objectType shouldBe ObjectTypeEnum.ADRES
                    }
                }

                and("a zaakobject is created for the BAG address") {
                    with(zaakobjectRequestSlot.captured.shouldBeInstanceOf<ZaakobjectAdresRequest>()) {
                        this.zaak shouldBe zaak.url
                        `object` shouldBe restBagAdres.url
                        objectIdentificatie!!.identificatie shouldBe "fakeIdentificatie"
                    }
                }
            }
        }

        given("A BAG object that is already linked to the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val restBagObjectGegevens = RESTBAGObjectGegevens().apply {
                zaakUuid = zaak.uuid
                zaakobject = RESTWoonplaats().apply {
                    url = URI("https://example.com/woonplaats/1")
                }
            }
            val zaakobjectListParametersSlot = slot<ZaakobjectListParameters>()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every {
                zrcClientService.listZaakobjecten(capture(zaakobjectListParametersSlot))
            } returns Results(1, listOf(createZaakobjectPand()))

            `when`("the BAG object is added") {
                bagRestService.create(restBagObjectGegevens)

                then("no new zaakobject is created") {
                    zaakobjectListParametersSlot.captured.objectType shouldBe ObjectTypeEnum.WOONPLAATS
                    verify(exactly = 0) {
                        zrcClientService.createZaakobject(any())
                    }
                }
            }
        }

        given("A nummeraanduiding, a pand and an openbare ruimte that are not yet linked to the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val zaakobjectListParametersList = mutableListOf<ZaakobjectListParameters>()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every {
                zrcClientService.listZaakobjecten(capture(zaakobjectListParametersList))
            } returns Results(0, emptyList())
            every { zrcClientService.createZaakobject(any()) } returns mockk()

            `when`("the BAG objects are added") {
                listOf(RESTNummeraanduiding(), RESTPand(), RESTOpenbareRuimte()).forEach {
                    bagRestService.create(
                        RESTBAGObjectGegevens().apply {
                            zaakUuid = zaak.uuid
                            zaakobject = it
                        }
                    )
                }

                then("the existing links are searched for with the zaakobject type matching the BAG object type") {
                    zaakobjectListParametersList.map { it.objectType } shouldBe listOf(
                        ObjectTypeEnum.OVERIGE,
                        ObjectTypeEnum.PAND,
                        ObjectTypeEnum.OPENBARE_RUIMTE
                    )
                    verify(exactly = 3) {
                        zrcClientService.createZaakobject(any())
                    }
                }
            }
        }

        given("A user that may not add BAG objects to the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val restBagObjectGegevens = RESTBAGObjectGegevens().apply {
                zaakUuid = zaak.uuid
                zaakobject = createRESTBAGAdres()
            }
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                policyService.readZaakRechten(zaak, loggedInUser)
            } returns createZaakRechten(toevoegenBagObject = false)

            `when`("the BAG object is added") {
                shouldThrow<PolicyException> {
                    bagRestService.create(restBagObjectGegevens)
                }

                then("no zaakobject is created") {
                    verify(exactly = 0) {
                        zrcClientService.createZaakobject(any())
                    }
                }
            }
        }
    }

    context("Removing a BAG object from a zaak") {
        given("A BAG object linked to a zaak and a user that may handle the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val zaakobject = createZaakobjectPand()
            val restBagObjectGegevens = RESTBAGObjectGegevens().apply {
                uuid = zaakobject.uuid
                zaakUuid = zaak.uuid
                redenWijzigen = "fakeRedenWijzigen"
            }
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every { zrcClientService.readZaakobject(zaakobject.uuid) } returns zaakobject
            every { zrcClientService.deleteZaakobject(zaakobject, "fakeRedenWijzigen") } returns Unit

            `when`("the BAG object is removed") {
                bagRestService.delete(restBagObjectGegevens)

                then("the zaakobject is deleted with the given reason") {
                    verify(exactly = 1) {
                        zrcClientService.deleteZaakobject(zaakobject, "fakeRedenWijzigen")
                    }
                }
            }
        }

        given("A user that may not handle the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val restBagObjectGegevens = RESTBAGObjectGegevens().apply {
                uuid = UUID.randomUUID()
                zaakUuid = zaak.uuid
            }
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                policyService.readZaakRechten(zaak, loggedInUser)
            } returns createZaakRechten(behandelen = false)

            `when`("the BAG object is removed") {
                shouldThrow<PolicyException> {
                    bagRestService.delete(restBagObjectGegevens)
                }

                then("no zaakobject is deleted") {
                    verify(exactly = 0) {
                        zrcClientService.deleteZaakobject(any(), any())
                    }
                }
            }
        }
    }

    context("Listing the BAG objects of a zaak") {
        given("A zaak with a pand, an openbare ruimte and a productaanvraag as zaakobjecten") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            val zaakobjectPand = createZaakobjectPand(
                zaakURI = zaak.url,
                objectPand = createObjectPand(identificatie = "fakePandIdentificatie")
            )
            val zaakobjectOpenbareRuimte = createZaakobjectOpenbareRuimte(zaakURI = zaak.url)
            val zaakobjecten = listOf<Zaakobject>(
                zaakobjectPand,
                createZaakobjectProductaanvraag(zaakURI = zaak.url),
                zaakobjectOpenbareRuimte
            )
            val zaakobjectListParametersSlot = slot<ZaakobjectListParameters>()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every {
                zrcClientService.listZaakobjecten(capture(zaakobjectListParametersSlot))
            } returns Results(zaakobjecten.size, zaakobjecten)

            `when`("the BAG objects of the zaak are listed") {
                val restBagObjectGegevensList = bagRestService.listBagObjectsForZaak(zaak.uuid)

                then("only the BAG zaakobjecten of the zaak are returned") {
                    zaakobjectListParametersSlot.captured.zaak shouldBe zaak.url
                    restBagObjectGegevensList shouldHaveSize 2
                    with(restBagObjectGegevensList[0]) {
                        uuid shouldBe zaakobjectPand.uuid
                        zaakUuid shouldBe zaak.uuid
                        zaakobject.shouldBeInstanceOf<RESTPand>()
                        zaakobject.identificatie shouldBe "fakePandIdentificatie"
                    }
                    with(restBagObjectGegevensList[1]) {
                        uuid shouldBe zaakobjectOpenbareRuimte.uuid
                        zaakobject.shouldBeInstanceOf<RESTOpenbareRuimte>()
                    }
                }
            }
        }

        given("A zaak without zaakobjecten") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every { zrcClientService.listZaakobjecten(any()) } returns Results(0, emptyList())

            `when`("the BAG objects of the zaak are listed") {
                val restBagObjectGegevensList = bagRestService.listBagObjectsForZaak(zaak.uuid)

                then("an empty list is returned") {
                    restBagObjectGegevensList.shouldBeEmpty()
                }
            }
        }

        given("A user that may not read the zaak") {
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten(lezen = false)

            `when`("the BAG objects of the zaak are listed") {
                shouldThrow<PolicyException> {
                    bagRestService.listBagObjectsForZaak(zaak.uuid)
                }

                then("the zaakobjecten are not retrieved") {
                    verify(exactly = 0) {
                        zrcClientService.listZaakobjecten(any())
                    }
                }
            }
        }
    }
})
