/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zoeken.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.createResultsOfZaakObjecten
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createResultaat
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createUser
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import java.net.URI
import java.time.ZoneId
import java.util.Date

class ZaakZoekObjectConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val identityService = mockk<IdentityService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakZoekenObjectConverter = ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        zgwApiService,
        identityService,
        flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a zaak with betrokkenen, without open tasks, zaak objecten and communication channels") {
        val zaakType = createZaakType()
        val resultaatType = createResultaatType()
        val resultaat = createResultaat(
            resultaatTypeURI = resultaatType.url
        )
        val zaak = createZaak(
            resultaat = resultaat.url,
            zaakTypeURI = zaakType.url
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_adviseur"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_belanghebbende"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()

        // combine multiple every calls below into one every call

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )
        every { zrcClientService.readResultaat(zaak.resultaat) } returns resultaat

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolInitiator
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerkerBehandelaar
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { ztcClientService.readResultaattype(resultaat.resultaattype) } returns resultaatType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0

        When("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            Then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    getObjectId() shouldBe zaak.uuid.toString()
                    getType() shouldBe ZoekObjectType.ZAAK
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.name
                    isAfgehandeld shouldBe !zaak.isOpen
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented yet
                    locatie shouldBe null
                    betrokkenen.size shouldBe rollenZaak.size
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolAdviseur.omschrijving}",
                        listOf(rolAdviseur.identificatienummer)
                    )
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                        listOf(rolBelanghebbende.identificatienummer)
                    )
                    getZaakIndicaties() shouldNotContain ZaakIndicatie.HEROPEND
                    resultaattypeOmschrijving shouldBe resultaatType.omschrijving
                }
            }
        }
    }

    Given("a reopened zaak with status and decisions") {
        val zaakType = createZaakType(
            besluittypen = listOf(URI("decision1"), URI("decision2"))
        )
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            status = URI("status")
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_adviseur"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_belanghebbende"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()
        val zaakStatus = createZaakStatus()
        val zaakStatusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND
        }
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolInitiator
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerkerBehandelaar
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns zaakStatusType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )

        When("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            Then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    getObjectId() shouldBe zaak.uuid.toString()
                    getType() shouldBe ZoekObjectType.ZAAK
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.name
                    isAfgehandeld shouldBe !zaak.isOpen
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented (yet?)
                    locatie shouldBe null

                    betrokkenen.size shouldBe rollenZaak.size
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolAdviseur.omschrijving}",
                        listOf(rolAdviseur.identificatienummer)
                    )
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                        listOf(rolBelanghebbende.identificatienummer)
                    )
                    getZaakIndicaties() shouldContain ZaakIndicatie.HEROPEND
                }
            }
        }
    }
})
