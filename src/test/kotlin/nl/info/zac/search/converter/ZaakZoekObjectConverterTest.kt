/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createResultaat
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.model.createResultsOfZaakObjecten
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.net.URI
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class ZaakZoekObjectConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    val identityService = mockk<IdentityService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakZoekenObjectConverter = ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        zgwApiService,
        identityService,
        flowableTaskService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given(
        """
        A zaak with betrokkenen, without open tasks, zaak objecten, an archief nominatie, 
        and communication channels
        """
    ) {
        val zaakType = createZaakType()
        val resultaatType = createResultaatType()
        val resultaat = createResultaat(
            resultaatTypeURI = resultaatType.url
        )
        val zaak = createZaak(
            resultaat = resultaat.url,
            zaaktypeUri = zaakType.url,
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_adviseur"),
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_belanghebbende"),
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )
        every { zrcClientService.readResultaat(zaak.resultaat) } returns resultaat
        every { zgwApiService.findInitiatorRoleForZaak(zaak, rollenZaak) } returns rolInitiator
        every { zgwApiService.findGroepForZaak(zaak, rollenZaak) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak, rollenZaak) } returns rolMedewerkerBehandelaar
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie!!.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { ztcClientService.readResultaattype(resultaat.resultaattype) } returns resultaatType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
            createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
        )

        `when`("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            then("the roles for the zaak are retrieved from the ZRC API exactly once") {
                verify(exactly = 1) { zrcClientService.listRollen(zaak) }
            }

            then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    getObjectId() shouldBe zaak.uuid.toString()
                    getType() shouldBe ZoekObjectType.ZAAK
                    isZaakspecifiekGeautoriseerd shouldBe true
                    archiefNominatie shouldBe "VERNIETIGEN"
                    archiefActiedatum shouldBe null
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.name
                    isAfgehandeld shouldBe !zaak.isOpen()
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented yet
                    locatie shouldBe null
                    with(betrokkenen!!) {
                        size shouldBe rollenZaak.size
                        this shouldContain Pair(
                            "zaak_betrokkene_${rolAdviseur.omschrijving}",
                            listOf("P-${rolAdviseur.identificatienummer!!}")
                        )
                        this shouldContain Pair(
                            "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                            listOf("P-${rolBelanghebbende.identificatienummer!!}")
                        )
                    }
                    getZaakIndicaties() shouldNotContain ZaakIndicatie.HEROPEND
                    resultaattypeOmschrijving shouldBe resultaatType.omschrijving
                }
            }
        }
    }

    given("a reopened zaak with status and besluiten") {
        val zaakType = createZaakType(
            besluittypen = listOf(URI("fakeBesluit1"), URI("fakeBesluit2"))
        )
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            status = URI("status")
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_adviseur"),
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "fake_role_belanghebbende"),
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = "fakeBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()
        val zaakStatus = createZaakStatus()
        val zaakStatusType = createStatusType().apply {
            omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND
        }
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zgwApiService.findInitiatorRoleForZaak(zaak, rollenZaak) } returns rolInitiator
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zgwApiService.findGroepForZaak(zaak, rollenZaak) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak, rollenZaak) } returns rolMedewerkerBehandelaar
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie!!.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns zaakStatusType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            then("the roles for the zaak are retrieved from the ZRC API exactly once") {
                verify(exactly = 1) { zrcClientService.listRollen(zaak) }
            }

            then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    getObjectId() shouldBe zaak.uuid.toString()
                    getType() shouldBe ZoekObjectType.ZAAK
                    isZaakspecifiekGeautoriseerd shouldBe false
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.name
                    isAfgehandeld shouldBe !zaak.isOpen()
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented (yet?)
                    locatie shouldBe null

                    with(betrokkenen!!) {
                        size shouldBe rollenZaak.size
                        this shouldContain Pair(
                            "zaak_betrokkene_${rolAdviseur.omschrijving}",
                            listOf("P-${rolAdviseur.identificatienummer!!}")
                        )
                        this shouldContain Pair(
                            "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                            listOf("P-${rolBelanghebbende.identificatienummer!!}")
                        )
                    }
                    getZaakIndicaties() shouldContain ZaakIndicatie.HEROPEND
                }
            }
        }
    }

    given("a zaak with no roles at all") {
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val zaakObjectenList = emptyList<Zaakobject>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zrcClientService.listRollen(zaak) } returns emptyList()
        every { zgwApiService.findInitiatorRoleForZaak(zaak, emptyList()) } returns null
        every { zgwApiService.findGroepForZaak(zaak, emptyList()) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak, emptyList()) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            then("the roles for the zaak are retrieved from the ZRC API exactly once") {
                verify(exactly = 1) { zrcClientService.listRollen(zaak) }
            }

            then("the zaak zoek object has no initiator, group or behandelaar set") {
                with(zaakZoekObject) {
                    initiatorIdentificatie shouldBe null
                    groepID shouldBe null
                    behandelaarGebruikersnaam shouldBe null
                    isToegekend shouldBe false
                    betrokkenen shouldBe null
                }
            }
        }
    }

    given("an already-retrieved zaak, converted via the zaak-driven combined reindex entry point") {
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val zaakObjectenList = emptyList<Zaakobject>()

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        every { zgwApiService.findInitiatorRoleForZaak(zaak, emptyList()) } returns null
        every { zgwApiService.findGroepForZaak(zaak, emptyList()) } returns null
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak, emptyList()) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )

        `when`("the zaak is converted via the overload that takes the zaak directly") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak) { true }

            then("the zaak zoek object is still correctly populated") {
                zaakZoekObject.getObjectId() shouldBe zaak.uuid.toString()
                zaakZoekObject.isZaakspecifiekGeautoriseerd shouldBe true
            }

            then("the zaak is never read again from the ZRC API, since it was already supplied") {
                verify(exactly = 0) { zrcClientService.readZaak(any<UUID>()) }
            }
        }
    }
})
