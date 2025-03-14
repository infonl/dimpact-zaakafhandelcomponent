/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.model.generated.Adres
import net.atos.client.kvk.KvkClientService
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.createAdres
import nl.info.client.brp.model.createAdressering
import nl.info.client.brp.model.createPersoon
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.productaanvraag.ProductaanvraagService

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
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
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
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        configuratieService = configuratieService
    )

    Given("A logged in user and a zaak with a behandelaar") {
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
        every { brpClientService.retrievePersoon(rolNatuurlijkPersoon.identificatienummer) } returns persoon
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
})
