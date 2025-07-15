/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.documentcreation.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.shared.model.Results
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.createAdres
import nl.info.client.brp.model.createAdressering
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.generated.Adres
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolVestiging
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.productaanvraag.ProductaanvraagService
import java.util.Optional

class DocumentCreationDataConverterTest : BehaviorSpec({
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val brpClientService = mockk<BrpClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val objectsClientService = mockk<ObjectsClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val configuratieService = mockk<ConfiguratieService>()
    val documentCreationDataConverter = DocumentCreationDataConverter(
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        brpClientService = brpClientService,
        kvkClientService = kvkClientService,
        objectsClientService = objectsClientService,
        flowableTaskService = flowableTaskService,
        identityService = identityService,
        productaanvraagService = productaanvraagService,
        configuratieService = configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A logged-in user and a zaak with a behandelaar and an initiator of type natuurlijk persoon") {
        val loggedInUser = createLoggedInUser()
        val rolNatuurlijkPersoon =
            createRolNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val persoon = createPersoon(
            address = createAdressering(),
            verblijfplaats = createAdres()
        )
        val zaakType = createZaakType()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNatuurlijkPersoon
        every {
            brpClientService.retrievePersoon(rolNatuurlijkPersoon.identificatienummer, any())
        } returns persoon
        every { zrcClientService.listZaakobjecten(any()) } returns Results(emptyList(), 0)
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerker
        every { zgwApiService.findGroepForZaak(zaak) } returns rolOrganisatorischeEenheid
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

        When("SmartDocuments data is created") {
            val data = documentCreationDataConverter.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            Then("the data is created correctly") {
                with(data) {
                    with(aanvragerData!!) {
                        naam shouldBe persoon.naam
                        straat shouldBe (persoon.verblijfplaats as Adres).verblijfadres.officieleStraatnaam
                        huisnummer shouldBe (persoon.verblijfplaats as Adres).verblijfadres.huisnummer.toString()
                        postcode shouldBe (persoon.verblijfplaats as Adres).verblijfadres.postcode
                        woonplaats shouldBe (persoon.verblijfplaats as Adres).verblijfadres.woonplaats
                    }
                    with(gebruikerData) {
                        id shouldBe loggedInUser.id
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe "${rolMedewerker.betrokkeneIdentificatie!!.voorletters} " +
                            "${rolMedewerker.betrokkeneIdentificatie!!.achternaam}"
                        groep shouldBe rolOrganisatorischeEenheid.naam
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }

    Given("A logged-in user and a zaak without a behandelaar and an initiator of type vestiging") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val resultaatItem = createResultaatItem()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every {
            kvkClientService.findVestiging(rolVestiging.identificatienummer)
        } returns Optional.of(resultaatItem)
        every { zrcClientService.listZaakobjecten(any()) } returns Results(emptyList(), 0)
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

        When("SmartDocuments data is created") {
            val data = documentCreationDataConverter.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            Then("the data is created correctly") {
                with(data) {
                    with(aanvragerData!!) {
                        naam shouldBe resultaatItem.naam
                        straat shouldBe resultaatItem.adres.binnenlandsAdres.straatnaam
                        huisnummer shouldBe
                            "${resultaatItem.adres.binnenlandsAdres.huisnummer}${resultaatItem.adres.binnenlandsAdres.huisletter}"
                        postcode shouldBe resultaatItem.adres.binnenlandsAdres.postcode
                        woonplaats shouldBe resultaatItem.adres.binnenlandsAdres.plaats
                    }
                    with(gebruikerData) {
                        id shouldBe loggedInUser.id
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe null
                        groep shouldBe null
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }

    Given(
        """
        A logged-in user and a zaak without a behandelaar and an initiator of type niet-natuurlijk persoon
        with a vestigingsnummer
        """
    ) {
        val loggedInUser = createLoggedInUser()
        val vestigingsNummer = "fakeVestigingsNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    vestigingsnummer = vestigingsNummer
                )
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val resultaatItem = createResultaatItem()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every {
            kvkClientService.findVestiging(vestigingsNummer)
        } returns Optional.of(resultaatItem)
        every { zrcClientService.listZaakobjecten(any()) } returns Results(emptyList(), 0)
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

        When("SmartDocuments data is created") {
            val data = documentCreationDataConverter.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            Then("the data is created correctly") {
                with(data) {
                    with(aanvragerData!!) {
                        naam shouldBe resultaatItem.naam
                        straat shouldBe resultaatItem.adres.binnenlandsAdres.straatnaam
                        huisnummer shouldBe
                            "${resultaatItem.adres.binnenlandsAdres.huisnummer}${resultaatItem.adres.binnenlandsAdres.huisletter}"
                        postcode shouldBe resultaatItem.adres.binnenlandsAdres.postcode
                        woonplaats shouldBe resultaatItem.adres.binnenlandsAdres.plaats
                    }
                    with(gebruikerData) {
                        id shouldBe loggedInUser.id
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe null
                        groep shouldBe null
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }
})
